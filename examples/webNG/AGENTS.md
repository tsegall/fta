# AGENTS.md — webNG

This file documents the facts required to rebuild the webNG application.

## Overview

webNG is a Spring Boot + Vue 3 web application that exposes the FTA (Fast Text Analyzer) library through a browser UI. The backend is a JSON REST API; the frontend is a single-page Vue app built with Vite and Tailwind CSS.

## Build and Run

```bash
# First time only — generate the Gradle wrapper
gradle wrapper --gradle-version 9.5.1

# Build frontend and start the server (production mode)
./gradlew bootRun

# Frontend development (hot reload) — run both in parallel
./gradlew bootRun                        # terminal 1: Spring Boot on :8080
cd frontend && npm install && npm run dev # terminal 2: Vite dev server on :5173
```

Java requirement: Java 21 runtime (DataSketches transitive dep requires it). Compile target is Java 17.

## Module Structure

```
webNG/
├── build.gradle                          # Gradle build: Spring Boot + node-gradle plugin
├── settings.gradle
├── src/main/
│   ├── java/com/cobber/fta/
│   │   ├── FTAApplication.java           # @SpringBootApplication entry point
│   │   ├── AnalysisController.java       # REST endpoints: /api/analyze, /api/types, /api/version
│   │   ├── AnalysisService.java          # Core FTA processing + semantic types lookup
│   │   └── dto/
│   │       ├── AnalysisResponse.java     # { filename, locale, recordCount, fields[] }
│   │       ├── FieldResult.java          # Per-column result with embedded JsonNode details
│   │       └── SemanticTypeInfo.java     # { id, description, documentation[], languages[] }
│   └── resources/
│       ├── application.properties        # Multipart limits (50 MB)
│       └── static/                       # Vite build output (gitignored, generated at build time)
└── frontend/
    ├── package.json                      # Vue 3, Vue Router, Tailwind CSS, Vite
    ├── vite.config.js                    # outDir → ../src/main/resources/static; /api proxy to :8080; @tailwindcss/vite plugin
    ├── index.html                        # Google Fonts (Inter)
    └── src/
        ├── main.js                       # App bootstrap; hash-history Vue Router (routes defined inline)
        ├── style.css                     # @import "tailwindcss"; @theme font; @layer component classes
        ├── App.vue                       # Sidebar layout; locale input; fetches /api/version
        ├── composables/
        │   └── useLocale.js              # Shared locale ref (module-level, singleton)
        ├── views/
        │   ├── AnalysisView.vue          # Upload form + record-limit input + results table + JSON panel
        │   ├── TypesView.vue             # Searchable semantic types table; re-fetches on locale change
        │   └── AboutView.vue             # Static about page
        └── components/
            ├── FileUploadZone.vue        # Drag-and-drop + click-to-pick CSV upload
            ├── ResultsTable.vue          # Sortable/filterable results table; emits 'select'
            └── JsonPanel.vue             # Slide-over panel with syntax-highlighted JSON
```

## REST API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/analyze` | Multipart: `file` (CSV), `locale` (optional), `recordCount` (default 100). Returns `AnalysisResponse`. |
| `GET` | `/api/types` | Query param: `locale` (default `en`). Returns `SemanticTypeInfo[]`. |
| `GET` | `/api/version` | Returns `{ "version": "<fta-version>" }`. |

CORS is allowed from `http://localhost:5173` (Vite dev server).

## Key Design Decisions

**Vite output path**: `vite.config.js` writes the production build directly to `../src/main/resources/static` so Spring Boot's static resource handler serves it without any copy task. This directory is generated — do not commit it.

**Gradle frontend integration**: The `com.github.node-gradle.node` plugin (v7.1.0) provides `npmInstall` automatically. A custom `npmBuild` task depends on it and runs `npm run build`. `processResources` depends on `npmBuild`, so the frontend is always built before the JAR is assembled.

**Vue Router**: Uses `createWebHashHistory()` (hash-based URLs like `/#/analysis`). No Spring Boot fallback route is needed because hash fragments are never sent to the server.

**CSV parsing**: FastCSV 4.x `NamedCsvRecordHandler` is used with `allowDuplicateHeaderFields(true)` to handle CSVs whose header row has repeated column names. File content is always read as UTF-8 (`StandardCharsets.UTF_8`).

**FTA integration**: `AnalysisService.analyze()` creates an `AnalyzerContext` + `TextAnalyzer` template, sets the locale, then runs a `RecordAnalyzer` over the CSV rows. Results are converted to `FieldResult` records; the full FTA JSON output is embedded as a `JsonNode` (not a string) so it is returned as a nested JSON object.

**Null safety**: `SemanticType.getDocumentation()` can return `null` for plugins without documentation links. The mapping in `AnalysisService.getSemanticTypes()` guards this with `!= null ? Arrays.asList(...) : List.of()`.

**Shared locale state**: `composables/useLocale.js` exports a single module-level `ref('')`. Because it is module-level (not created inside `setup()`), all components that import it share the same reactive value. The locale input lives in `App.vue`'s sidebar so it persists across page navigation. `AnalysisView` reads it when submitting; `TypesView` watches it with `watch(locale, fetchTypes, { immediate: true })` and re-fetches from `/api/types?locale=<value>` whenever it changes.

**Tailwind CSS integration**: Uses Tailwind CSS 4 via the `@tailwindcss/vite` Vite plugin — no PostCSS pipeline or `tailwind.config.js` required. `style.css` starts with `@import "tailwindcss"` and uses a `@theme` block for the Inter font override. Content detection is automatic via Vite's import graph.

**JSON syntax highlighting**: Implemented in `JsonPanel.vue` using a single regex replace over `JSON.stringify` output — no external library. Keys are blue, strings green, numbers yellow, booleans purple, nulls red.

## Dependencies

### Backend
- Spring Boot 3.5.3 (`spring-boot-starter-web`)
- `com.cobber.fta:fta:18.+`
- `de.siegmar:fastcsv:4.2.0`
- `com.github.node-gradle.node:7.1.0` (Gradle plugin)

### Frontend
- Vue 3 (`^3.4.0`)
- Vue Router (`^5.0.0`)
- Tailwind CSS (`^4.3.0`) + `@tailwindcss/vite`
- Vite (`^8.0.0`) + `@vitejs/plugin-vue` (`^6.0.0`)

## FTA API Surface Used

```java
// Analysis
new AnalyzerContext(null, DateResolutionMode.Auto, filename, headerArray)
new TextAnalyzer(context)
template.setLocale(locale)
new RecordAnalyzer(template)
recordAnalyzer.train(String[] row)
recordAnalyzer.getResult().getStreamResults()   // → TextAnalysisResult[]

// Per result
result.getName()
result.isSemanticType()
result.getType().toString()
result.getTypeModifier()
result.getSemanticType()
result.getMinValue() / result.getMaxValue()
result.asJSON(false, 0)   // full JSON string → parsed to JsonNode

// Semantic types
SemanticType.getActiveSemanticTypes(Locale)
st.getId() / st.getDescription() / st.getDocumentation() / st.getLanguages()
// Note: getDocumentation() may return null

// Version
Utils.getVersion()
```
