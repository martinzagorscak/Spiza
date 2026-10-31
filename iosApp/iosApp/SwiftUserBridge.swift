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
        users.whereField(FieldPath.documentID(), in: userIds).getDocuments { snapshot, error in
            let profiles = snapshot?.documents.map { $0.toUserProfileDto() } ?? []
            onComplete(profiles, error?.toBridgeError())
        }
    }
}

extension QueryDocumentSnapshot {
    fileprivate func toUserProfileDto() -> UserProfileDto {
        let fields = UserFields.shared
        let data = self.data()
        return UserProfileDto(
            id: documentID,
            name: data[fields.NAME] as? String,
            email: data[fields.EMAIL] as? String,
            photoUrl: data[fields.PHOTO_URL] as? String
        )
    }
}
