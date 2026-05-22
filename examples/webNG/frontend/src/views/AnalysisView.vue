<script setup>
import { ref, computed } from 'vue'
import FileUploadZone from '../components/FileUploadZone.vue'
import ResultsTable from '../components/ResultsTable.vue'
import JsonPanel from '../components/JsonPanel.vue'
import GenerationPanel from '../components/GenerationPanel.vue'
import { useLocale } from '../composables/useLocale.js'

const selectedFile = ref(null)
const { locale } = useLocale()
const recordCount = ref(100)
const loading = ref(false)
const error = ref('')
const results = ref(null)
const selectedField = ref(null)
const fakerSpecOpen = ref(false)

const DATE_TYPES = new Set(['LOCALDATE', 'LOCALTIME', 'LOCALDATETIME', 'OFFSETDATETIME'])

function fakerType(field) {
  return field.isSemanticType ? field.semanticTypeName : field.type.toUpperCase()
}

const fakerSpecField = computed(() => {
  if (!results.value) return null
  const spec = results.value.fields.map((field, index) => {
    const entry = { fieldName: field.fieldName, index, type: fakerType(field) }
    if (!field.isSemanticType && DATE_TYPES.has(entry.type) && field.details?.typeQualifier)
      entry.format = field.details.typeQualifier
    return entry
  })
  return { fieldName: 'Faker Specification', details: spec }
})

async function submit() {
  if (!selectedFile.value) return
  loading.value = true
  error.value = ''
  results.value = null

  const form = new FormData()
  form.append('file', selectedFile.value)
  if (locale.value) form.append('locale', locale.value)
  form.append('recordCount', String(recordCount.value))

  try {
    const r = await fetch('/api/analyze', { method: 'POST', body: form })
    const data = await r.json()
    if (!r.ok) throw new Error(data.error || 'Analysis failed')
    results.value = data
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="p-8 max-w-screen-xl mx-auto">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">Analysis</h1>
      <p class="text-gray-500 mt-1 text-sm">Upload a CSV file to detect base types and semantic types.</p>
    </div>

    <!-- Form card -->
    <div class="card p-6 mb-6">
      <FileUploadZone @change="selectedFile = $event" />

      <div class="mt-5 max-w-xs">
        <label class="block text-xs font-medium text-gray-600 mb-1">Record limit</label>
        <input v-model.number="recordCount" type="number" min="1" max="10000" class="input" />
      </div>

      <div class="mt-5 flex items-center gap-3">
        <button
          class="btn-primary"
          :disabled="!selectedFile || loading"
          @click="submit"
        >
          <svg v-if="loading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
          </svg>
          <svg v-else class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 15.803 7.5 7.5 0 0016.803 15.803z" />
          </svg>
          {{ loading ? 'Analyzing…' : 'Analyze' }}
        </button>
        <span v-if="results" class="text-xs text-gray-500">
          {{ results.fields.length }} field{{ results.fields.length !== 1 ? 's' : '' }} · {{ results.locale }}
        </span>
      </div>

      <div v-if="error" class="mt-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
        {{ error }}
      </div>
    </div>

    <!-- Results -->
    <ResultsTable
      v-if="results"
      :fields="results.fields"
      @select="selectedField = $event"
      @fakerSpec="fakerSpecOpen = true"
    />

    <!-- Generate records -->
    <GenerationPanel
      v-if="results"
      :spec="fakerSpecField?.details"
      :locale="results.locale"
    />

    <!-- Field JSON panel -->
    <JsonPanel :field="selectedField" @close="selectedField = null" />

    <!-- Faker spec JSON panel -->
    <JsonPanel
      :field="fakerSpecOpen ? fakerSpecField : null"
      label="Faker Specification"
      @close="fakerSpecOpen = false"
    />
  </div>
</template>
