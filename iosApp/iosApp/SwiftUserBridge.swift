import FirebaseFirestore
import Foundation
import Shared

final class SwiftUserBridge: NSObject, IosUserBridge {
    private var users: CollectionReference { Firestore.firestore().collection(UserFields.shared.COLLECTION_USERS) }

    func saveUserProfile(profile: UserProfileDto, onComplete: @escaping (BridgeError?) -> Void) {
        let fields = UserFields.shared
        let docRef = users.document(profile.id)
        docRef.getDocument { snapshot, error in
            if let snapshot = snapshot, !snapshot.exists {
                var data: [String: Any] = [:]
                if let name = profile.name { data[fields.NAME] = name }
                if let email = profile.email { data[fields.EMAIL] = email }
                if let photoUrl = profile.photoUrl { data[fields.PHOTO_URL] = photoUrl }

                docRef.setData(data) { error in
                    onComplete(error?.toBridgeError())
                }
            } else {
                onComplete(error?.toBridgeError())
            }
        }
    }

    func getUsers(userIds: [String], onComplete: @escaping ([UserProfileDto], BridgeError?) -> Void) {
        if userIds.isEmpty {
            onComplete([], nil)
            return
        }
        // One get per id: the rules deny listing users, so a whereField(FieldPath.documentID(), in:) query would be rejected.
        // Same as FirestoreUserDataSource.getUsers: deduplicated ids, input order kept, missing profiles skipped, first error wins.
        var uniqueIds: [String] = []
        for userId in userIds where !uniqueIds.contains(userId) {
            uniqueIds.append(userId)
        }

        let lock = NSLock()
        let group = DispatchGroup()
        var snapshots = [DocumentSnapshot?](repeating: nil, count: uniqueIds.count)
        var firstError: Error?

        for (index, userId) in uniqueIds.enumerated() {
            group.enter()
            users.document(userId).getDocument { snapshot, error in
                lock.lock()
                if let error = error {
                    if firstError == nil { firstError = error }
                } else {
                    snapshots[index] = snapshot
                }
                lock.unlock()
                group.leave()
            }
        }

        group.notify(queue: .main) {
            if let error = firstError {
                onComplete([], error.toBridgeError())
                return
            }
            let profiles = snapshots
                .compactMap { $0 }
                .filter { $0.exists }
                .map { $0.toUserProfileDto() }
            onComplete(profiles, nil)
        }
    }
}

extension DocumentSnapshot {
    fileprivate func toUserProfileDto() -> UserProfileDto {
        let fields = UserFields.shared
        let data = self.data() ?? [:]
        return UserProfileDto(
            id: documentID,
            name: data[fields.NAME] as? String,
            email: data[fields.EMAIL] as? String,
            photoUrl: data[fields.PHOTO_URL] as? String
        )
    }
}
