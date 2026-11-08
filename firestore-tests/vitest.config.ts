import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    // All test files share one emulator; run them one after another so clearFirestore() in one file can't wipe another's data.
    fileParallelism: false,
    testTimeout: 15_000,
    hookTimeout: 30_000,
  },
});
