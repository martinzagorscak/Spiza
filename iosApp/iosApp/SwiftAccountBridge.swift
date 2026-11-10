import FirebaseFirestore
import Foundation
import Shared

final class SwiftAccountBridge: NSObject, IosAccountBridge {
    // Same as FirestoreAccountDataSource: server reads only, then ONE batch, so a rejected write (e.g. by the rules) leaves the
    // account untouched instead of half-deleted. At most 3 households per user, each owned one with at most 100 items, plus the
    // join key and the profile (~310 writes) stays below the 500 writes limit; above it Firestore rejects the whole batch.
    func deleteAccountData(userId: String, onComplete: @escaping (BridgeError?) -> Void) {
        let fields = HouseholdFields.shared
        let firestore = Firestore.firestore()
        firestore.collection(fields.COLLECTION_JOIN_KEYS)
            .whereField(fields.USER_ID, isEqualTo: userId)
            .getDocuments(source: .server) { joinKeySnapshot, error in
                if let error = error {
                    onComplete(error.toBridgeError())
                    return
                }
                firestore.collection(fields.COLLECTION_HOUSEHOLDS)
                    .whereField(fields.JOINED_MEMBERS, arrayContains: userId)
                    .getDocuments(source: .server) { householdSnapshot, error in
                        if let error = error {
                            onComplete(error.toBridgeError())
                            return
                        }
                        var ownedHouseholds: [DocumentReference] = []
                        var joinedHouseholds: [DocumentReference] = []
                        for household in householdSnapshot?.documents ?? [] {
                            // Unlike the listeners, a malformed household is not skipped: it would stay behind with the user in it.
                            guard let ownerId = household.data()[fields.OWNER_ID] as? String else {
                                onComplete(
                                    BridgeError(reason: "Unknown", message: "Household \(household.documentID) has no readable owner"))
                                return
                            }
                            if ownerId == userId {
                                ownedHouseholds.append(household.reference)
                            } else {
                                joinedHouseholds.append(household.reference)
                            }
                        }
                        Self.itemsOf(ownedHouseholds) { items, error in
                            if let error = error {
                                onComplete(error.toBridgeError())
                                return
                            }
                            let batch = firestore.batch()
                            joinKeySnapshot?.documents.forEach { batch.deleteDocument($0.reference) }
                            items.forEach { batch.deleteDocument($0) }
                            // Also for the other members: a household can not exist without its owner.
                            ownedHouseholds.forEach { batch.deleteDocument($0) }
                            joinedHouseholds.forEach {
                                batch.updateData([fields.JOINED_MEMBERS: FieldValue.arrayRemove([userId])], forDocument: $0)
                            }
                            batch.deleteDocument(firestore.collection(UserFields.shared.COLLECTION_USERS).document(userId))
                            batch.commit { error in
                                onComplete(error?.toBridgeError())
                            }
                        }
                    }
            }
    }

    /// Firestore does not delete subcollections, so the items of owned households go in the same batch. First error wins.
    private static func itemsOf(
        _ households: [DocumentReference],
        completion: @escaping ([DocumentReference], Error?) -> Void
    ) {
        let lock = NSLock()
        let group = DispatchGroup()
        var items: [DocumentReference] = []
        var firstError: Error?

        for household in households {
            group.enter()
            household.collection(HouseholdFields.shared.ITEMS).getDocuments(source: .server) { snapshot, error in
                lock.lock()
                if let error = error {
                    if firstError == nil { firstError = error }
                } else {
                    items.append(contentsOf: snapshot?.documents.map { $0.reference } ?? [])
                }
                lock.unlock()
                group.leave()
            }
        }

        // On main, like Firestore's own completion callbacks, so the batch below is built on the same queue as without items.
        group.notify(queue: .main) {
            completion(items, firstError)
        }
    }
}
