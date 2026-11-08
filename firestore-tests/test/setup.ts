import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import {
  initializeTestEnvironment,
  type RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import { doc, setDoc, type Firestore } from "firebase/firestore";

// The deployed rules file is the source of truth; tests always read it from the repo root.
const RULES_PATH = fileURLToPath(new URL("../../firestore.rules", import.meta.url));

export const OWNER = "owner-uid";
export const MEMBER = "member-uid";
export const STRANGER = "stranger-uid";
export const HOUSEHOLD_ID = "household-1";
export const ITEM_ID = "item-1";
export const JOIN_KEY = "4f7c1f0e-0000-4000-8000-000000000001";

export async function createTestEnvironment(): Promise<RulesTestEnvironment> {
  return initializeTestEnvironment({
    projectId: "demo-spiza",
    firestore: { rules: readFileSync(RULES_PATH, "utf8") },
  });
}

export function household(overrides: Record<string, unknown> = {}) {
  return {
    name: "Home",
    description: "",
    ownerId: OWNER,
    joinedMembers: [OWNER, MEMBER],
    ...overrides,
  };
}

export function item(overrides: Record<string, unknown> = {}) {
  return { text: "Milk", isChecked: false, position: 1024, ...overrides };
}

/** Writes documents bypassing the rules, to arrange the state a test starts from. */
export async function seed(
  env: RulesTestEnvironment,
  write: (db: Firestore) => Promise<void>,
): Promise<void> {
  await env.withSecurityRulesDisabled(async (context) => {
    await write(context.firestore() as unknown as Firestore);
  });
}

export async function seedHouseholdWithItem(env: RulesTestEnvironment): Promise<void> {
  await seed(env, async (db) => {
    await setDoc(doc(db, "households", HOUSEHOLD_ID), household());
    await setDoc(doc(db, "households", HOUSEHOLD_ID, "items", ITEM_ID), item());
  });
}

/** Firestore of a signed-in user, typed for the modular API. */
export function asUser(env: RulesTestEnvironment, uid: string): Firestore {
  return env.authenticatedContext(uid).firestore() as unknown as Firestore;
}

export function asSignedOut(env: RulesTestEnvironment): Firestore {
  return env.unauthenticatedContext().firestore() as unknown as Firestore;
}
