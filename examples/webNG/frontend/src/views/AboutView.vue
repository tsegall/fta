<template>
  <div class="p-8 max-w-2xl">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">About</h1>
    </div>

    <div class="card p-6 space-y-4 text-sm text-gray-700 leading-relaxed">
      <p>
        <strong>FTA (Fast Text Analyzer)</strong> is a Java library for semantic type detection and data profiling.
        It identifies both base types (Boolean, Double, Long, LocalDate, etc.) and semantic types
        (Email, Phone, SSN, Gender, Country, etc.) from text data.
      </p>
      <p>
        FTA supports 200+ semantic types across 15+ languages and approximately 750 locales for date detection.
      </p>
      <p>
        <strong>How to use:</strong> Upload a CSV file on the Analysis page, optionally specify a locale
        (e.g. <code class="bg-gray-100 px-1 rounded">en-US</code>, <code class="bg-gray-100 px-1 rounded">ja-JP</code>,
        <code class="bg-gray-100 px-1 rounded">fr-FR</code>), and set how many records to sample. Click
        <em>Analyze</em> to see per-column type detection results. Click any row to view the full JSON analysis.
      </p>
      <div class="pt-2 border-t border-gray-100">
        <p class="text-gray-500 text-xs">
          Maven: <code class="bg-gray-100 px-1 rounded">com.cobber.fta:fta</code>
        </p>
      </div>
    </div>
  </div>
</template>
