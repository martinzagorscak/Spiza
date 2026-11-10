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

    private func ownJoinKeys(_ userId: String) -> Query {
        joinKeys.whereField(HouseholdFields.shared.USER_ID, isEqualTo: userId)
    }

    // Join keys are document ids (joinKeys/{joinKey} -> { userId }). A user has at most one key:
    // creating a new one replaces the previous ones in the same batch.
    func createHouseholdJoinKey(userId: String, onComplete: @escaping (String?, BridgeError?) -> Void) {
        let generatedKey = UUID().uuidString.lowercased()
        ownJoinKeys(userId).getDocuments { snapshot, error in
            if let error = error {
                onComplete(nil, error.toBridgeError())
                return
            }

            let batch = Firestore.firestore().batch()
            snapshot?.documents.forEach { batch.deleteDocument($0.reference) }
            batch.setData([HouseholdFields.shared.USER_ID: userId], forDocument: self.joinKeys.document(generatedKey))
            batch.commit { error in
                if let error = error {
                    onComplete(nil, error.toBridgeError())
                } else {
                    onComplete(generatedKey, nil)
                }
            }
        }
    }

    func deleteHouseholdJoinKey(userId: String, onComplete: @escaping (BridgeError?) -> Void) {
        ownJoinKeys(userId).getDocuments { snapshot, error in
            if let error = error {
                onComplete(error.toBridgeError())
                return
            }

            let batch = Firestore.firestore().batch()
            snapshot?.documents.forEach { batch.deleteDocument($0.reference) }
            batch.commit(completion: self.completion(onComplete))
        }
    }

    func getHouseholdJoinKey(userId: String, onComplete: @escaping (String?, BridgeError?) -> Void) {
        ownJoinKeys(userId).limit(to: 1).getDocuments { snapshot, error in
            if let error = error {
                onComplete(nil, error.toBridgeError())
            } else {
                onComplete(snapshot?.documents.first?.documentID, nil)
            }
        }
    }

    func getJoinKeyUserId(joinKey: String, onComplete: @escaping (String?, BridgeError?) -> Void) {
        joinKeys.document(joinKey).getDocument { snapshot, error in
            if let error = error {
                onComplete(nil, error.toBridgeError())
            } else {
                onComplete(snapshot?.data()?[HouseholdFields.shared.USER_ID] as? String, nil)
            }
        }
    }

    // Adding the member and consuming their join key is one atomic write, so it never half-succeeds.
    func addMemberToHousehold(
        householdId: String,
        userId: String,
        joinKey: String,
        onComplete: @escaping (BridgeError?) -> Void
    ) {
        let batch = Firestore.firestore().batch()
        batch.updateData(
            [HouseholdFields.shared.JOINED_MEMBERS: FieldValue.arrayUnion([userId])],
            forDocument: households.document(householdId)
        )
        batch.deleteDocument(joinKeys.document(joinKey))
        batch.commit(completion: completion(onComplete))
    }

    func removeMemberFromHousehold(householdId: String, memberId: String, onComplete: @escaping (BridgeError?) -> Void) {
        let fields = HouseholdFields.shared
        households.document(householdId).updateData(
            [
                fields.JOINED_MEMBERS: FieldValue.arrayRemove([memberId])
            ], completion: completion(onComplete))
    }

    // Firestore does not delete subcollections, so the items go in the same batch as the household.
    func deleteHousehold(householdId: String, onComplete: @escaping (BridgeError?) -> Void) {
        items(householdId).getDocuments { snapshot, error in
            if let error = error {
                onComplete(error.toBridgeError())
                return
            }

            let batch = Firestore.firestore().batch()
            snapshot?.documents.forEach { batch.deleteDocument($0.reference) }
            batch.deleteDocument(self.households.document(householdId))
            batch.commit(completion: self.completion(onComplete))
        }
    }

    func observeHouseholds(
        userId: String,
        onChange: @escaping ([HouseholdDto]) -> Void,
        onError: @escaping (BridgeError) -> Void
    ) -> IosListenerHandle {
        let registration =
            households
            .whereField(HouseholdFields.shared.JOINED_MEMBERS, arrayContains: userId)
            .addSnapshotListener { snapshot, error in
                if let error = error {
                    onError(error.toBridgeError())
                } else if let snapshot = snapshot {
                    onChange(snapshot.documents.compactMap { $0.toHouseholdDto() })
                }
            }
        return FirestoreListenerHandle(registration)
    }

    func observeItems(
        householdId: String,
        onChange: @escaping ([ArticleItemDto]) -> Void,
        onError: @escaping (BridgeError) -> Void
    ) -> IosListenerHandle {
        let registration = items(householdId).addSnapshotListener { snapshot, error in
            if let error = error {
                onError(error.toBridgeError())
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
        onComplete: @escaping (String?, BridgeError?) -> Void
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
                onComplete(nil, error.toBridgeError())
            } else {
                onComplete(document.documentID, nil)
            }
        }
    }

    func updateHousehold(
        householdId: String,
        name: String,
        description: String,
        onComplete: @escaping (BridgeError?) -> Void
    ) {
        let fields = HouseholdFields.shared
        households.document(householdId).updateData([
            fields.NAME: name,
            fields.DESCRIPTION: description,
        ], completion: completion(onComplete))
    }

    func addItem(householdId: String, text: String, position: Double, onComplete: @escaping (BridgeError?) -> Void) {
        let fields = HouseholdFields.shared
        items(householdId).document().setData(
            [
                fields.TEXT: text,
                fields.IS_CHECKED: false,
                fields.POSITION: position,
            ], completion: completion(onComplete))
    }

    func updateItemText(householdId: String, itemId: String, text: String, onComplete: @escaping (BridgeError?) -> Void) {
        items(householdId).document(itemId)
            .updateData([HouseholdFields.shared.TEXT: text], completion: completion(onComplete))
    }

    func setItemChecked(householdId: String, itemId: String, isChecked: Bool, onComplete: @escaping (BridgeError?) -> Void) {
        items(householdId).document(itemId)
            .updateData([HouseholdFields.shared.IS_CHECKED: isChecked], completion: completion(onComplete))
    }

    func moveItem(householdId: String, itemId: String, position: Double, onComplete: @escaping (BridgeError?) -> Void) {
        items(householdId).document(itemId)
            .updateData([HouseholdFields.shared.POSITION: position], completion: completion(onComplete))
    }

    func deleteItem(householdId: String, itemId: String, onComplete: @escaping (BridgeError?) -> Void) {
        items(householdId).document(itemId).delete(completion: completion(onComplete))
    }

    func clearCheckedItems(householdId: String, onComplete: @escaping (BridgeError?) -> Void) {
        items(householdId)
            .whereField(HouseholdFields.shared.IS_CHECKED, isEqualTo: true)
            .getDocuments { snapshot, error in
                if let error = error {
                    onComplete(error.toBridgeError())
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


    private func completion(_ onComplete: @escaping (BridgeError?) -> Void) -> (Error?) -> Void {
        { error in onComplete(error?.toBridgeError()) }
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

extension Error {
    /// Maps a Firebase error to a BridgeError whose reason is a Kotlin DataException.Reason name.
    /// Firestore uses gRPC status codes: 4 deadlineExceeded, 5 notFound, 7 permissionDenied, 8 resourceExhausted (quota), 14 unavailable,
    /// 16 unauthenticated. Firebase Auth reports a missing connection or timeout as 17020 networkError.
    func toBridgeError() -> BridgeError {
        let nsError = self as NSError
        let reason: String
        switch (nsError.domain, nsError.code) {
        case ("FIRFirestoreErrorDomain", 4), ("FIRFirestoreErrorDomain", 14), ("FIRAuthErrorDomain", 17020), (NSURLErrorDomain, _):
            reason = "Offline"
        case ("FIRFirestoreErrorDomain", 7), ("FIRFirestoreErrorDomain", 16):
            reason = "PermissionDenied"
        case ("FIRFirestoreErrorDomain", 5):
            reason = "NotFound"
        case ("FIRFirestoreErrorDomain", 8):
            reason = "QuotaExceeded"
        default:
            reason = "Unknown"
        }
        return BridgeError(reason: reason, message: localizedDescription)
    }
}
