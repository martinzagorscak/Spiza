import { assertSucceeds, type RulesTestEnvironment } from "@firebase/rules-unit-testing";
import {
  arrayRemove,
  collection,
  deleteDoc,
  doc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from "firebase/firestore";
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";
import { asUser, createTestEnvironment, household, JOIN_KEY, MEMBER, OWNER, seed } from "./setup";

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

// MEMBER deletes their account: they own OWNED_ID (shared with OWNER) and are only a member of JOINED_ID (owned by OWNER).
const OWNED_ID = "household-owned";
const JOINED_ID = "household-joined";
const SECOND_JOIN_KEY = "4f7c1f0e-0000-4000-8000-000000000002";

describe("account deletion", () => {
  beforeEach(async () => {
    await seed(env, async (db) => {
      await setDoc(doc(db, "households", OWNED_ID), household({ ownerId: MEMBER, joinedMembers: [MEMBER, OWNER] }));
      await setDoc(doc(db, "households", OWNED_ID, "items", "item-1"), { text: "Milk", isChecked: false, position: 1024 });
      await setDoc(doc(db, "households", OWNED_ID, "items", "item-2"), { text: "Bread", isChecked: true, position: 2048 });
      await setDoc(doc(db, "households", JOINED_ID), household({ ownerId: OWNER, joinedMembers: [OWNER, MEMBER] }));
      await setDoc(doc(db, "joinKeys", JOIN_KEY), { userId: MEMBER });
      await setDoc(doc(db, "joinKeys", SECOND_JOIN_KEY), { userId: MEMBER });
      await setDoc(doc(db, "users", MEMBER), { name: "Ana", email: "ana@example.com" });
    });
  });

  // The same reads and writes, in the same order, as the client's account deletion.
  it("allows a user to delete everything they own and leave the rest, in the order the app does it", async () => {
    const db = asUser(env, MEMBER);

    // 1. Own join keys first (one batch), so nobody can add the user to a household after the memberships are read.
    const keys = await assertSucceeds(getDocs(query(collection(db, "joinKeys"), where("userId", "==", MEMBER))));
    expect(keys.size).toBe(2);
    const keysBatch = writeBatch(db);
    keys.docs.forEach((it) => keysBatch.delete(it.ref));
    await assertSucceeds(keysBatch.commit());

    // 2. Current memberships.
    const households = await assertSucceeds(
      getDocs(query(collection(db, "households"), where("joinedMembers", "array-contains", MEMBER))),
    );
    expect(households.docs.map((it) => it.id).sort()).toEqual([JOINED_ID, OWNED_ID]);

    // 3. Owned household: items and the household document in one batch.
    const items = await assertSucceeds(getDocs(collection(db, "households", OWNED_ID, "items")));
    const batch = writeBatch(db);
    items.docs.forEach((it) => batch.delete(it.ref));
    batch.delete(doc(db, "households", OWNED_ID));
    await assertSucceeds(batch.commit());

    // 4. Household where the user is only a member: leave it.
    await assertSucceeds(updateDoc(doc(db, "households", JOINED_ID), { joinedMembers: arrayRemove(MEMBER) }));

    // 5. Own profile, last.
    await assertSucceeds(deleteDoc(doc(db, "users", MEMBER)));

    // Nothing of the user is left; the other member's household still exists without them.
    await seed(env, async (admin) => {
      expect((await getDocs(collection(admin, "households", OWNED_ID, "items"))).size).toBe(0);
      expect((await getDocs(collection(admin, "joinKeys"))).size).toBe(0);
      expect((await getDocs(collection(admin, "users"))).size).toBe(0);
      const remaining = await getDocs(collection(admin, "households"));
      expect(remaining.docs.map((it) => it.id)).toEqual([JOINED_ID]);
      expect(remaining.docs[0].data().joinedMembers).toEqual([OWNER]);
    });
  });
});
