<script setup>
import { ref } from 'vue'

const props = defineProps({
  spec: { type: Array, default: null },
  locale: { type: String, default: '' }
})

const count = ref(100)
const loading = ref(false)
const error = ref('')
const output = ref('')
const copied = ref(false)

async function generate() {
  loading.value = true
  error.value = ''
  output.value = ''

  try {
    const r = await fetch('/api/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ spec: props.spec, count: count.value, locale: props.locale })
    })
    if (!r.ok) {
      const data = await r.json()
      throw new Error(data.error || 'Generation failed')
    }
    output.value = await r.text()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function copy() {
  await navigator.clipboard.writeText(output.value)
  copied.value = true
  setTimeout(() => { copied.value = false }, 1500)
}
</script>

<template>
  <div v-if="spec" class="mt-6">
    <div class="mb-3">
      <h2 class="text-base font-semibold text-gray-800">Generate Records</h2>
    </div>

    <div class="card p-6">
      <div class="flex items-end gap-4">
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1">Record count</label>
          <input v-model.number="count" type="number" min="1" max="10000" class="input w-36" />
        </div>
        <button class="btn-primary" :disabled="loading" @click="generate">
          <svg v-if="loading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
          </svg>
          <svg v-else class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M5 3l14 9-14 9V3z"/>
          </svg>
          {{ loading ? 'Generating…' : 'Generate' }}
        </button>
      </div>

      <div v-if="error" class="mt-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
        {{ error }}
      </div>

      <div v-if="output" class="mt-5">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs text-gray-500">{{ count }} record{{ count !== 1 ? 's' : '' }} generated</span>
          <button class="btn-ghost text-xs" @click="copy">{{ copied ? 'Copied!' : 'Copy' }}</button>
        </div>
        <div class="card overflow-hidden">
          <div class="bg-slate-900 px-5 py-4 overflow-auto max-h-96">
            <pre class="text-xs leading-5 font-mono text-slate-300">{{ output }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
