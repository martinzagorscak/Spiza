import { assertFails, assertSucceeds, type RulesTestEnvironment } from "@firebase/rules-unit-testing";
import {
  arrayRemove,
  arrayUnion,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from "firebase/firestore";
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";
import {
  asSignedOut,
  asUser,
  createTestEnvironment,
  household,
  HOUSEHOLD_ID,
  ITEM_ID,
  JOIN_KEY,
  MEMBER,
  OWNER,
  seed,
  seedHouseholdWithItem,
  STRANGER,
} from "./setup";

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

const householdRef = (db: ReturnType<typeof asUser>) => doc(db, "households", HOUSEHOLD_ID);

describe("households: create", () => {
  it("allows a user to create a household they own and are the only member of", async () => {
    await assertSucceeds(
      setDoc(householdRef(asUser(env, OWNER)), household({ joinedMembers: [OWNER] })),
    );
  });

  // region Error handling

  it("rejects a household owned by someone else", async () => {
    await assertFails(
      setDoc(householdRef(asUser(env, STRANGER)), household({ joinedMembers: [STRANGER] })),
    );
  });

  it("rejects a new household that already has other members", async () => {
    await assertFails(setDoc(householdRef(asUser(env, OWNER)), household()));
  });

  it("rejects unknown fields", async () => {
    await assertFails(
      setDoc(householdRef(asUser(env, OWNER)), household({ joinedMembers: [OWNER], color: "red" })),
    );
  });

  it("rejects fields of the wrong type", async () => {
    await assertFails(
      setDoc(householdRef(asUser(env, OWNER)), household({ joinedMembers: [OWNER], name: 42 })),
    );
  });

  it("rejects signed-out users", async () => {
    await assertFails(setDoc(householdRef(asSignedOut(env)), household({ joinedMembers: [OWNER] })));
  });

  // endregion
});

describe("households: read", () => {
  beforeEach(async () => {
    await seedHouseholdWithItem(env);
  });

  it("allows the owner and members to read the household", async () => {
    await assertSucceeds(getDoc(householdRef(asUser(env, OWNER))));
    await assertSucceeds(getDoc(householdRef(asUser(env, MEMBER))));
  });

  // The households screen listens to this query (FirestoreHouseholdDataSource.observeHouseholds).
  it("allows a user to list the households they are a member of", async () => {
    const db = asUser(env, MEMBER);

    await assertSucceeds(getDocs(query(collection(db, "households"), where("joinedMembers", "array-contains", MEMBER))));
  });

  // region Error handling

  it("rejects listing households without the membership filter", async () => {
    await assertFails(getDocs(collection(asUser(env, STRANGER), "households")));
  });

  it("rejects listing the households of another user", async () => {
    const db = asUser(env, STRANGER);

    await assertFails(getDocs(query(collection(db, "households"), where("joinedMembers", "array-contains", MEMBER))));
  });

  it("rejects users who are not members", async () => {
    await assertFails(getDoc(householdRef(asUser(env, STRANGER))));
  });

  it("rejects signed-out users", async () => {
    await assertFails(getDoc(householdRef(asSignedOut(env))));
  });

  // endregion
});

describe("households: update by the owner", () => {
  beforeEach(async () => {
    await seedHouseholdWithItem(env);
  });

  it("allows the owner to rename the household and change its description", async () => {
    await assertSucceeds(
      updateDoc(householdRef(asUser(env, OWNER)), { name: "Flat", description: "Shared flat" }),
    );
  });

  // Same batch as FirestoreHouseholdDataSource.addMemberToHousehold: add the member and consume their join key.
  it("allows the owner to add a member and consume their join key in one batch", async () => {
    await seed(env, (db) => setDoc(doc(db, "joinKeys", JOIN_KEY), { userId: STRANGER }));
    const db = asUser(env, OWNER);
    const batch = writeBatch(db);
    batch.update(householdRef(db), { joinedMembers: arrayUnion(STRANGER) });
    batch.delete(doc(db, "joinKeys", JOIN_KEY));

    await assertSucceeds(batch.commit());
  });

  it("allows the owner to remove a member", async () => {
    await assertSucceeds(updateDoc(householdRef(asUser(env, OWNER)), { joinedMembers: arrayRemove(MEMBER) }));
  });

  // region Error handling

  // Consuming deletes the key and only an existing key may be deleted, so a key can add its user only once.
  it("rejects a second owner consuming a join key that was already consumed", async () => {
    await seed(env, async (db) => {
      await setDoc(doc(db, "joinKeys", JOIN_KEY), { userId: STRANGER });
      await setDoc(doc(db, "households", "household-2"), household({ ownerId: MEMBER, joinedMembers: [MEMBER] }));
    });
    const first = asUser(env, OWNER);
    const firstBatch = writeBatch(first);
    firstBatch.update(householdRef(first), { joinedMembers: arrayUnion(STRANGER) });
    firstBatch.delete(doc(first, "joinKeys", JOIN_KEY));
    await assertSucceeds(firstBatch.commit());
    const second = asUser(env, MEMBER);
    const secondBatch = writeBatch(second);
    secondBatch.update(doc(second, "households", "household-2"), { joinedMembers: arrayUnion(STRANGER) });
    secondBatch.delete(doc(second, "joinKeys", JOIN_KEY));

    await assertFails(secondBatch.commit());

    await seed(env, async (db) => {
      expect((await getDoc(doc(db, "households", "household-2"))).data()?.joinedMembers).toEqual([MEMBER]);
    });
  });

  it("rejects transferring ownership", async () => {
    await assertFails(updateDoc(householdRef(asUser(env, OWNER)), { ownerId: MEMBER }));
  });

  it("rejects the owner removing themselves", async () => {
    await assertFails(updateDoc(householdRef(asUser(env, OWNER)), { joinedMembers: [MEMBER] }));
  });

  it("rejects unknown fields", async () => {
    await assertFails(updateDoc(householdRef(asUser(env, OWNER)), { color: "red" }));
  });

  it("rejects fields of the wrong type", async () => {
    await assertFails(updateDoc(householdRef(asUser(env, OWNER)), { name: 42 }));
    await assertFails(updateDoc(householdRef(asUser(env, OWNER)), { joinedMembers: OWNER }));
  });

  it("rejects signed-out users", async () => {
    await assertFails(updateDoc(householdRef(asSignedOut(env)), { name: "Flat" }));
  });

  // endregion
});

describe("households: update by a member", () => {
  beforeEach(async () => {
    await seedHouseholdWithItem(env);
  });

  // Same write as FirestoreHouseholdDataSource.removeMemberFromHousehold.
  it("allows a member to leave the household", async () => {
    await assertSucceeds(updateDoc(householdRef(asUser(env, MEMBER)), { joinedMembers: arrayRemove(MEMBER) }));
  });

  // region Error handling

  it("rejects a member removing someone else", async () => {
    await assertFails(updateDoc(householdRef(asUser(env, MEMBER)), { joinedMembers: arrayRemove(OWNER) }));
  });

  it("rejects a member renaming the household", async () => {
    await assertFails(updateDoc(householdRef(asUser(env, MEMBER)), { name: "Mine" }));
  });

  it("rejects a member adding someone", async () => {
    await assertFails(
      updateDoc(householdRef(asUser(env, MEMBER)), { joinedMembers: [OWNER, MEMBER, STRANGER] }),
    );
  });

  it("rejects users who are not members joining by themselves", async () => {
    await assertFails(updateDoc(householdRef(asUser(env, STRANGER)), { joinedMembers: arrayUnion(STRANGER) }));
  });

  it("rejects users who are not members removing someone", async () => {
    await assertFails(updateDoc(householdRef(asUser(env, STRANGER)), { joinedMembers: arrayRemove(MEMBER) }));
  });

  // endregion
});

describe("households: delete", () => {
  beforeEach(async () => {
    await seedHouseholdWithItem(env);
  });

  // The app deletes the items subcollection and the household in one batch; the item rule reads the parent before the batch.
  it("allows the owner to delete the household together with its items in one batch", async () => {
    const db = asUser(env, OWNER);
    const batch = writeBatch(db);
    batch.delete(doc(db, "households", HOUSEHOLD_ID, "items", ITEM_ID));
    batch.delete(householdRef(db));

    await assertSucceeds(batch.commit());
  });

  // region Error handling

  it("rejects signed-out users", async () => {
    await assertFails(deleteDoc(householdRef(asSignedOut(env))));
  });

  it("rejects a member deleting the household", async () => {
    await assertFails(deleteDoc(householdRef(asUser(env, MEMBER))));
  });

  it("rejects users who are not members", async () => {
    await assertFails(deleteDoc(householdRef(asUser(env, STRANGER))));
  });

  // endregion
});
