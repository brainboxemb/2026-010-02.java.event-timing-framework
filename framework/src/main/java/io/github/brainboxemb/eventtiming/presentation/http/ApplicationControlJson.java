package io.github.brainboxemb.eventtiming.presentation.http;

import io.github.brainboxemb.eventtiming.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

/** Explicit JSON mapping for the IF-03 first-executable contract. */
final class ApplicationControlJson {
    private ApplicationControlJson() {
    }

    static String version(BuildIdentity identity) {
        return "{"
                + "\"application\":" + quote(identity.application()) + ","
                + "\"version\":" + quote(identity.version()) + ","
                + "\"revision\":" + quote(identity.revision()) + ","
                + "\"sourceRef\":" + quote(identity.sourceRef()) + ","
                + "\"buildOrigin\":" + quote(identity.buildOrigin()) + ","
                + "\"dirty\":" + identity.dirty() + ","
                + "\"apiVersion\":" + quote(identity.apiVersion())
                + "}";
    }

    static String status(BuildIdentity identity, ApplicationStatus status) {
        return "{"
                + "\"apiVersion\":" + quote(identity.apiVersion()) + ","
                + "\"build\":" + version(identity) + ","
                + "\"application\":{"
                + "\"state\":" + quote(status.applicationState()) + ","
                + "\"startedAt\":" + quote(status.startedAt().toString())
                + "},"
                + "\"timingNodes\":[{"
                + "\"timingNodeId\":" + quote(status.timingNodeId().value()) + ","
                + "\"lifecycle\":" + quote(status.timingNodeLifecycle().name())
                + "}],"
                + "\"problems\":[]"
                + "}";
    }

    static String error(String code, String message) {
        return "{"
                + "\"apiVersion\":\"1\","
                + "\"error\":{"
                + "\"code\":" + quote(code) + ","
                + "\"message\":" + quote(message)
                + "}}";
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2);
        result.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        result.append("\\u");
                        String hex = Integer.toHexString(ch);
                        for (int pad = hex.length(); pad < 4; pad++) {
                            result.append('0');
                        }
                        result.append(hex);
                    } else {
                        result.append(ch);
                    }
            }
        }
        result.append('"');
        return result.toString();
    }
}
