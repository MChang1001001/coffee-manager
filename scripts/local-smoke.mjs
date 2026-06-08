#!/usr/bin/env node

const DEFAULTS = {
  backendUrl: 'http://localhost:8080',
  frontendUrl: 'http://localhost:5173',
  username: 'admin',
  password: 'admin123456',
}

const PNG_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAFgwJ/l4S8WQAAAABJRU5ErkJggg=='

const config = readConfig()
const stamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
const smokePrefix = `[SMOKE_TEST] ${stamp}`
const state = {
  coffeeId: null,
  reviewId: null,
  secondReviewId: null,
  brewId: null,
  secondBrewId: null,
  coverUrl: null,
  drinkStatusSamples: [],
  deleted: {
    coffee: false,
    review: false,
    secondReview: false,
    brew: false,
    secondBrew: false,
  },
  checks: [],
  warnings: [],
}

main().catch(async (error) => {
  state.warnings.push(`Smoke failed: ${error.message}`)
  await cleanup()
  printSummary('FAIL', error)
  process.exitCode = 1
})

async function main() {
  console.log('Local MVP smoke test starting...')
  console.log(`Backend: ${config.backendUrl}`)
  console.log(`Frontend: ${config.frontendUrl}${config.skipFrontend ? ' (skipped)' : ''}`)
  console.log(`Username: ${config.username}`)

  await checkBackendHealth()
  await checkFrontendIfAvailable()

  const token = await login()
  await smokeCoffeeEnums(token)
  await smokeCoffee(token)
  await smokeDrinkStatusFilters(token)
  await smokeFileUpload(token)
  await smokeReview(token)
  await smokeBrew(token)
  await cleanup(token)

  printSummary('PASS')
}

async function checkBackendHealth() {
  const body = await requestApi('GET', '/api/health')
  assert(body.data?.status === 'ok', 'Backend health status is not ok')
  assert(body.data?.database === 'ok', 'Backend health database is not ok')
  state.checks.push('Backend health: ok')
}

async function checkFrontendIfAvailable() {
  if (config.skipFrontend) {
    state.warnings.push('Frontend check skipped by --skip-frontend or SMOKE_SKIP_FRONTEND=true.')
    return
  }

  try {
    const pageResponse = await fetch(joinUrl(config.frontendUrl, '/coffee'))
    assert(pageResponse.ok, `Frontend /coffee returned HTTP ${pageResponse.status}`)

    const proxyBody = await requestApi('GET', '/api/health', {
      baseUrl: config.frontendUrl,
      expectApiEnvelope: true,
    })
    assert(proxyBody.data?.database === 'ok', 'Frontend /api proxy health database is not ok')

    state.checks.push('Frontend /coffee: ok')
    state.checks.push('Vite proxy /api: ok')
  } catch (error) {
    state.warnings.push(
      `Frontend optional check skipped/failed: ${error.message}. Core smoke continues against backend.`
    )
  }
}

async function login() {
  const body = await requestApi('POST', '/api/auth/login', {
    json: {
      username: config.username,
      password: config.password,
    },
  })
  assert(body.data?.token, 'Login did not return token')
  state.checks.push(`Login/token: ok (${body.data.user?.username ?? config.username})`)
  return body.data.token
}

async function smokeCoffeeEnums(token) {
  const body = await requestApi('GET', '/api/enums/coffee', { token })
  assert(Array.isArray(body.data?.roastLevels), 'Coffee enums roastLevels is not an array')
  assert(Array.isArray(body.data?.processMethods), 'Coffee enums processMethods is not an array')
  assert(Array.isArray(body.data?.origins), 'Coffee enums origins is not an array')
  assert(Array.isArray(body.data?.varieties), 'Coffee enums varieties is not an array')
  assert(
    body.data.roastLevels.some((option) => option.label === '浅烘' && option.value === 'LIGHT'),
    'Coffee enums roastLevels did not include LIGHT'
  )
  assert(
    body.data.processMethods.some((option) => option.label === '水洗' && option.value === '水洗'),
    'Coffee enums processMethods did not include 水洗'
  )
  assert(
    body.data.origins.some((option) => option.label === '埃塞俄比亚' && option.value === '埃塞俄比亚'),
    'Coffee enums origins did not include 埃塞俄比亚'
  )
  assert(
    body.data.varieties.some((option) => option.label === '瑰夏' && option.value === '瑰夏'),
    'Coffee enums varieties did not include 瑰夏'
  )
  state.checks.push('Coffee enums: ok (/api/enums/coffee)')
}

async function smokeCoffee(token) {
  await requestApi('GET', '/api/coffee-beans?page=1&pageSize=10', { token })

  const createBody = await requestApi('POST', '/api/coffee-beans', {
    token,
    json: {
      name: `${smokePrefix} Coffee Bean`,
      origin: 'Ethiopia',
      region: 'Yirgacheffe',
      farm: 'Smoke Farm',
      variety: 'Heirloom',
      processMethod: 'Washed',
      roastLevel: 'Light',
      roaster: 'Smoke Roaster',
      roastDate: '2026-05-01',
      bestFromDate: '2026-05-08',
      bestToDate: '2026-06-05',
      purchaseDate: '2026-05-10',
      openDate: null,
      finishDate: null,
      netWeightGrams: 200,
      price: 88,
      currency: 'CNY',
      status: 'UNOPENED',
      coverImageUrl: null,
      notes: `${smokePrefix} coffee create`,
    },
  })

  state.coffeeId = createBody.data?.id
  assert(Number.isInteger(state.coffeeId) && state.coffeeId > 0, 'Coffee create did not return id')

  const detailBody = await requestApi('GET', `/api/coffee-beans/${state.coffeeId}`, { token })
  assert(detailBody.data?.name === `${smokePrefix} Coffee Bean`, 'Coffee detail name mismatch')
  assert(detailBody.data?.variety === 'Heirloom', 'Coffee detail variety mismatch after create')
  assert(detailBody.data?.bestFromDate === '2026-05-08', 'Coffee detail bestFromDate mismatch')
  assert(detailBody.data?.bestToDate === '2026-06-05', 'Coffee detail bestToDate mismatch')
  await assertCoffeeListVariety(token, 'Heirloom', 'after create')

  const updateBody = await requestApi('PUT', `/api/coffee-beans/${state.coffeeId}`, {
    token,
    json: buildCoffeeUpdatePayload(null),
  })
  assert(updateBody.data === true, 'Coffee update did not return true')
  const updatedDetailBody = await requestApi('GET', `/api/coffee-beans/${state.coffeeId}`, { token })
  assert(updatedDetailBody.data?.variety === 'Updated Heirloom', 'Coffee detail variety mismatch after update')
  await assertCoffeeListVariety(token, 'Updated Heirloom', 'after update')

  state.checks.push(`Coffee list/create/detail/update/variety: ok (id=${state.coffeeId})`)
}

async function smokeDrinkStatusFilters(token) {
  const origin = `${smokePrefix} Drink Status Origin`
  const samples = [
    {
      status: 'NO_DATE',
      name: `${smokePrefix} NO_DATE Coffee Bean`,
      roastLevel: 'Smoke No Date Roast',
      processMethod: 'Smoke No Date Process',
      bestFromDate: null,
      bestToDate: null,
    },
    {
      status: 'RESTING',
      name: `${smokePrefix} RESTING Coffee Bean`,
      roastLevel: 'Smoke Resting Roast',
      processMethod: 'Smoke Resting Process',
      bestFromDate: localDateOffset(5),
      bestToDate: localDateOffset(20),
    },
    {
      status: 'READY',
      name: `${smokePrefix} READY Coffee Bean`,
      roastLevel: 'Smoke Ready Roast',
      processMethod: 'Smoke Ready Process',
      bestFromDate: localDateOffset(-14),
      bestToDate: localDateOffset(14),
    },
    {
      status: 'EXPIRING_SOON',
      name: `${smokePrefix} EXPIRING_SOON Coffee Bean`,
      roastLevel: 'Smoke Expiring Soon Roast',
      processMethod: 'Smoke Expiring Soon Process',
      bestFromDate: localDateOffset(-14),
      bestToDate: localDateOffset(5),
    },
    {
      status: 'EXPIRED',
      name: `${smokePrefix} EXPIRED Coffee Bean`,
      roastLevel: 'Smoke Expired Roast',
      processMethod: 'Smoke Expired Process',
      bestFromDate: localDateOffset(-30),
      bestToDate: localDateOffset(-5),
    },
  ]

  for (const sample of samples) {
    const createBody = await requestApi('POST', '/api/coffee-beans', {
      token,
      json: {
        name: sample.name,
        origin,
        region: 'Drink Status Smoke',
        farm: 'Smoke Farm',
        variety: 'Smoke Variety',
        processMethod: sample.processMethod,
        roastLevel: sample.roastLevel,
        roaster: 'Smoke Drink Status Roaster',
        roastDate: localDateOffset(-35),
        bestFromDate: sample.bestFromDate,
        bestToDate: sample.bestToDate,
        purchaseDate: localDateOffset(-36),
        openDate: null,
        finishDate: null,
        netWeightGrams: 100,
        price: 10,
        currency: 'CNY',
        status: 'UNOPENED',
        coverImageUrl: null,
        notes: `${smokePrefix} drink status ${sample.status}`,
      },
    })

    const id = createBody.data?.id
    assert(Number.isInteger(id) && id > 0, `${sample.status} coffee create did not return id`)
    state.drinkStatusSamples.push({ ...sample, id, origin, deleted: false })
  }

  for (const sample of state.drinkStatusSamples) {
    const query = buildQueryString({
      page: 1,
      pageSize: 10,
      origin,
      drinkStatus: sample.status,
    })
    const page = await requestApi('GET', `/api/coffee-beans?${query}`, { token })
    assertDrinkStatusPage(page, sample, state.drinkStatusSamples, `${sample.status} drinkStatus`)
  }

  const readySample = state.drinkStatusSamples.find((sample) => sample.status === 'READY')
  assert(readySample, 'READY smoke sample was not created')
  const combinedQuery = buildQueryString({
    page: 1,
    pageSize: 10,
    origin,
    roastLevel: readySample.roastLevel,
    processMethod: readySample.processMethod,
    drinkStatus: readySample.status,
  })
  const combinedPage = await requestApi('GET', `/api/coffee-beans?${combinedQuery}`, { token })
  assertDrinkStatusPage(combinedPage, readySample, state.drinkStatusSamples, 'Combined READY filters')

  const summary = state.drinkStatusSamples
    .map((sample) => `${sample.status}=${sample.id}`)
    .join(', ')
  state.checks.push(`Coffee drinkStatus filters/combined filters: ok (${summary})`)
}

async function smokeFileUpload(token) {
  assert(state.coffeeId, 'Cannot upload cover before coffee is created')

  const formData = new FormData()
  const imageBytes = Buffer.from(PNG_BASE64, 'base64')
  const imageBlob = new Blob([imageBytes], { type: 'image/png' })
  formData.append('file', imageBlob, `${smokePrefix.replaceAll(/[^a-zA-Z0-9_-]/g, '_')}.png`)

  const body = await requestApi('POST', '/api/files/coffee-cover', {
    token,
    body: formData,
  })

  state.coverUrl = body.data?.url
  assert(
    typeof state.coverUrl === 'string' && state.coverUrl.startsWith('/uploads/coffee-covers/'),
    'Cover upload url does not match /uploads/coffee-covers/{filename}'
  )

  const staticResponse = await fetch(joinUrl(config.backendUrl, state.coverUrl))
  assert(staticResponse.ok, `Backend static cover returned HTTP ${staticResponse.status}`)
  assert(
    staticResponse.headers.get('content-type')?.startsWith('image/'),
    'Backend static cover content-type is not image/*'
  )

  if (!config.skipFrontend) {
    try {
      const proxyStaticResponse = await fetch(joinUrl(config.frontendUrl, state.coverUrl))
      assert(
        proxyStaticResponse.ok,
        `Frontend /uploads proxy returned HTTP ${proxyStaticResponse.status}`
      )
      state.checks.push('Vite proxy /uploads: ok')
    } catch (error) {
      state.warnings.push(`Frontend /uploads optional check skipped/failed: ${error.message}`)
    }
  }

  const updateBody = await requestApi('PUT', `/api/coffee-beans/${state.coffeeId}`, {
    token,
    json: buildCoffeeUpdatePayload(state.coverUrl),
  })
  assert(updateBody.data === true, 'Coffee cover update did not return true')

  state.checks.push(`File upload/static access: ok (${state.coverUrl})`)
}

async function smokeReview(token) {
  assert(state.coffeeId, 'Cannot smoke review before coffee is created')

  await requestApi('GET', `/api/coffee-beans/${state.coffeeId}/reviews?page=1&pageSize=10`, {
    token,
  })

  const createBody = await requestApi('POST', `/api/coffee-beans/${state.coffeeId}/reviews`, {
    token,
    json: {
      overallRating: 4.5,
      aromaRating: 4.0,
      acidityRating: 4.0,
      sweetnessRating: 4.5,
      bitternessRating: 1.5,
      bodyRating: 3.5,
      aftertasteRating: 4.0,
      content: `${smokePrefix} review create`,
    },
  })

  state.reviewId = createBody.data?.id
  assert(Number.isInteger(state.reviewId) && state.reviewId > 0, 'Review create did not return id')
  await assertCoffeeAggregates(token, { reviewCount: 1, overallRating: 4.5 }, 'after first review create')

  const secondCreateBody = await requestApi('POST', `/api/coffee-beans/${state.coffeeId}/reviews`, {
    token,
    json: {
      overallRating: 3.5,
      aromaRating: 3.5,
      acidityRating: 3.0,
      sweetnessRating: 3.5,
      bitternessRating: 2.0,
      bodyRating: 3.0,
      aftertasteRating: 3.5,
      content: `${smokePrefix} review create second`,
    },
  })

  state.secondReviewId = secondCreateBody.data?.id
  assert(
    Number.isInteger(state.secondReviewId) && state.secondReviewId > 0,
    'Second review create did not return id'
  )
  await assertCoffeeAggregates(token, { reviewCount: 2, overallRating: 4.0 }, 'after second review create')

  await requestApi('GET', `/api/reviews/${state.reviewId}`, { token })

  const updateBody = await requestApi('PUT', `/api/reviews/${state.reviewId}`, {
    token,
    json: {
      overallRating: 4.0,
      aromaRating: 4.0,
      acidityRating: 3.5,
      sweetnessRating: 4.0,
      bitternessRating: 1.5,
      bodyRating: 3.5,
      aftertasteRating: 4.0,
      content: `${smokePrefix} review update`,
    },
  })
  assert(updateBody.data === true, 'Review update did not return true')
  await assertCoffeeAggregates(token, { reviewCount: 2, overallRating: 3.8 }, 'after review update')

  const deleteSecondBody = await requestApi('DELETE', `/api/reviews/${state.secondReviewId}`, { token })
  state.deleted.secondReview = deleteSecondBody.data === true
  assert(state.deleted.secondReview, 'Second review delete did not return true')
  await assertCoffeeAggregates(token, { reviewCount: 1, overallRating: 4.0 }, 'after second review delete')

  const deleteBody = await requestApi('DELETE', `/api/reviews/${state.reviewId}`, { token })
  state.deleted.review = deleteBody.data === true
  assert(state.deleted.review, 'Review delete did not return true')
  await assertCoffeeAggregates(token, { reviewCount: 0, overallRating: null }, 'after all reviews delete')

  state.checks.push(`Review list/create/detail/update/delete/aggregates: ok (ids=${state.reviewId}, ${state.secondReviewId})`)
}

async function smokeBrew(token) {
  assert(state.coffeeId, 'Cannot smoke brew before coffee is created')

  await requestApi('GET', `/api/coffee-beans/${state.coffeeId}/brew-records?page=1&pageSize=10`, {
    token,
  })

  const createBody = await requestApi('POST', `/api/coffee-beans/${state.coffeeId}/brew-records`, {
    token,
    json: {
      brewMethod: 'V60',
      beanAmountGrams: 15,
      waterAmountMl: 225,
      ratio: '1:15',
      waterTemperature: 92,
      grindSize: 'medium fine',
      brewTimeSeconds: 150,
      resultSummary: 'balanced',
      resultNotes: `${smokePrefix} brew create`,
      isRecommended: true,
    },
  })

  state.brewId = createBody.data?.id
  assert(Number.isInteger(state.brewId) && state.brewId > 0, 'Brew create did not return id')
  await assertCoffeeAggregates(token, { brewCount: 1 }, 'after first brew create')

  const secondCreateBody = await requestApi('POST', `/api/coffee-beans/${state.coffeeId}/brew-records`, {
    token,
    json: {
      brewMethod: 'French Press',
      beanAmountGrams: 18,
      waterAmountMl: 270,
      ratio: '1:15',
      waterTemperature: 91,
      grindSize: 'coarse',
      brewTimeSeconds: 240,
      resultSummary: 'round',
      resultNotes: `${smokePrefix} brew create second`,
      isRecommended: true,
    },
  })

  state.secondBrewId = secondCreateBody.data?.id
  assert(Number.isInteger(state.secondBrewId) && state.secondBrewId > 0, 'Second brew create did not return id')
  await assertCoffeeAggregates(token, { brewCount: 2 }, 'after second brew create')

  await requestApi('GET', `/api/brew-records/${state.brewId}`, { token })

  const updateBody = await requestApi('PUT', `/api/brew-records/${state.brewId}`, {
    token,
    json: {
      brewMethod: 'V60',
      beanAmountGrams: 16,
      waterAmountMl: 240,
      ratio: '1:15',
      waterTemperature: 93,
      grindSize: 'medium',
      brewTimeSeconds: 160,
      resultSummary: 'cleaner',
      resultNotes: `${smokePrefix} brew update`,
      isRecommended: false,
    },
  })
  assert(updateBody.data === true, 'Brew update did not return true')
  await assertCoffeeAggregates(token, { brewCount: 2 }, 'after brew update')

  const deleteSecondBody = await requestApi('DELETE', `/api/brew-records/${state.secondBrewId}`, { token })
  state.deleted.secondBrew = deleteSecondBody.data === true
  assert(state.deleted.secondBrew, 'Second brew delete did not return true')
  await assertCoffeeAggregates(token, { brewCount: 1 }, 'after second brew delete')

  const deleteBody = await requestApi('DELETE', `/api/brew-records/${state.brewId}`, { token })
  state.deleted.brew = deleteBody.data === true
  assert(state.deleted.brew, 'Brew delete did not return true')
  await assertCoffeeAggregates(token, { brewCount: 0 }, 'after all brew records delete')

  state.checks.push(`Brew list/create/detail/update/delete/aggregates: ok (ids=${state.brewId}, ${state.secondBrewId})`)
}

async function cleanup(token) {
  if (!token) {
    try {
      token = await login()
    } catch {
      return
    }
  }

  if (state.reviewId && !state.deleted.review) {
    try {
      const body = await requestApi('DELETE', `/api/reviews/${state.reviewId}`, { token })
      state.deleted.review = body.data === true
    } catch (error) {
      state.warnings.push(`Review cleanup failed: ${error.message}`)
    }
  }

  if (state.secondReviewId && !state.deleted.secondReview) {
    try {
      const body = await requestApi('DELETE', `/api/reviews/${state.secondReviewId}`, { token })
      state.deleted.secondReview = body.data === true
    } catch (error) {
      state.warnings.push(`Second review cleanup failed: ${error.message}`)
    }
  }

  if (state.brewId && !state.deleted.brew) {
    try {
      const body = await requestApi('DELETE', `/api/brew-records/${state.brewId}`, { token })
      state.deleted.brew = body.data === true
    } catch (error) {
      state.warnings.push(`Brew cleanup failed: ${error.message}`)
    }
  }

  if (state.secondBrewId && !state.deleted.secondBrew) {
    try {
      const body = await requestApi('DELETE', `/api/brew-records/${state.secondBrewId}`, { token })
      state.deleted.secondBrew = body.data === true
    } catch (error) {
      state.warnings.push(`Second brew cleanup failed: ${error.message}`)
    }
  }

  for (const sample of state.drinkStatusSamples) {
    if (sample.id && !sample.deleted) {
      try {
        const body = await requestApi('DELETE', `/api/coffee-beans/${sample.id}`, { token })
        sample.deleted = body.data === true
      } catch (error) {
        state.warnings.push(`${sample.status} coffee cleanup failed: ${error.message}`)
      }
    }
  }

  if (state.coffeeId && !state.deleted.coffee) {
    try {
      const body = await requestApi('DELETE', `/api/coffee-beans/${state.coffeeId}`, { token })
      state.deleted.coffee = body.data === true
    } catch (error) {
      state.warnings.push(`Coffee cleanup failed: ${error.message}`)
    }
  }
}

async function assertCoffeeAggregates(token, expected, label) {
  const detailBody = await requestApi('GET', `/api/coffee-beans/${state.coffeeId}`, { token })
  assertAggregateFields(detailBody.data, expected, `Coffee detail aggregates ${label}`)

  const listBody = await requestApi('GET', '/api/coffee-beans?page=1&pageSize=50', { token })
  const item = listBody.data?.items?.find((coffeeBean) => coffeeBean.id === state.coffeeId)
  assert(item, `Coffee list did not include smoke bean ${label}`)
  assertAggregateFields(item, expected, `Coffee list aggregates ${label}`)
}

function assertDrinkStatusPage(page, expectedSample, allSamples, label) {
  const items = page.data?.items
  assert(Array.isArray(items), `${label}: Coffee list did not return items`)

  const ids = items.map((coffeeBean) => coffeeBean.id)
  assert(ids.includes(expectedSample.id), `${label}: expected sample was not returned`)

  for (const sample of allSamples) {
    if (sample.id !== expectedSample.id) {
      assert(!ids.includes(sample.id), `${label}: included ${sample.status} sample unexpectedly`)
    }
  }

  assertNumberEquals(page.data?.total, 1, `${label}: total`)
}

function assertAggregateFields(source, expected, label) {
  if (Object.prototype.hasOwnProperty.call(expected, 'reviewCount')) {
    assertNumberEquals(source?.reviewCount, expected.reviewCount, `${label}: reviewCount`)
  }
  if (Object.prototype.hasOwnProperty.call(expected, 'overallRating')) {
    if (expected.overallRating === null) {
      assert(source?.overallRating === null, `${label}: overallRating expected null, got ${source?.overallRating}`)
    } else {
      assertNumberEquals(source?.overallRating, expected.overallRating, `${label}: overallRating`)
    }
  }
  if (Object.prototype.hasOwnProperty.call(expected, 'brewCount')) {
    assertNumberEquals(source?.brewCount, expected.brewCount, `${label}: brewCount`)
  }
}

async function assertCoffeeListVariety(token, expectedVariety, label) {
  const query = buildQueryString({
    page: 1,
    pageSize: 50,
    keyword: smokePrefix,
  })
  const listBody = await requestApi('GET', `/api/coffee-beans?${query}`, { token })
  const item = listBody.data?.items?.find((coffeeBean) => coffeeBean.id === state.coffeeId)
  assert(item, `Coffee list did not include smoke bean ${label}`)
  assert(item.variety === expectedVariety, `Coffee list variety mismatch ${label}`)
}

function assertNumberEquals(actual, expected, label) {
  const actualNumber = Number(actual)
  assert(
    Number.isFinite(actualNumber) && Math.abs(actualNumber - expected) < 0.0001,
    `${label} expected ${expected}, got ${actual}`
  )
}

function formatDrinkStatusSampleIds() {
  if (state.drinkStatusSamples.length === 0) {
    return 'not created'
  }

  return state.drinkStatusSamples
    .map((sample) => `${sample.status}=${sample.id}`)
    .join(', ')
}

function formatDrinkStatusSampleDeletes() {
  if (state.drinkStatusSamples.length === 0) {
    return 'no samples'
  }

  return state.drinkStatusSamples.every((sample) => sample.deleted) ? 'yes' : 'partial/no'
}

async function requestApi(method, path, options = {}) {
  const headers = {}
  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`
  }

  let body = options.body
  if (options.json !== undefined) {
    headers['Content-Type'] = 'application/json; charset=utf-8'
    body = JSON.stringify(options.json)
  }

  const response = await fetch(joinUrl(options.baseUrl ?? config.backendUrl, path), {
    method,
    headers,
    body,
  })
  const responseText = await response.text()
  let parsed
  try {
    parsed = responseText ? JSON.parse(responseText) : null
  } catch {
    throw new Error(`${method} ${path} returned non-JSON response: ${responseText.slice(0, 160)}`)
  }

  if (!response.ok) {
    throw new Error(`${method} ${path} returned HTTP ${response.status}: ${responseText}`)
  }
  if ((options.expectApiEnvelope ?? true) && parsed?.code !== 0) {
    throw new Error(`${method} ${path} returned API error: ${responseText}`)
  }
  return parsed
}

function buildCoffeeUpdatePayload(coverImageUrl) {
  return {
    name: `${smokePrefix} Coffee Bean Updated`,
    origin: 'Ethiopia',
    region: 'Yirgacheffe',
    farm: 'Smoke Farm',
    variety: 'Updated Heirloom',
    processMethod: 'Natural',
    roastLevel: 'Medium',
    roaster: 'Smoke Roaster',
    roastDate: '2026-05-01',
    bestFromDate: '2026-05-08',
    bestToDate: '2026-06-05',
    purchaseDate: '2026-05-10',
    openDate: '2026-05-12',
    finishDate: null,
    netWeightGrams: 200,
    price: 88,
    currency: 'CNY',
    status: 'OPENED',
    coverImageUrl,
    notes: `${smokePrefix} coffee update`,
  }
}

function printSummary(status, error) {
  console.log('')
  console.log(`SMOKE_TEST_RESULT: ${status}`)
  if (error) {
    console.log(`Error: ${error.message}`)
  }
  console.log('')
  console.log('Checks:')
  for (const check of state.checks) {
    console.log(`- ${check}`)
  }
  if (state.warnings.length > 0) {
    console.log('')
    console.log('Warnings:')
    for (const warning of state.warnings) {
      console.log(`- ${warning}`)
    }
  }
  console.log('')
  console.log('Created test data:')
  console.log(`- Coffee bean id: ${state.coffeeId ?? 'not created'}`)
  console.log(`- Review id: ${state.reviewId ?? 'not created'}`)
  console.log(`- Second review id: ${state.secondReviewId ?? 'not created'}`)
  console.log(`- Brew record id: ${state.brewId ?? 'not created'}`)
  console.log(`- Second brew record id: ${state.secondBrewId ?? 'not created'}`)
  console.log(`- Drink status coffee ids: ${formatDrinkStatusSampleIds()}`)
  console.log(`- Cover url: ${state.coverUrl ?? 'not uploaded'}`)
  console.log('')
  console.log('Delete calls executed:')
  console.log(`- Coffee bean delete: ${state.deleted.coffee ? 'yes' : 'no'}`)
  console.log(`- Review delete: ${state.deleted.review ? 'yes' : 'no'}`)
  console.log(`- Second review delete: ${state.deleted.secondReview ? 'yes' : 'no'}`)
  console.log(`- Brew record delete: ${state.deleted.brew ? 'yes' : 'no'}`)
  console.log(`- Second brew record delete: ${state.deleted.secondBrew ? 'yes' : 'no'}`)
  console.log(`- Drink status coffee delete: ${formatDrinkStatusSampleDeletes()}`)
  console.log('')
  console.log('Side effects:')
  console.log('- Delete endpoints are logical deletes; smoke records may remain in MySQL with deleted=1.')
  console.log('- Uploaded smoke cover files may remain in the local FILE_UPLOAD_PATH directory.')
  console.log('- This script is for local MVP smoke checks only, not CI or production validation.')
}

function readConfig() {
  const args = parseArgs(process.argv.slice(2))
  return {
    backendUrl: normalizeBaseUrl(args.backendUrl ?? process.env.SMOKE_BACKEND_URL ?? DEFAULTS.backendUrl),
    frontendUrl: normalizeBaseUrl(args.frontendUrl ?? process.env.SMOKE_FRONTEND_URL ?? DEFAULTS.frontendUrl),
    username: args.username ?? process.env.SMOKE_USERNAME ?? DEFAULTS.username,
    password: args.password ?? process.env.SMOKE_PASSWORD ?? DEFAULTS.password,
    skipFrontend:
      args.skipFrontend === true || String(process.env.SMOKE_SKIP_FRONTEND).toLowerCase() === 'true',
  }
}

function parseArgs(args) {
  const parsed = {}
  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index]
    if (arg === '--backend-url') {
      parsed.backendUrl = args[++index]
    } else if (arg === '--frontend-url') {
      parsed.frontendUrl = args[++index]
    } else if (arg === '--username') {
      parsed.username = args[++index]
    } else if (arg === '--password') {
      parsed.password = args[++index]
    } else if (arg === '--skip-frontend') {
      parsed.skipFrontend = true
    } else if (arg === '--help' || arg === '-h') {
      printHelpAndExit()
    } else {
      throw new Error(`Unknown argument: ${arg}`)
    }
  }
  return parsed
}

function printHelpAndExit() {
  console.log(`Usage: node scripts/local-smoke.mjs [options]

Options:
  --backend-url <url>   Backend base URL. Default: ${DEFAULTS.backendUrl}
  --frontend-url <url>  Frontend base URL. Default: ${DEFAULTS.frontendUrl}
  --username <name>     Login username. Default: ${DEFAULTS.username}
  --password <value>    Login password. Default: ${DEFAULTS.password}
  --skip-frontend       Skip optional frontend/Vite proxy checks.

Environment overrides:
  SMOKE_BACKEND_URL, SMOKE_FRONTEND_URL, SMOKE_USERNAME, SMOKE_PASSWORD, SMOKE_SKIP_FRONTEND
`)
  process.exit(0)
}

function normalizeBaseUrl(value) {
  return String(value).replace(/\/+$/, '')
}

function joinUrl(baseUrl, path) {
  return `${normalizeBaseUrl(baseUrl)}${path.startsWith('/') ? path : `/${path}`}`
}

function buildQueryString(params) {
  const searchParams = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) {
      searchParams.set(key, String(value))
    }
  })
  return searchParams.toString()
}

function localDateOffset(days) {
  const today = new Date()
  const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() + days)
  return formatLocalDate(date)
}

function formatLocalDate(date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-')
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}
