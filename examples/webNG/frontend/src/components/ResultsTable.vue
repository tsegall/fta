<script setup>
import { ref, computed } from 'vue'

const props = defineProps({ fields: { type: Array, required: true } })
const emit = defineEmits(['select'])

const search = ref('')
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

const rows = computed(() => {
  const q = search.value.toLowerCase()
  let list = q
    ? props.fields.filter(f =>
        (f.fieldName || '').toLowerCase().includes(q) ||
        (f.type || '').toLowerCase().includes(q) ||
        (f.semanticTypeName || '').toLowerCase().includes(q) ||
        (f.typeModifier || '').toLowerCase().includes(q)
      )
    : [...props.fields]

  if (sortKey.value) {
    list.sort((a, b) => {
      const av = String(a[sortKey.value] ?? '')
      const bv = String(b[sortKey.value] ?? '')
      return sortDir.value === 'asc' ? av.localeCompare(bv) : bv.localeCompare(av)
    })
  }
  return list
})
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-3">
      <h2 class="text-base font-semibold text-gray-800">Results</h2>
      <input v-model="search" class="input w-56 text-xs" placeholder="Filter fields…" />
    </div>

    <div class="card overflow-x-auto">
      <table class="w-full text-left whitespace-nowrap">
        <thead class="bg-gray-50 border-b border-gray-200">
          <tr>
            <th class="th" @click="toggleSort('fieldName')">Field {{ sortIcon('fieldName') }}</th>
            <th class="th">Semantic</th>
            <th class="th" @click="toggleSort('type')">Base Type {{ sortIcon('type') }}</th>
            <th class="th" @click="toggleSort('typeModifier')">Modifier {{ sortIcon('typeModifier') }}</th>
            <th class="th" @click="toggleSort('semanticTypeName')">Semantic Type {{ sortIcon('semanticTypeName') }}</th>
            <th class="th" @click="toggleSort('minValue')">Min {{ sortIcon('minValue') }}</th>
            <th class="th" @click="toggleSort('maxValue')">Max {{ sortIcon('maxValue') }}</th>
            <th class="th w-16"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="field in rows" :key="field.fieldName"
            class="hover:bg-gray-50 cursor-pointer"
            @click="emit('select', field)">
            <td class="td font-medium text-gray-900">{{ field.fieldName }}</td>
            <td class="td">
              <span v-if="field.isSemanticType"
                class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">
                Yes
              </span>
              <span v-else class="text-gray-400 text-xs">No</span>
            </td>
            <td class="td font-mono text-xs text-gray-600">{{ field.type }}</td>
            <td class="td font-mono text-xs text-gray-500">{{ field.typeModifier ?? '—' }}</td>
            <td class="td">
              <span v-if="field.semanticTypeName"
                class="font-mono text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded">
                {{ field.semanticTypeName }}
              </span>
              <span v-else class="text-gray-300">—</span>
            </td>
            <td class="td text-xs text-gray-500 max-w-[120px] truncate">{{ field.minValue ?? '—' }}</td>
            <td class="td text-xs text-gray-500 max-w-[120px] truncate">{{ field.maxValue ?? '—' }}</td>
            <td class="td">
              <button class="btn-ghost text-xs" @click.stop="emit('select', field)">
                JSON
              </button>
            </td>
          </tr>
          <tr v-if="rows.length === 0">
            <td colspan="8" class="td text-center text-gray-400 py-8">No fields match your filter.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
