package io.github.brainboxemb.eventtiming.systemtest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Retained build evidence from one separate-process black-box verification. */
final class BlackBoxEvidence {
    private final File directory;

    private BlackBoxEvidence(File directory) {
        this.directory = directory;
    }

    static BlackBoxEvidence create(String rootPath, String verificationId)
            throws IOException {
        File directory = new File(rootPath, verificationId).getAbsoluteFile();
        deleteRecursively(directory);
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Unable to create evidence directory: " + directory);
        }
        return new BlackBoxEvidence(directory);
    }

    File directory() {
        return directory;
    }

    File file(String name) {
        return new File(directory, name);
    }

    void writeProcessOutput(String output) {
        writeQuietly("application-output.txt", output);
    }

    void writeResult(boolean passed, Throwable failure) {
        StringBuilder result = new StringBuilder();
        result.append(passed ? "PASS" : "FAIL").append(System.lineSeparator());
        if (failure != null) {
            result.append(failure.getClass().getName());
            if (failure.getMessage() != null) {
                result.append(": ").append(failure.getMessage());
            }
            result.append(System.lineSeparator());
        }
        writeQuietly("result.txt", result.toString());
    }

    private void writeQuietly(String name, String text) {
        try {
            Files.write(
                    file(name).toPath(),
                    text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            System.err.println(
                    "Unable to write black-box evidence "
                            + file(name)
                            + ": "
                            + ex.getMessage());
        }
    }

    private static void deleteRecursively(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                throw new IOException("Unable to list evidence directory: " + file);
            }
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            throw new IOException("Unable to delete previous evidence: " + file);
        }
    }
}
