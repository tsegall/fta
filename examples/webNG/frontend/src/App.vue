<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { useLocale } from './composables/useLocale.js'

const version = ref('')
const { locale } = useLocale()

onMounted(async () => {
  try {
    const r = await fetch('/api/version')
    const data = await r.json()
    version.value = data.version
  } catch (_) {}
})

const navItems = [
  { path: '/analysis', label: 'Analysis', icon: 'M9 17.25v1.007a3 3 0 01-.879 2.122L7.5 21h9l-.621-.621A3 3 0 0115 18.257V17.25m6-12V15a2.25 2.25 0 01-2.25 2.25H3.75A2.25 2.25 0 011.5 15V5.25m19.5 0A2.25 2.25 0 0018.75 3H5.25A2.25 2.25 0 003 5.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 7.409A2.25 2.25 0 012.25 5.493V5.25' },
  { path: '/types',    label: 'Types',    icon: 'M3.75 9.776c.112-.017.227-.026.344-.026h15.812c.117 0 .232.009.344.026m-16.5 0a2.25 2.25 0 00-1.883 2.542l.857 6a2.25 2.25 0 002.227 1.932H19.05a2.25 2.25 0 002.227-1.932l.857-6a2.25 2.25 0 00-1.883-2.542m-16.5 0V6A2.25 2.25 0 016 3.75h3.879a1.5 1.5 0 011.06.44l2.122 2.12a1.5 1.5 0 001.06.44H18A2.25 2.25 0 0120.25 9v.776' },
  { path: '/about',   label: 'About',    icon: 'M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z' },
]
</script>

<template>
  <div class="flex h-screen overflow-hidden">
    <!-- Sidebar -->
    <aside class="w-56 flex-shrink-0 bg-slate-800 flex flex-col">
      <div class="px-5 py-6">
        <div class="flex items-center gap-2">
          <div class="w-7 h-7 bg-blue-500 rounded-lg flex items-center justify-center">
            <svg class="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 14.25v2.25m3-4.5v4.5m3-6.75v6.75m3-9v9M6 20.25h12A2.25 2.25 0 0020.25 18V6A2.25 2.25 0 0018 3.75H6A2.25 2.25 0 003.75 6v12A2.25 2.25 0 006 20.25z" />
            </svg>
          </div>
          <span class="text-white font-semibold text-base">FTA Analyzer</span>
        </div>
      </div>

      <nav class="flex-1 px-3 space-y-1">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          active-class="active"
        >
          <svg class="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" :d="item.icon" />
          </svg>
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="px-4 py-4 border-t border-slate-700 space-y-1">
        <label class="block text-xs font-medium text-slate-400 px-1">Locale</label>
        <input
          v-model="locale"
          class="w-full rounded-lg bg-slate-700 border border-slate-600 px-3 py-1.5 text-sm text-white placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
          placeholder="e.g. en-US, ja-JP"
        />
        <p class="text-slate-500 text-xs px-1 pt-1">Fast Text Analyzer v{{ version }}</p>
      </div>
    </aside>

    <!-- Main content -->
    <main class="flex-1 overflow-auto bg-gray-50">
      <RouterView />
    </main>
  </div>
</template>
