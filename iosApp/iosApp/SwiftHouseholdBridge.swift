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
    private var households: CollectionReference { Firestore.firestore().collection(HouseholdFields.shared.COLLECTION) }

    private func items(_ householdId: String) -> CollectionReference {
        households.document(householdId).collection(HouseholdFields.shared.ITEMS)
    }

    func observeHouseholds(
        userId: String,
        onChange: @escaping ([HouseholdDto]) -> Void,
        onError: @escaping (String) -> Void
    ) -> IosListenerHandle {
        let registration = households
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
            fields.PENDING_MEMBERS: [String](),
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
        items(householdId).document().setData([
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
            pendingMembers: data[fields.PENDING_MEMBERS] as? [String] ?? []
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
