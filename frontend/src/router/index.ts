import { createRouter, createWebHistory } from 'vue-router'
import CoffeeView from '../views/CoffeeView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/coffee',
    },
    {
      path: '/coffee',
      name: 'coffee',
      component: CoffeeView,
    },
  ],
})

export default router
