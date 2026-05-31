import { createRouter, createWebHistory } from 'vue-router'
import CoffeeBeanDetailView from '../views/CoffeeBeanDetailView.vue'
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
    {
      path: '/coffee-beans/:id',
      name: 'coffee-bean-detail',
      component: CoffeeBeanDetailView,
    },
  ],
})

export default router
