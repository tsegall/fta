<script setup>
import { ref, computed, watch } from 'vue'
import { useLocale } from '../composables/useLocale.js'

const { locale } = useLocale()
const allTypes = ref([])
const search = ref('')
const loading = ref(false)
const error = ref('')
const sortKey = ref('')
const sortDir = ref('asc')

function toggleSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = 'asc'
  }
}

function sortIcon(key) {
  if (sortKey.value !== key) return '↕'
  return sortDir.value === 'asc' ? '↑' : '↓'
}

async function fetchTypes() {
  loading.value = true
  error.value = ''
  try {
    const params = locale.value ? `?locale=${encodeURIComponent(locale.value)}` : ''
    const r = await fetch(`/api/types${params}`)
    if (!r.ok) throw new Error('Failed to load types')
    allTypes.value = await r.json()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

watch(locale, fetchTypes, { immediate: true })

const filtered = computed(() => {
  const q = search.value.toLowerCase()
  const rows = q
    ? allTypes.value.filter(t =>
        t.id.toLowerCase().includes(q) ||
        t.description.toLowerCase().includes(q) ||
        (t.languages || []).some(l => l.toLowerCase().includes(q))
      )
    : [...allTypes.value]

  if (sortKey.value) {
    const k = sortKey.value
    rows.sort((a, b) => {
      const av = k === 'languages' ? (a.languages || []).length : String(a[k] ?? '')
      const bv = k === 'languages' ? (b.languages || []).length : String(b[k] ?? '')
      const cmp = typeof av === 'number' ? av - bv : av.localeCompare(bv)
      return sortDir.value === 'asc' ? cmp : -cmp
    })
  }
  return rows
})
</script>

<template>
  <div class="p-8 max-w-screen-xl mx-auto">
    <div class="mb-8 flex items-start justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Semantic Types</h1>
        <p class="text-gray-500 mt-1 text-sm">{{ allTypes.length }} types available.</p>
      </div>
      <input v-model="search" class="input w-64" placeholder="Search types…" />
    </div>

    <div v-if="loading" class="text-gray-400 text-sm">Loading…</div>
    <div v-else-if="error" class="text-red-600 text-sm">{{ error }}</div>

    <div v-else class="card overflow-hidden">
      <table class="w-full text-left">
        <thead class="bg-gray-50 border-b border-gray-200">
          <tr>
            <th class="th w-64 cursor-pointer" @click="toggleSort('id')">ID {{ sortIcon('id') }}</th>
            <th class="th cursor-pointer" @click="toggleSort('description')">Description {{ sortIcon('description') }}</th>
            <th class="th w-40 cursor-pointer" @click="toggleSort('languages')">Languages {{ sortIcon('languages') }}</th>
            <th class="th w-32">Docs</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="t in filtered" :key="t.id" class="hover:bg-gray-50">
            <td class="td">
              <span class="font-mono text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded">{{ t.id }}</span>
            </td>
            <td class="td text-gray-600">{{ t.description }}</td>
            <td class="td">
              <div class="flex flex-wrap gap-1">
                <span v-for="lang in t.languages" :key="lang"
                  class="text-xs bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">
                  {{ lang }}
                </span>
              </div>
            </td>
            <td class="td">
              <a v-for="link in t.documentation" :key="link" :href="link" target="_blank"
                class="text-xs text-blue-600 hover:underline block truncate max-w-xs">
                docs ↗
              </a>
            </td>
          </tr>
          <tr v-if="filtered.length === 0">
            <td colspan="4" class="td text-center text-gray-400 py-8">No types match your search.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
