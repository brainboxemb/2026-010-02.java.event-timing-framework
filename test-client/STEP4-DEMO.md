# Step-4 V04 Engineering Client demo

This checklist is the manual running-system evidence for SIP Step 4 after
VC-ST1-002 is green. It uses only public interfaces and the JavaFX Engineering
Client.

## Prepare

Use a clean checkout of the intended Java revision. The SI-01 application is
Java 8; the Engineering Client is Java 17.

Build the application with the repository Maven Wrapper:

```powershell
.\mvnw.cmd package -DskipTests
```

The V04 demo uses `config\step4-demo.yml`, which writes to the dedicated file
`data\step4-demo-timing-data.jsonl`. To start a fresh demo, remove only that
file:

```powershell
Remove-Item -Force data\step4-demo-timing-data.jsonl -ErrorAction SilentlyContinue
```

Do **not** remove the file between the first run and the restart/recovery part of
the demo.

## Start SI-01

From a terminal using Java 8:

```powershell
java -jar app\target\event-timing-app-0.2.3-SNAPSHOT.jar config\step4-demo.yml
```

Expected development endpoints:

```text
HTTP       http://127.0.0.1:8081
WebSocket  ws://127.0.0.1:8082/api/v1/events
Shell      127.0.0.1:8023
Live logs  127.0.0.1:8030
```

## Start the Engineering Client

In a second terminal using Java 17:

```powershell
.\mvnw.cmd -f test-client\pom.xml javafx:run
```

Open the **Timing** tab and choose **Connect live**.

## V04 flow

1. Verify the Timing view first shows **CONNECTED / syncing** or
   **RECONNECTING**, keeps state-changing controls disabled during rebuild, and
   only then becomes **LIVE**. Verify it shows:
   - TimingNode `timing-node-01`;
   - state `CLOSED`;
   - no current LocationId;
   - **Set location** enabled;
   - **Open** disabled;
   - **Auto-reg** disabled.
2. Enter LocationId `24` and choose **Set location**.
   - state remains `CLOSED`;
   - LocationId shows `24`;
   - **Open** becomes enabled.
3. Choose **Open**.
   - state becomes `OPEN`;
   - LocationId remains `24`;
   - **Set location** is disabled;
   - **Close** and **Auto-reg** are enabled.
4. Enter registration ID `N001`.
5. Use the explicit time below for deterministic evidence:
   `2026-10-01T12:00:00Z`.
6. Choose **Auto-reg**.
   - Last operation shows `seq 1`;
   - LogBook count becomes `1`;
   - the table contains sequence 1 / LocationId 24 / RegistrationId `N001`;
   - the record is shown as `AUTO_REG` with code `ADD`;
   - the **Events** tab contains one `TIMING_DATA_COMMITTED` event for that
     same record.
7. Choose **Close**.
   - state becomes `CLOSED`;
   - **Set location** becomes enabled again.
8. Change LocationId to `25` and verify the updated CLOSED state.

The GUI deliberately prevents a LocationId change while OPEN. The server-side
`NODE_NOT_CLOSED` rejection for a direct invalid request is already covered by
VC-ST1-002.

## Restart and recovery

1. Open the **Terminal** tab, connect to `127.0.0.1:8023` and enter `quit`.
2. Verify the application exits cleanly and the Timing view becomes stale or
   disconnected.
3. Keep `data\step4-demo-timing-data.jsonl`; restart SI-01 with the same demo
   config.
4. Reconnect the Engineering Client.
5. Verify:
   - the TimingNode starts `CLOSED`;
   - no operational LocationId is restored from historical TimingData;
   - LogBook count is still `1`;
   - sequence 1 / `N001` is visible after the bounded LogBook rebuild;
   - the new WebSocket session starts with `STATUS_SNAPSHOT`;
   - the old record is **not** replayed as a new
     `TIMING_DATA_COMMITTED` event.
6. Shut SI-01 down cleanly.

## Record evidence

Record these values with the pass/fail notes:

```text
SI-01 source revision :
SI-01 version         :
Engineering Client rev:
Operating system      :
SI-01 Java            :
Client Java           :

Initial CLOSED/no-location     PASS / FAIL
Location 24 + OPEN             PASS / FAIL
Auto-reg N001 -> seq 1         PASS / FAIL
Live committed event           PASS / FAIL
LogBook count/row              PASS / FAIL
Close + LocationId 25          PASS / FAIL
Restart persistence            PASS / FAIL
No historical live replay      PASS / FAIL
Clean shutdown                 PASS / FAIL
```

Screenshots may be retained as supporting UI evidence, but the pass/fail result
comes from the observed behaviour above.
