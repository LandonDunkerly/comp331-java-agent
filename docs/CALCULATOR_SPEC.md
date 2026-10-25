# Task Specification: Calculator.evaluate

**Goal:** Evaluate an arithmetic expression given as a string and return the result.

**Inputs:** `String expression`.

**Outputs:** A `double`. Supports `+ - * /`, parentheses, unary minus, and decimal numbers. Whitespace is ignored.

**Constraints:**
- Java 17, no external libraries, static method in a final class.
- `*` and `/` bind tighter than `+` and `-`; operators of equal precedence are left-associative.
- Must not mutate shared state or print anything.
- No code comments.

**Edge cases:**
- `"2+3*4"` -> `14`, `"(2+3)*4"` -> `20`
- `"10-4-3"` -> `3`, `"16/4/2"` -> `2`
- `"2*-3"` -> `-6`, `"1 - -1"` -> `2`, `"--1"` -> `1`
- `"7/2"` -> `3.5`
- Division by zero (`"1/0"`, `"5/(2-2)"`) -> `ArithmeticException`
- `null`, blank, or malformed input (`"2+"`, `"2 3"`, `"(2+3"`, `"1.2.3"`, `"abc"`) -> `IllegalArgumentException`

**Acceptance criteria:**
- All cases above covered by JUnit tests that pass via `mvn test`.
- Each public failure mode throws the exception type listed above.
- Methods are under about 25 lines.