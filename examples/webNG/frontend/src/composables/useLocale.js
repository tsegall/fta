import { ref } from 'vue'

// Module-level ref — shared across all component instances
const locale = ref('')

export function useLocale() {
  return { locale }
}
