# Task Specification: DurationFormatter.format

**Goal:** Convert a number of seconds into a short human-readable duration string.

**Inputs:** `long totalSeconds`.

**Outputs:** A `String` like `"1h 2m 3s"`. Zero-valued units are omitted. Zero input returns `"0s"`.

**Constraints:**
- Java 17, no external libraries, static method in a final utility class.
- Units are only hours, minutes, seconds (no days).
- Single space between units, no leading/trailing whitespace.
- Must not mutate state or print anything.

**Edge cases:**
- `0` -> `"0s"`
- Exact multiples (60 -> `"1m"`, 3600 -> `"1h"`)
- Skipped middle unit (3601 -> `"1h 1s"`)
- Very large values (100000 -> `"27h 46m 40s"`)
- Negative input -> `IllegalArgumentException`

**Acceptance criteria:**
- All cases above covered by JUnit tests that pass via `mvn test`.
- No overflow or exception for `Long.MAX_VALUE`.
- Method is under ~25 lines.
