import { assertFails, assertSucceeds, type RulesTestEnvironment } from "@firebase/rules-unit-testing";
import { collection, deleteDoc, doc, getDoc, getDocs, setDoc, updateDoc } from "firebase/firestore";
import { afterAll, beforeAll, beforeEach, describe, it } from "vitest";
import {
  asSignedOut,
  asUser,
  createTestEnvironment,
  HOUSEHOLD_ID,
  item,
  ITEM_ID,
  MEMBER,
  OWNER,
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
  await seedHouseholdWithItem(env);
});

const itemRef = (db: ReturnType<typeof asUser>, id = ITEM_ID) => doc(db, "households", HOUSEHOLD_ID, "items", id);
const itemsRef = (db: ReturnType<typeof asUser>) => collection(db, "households", HOUSEHOLD_ID, "items");

describe("items: members", () => {
  it("allows the owner and members to read the items", async () => {
    await assertSucceeds(getDocs(itemsRef(asUser(env, OWNER))));
    await assertSucceeds(getDoc(itemRef(asUser(env, MEMBER))));
  });

  it("allows a member to add an item", async () => {
    await assertSucceeds(setDoc(itemRef(asUser(env, MEMBER), "item-2"), item({ text: "Bread" })));
  });

  it("allows a member to check, rename and move an item", async () => {
    await assertSucceeds(
      updateDoc(itemRef(asUser(env, MEMBER)), { isChecked: true, text: "Oat milk", position: 2048.5 }),
    );
  });

  it("allows a member to delete an item", async () => {
    await assertSucceeds(deleteDoc(itemRef(asUser(env, MEMBER))));
  });

  // region Error handling

  it("rejects unknown fields", async () => {
    await assertFails(setDoc(itemRef(asUser(env, MEMBER), "item-2"), item({ quantity: 2 })));
  });

  it("rejects fields of the wrong type", async () => {
    await assertFails(setDoc(itemRef(asUser(env, MEMBER), "item-2"), item({ isChecked: "yes" })));
    await assertFails(updateDoc(itemRef(asUser(env, MEMBER)), { position: "first" }));
  });

  // endregion
});

describe("items: non-members", () => {
  // region Error handling

  it("rejects reading, adding, changing and deleting items of a household the user is not in", async () => {
    const db = asUser(env, STRANGER);

    await assertFails(getDocs(itemsRef(db)));
    await assertFails(setDoc(itemRef(db, "item-2"), item()));
    await assertFails(updateDoc(itemRef(db), { isChecked: true }));
    await assertFails(deleteDoc(itemRef(db)));
  });

  it("rejects signed-out users", async () => {
    const db = asSignedOut(env);

    await assertFails(getDocs(itemsRef(db)));
    await assertFails(setDoc(itemRef(db, "item-2"), item()));
    await assertFails(updateDoc(itemRef(db), { isChecked: true }));
    await assertFails(deleteDoc(itemRef(db)));
  });

  // endregion
});
