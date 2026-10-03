package io.github.brainboxemb.eventtiming.timingpoint.infra.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/** Compact operator-facing formatter shared by console, retained file and live logging. */
final class CompactLogFormatter extends Formatter {
    private static final String PROJECT_PACKAGE_PREFIX =
            "io.github.brainboxemb.eventtiming.timingpoint.";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ZoneId zoneId;

    CompactLogFormatter() {
        this(ZoneId.systemDefault());
    }

    CompactLogFormatter(ZoneId zoneId) {
        if (zoneId == null) {
            throw new IllegalArgumentException("zoneId must not be null");
        }
        this.zoneId = zoneId;
    }

    @Override
    public String format(LogRecord record) {
        StringBuilder line = new StringBuilder(160);
        line.append(TIME_FORMAT.format(
                        Instant.ofEpochMilli(record.getMillis()).atZone(zoneId)))
                .append(" - [")
                .append(LoggingControl.semanticLevel(record.getLevel()))
                .append("] - ")
                .append(formatMessage(record))
                .append(" - [")
                .append(source(record))
                .append(']')
                .append(System.lineSeparator());

        if (record.getThrown() != null) {
            StringWriter stack = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(stack));
            line.append(stack.toString());
            if (!stack.toString().endsWith(System.lineSeparator())) {
                line.append(System.lineSeparator());
            }
        }
        return line.toString();
    }

    String message(LogRecord record) {
        return formatMessage(record);
    }

    static String source(LogRecord record) {
        String className = record.getSourceClassName();
        String methodName = record.getSourceMethodName();
        if (className == null || className.trim().isEmpty()) {
            className = record.getLoggerName();
        }
        if (className == null || className.trim().isEmpty()) {
            className = "unknown";
        }
        String displayClass = className.startsWith(PROJECT_PACKAGE_PREFIX)
                ? projectSourceName(className)
                : className;
        if (methodName == null || methodName.trim().isEmpty()) {
            return displayClass;
        }
        return displayClass + "." + methodName;
    }

    private static String projectSourceName(String className) {
        String relative = className.substring(PROJECT_PACKAGE_PREFIX.length());
        int classSeparator = relative.lastIndexOf('.');
        if (classSeparator < 0) {
            return relative;
        }
        int packageSeparator = relative.lastIndexOf('.', classSeparator - 1);
        return packageSeparator < 0
                ? relative
                : relative.substring(packageSeparator + 1);
    }
}
