# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**FTA (Fast Text Analyzer)** is a Java library for semantic type detection and data profiling. It identifies both base types (Boolean, Double, Long, LocalDate, etc.) and semantic types (Email, Phone, SSN, Gender, Country, etc.) from text data. It supports 200+ semantic types across 15+ languages and ~750 locales for date detection.

**Maven coordinates**: `com.cobber.fta:fta` (full) or `com.cobber.fta:fta-core` (base types only)

## Build Commands

```bash
# Build and install distribution
./gradlew clean build installDist

# Run tests with coverage
./gradlew test jacocoTestReport

# Run tests for a specific module
./gradlew :types:test
./gradlew :core:test

# Run a single test class
./gradlew :types:test --tests "com.cobber.fta.TestDates"

# Run a single test method
./gradlew :types:test --tests "com.cobber.fta.TestDates.methodName"

# Generate JavaDoc
./gradlew javadoc

# Build and run examples
./gradlew examples.clean examples.build examples.run

# Check dependency updates
./gradlew dependencyUpdates
```

**Code quality**: SpotBugs (static analysis) and Checkstyle (config in `config/checkstyle/`) are configured. Test framework is TestNG. Coverage via Jacoco (reports in `build/reports/jacoco/test/`).

**Java target**: Java 17 (`options.release = 17`), but the runtime JVM must be **exactly Java 21** — DataSketches 6.2.0 requires it, and the build explicitly rejects JDK 22+. All build and test commands must be prefixed with `JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home`, e.g.:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home ./gradlew test
```

**Gradle version**: The wrapper targets Gradle 9.5.1 (set in `build.gradle` → `wrapper { gradleVersion = '9.5.1' }`).

**Debugging**: Capture analysis traces with `export FTA_TRACE="enabled=true,directory=/tmp,samples=10000"`, then replay with `cli/build/install/fta/bin/cli --replay <Stream>.fta`.

## Module Structure

```
core/             - Base type detection + date/time parsing (published as fta-core)
types/            - Semantic type detection, profiling, plugin system (published as fta)
cli/              - Command-line interface (Driver.java entry point)
examples/         - Standalone example projects (included builds)
examples/webNG/   - Spring Boot + Vue 3 web UI (included in examples.build; see its AGENTS.md)
```


### core module (`com.cobber.fta.*`)
- `dates/DateTimeParser` — Format detection and parsing across ~750 locales
- `token/` — Pattern/token-based text analysis (`Token`, `SimpleToken`, `FloatToken`, etc.)
- `core/FTAType` — Base type enum (Null, Boolean, Long, Double, LocalDate, LocalTime, etc.)

### types module (`com.cobber.fta.*`)
- `TextAnalyzer` — Single-column streaming/bulk analysis
- `RecordAnalyzer` — Multi-column record-mode analysis with cross-column context
- `Facts` — Profiling metrics (cardinality, patterns, min/max, histogram, signature)
- `LogicalType` / `LogicalTypeFactory` — Base classes for semantic types
  - `LogicalTypeRegExp` — Regex-based types
  - `LogicalTypeFinite` / `LogicalTypeFiniteSimple` — List/finite-set types
  - `LogicalTypeInfinite` — Infinite-set types
- `Plugins` — Plugin manager; loads from `types/src/main/resources/reference/plugins.json`
- `plugins/` — 57+ built-in semantic type implementations (Email, Gender, FirstName, GUID, etc.)
  - `plugins/address/` — Address component types
  - `plugins/identity/` — Identity document types (SSN variants, VAT, etc.)
- `types/src/main/resources/reference/` — 140+ CSV reference data files (countries, postal codes, etc.)

### cli module (`com.cobber.fta.driver.*`)
- `Driver` — Main entry point with argument parsing
- `FileProcessor` — CSV file processing
- `faker/` — Data generation utilities

## Three Analysis Modes

- **Streaming**: `TextAnalyzer.train(String)` — one value at a time; biased by early values
- **Bulk**: `TextAnalyzer.trainBulk(HashMap<String,Long>)` — pre-aggregated counts; faster, unbiased
- **Record**: `RecordAnalyzer.train(String[])` — multi-column with cross-column context biasing

## Plugin Architecture

Two plugin types:
1. **JSON-defined** — Regex or list-based, defined in `plugins.json` with locale/header biases
2. **Java-based** — Extend `LogicalType` for complex logic (check digits, address parsing, etc.)

When adding a new semantic type: add plugin definition or class consistent with existing patterns, provide locale/header biases if appropriate, add tests, and regenerate `SemanticTypes.md`:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home cli/build/install/fta/bin/cli --createSemanticTypesMarkdown > SemanticTypes.md
```

**Plugin `signature` field**: Java plugins require a `signature` in `plugins.json`. Run the full test suite after adding the plugin — the test framework will report the expected signature if it is missing or incorrect.

**Plugin `priority` field**: Lower number = higher importance; plugins are evaluated in ascending priority order and the first to exceed its confidence threshold wins. Built-in plugin priorities currently range from 100 (EMAIL) to 2500 (IDENTIFIER) — choose a value that places the new type correctly relative to existing ones. Set priority deliberately when a new type could overlap with an existing one.

**Testing a plugin against sample data**:

```bash
cli/build/install/fta/bin/cli --locale <locale> --pluginMode true --pluginName <SEMANTIC_TYPE> --col 0 --validatePlugin --verbose <file.csv>
```

This prints the plugin definition, then `true`/`false` for each input row, then the full analysis result.

## Releasing a New Version

**Version location**: the canonical version is defined in `settings.gradle` → `libs { version('fta', 'X.Y.Z') }`. That is the only place to change it.

**Example projects**: all 17 example `build.gradle` files reference the library as `fta:X.+` or `fta-core:X.+`. When the **major or minor** version changes, update the `X` in every example file to match. On patch-only bumps this is not required.

**Publishing**: releases reach Maven Central via GitHub Actions. Push a version tag (`git tag vX.Y.Z && git push --tags`) — the workflow stages the artifacts and promotes them to Central automatically. Do not run `PUBLISH.sh` manually; it has been superseded by the workflow.

## ChangeLog Conventions

`ChangeLog.md` (project root) uses [Conventional Commits](https://www.conventionalcommits.org/) format. Entries go under a `### <version>` section header, each prefixed with ` - ` (space-dash-space). Common types:

- `feat:` — new feature or enhancement
- `fix:` — bug fix
- `docs:` — documentation only
- `test:` — tests only
- `chore:` — dependency bumps, build system, infrastructure (e.g. `chore: Bump gradle to 9.5.1`)
- `refactor:` — code restructuring with no behaviour change

When bumping the version, create a new section for the new version containing **only** the current session's changes — never move or redistribute entries from already-published sections.

## Key Guardrails

- Do not alter large data catalogs under `types/src/main/resources/reference/` without documented justification and tests
- Do not relax validation regexes without evidence; prefer tighter validation
- Do not change public method signatures without a deprecation strategy
- Performance matters on hot paths (analyzers, validation); consider complexity for changes to `TextAnalyzer`, `Facts`, `LogicalType` subclasses
