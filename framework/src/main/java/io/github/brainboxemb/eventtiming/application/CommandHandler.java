package io.github.brainboxemb.eventtiming.application;

import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.util.function.Supplier;

/**
 * Shared transport-independent application boundary for client commands and queries.
 *
 * <p>This class is intentionally small. It is not a command bus, mediator framework or generic
 * message registry. New methods belong here only when a real client use case needs shared
 * application-level handling.</p>
 */
public final class CommandHandler {
    private final BuildIdentity buildIdentity;
    private final Supplier<ApplicationStatus> statusSupplier;

    public CommandHandler(
            BuildIdentity buildIdentity,
            Supplier<ApplicationStatus> statusSupplier) {
        if (buildIdentity == null) {
            throw new IllegalArgumentException("buildIdentity must not be null");
        }
        if (statusSupplier == null) {
            throw new IllegalArgumentException("statusSupplier must not be null");
        }
        this.buildIdentity = buildIdentity;
        this.statusSupplier = statusSupplier;
    }

    /** Returns the authoritative application build/version identity. */
    public BuildIdentity version() {
        return buildIdentity;
    }

    /** Returns the current application status for presentation adapters. */
    public ApplicationStatus status() {
        ApplicationStatus status = statusSupplier.get();
        if (status == null) {
            throw new IllegalStateException("statusSupplier returned null");
        }
        return status;
    }
}
