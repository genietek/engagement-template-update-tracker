# AI usage notes

Deliverable requested by the take-home. This is what was used, what was kept, and what was not trusted.

## Where AI helped

- Turned the PDF into a scoped design: registry projection, publish-time fan-out, accumulated pending summaries.
- Drafted Java ports, in-memory adapters, and the first cut of `PendingUpdateService`.
- Suggested mermaid structure and a first pass at the observability / failure-mode tables.

## Where output was corrected or ignored

- First implementation attempt was TypeScript. That was discarded; this is a Java assessment.
- An early decision handler cleared pending state without re-syncing. That is wrong: apply/decline still takes ~1 minute in engagement management, and a newer publish can land during that window. The hook now updates the registry and re-syncs against latest.
- “Generate a nice LLM summary” was not left as unconstrained prose. Summaries are deterministic in this slice; any future LLM adapter must pass a grounding check (`sourcePath` ∈ JSON diff). Audit wording is not something to trust unaudited.
- A full Spring Boot app was not generated. The brief is a design document plus a small slice, not a product build.

## How I would guide other engineers using AI on this system

- Use AI for boilerplate adapters, test cases from invariants, and first drafts of docs.
- Do not let it invent engagement-load “shortcuts.” The 1-minute constraint is a hard architectural input.
- Do not ship LLM summaries without a grounding gate and a content-authored release-note path.
- Require a human to review event contracts and decline/accumulate semantics before merge.

## Where AI should not be trusted in this domain

- Whether an update *should* be applied (professional judgment).
- Unchecked natural-language descriptions of methodology/procedure changes.
- Any design that scans or loads all engagement files to build a dashboard.
- Security/tenancy of the registry (what metadata is allowed to be shared across firms).
