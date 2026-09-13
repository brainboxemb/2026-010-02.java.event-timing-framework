package io.github.brainboxemb.eventtiming.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ApplicationStatusModelTest {
    @Test
    public void versionAndStatusReuseOneAuthoritativeBuildIdentity() {
        BuildIdentity identity = identity("revision-one");
        ApplicationStatusSnapshot snapshot = status(identity, ApplicationState.STARTING);
        ApplicationStatusModel model = new ApplicationStatusModel(identity, snapshot);

        assertSame(identity, model.buildIdentity());
        assertSame(identity, model.currentStatus().buildIdentity());
        assertEquals(identity.apiVersion(), model.currentStatus().apiVersion());
    }

    @Test
    public void snapshotDefensivelyCopiesStatusCollections() {
        BuildIdentity identity = identity("revision-one");
        List<TimingSystemStatus> timingSystems = new ArrayList<TimingSystemStatus>();
        List<ApplicationProblem> problems = new ArrayList<ApplicationProblem>();
        timingSystems.add(new TimingSystemStatus("system-a", TimingSystemLifecycle.CLOSED));
        problems.add(new ApplicationProblem("CONFIG_WARNING", ProblemSeverity.WARNING, "Synthetic warning"));

        ApplicationStatusSnapshot snapshot = new ApplicationStatusSnapshot(
                identity,
                ApplicationState.DEGRADED,
                Instant.parse("2026-09-13T06:00:00Z"),
                timingSystems,
                problems);

        timingSystems.clear();
        problems.clear();

        assertEquals(1, snapshot.timingSystems().size());
        assertEquals("system-a", snapshot.timingSystems().get(0).id());
        assertEquals(TimingSystemLifecycle.CLOSED, snapshot.timingSystems().get(0).lifecycle());
        assertEquals(1, snapshot.problems().size());
        assertEquals("CONFIG_WARNING", snapshot.problems().get(0).code());
        assertEquals(ProblemSeverity.WARNING, snapshot.problems().get(0).severity());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void snapshotTimingSystemsAreUnmodifiable() {
        ApplicationStatusSnapshot snapshot = status(identity("revision-one"), ApplicationState.RUNNING);
        snapshot.timingSystems().add(new TimingSystemStatus("system-b", TimingSystemLifecycle.CLOSED));
    }

    @Test
    public void currentStatusCanBeReplacedWithoutChangingBuildIdentity() {
        BuildIdentity identity = identity("revision-one");
        ApplicationStatusModel model = new ApplicationStatusModel(
                identity,
                status(identity, ApplicationState.STARTING));
        ApplicationStatusSnapshot running = status(identity, ApplicationState.RUNNING);

        model.replaceStatus(running);

        assertSame(running, model.currentStatus());
        assertSame(identity, model.currentStatus().buildIdentity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsStatusFromAnotherBuildIdentityInstance() {
        BuildIdentity authoritative = identity("revision-one");
        BuildIdentity duplicate = identity("revision-one");
        new ApplicationStatusModel(authoritative, status(duplicate, ApplicationState.STARTING));
    }

    @Test(expected = IllegalArgumentException.class)
    public void problemRejectsBlankMachineCode() {
        new ApplicationProblem(" ", ProblemSeverity.ERROR, "Synthetic error");
    }

    @Test(expected = IllegalArgumentException.class)
    public void timingSystemRejectsBlankId() {
        new TimingSystemStatus(" ", TimingSystemLifecycle.CLOSED);
    }

    private static BuildIdentity identity(String revision) {
        return BuildIdentity.firstApiVersion(
                "event-timing-app",
                "0.2.0-SNAPSHOT",
                revision,
                "2026-09-13T06:00:00Z");
    }

    private static ApplicationStatusSnapshot status(
            BuildIdentity identity,
            ApplicationState state) {
        return new ApplicationStatusSnapshot(
                identity,
                state,
                Instant.parse("2026-09-13T06:00:00Z"),
                Collections.singletonList(
                        new TimingSystemStatus("system-a", TimingSystemLifecycle.CLOSED)),
                Collections.<ApplicationProblem>emptyList());
    }
}
