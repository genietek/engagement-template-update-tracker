# Engagement Template Update Tracker

Java 21 slice for the Caseware take-home: a queryable engagement→template index, pending-update accumulation, and human-readable summaries — without loading engagement files.

The design (primary deliverable) is in [DESIGN.md](DESIGN.md). AI usage notes are in [AI_USAGE.md](AI_USAGE.md).

## What this implements

| Requirement | Where |
| --- | --- |
| See at a glance which engagements have pending updates | `PendingUpdateService.listAtAGlance` |
| Human-readable summary of inbound changes | `DeterministicSummarizer` over a JSON diff |
| Apply / decline (recording the decision; applying content is out of scope) | `onDecision` |
| Multiple template updates before the user decides | accumulated `applied → latest` plus per-hop changelog |
| Do not load engagements (~1 minute constraint) | no engagement-loader port exists |
| Interfaces / contracts / tests | `port` package + JUnit 5 |

This is not a web application. The assignment asks for a design document and an optional small slice, not a full product.

## Prerequisites

- **JDK 21+** (`java -version` should show 21 or newer)
- **Apache Maven 3.8+** (`mvn -version`)

If Maven is missing:

```bash
# Debian/Ubuntu
sudo apt-get update && sudo apt-get install -y maven

# macOS
brew install maven
```

## Step-by-step

### 1. Clone / open the project

```bash
cd engagement-template-update-tracker
```

### 2. Run the tests

```bash
mvn test
```

Expected: all tests pass (pending-update scenarios, JSON diff, summary grounding).

### 3. Run the demo

```bash
mvn -q exec:java
```

The demo walks a firm with three engagements on template `audit-ifrs`:

1. Create three files on v1 — nothing pending
2. Publish v2 — all three pending, human-readable summary
3. Publish v3 before anyone decides — summaries accumulate to v1→v3
4. Alpha **applies** v3, Beta **declines** v3, Gamma still pending
5. Publish v4 — Alpha sees v3→v4, Beta/Gamma see v1→v4

### 4. (Optional) Compile only

```bash
mvn -q compile
```

## Project layout

```
DESIGN.md
src/main/java/com/caseware/templateupdate/
  app/PendingUpdateService.java    # domain slice
  port/                            # contracts
  model/                           # records / events
  diff/JsonDiffer.java
  summary/DeterministicSummarizer.java
  adapter/inmemory/                # test/demo adapters
  demo/DemoApp.java
src/test/java/                     # JUnit 5
```
