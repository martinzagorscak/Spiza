import FirebaseFirestore
import Foundation
import Shared

private final class FirestoreListenerHandle: NSObject, IosListenerHandle {
    private let registration: ListenerRegistration

    init(_ registration: ListenerRegistration) {
        self.registration = registration
    }

    func remove() {
        registration.remove()
    }
}

final class SwiftHouseholdBridge: NSObject, IosHouseholdBridge {
    private var households: CollectionReference { Firestore.firestore().collection(HouseholdFields.shared.COLLECTION_HOUSEHOLDS) }
    private var joinKeys: CollectionReference { Firestore.firestore().collection(HouseholdFields.shared.COLLECTION_JOIN_KEYS) }

    private func items(_ householdId: String) -> CollectionReference {
        households.document(householdId).collection(HouseholdFields.shared.ITEMS)
    }

    func createHouseholdJoinKey(userId: String, onComplete: @escaping (String?, String?) -> Void) {
        let fields = HouseholdFields.shared
        let generatedKey = UUID().uuidString.lowercased()
        joinKeys.document(userId).setData([
            fields.JOIN_KEY: generatedKey
        ]) { error in
            if let error = error {
                onComplete(nil, error.localizedDescription)
            } else {
                onComplete(generatedKey, nil)
            }
        }
    }

    func getHouseholdJoinKey(userId: String, onComplete: @escaping (String?, String?) -> Void) {
        let fields = HouseholdFields.shared
        joinKeys.document(userId).getDocument { snapshot, error in
            if let error = error {
                onComplete(nil, error.localizedDescription)
            } else if let snapshot = snapshot, snapshot.exists, let data = snapshot.data(), let joinKey = data[fields.JOIN_KEY] as? String {
                onComplete(joinKey, nil)
            } else {
                onComplete(nil, nil)
            }
        }
    }

    private func deleteJoinKey(userId: String, onComplete: @escaping (String?) -> Void) {
        joinKeys.document(userId).delete(completion: completion(onComplete))
    }

    func addMemberToHousehold(
        householdId: String, userJoinKey: String,
        onComplete: @escaping (KotlinBoolean, String?) -> Void,
    ) {
        let fields = HouseholdFields.shared
        joinKeys.whereField(fields.JOIN_KEY, isEqualTo: userJoinKey).getDocuments { snapshot, error in
            if let error = error {
                onComplete(false, error.localizedDescription)
                return
            }

            guard let documents = snapshot?.documents, let firstDoc = documents.first else {
                onComplete(false, nil)
                return
            }

            let userId = firstDoc.documentID
            let householdRef = self.households.document(householdId)

            householdRef.updateData([
                fields.JOINED_MEMBERS: FieldValue.arrayUnion([userId])
            ]) { error in
                if let error = error {
                    onComplete(false, error.localizedDescription)
                } else {
                    self.deleteJoinKey(userId: userId) { deleteError in
                        onComplete(KotlinBoolean(bool: deleteError == nil), deleteError)
                    }
                }
            }
        }
    }

    func removeMemberFromHousehold(householdId: String, memberId: String, onComplete: @escaping (String?) -> Void) {
        let fields = HouseholdFields.shared
        households.document(householdId).updateData(
            [
                fields.JOINED_MEMBERS: FieldValue.arrayRemove([memberId])
            ], completion: completion(onComplete))
    }

    func deleteHousehold(householdId: String, onComplete: @escaping (String?) -> Void) {
        households.document(householdId).delete(completion: completion(onComplete))
    }

    func observeHouseholds(
        userId: String,
        onChange: @escaping ([HouseholdDto]) -> Void,
        onError: @escaping (String) -> Void
    ) -> IosListenerHandle {
        let registration =
            households
            .whereField(HouseholdFields.shared.JOINED_MEMBERS, arrayContains: userId)
            .addSnapshotListener { snapshot, error in
                if let error = error {
                    onError(error.localizedDescription)
                } else if let snapshot = snapshot {
                    onChange(snapshot.documents.compactMap { $0.toHouseholdDto() })
                }
            }
        return FirestoreListenerHandle(registration)
    }

    func observeItems(
        householdId: String,
        onChange: @escaping ([ArticleItemDto]) -> Void,
        onError: @escaping (String) -> Void
    ) -> IosListenerHandle {
        let registration = items(householdId).addSnapshotListener { snapshot, error in
            if let error = error {
                onError(error.localizedDescription)
            } else if let snapshot = snapshot {
                onChange(snapshot.documents.compactMap { $0.toArticleItemDto() })
            }
        }
        return FirestoreListenerHandle(registration)
    }

    func createHousehold(
        ownerId: String,
        name: String,
        description: String,
        onComplete: @escaping (String?, String?) -> Void
    ) {
        let fields = HouseholdFields.shared
        let document = households.document()
        document.setData([
            fields.NAME: name,
            fields.DESCRIPTION: description,
            fields.OWNER_ID: ownerId,
            fields.JOINED_MEMBERS: [ownerId],
        ]) { error in
            if let error = error {
                onComplete(nil, error.localizedDescription)
            } else {
                onComplete(document.documentID, nil)
            }
        }
    }

    func addItem(householdId: String, text: String, position: Double, onComplete: @escaping (String?) -> Void) {
        let fields = HouseholdFields.shared
        items(householdId).document().setData(
            [
                fields.TEXT: text,
                fields.IS_CHECKED: false,
                fields.POSITION: position,
            ], completion: completion(onComplete))
    }

    func updateItemText(householdId: String, itemId: String, text: String, onComplete: @escaping (String?) -> Void) {
        items(householdId).document(itemId)
            .updateData([HouseholdFields.shared.TEXT: text], completion: completion(onComplete))
    }

    func setItemChecked(householdId: String, itemId: String, isChecked: Bool, onComplete: @escaping (String?) -> Void) {
        items(householdId).document(itemId)
            .updateData([HouseholdFields.shared.IS_CHECKED: isChecked], completion: completion(onComplete))
    }

    func moveItem(householdId: String, itemId: String, position: Double, onComplete: @escaping (String?) -> Void) {
        items(householdId).document(itemId)
            .updateData([HouseholdFields.shared.POSITION: position], completion: completion(onComplete))
    }

    func deleteItem(householdId: String, itemId: String, onComplete: @escaping (String?) -> Void) {
        items(householdId).document(itemId).delete(completion: completion(onComplete))
    }

    func clearCheckedItems(householdId: String, onComplete: @escaping (String?) -> Void) {
        items(householdId)
            .whereField(HouseholdFields.shared.IS_CHECKED, isEqualTo: true)
            .getDocuments { snapshot, error in
                if let error = error {
                    onComplete(error.localizedDescription)
                    return
                }

                guard let documents = snapshot?.documents else {
                    onComplete(nil)
                    return
                }

                let batch = Firestore.firestore().batch()
                documents.forEach { batch.deleteDocument($0.reference) }
                batch.commit(completion: self.completion(onComplete))
            }
    }


    private func completion(_ onComplete: @escaping (String?) -> Void) -> (Error?) -> Void {
        { error in onComplete(error?.localizedDescription) }
    }
}

extension QueryDocumentSnapshot {
    fileprivate func toHouseholdDto() -> HouseholdDto? {
        let fields = HouseholdFields.shared
        let data = self.data()
        guard let name = data[fields.NAME] as? String else { return nil }
        return HouseholdDto(
            id: documentID,
            name: name,
            description: data[fields.DESCRIPTION] as? String ?? "",
            ownerId: data[fields.OWNER_ID] as? String ?? "",
            joinedMembers: data[fields.JOINED_MEMBERS] as? [String] ?? [],
        )
    }

    fileprivate func toArticleItemDto() -> ArticleItemDto? {
        let fields = HouseholdFields.shared
        let data = self.data()
        guard let position = data[fields.POSITION] as? Double else { return nil }
        return ArticleItemDto(
            id: documentID,
            text: data[fields.TEXT] as? String ?? "",
            isChecked: data[fields.IS_CHECKED] as? Bool ?? false,
            position: position
        )
    }
}
