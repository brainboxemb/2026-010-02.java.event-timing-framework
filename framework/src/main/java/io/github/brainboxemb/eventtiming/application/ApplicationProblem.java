package io.github.brainboxemb.eventtiming.application;

/** Immutable machine-readable problem entry in an application status snapshot. */
public final class ApplicationProblem {
    private final String code;
    private final ProblemSeverity severity;
    private final String message;

    public ApplicationProblem(String code, ProblemSeverity severity, String message) {
        this.code = requireText(code, "code");
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }
        this.severity = severity;
        this.message = requireText(message, "message");
    }

    public String code() {
        return code;
    }

    public ProblemSeverity severity() {
        return severity;
    }

    public String message() {
        return message;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
