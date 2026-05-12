<script setup>
import { ref } from 'vue'

const emit = defineEmits(['change'])
const isDragging = ref(false)
const fileName = ref('')
const fileInput = ref(null)

function onDrop(e) {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) pick(file)
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (file) pick(file)
}

function pick(file) {
  fileName.value = file.name
  emit('change', file)
}

function openPicker() {
  fileInput.value?.click()
}
</script>

<template>
  <div
    class="relative border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors duration-150"
    :class="isDragging ? 'border-blue-400 bg-blue-50' : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'"
    @click="openPicker"
    @dragover.prevent="isDragging = true"
    @dragleave.prevent="isDragging = false"
    @drop.prevent="onDrop"
  >
    <input ref="fileInput" type="file" accept=".csv,text/csv" class="hidden" @change="onFileChange" />

    <svg class="mx-auto w-10 h-10 mb-3" :class="fileName ? 'text-blue-500' : 'text-gray-300'"
      fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
      <path stroke-linecap="round" stroke-linejoin="round"
        d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
    </svg>

    <p v-if="fileName" class="text-sm font-medium text-blue-700">{{ fileName }}</p>
    <p v-else class="text-sm text-gray-500">
      <span class="font-medium text-blue-600">Click to upload</span> or drag and drop
    </p>
    <p class="text-xs text-gray-400 mt-1">CSV files only</p>
  </div>
</template>
