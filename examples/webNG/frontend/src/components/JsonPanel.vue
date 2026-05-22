<script setup>
import { computed } from 'vue'

const props = defineProps({
  field: { type: Object, default: null },
  label: { type: String, default: 'Field details' }
})
const emit = defineEmits(['close'])

const highlighted = computed(() => {
  if (!props.field?.details) return ''
  const str = JSON.stringify(props.field.details, null, 2)
  return str.replace(
    /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g,
    (match) => {
      if (/^"/.test(match)) {
        if (/:$/.test(match)) return `<span class="text-blue-300">${match}</span>`
        return `<span class="text-green-300">${match}</span>`
      }
      if (/true|false/.test(match)) return `<span class="text-purple-300">${match}</span>`
      if (/null/.test(match)) return `<span class="text-red-300">${match}</span>`
      return `<span class="text-yellow-300">${match}</span>`
    }
  )
})
</script>

<template>
  <Transition
    enter-active-class="transition-transform duration-200 ease-out"
    enter-from-class="translate-x-full"
    enter-to-class="translate-x-0"
    leave-active-class="transition-transform duration-150 ease-in"
    leave-from-class="translate-x-0"
    leave-to-class="translate-x-full"
  >
    <div v-if="field"
      class="fixed inset-y-0 right-0 w-[540px] bg-slate-900 shadow-2xl flex flex-col z-50 border-l border-slate-700"
    >
      <!-- Header -->
      <div class="flex items-center justify-between px-5 py-4 border-b border-slate-700">
        <div>
          <p class="text-xs text-slate-400 uppercase tracking-wider">{{ label }}</p>
          <h2 class="text-white font-semibold mt-0.5">{{ field.fieldName }}</h2>
        </div>
        <button
          class="text-slate-400 hover:text-white transition-colors rounded-lg p-1.5 hover:bg-slate-700"
          @click="emit('close')"
        >
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Summary chips -->
      <div class="flex flex-wrap gap-2 px-5 py-3 border-b border-slate-700 bg-slate-800">
        <span class="text-xs bg-slate-700 text-slate-300 px-2 py-1 rounded">
          {{ field.type }}
        </span>
        <span v-if="field.typeModifier" class="text-xs bg-slate-700 text-slate-300 px-2 py-1 rounded">
          {{ field.typeModifier }}
        </span>
        <span v-if="field.semanticTypeName" class="text-xs bg-blue-600 text-white px-2 py-1 rounded">
          {{ field.semanticTypeName }}
        </span>
        <span v-if="field.isSemanticType" class="text-xs bg-green-600 text-white px-2 py-1 rounded">
          Semantic
        </span>
      </div>

      <!-- JSON body -->
      <div class="flex-1 overflow-auto px-5 py-4">
        <pre class="text-xs leading-5 font-mono text-slate-300"
          v-html="highlighted"
        />
      </div>
    </div>
  </Transition>

  <!-- Backdrop -->
  <Transition
    enter-active-class="transition-opacity duration-200"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition-opacity duration-150"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div v-if="field"
      class="fixed inset-0 bg-black/30 z-40"
      @click="emit('close')"
    />
  </Transition>
</template>
