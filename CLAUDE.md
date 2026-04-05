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

**Java target**: Java 17 (`options.release = 17`).

**Debugging**: Capture analysis traces with `export FTA_TRACE="enabled=true,directory=/tmp,samples=10000"`, then replay with `cli/build/install/fta/bin/cli --replay <Stream>.fta`.

## Module Structure

```
core/      - Base type detection + date/time parsing (published as fta-core)
types/     - Semantic type detection, profiling, plugin system (published as fta)
cli/       - Command-line interface (Driver.java entry point)
examples/  - Standalone example projects (included builds)
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

When adding a new semantic type: add plugin definition or class consistent with existing patterns, provide locale/header biases if appropriate, add tests, and update `SemanticTypes.md`.

## Key Guardrails (from AGENTS.md)

- Do not alter large data catalogs under `types/src/main/resources/reference/` without documented justification and tests
- Do not relax validation regexes without evidence; prefer tighter validation
- Do not change public method signatures without a deprecation strategy
- Performance matters on hot paths (analyzers, validation); consider complexity for changes to `TextAnalyzer`, `Facts`, `LogicalType` subclasses
