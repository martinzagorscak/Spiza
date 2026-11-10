import { assertFails, assertSucceeds, type RulesTestEnvironment } from "@firebase/rules-unit-testing";
import {
  arrayRemove,
  collection,
  doc,
  type Firestore,
  getDoc,
  getDocs,
  query,
  setDoc,
  where,
  writeBatch,
  type WriteBatch,
} from "firebase/firestore";
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";
import { asUser, createTestEnvironment, household, item, JOIN_KEY, MEMBER, OWNER, seed, STRANGER } from "./setup";

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

// ConstraintsManager limits (client-side): HOUSEHOLDS_PER_USER and ITEMS_PER_HOUSEHOLD.
const MAX_HOUSEHOLDS = 3;
const MAX_ITEMS = 100;

/**
 * The same reads and the same single batch as the client's account deletion: own join keys, every household the user is
 * a member of and the items of the ones they own, then ONE batch that deletes the keys, the owned households with their
 * items, leaves the other households and deletes the profile. [extra] adds writes to the batch before it is committed.
 */
async function deleteAccount(db: Firestore, uid: string, extra: (batch: WriteBatch) => void = () => {}): Promise<void> {
  const keys = await assertSucceeds(getDocs(query(collection(db, "joinKeys"), where("userId", "==", uid))));
  const households = await assertSucceeds(
    getDocs(query(collection(db, "households"), where("joinedMembers", "array-contains", uid))),
  );
  const owned = households.docs.filter((it) => it.data().ownerId === uid);
  const joined = households.docs.filter((it) => it.data().ownerId !== uid);
  const ownedItems = await Promise.all(
    owned.map((it) => assertSucceeds(getDocs(collection(db, "households", it.id, "items")))),
  );

  const batch = writeBatch(db);
  keys.docs.forEach((it) => batch.delete(it.ref));
  ownedItems.forEach((items) => items.docs.forEach((it) => batch.delete(it.ref)));
  owned.forEach((it) => batch.delete(it.ref));
  joined.forEach((it) => batch.update(it.ref, { joinedMembers: arrayRemove(uid) }));
  batch.delete(doc(db, "users", uid));
  extra(batch);
  await batch.commit();
}

/** Every document path in the emulator, read with the rules disabled. */
async function allPaths(): Promise<string[]> {
  const paths: string[] = [];
  await seed(env, async (admin) => {
    for (const name of ["joinKeys", "users", "households"]) {
      for (const it of (await getDocs(collection(admin, name))).docs) {
        paths.push(it.ref.path);
        if (name === "households") {
          (await getDocs(collection(admin, "households", it.id, "items"))).docs.forEach((item) => paths.push(item.ref.path));
        }
      }
    }
  });
  return paths.sort();
}

describe("account deletion: one batch", () => {
  beforeEach(async () => {
    await seed(env, async (db) => {
      await setDoc(doc(db, "households", OWNED_ID), household({ ownerId: MEMBER, joinedMembers: [MEMBER, OWNER] }));
      await setDoc(doc(db, "households", OWNED_ID, "items", "item-1"), item({ text: "Milk" }));
      await setDoc(doc(db, "households", OWNED_ID, "items", "item-2"), item({ text: "Bread", isChecked: true, position: 2048 }));
      await setDoc(doc(db, "households", JOINED_ID), household({ ownerId: OWNER, joinedMembers: [OWNER, MEMBER] }));
      await setDoc(doc(db, "households", JOINED_ID, "items", "item-1"), item());
      await setDoc(doc(db, "joinKeys", JOIN_KEY), { userId: MEMBER });
      await setDoc(doc(db, "joinKeys", SECOND_JOIN_KEY), { userId: MEMBER });
      await setDoc(doc(db, "users", MEMBER), { name: "Ana", email: "ana@example.com" });
      await setDoc(doc(db, "users", OWNER), { name: "Ivan", email: "ivan@example.com" });
    });
  });

  it("deletes everything the user owns and leaves the other households, in one batch", async () => {
    await assertSucceeds(deleteAccount(asUser(env, MEMBER), MEMBER));

    // Nothing of the user is left; the other member's household, its items and profile still exist without them.
    expect(await allPaths()).toEqual([
      `households/${JOINED_ID}`,
      `households/${JOINED_ID}/items/item-1`,
      `users/${OWNER}`,
    ]);
    await seed(env, async (admin) => {
      expect((await getDoc(doc(admin, "households", JOINED_ID))).data()?.joinedMembers).toEqual([OWNER]);
    });
  });

  // The client runs a second pass right after the first batch; with nothing left it only deletes the missing profile.
  it("allows a second pass when everything is already deleted", async () => {
    const db = asUser(env, MEMBER);
    await assertSucceeds(deleteAccount(db, MEMBER));
    const afterFirstPass = await allPaths();

    await assertSucceeds(deleteAccount(db, MEMBER));

    expect(await allPaths()).toEqual(afterFirstPass);
  });

  // An owner may add the user with their join key while the first pass runs; the second pass removes them again.
  it("removes the user from a household they were added to between the two passes", async () => {
    const db = asUser(env, MEMBER);
    await assertSucceeds(deleteAccount(db, MEMBER));
    await seed(env, async (admin) => {
      await setDoc(doc(admin, "households", "added-later"), household({ ownerId: OWNER, joinedMembers: [OWNER, MEMBER] }));
      await setDoc(doc(admin, "households", "added-later", "items", "item-1"), item());
    });

    await assertSucceeds(deleteAccount(db, MEMBER));

    await seed(env, async (admin) => {
      expect((await getDoc(doc(admin, "households", "added-later"))).data()?.joinedMembers).toEqual([OWNER]);
      expect((await getDoc(doc(admin, "households", "added-later", "items", "item-1"))).exists()).toBe(true);
    });
  });

  // Worst case the app allows (and a bit more: membership of other households on top of the owned maximum, which a race
  // between two devices could produce). Proves 308 writes and the item rule's get() of the parent fit in one batch.
  it("deletes the maximum number of owned households and items in one batch", async () => {
    await seed(env, async (db) => {
      for (let h = 0; h < MAX_HOUSEHOLDS; h++) {
        const id = `max-household-${h}`;
        const batch = writeBatch(db);
        batch.set(doc(db, "households", id), household({ ownerId: STRANGER, joinedMembers: [STRANGER] }));
        for (let i = 0; i < MAX_ITEMS; i++) {
          batch.set(doc(db, "households", id, "items", `item-${i}`), item({ position: i * 1024 }));
        }
        await batch.commit();
      }
      await setDoc(doc(db, "households", "stranger-other"), household({ ownerId: OWNER, joinedMembers: [OWNER, STRANGER] }));
      await setDoc(doc(db, "households", "stranger-other-2"), household({ ownerId: MEMBER, joinedMembers: [MEMBER, STRANGER] }));
      await setDoc(doc(db, "joinKeys", "stranger-key-1"), { userId: STRANGER });
      await setDoc(doc(db, "joinKeys", "stranger-key-2"), { userId: STRANGER });
      await setDoc(doc(db, "users", STRANGER), { name: "Marko", email: "marko@example.com" });
    });
    const before = await allPaths();

    await assertSucceeds(deleteAccount(asUser(env, STRANGER), STRANGER));

    const after = await allPaths();
    expect(after.filter((path) => path.includes("max-household") || path.includes("stranger-key"))).toEqual([]);
    expect(after).not.toContain(`users/${STRANGER}`);
    // Only the stranger's documents are gone; everything of MEMBER and OWNER is untouched.
    const strangers = before.filter(
      (path) => path.includes("max-household") || path.includes("stranger-key") || path === `users/${STRANGER}`,
    );
    expect(strangers.length).toBe(MAX_HOUSEHOLDS * (MAX_ITEMS + 1) + 3);
    expect(after).toEqual(before.filter((path) => strangers.includes(path) === false));
    await seed(env, async (admin) => {
      for (const id of ["stranger-other", "stranger-other-2"]) {
        expect((await getDoc(doc(admin, "households", id))).data()?.joinedMembers).not.toContain(STRANGER);
      }
    });
  });

  // region Error handling

  it.each([
    ["deleting another user's profile", (db: Firestore) => (batch: WriteBatch) => batch.delete(doc(db, "users", OWNER))],
    ["deleting a household the user does not own", (db: Firestore) => (batch: WriteBatch) => batch.delete(doc(db, "households", JOINED_ID))],
    ["deleting an item of a household the user is not a member of", (db: Firestore) => (batch: WriteBatch) => batch.delete(doc(db, "households", "foreign", "items", "item-1"))],
  ])("rejects the whole batch and deletes nothing when it also contains %s", async (_, rejectedWrite) => {
    await seed(env, async (db) => {
      await setDoc(doc(db, "households", "foreign"), household({ ownerId: OWNER, joinedMembers: [OWNER] }));
      await setDoc(doc(db, "households", "foreign", "items", "item-1"), item());
    });
    const before = await allPaths();
    const db = asUser(env, MEMBER);

    await assertFails(deleteAccount(db, MEMBER, rejectedWrite(db)));

    expect(await allPaths()).toEqual(before);
    await seed(env, async (admin) => {
      expect((await getDoc(doc(admin, "households", JOINED_ID))).data()?.joinedMembers).toEqual([OWNER, MEMBER]);
    });
  });

  it("rejects another user deleting someone's account documents", async () => {
    const db = asUser(env, OWNER);
    const batch = writeBatch(db);
    batch.delete(doc(db, "users", MEMBER));
    batch.delete(doc(db, "households", OWNED_ID));
    batch.update(doc(db, "households", JOINED_ID), { joinedMembers: arrayRemove(MEMBER) });

    await assertFails(batch.commit());

    expect(await allPaths()).toContain(`users/${MEMBER}`);
    expect(await allPaths()).toContain(`households/${OWNED_ID}`);
  });

  // endregion
});
