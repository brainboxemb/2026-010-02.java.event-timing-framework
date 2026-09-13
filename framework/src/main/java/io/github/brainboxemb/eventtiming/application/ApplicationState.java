package io.github.brainboxemb.eventtiming.application;

/** First-executable application states defined by the IF-03 status contract. */
public enum ApplicationState {
    STARTING,
    RUNNING,
    DEGRADED,
    STOPPING
}
