package io.github.brainboxemb.eventtiming.testkit;

/** Tiny bootstrap assertion helper proving the testkit is consumable as a separate module. */
public final class ModuleAssertions {
    private ModuleAssertions() {
    }

    public static void requireEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
