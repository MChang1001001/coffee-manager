<template>
  <div class="app-root">
    <header class="app-header">
      <RouterLink class="brand" to="/coffee">Coffee Manager</RouterLink>
      <nav class="app-nav" aria-label="主导航">
        <RouterLink to="/coffee">咖啡豆</RouterLink>
      </nav>
    </header>

    <RouterView />
  </div>
</template>
