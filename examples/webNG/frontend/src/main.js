import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import './style.css'
import App from './App.vue'
import AnalysisView from './views/AnalysisView.vue'
import TypesView from './views/TypesView.vue'
import AboutView from './views/AboutView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/analysis' },
    { path: '/analysis', component: AnalysisView },
    { path: '/types', component: TypesView },
    { path: '/about', component: AboutView },
  ],
})

createApp(App).use(router).mount('#app')
