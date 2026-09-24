package io.github.brainboxemb.eventtiming.application;

/**
 * Shared transport-independent application boundary for client commands and simple queries.
 *
 * <p>The first executable only needs the authoritative build/version query. Keeping that query
 * behind this boundary prevents console, shell and later HTTP adapters from each reaching into
 * application state directly or inventing their own version semantics.</p>
 *
 * <p>This class is intentionally small. It is not a command bus, mediator framework or generic
 * message registry. New methods belong here only when a real client use case needs shared
 * application-level handling.</p>
 */
public final class CommandHandler {
    private final BuildIdentity buildIdentity;

    public CommandHandler(BuildIdentity buildIdentity) {
        if (buildIdentity == null) {
            throw new IllegalArgumentException("buildIdentity must not be null");
        }
        this.buildIdentity = buildIdentity;
    }

    /** Returns the authoritative application build/version identity. */
    public BuildIdentity version() {
        return buildIdentity;
    }
}
