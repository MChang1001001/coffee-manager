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
  brewId: null,
  coverUrl: null,
  deleted: {
    coffee: false,
    review: false,
    brew: false,
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
  await smokeCoffee(token)
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

  const updateBody = await requestApi('PUT', `/api/coffee-beans/${state.coffeeId}`, {
    token,
    json: buildCoffeeUpdatePayload(null),
  })
  assert(updateBody.data === true, 'Coffee update did not return true')

  state.checks.push(`Coffee list/create/detail/update: ok (id=${state.coffeeId})`)
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

  state.checks.push(`Review list/create/detail/update: ok (id=${state.reviewId})`)
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

  state.checks.push(`Brew list/create/detail/update: ok (id=${state.brewId})`)
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

  if (state.brewId && !state.deleted.brew) {
    try {
      const body = await requestApi('DELETE', `/api/brew-records/${state.brewId}`, { token })
      state.deleted.brew = body.data === true
    } catch (error) {
      state.warnings.push(`Brew cleanup failed: ${error.message}`)
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
    variety: 'Heirloom',
    processMethod: 'Natural',
    roastLevel: 'Medium',
    roaster: 'Smoke Roaster',
    roastDate: '2026-05-01',
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
  console.log(`- Brew record id: ${state.brewId ?? 'not created'}`)
  console.log(`- Cover url: ${state.coverUrl ?? 'not uploaded'}`)
  console.log('')
  console.log('Delete calls executed:')
  console.log(`- Coffee bean delete: ${state.deleted.coffee ? 'yes' : 'no'}`)
  console.log(`- Review delete: ${state.deleted.review ? 'yes' : 'no'}`)
  console.log(`- Brew record delete: ${state.deleted.brew ? 'yes' : 'no'}`)
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

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}
