import { assertFails, assertSucceeds, type RulesTestEnvironment } from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  documentId,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
} from "firebase/firestore";
import { afterAll, beforeAll, beforeEach, describe, it } from "vitest";
import { asSignedOut, asUser, createTestEnvironment, JOIN_KEY, MEMBER, OWNER, seed } from "./setup";

let env: RulesTestEnvironment;

beforeAll(async () => {
  env = await createTestEnvironment();
});

afterAll(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

const profile = { name: "Ana", email: "ana@example.com", photoUrl: null };

describe("users", () => {
  it("allows a user to create their own profile", async () => {
    await assertSucceeds(setDoc(doc(asUser(env, MEMBER), "users", MEMBER), profile));
  });

  // iOS leaves out missing fields instead of writing null.
  it("allows a profile with only some of the fields", async () => {
    await assertSucceeds(setDoc(doc(asUser(env, MEMBER), "users", MEMBER), { name: "Ana" }));
  });

  it("allows any signed-in user to read profiles, to show household members", async () => {
    await seed(env, (db) => setDoc(doc(db, "users", MEMBER), profile));

    await assertSucceeds(getDoc(doc(asUser(env, OWNER), "users", MEMBER)));
  });

  // Same as FirestoreUserDataSource.getUsers / SwiftUserBridge.getUsers for the member list of a household: one get per id,
  // including ids without a profile document (skipped by the app).
  it("allows a signed-in user to fetch several profiles by id, one document at a time", async () => {
    await seed(env, (db) => setDoc(doc(db, "users", MEMBER), profile));
    const db = asUser(env, OWNER);

    await assertSucceeds(Promise.all([getDoc(doc(db, "users", MEMBER)), getDoc(doc(db, "users", OWNER))]));
  });

  // Last step of account deletion.
  it("allows a user to delete their own profile", async () => {
    await seed(env, (db) => setDoc(doc(db, "users", MEMBER), profile));

    await assertSucceeds(deleteDoc(doc(asUser(env, MEMBER), "users", MEMBER)));
  });

  // region Error handling

  it("rejects creating someone else's profile", async () => {
    await assertFails(setDoc(doc(asUser(env, OWNER), "users", MEMBER), profile));
  });

  it("rejects unknown fields", async () => {
    await assertFails(setDoc(doc(asUser(env, MEMBER), "users", MEMBER), { ...profile, isAdmin: true }));
  });

  // The app only creates the profile once; nobody may change it afterwards.
  it("rejects updating a profile, even your own", async () => {
    await seed(env, (db) => setDoc(doc(db, "users", MEMBER), profile));

    await assertFails(updateDoc(doc(asUser(env, MEMBER), "users", MEMBER), { name: "Someone else" }));
  });

  // Listing would let anyone enumerate every profile and email.
  it("rejects listing profiles", async () => {
    await seed(env, (db) => setDoc(doc(db, "users", MEMBER), profile));
    const db = asUser(env, OWNER);

    await assertFails(getDocs(collection(db, "users")));
  });

  // The query older builds used; it is a list, so it is denied even for ids the caller knows.
  it("rejects querying profiles by id", async () => {
    await seed(env, (db) => setDoc(doc(db, "users", MEMBER), profile));
    const db = asUser(env, OWNER);

    await assertFails(getDocs(query(collection(db, "users"), where(documentId(), "in", [MEMBER, OWNER]))));
  });

  it("rejects deleting someone else's profile", async () => {
    await seed(env, (db) => setDoc(doc(db, "users", MEMBER), profile));

    await assertFails(deleteDoc(doc(asUser(env, OWNER), "users", MEMBER)));
  });

  it("rejects signed-out users", async () => {
    await assertFails(getDoc(doc(asSignedOut(env), "users", MEMBER)));
    await assertFails(setDoc(doc(asSignedOut(env), "users", MEMBER), profile));
    await assertFails(deleteDoc(doc(asSignedOut(env), "users", MEMBER)));
  });

  // endregion
});

describe("joinKeys", () => {
  const keyRef = (db: ReturnType<typeof asUser>) => doc(db, "joinKeys", JOIN_KEY);

  it("allows a user to create a join key for themselves", async () => {
    await assertSucceeds(setDoc(keyRef(asUser(env, MEMBER)), { userId: MEMBER }));
  });

  it("allows any signed-in user who knows the key to read it, so the owner can find the user to add", async () => {
    await seed(env, (db) => setDoc(doc(db, "joinKeys", JOIN_KEY), { userId: MEMBER }));

    await assertSucceeds(getDoc(keyRef(asUser(env, OWNER))));
  });

  it("allows a user to list their own keys", async () => {
    await seed(env, (db) => setDoc(doc(db, "joinKeys", JOIN_KEY), { userId: MEMBER }));
    const db = asUser(env, MEMBER);

    await assertSucceeds(getDocs(query(collection(db, "joinKeys"), where("userId", "==", MEMBER))));
  });

  it("allows deleting a key, when the owner consumes it or the user replaces it", async () => {
    await seed(env, (db) => setDoc(doc(db, "joinKeys", JOIN_KEY), { userId: MEMBER }));

    await assertSucceeds(deleteDoc(keyRef(asUser(env, OWNER))));
  });

  // region Error handling

  it("rejects creating a key for someone else", async () => {
    await assertFails(setDoc(keyRef(asUser(env, OWNER)), { userId: MEMBER }));
  });

  it("rejects unknown fields", async () => {
    await assertFails(setDoc(keyRef(asUser(env, MEMBER)), { userId: MEMBER, householdId: "household-1" }));
  });

  it("rejects updating a key, a new key is a new document", async () => {
    await seed(env, (db) => setDoc(doc(db, "joinKeys", JOIN_KEY), { userId: MEMBER }));

    await assertFails(updateDoc(keyRef(asUser(env, MEMBER)), { userId: OWNER }));
  });

  it("rejects listing keys of other users", async () => {
    await seed(env, (db) => setDoc(doc(db, "joinKeys", JOIN_KEY), { userId: MEMBER }));
    const db = asUser(env, OWNER);

    await assertFails(getDocs(collection(db, "joinKeys")));
    await assertFails(getDocs(query(collection(db, "joinKeys"), where("userId", "==", MEMBER))));
  });

  it("rejects signed-out users", async () => {
    await seed(env, (db) => setDoc(doc(db, "joinKeys", JOIN_KEY), { userId: MEMBER }));
    const db = asSignedOut(env);

    await assertFails(getDoc(keyRef(db)));
    await assertFails(setDoc(keyRef(db), { userId: MEMBER }));
    await assertFails(getDocs(query(collection(db, "joinKeys"), where("userId", "==", MEMBER))));
    await assertFails(deleteDoc(keyRef(db)));
  });

  // endregion
});
