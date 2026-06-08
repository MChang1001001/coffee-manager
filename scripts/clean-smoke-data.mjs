#!/usr/bin/env node

import { spawn, spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const SMOKE_MARKER = '[SMOKE_TEST]'
const APP_CONFIG_PATH = path.join('backend', 'src', 'main', 'resources', 'application.yml')
const DEFAULT_DB_URL =
  'jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true'
const DEFAULT_DB_USERNAME = 'root'
const DEFAULT_DB_PASSWORD = '123456'
const DEFAULT_UPLOAD_PATH = 'uploads'
const DEFAULT_PUBLIC_PREFIX = '/uploads'
const DEFAULT_COVER_DIRECTORY = 'coffee-covers'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

main().catch((error) => {
  console.log(`Error: ${error.message}`)
  console.log('')
  console.log('CLEAN_SMOKE_RESULT: FAIL')
  process.exitCode = 1
})

async function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
    return
  }
  if (args.dryRun && args.execute) {
    console.log('Mode: invalid')
    throw new Error('Cannot pass --dry-run and --execute at the same time.')
  }

  const mode = args.execute ? 'execute' : 'dry-run'
  const projectRoot = resolveProjectRoot()
  const appConfig = await readAppConfig(projectRoot)
  const dbConfig = resolveDatabaseConfig(appConfig)
  const storageConfig = resolveStorageConfig(projectRoot, appConfig)
  const mysqlClientPath = findMysqlClientPath()

  const report = createReport(mode, projectRoot, dbConfig, storageConfig, mysqlClientPath)

  try {
    const smokeCoffeeBeans = await loadSmokeCoffeeBeans(mysqlClientPath, dbConfig)
    report.smokeCoffeeBeans = smokeCoffeeBeans
    const smokeCoffeeBeanIds = smokeCoffeeBeans.map((coffeeBean) => coffeeBean.id)

    report.reviews = await loadAssociatedRows(
      mysqlClientPath,
      dbConfig,
      'coffee_reviews',
      smokeCoffeeBeanIds
    )
    report.brewRecords = await loadAssociatedRows(
      mysqlClientPath,
      dbConfig,
      'brew_records',
      smokeCoffeeBeanIds
    )
    report.uploads = await scanSafeSmokeUploads(smokeCoffeeBeans, storageConfig)

    printScanSummary(report)

    if (mode === 'execute') {
      report.cleaned.uploadFiles = await deleteUploadFiles(report.uploads.safeFiles)
      report.cleaned.database = await deleteSmokeRows(mysqlClientPath, dbConfig, smokeCoffeeBeanIds)
    } else {
      report.skipped.dryRunProtectedTargets =
        report.smokeCoffeeBeans.length +
        report.reviews.length +
        report.brewRecords.length +
        report.uploads.safeFiles.length
    }
  } catch (error) {
    report.errors.push(error.message)
  }

  printFinalReport(report)
  if (report.errors.length > 0) {
    process.exitCode = 1
  }
}

function parseArgs(args) {
  const parsed = {
    dryRun: false,
    execute: false,
    help: false,
  }

  for (const arg of args) {
    if (arg === '--dry-run') {
      parsed.dryRun = true
    } else if (arg === '--execute') {
      parsed.execute = true
    } else if (arg === '--help' || arg === '-h') {
      parsed.help = true
    } else {
      throw new Error(`Unknown argument: ${arg}`)
    }
  }

  return parsed
}

function printHelp() {
  console.log(`Usage: node scripts/clean-smoke-data.mjs [--dry-run|--execute]

Modes:
  default      Dry-run preview. No database rows or upload files are deleted.
  --dry-run    Explicit dry-run preview.
  --execute    Physically delete safe [SMOKE_TEST] database rows and DB-linked smoke uploads.

Environment:
  DB_URL, DB_USERNAME, DB_PASSWORD     Match backend/src/main/resources/application.yml.
  FILE_UPLOAD_PATH                     Optional upload root. Defaults follow local project docs.
  MYSQL_CLIENT_PATH                    Optional absolute path to mysql/mysql.exe.
`)
}

function resolveProjectRoot() {
  const candidates = [
    path.resolve(__dirname, '..'),
    process.cwd(),
    path.resolve(process.cwd(), '..'),
  ]

  for (const candidate of candidates) {
    if (
      existsSync(path.join(candidate, 'backend', 'pom.xml')) &&
      existsSync(path.join(candidate, 'frontend', 'package.json'))
    ) {
      return candidate
    }
  }

  throw new Error('Cannot locate Coffee Manager project root.')
}

async function readAppConfig(projectRoot) {
  const configPath = path.join(projectRoot, APP_CONFIG_PATH)
  const raw = await fs.readFile(configPath, 'utf8')
  return {
    dbUrl: resolvePlaceholder(readNestedYamlScalar(raw, ['spring', 'datasource', 'url'])),
    dbUsername: resolvePlaceholder(readNestedYamlScalar(raw, ['spring', 'datasource', 'username'])),
    dbPassword: resolvePlaceholder(readNestedYamlScalar(raw, ['spring', 'datasource', 'password'])),
    uploadPath: resolvePlaceholder(readNestedYamlScalar(raw, ['app', 'file', 'upload-path'])),
    publicPrefix: readNestedYamlScalar(raw, ['app', 'file', 'public-prefix']),
    coffeeCoverDirectory: readNestedYamlScalar(raw, [
      'app',
      'file',
      'coffee-cover-directory',
    ]),
  }
}

function readNestedYamlScalar(raw, targetPath) {
  const stack = []
  for (const line of raw.split(/\r?\n/)) {
    if (!line.trim() || line.trimStart().startsWith('#')) {
      continue
    }

    const match = line.match(/^(\s*)([A-Za-z0-9_-]+):(?:\s*(.*))?$/)
    if (!match) {
      continue
    }

    const indent = match[1].length
    const key = match[2]
    const value = match[3]?.trim()
    while (stack.length > 0 && indent <= stack[stack.length - 1].indent) {
      stack.pop()
    }
    stack.push({ indent, key })

    const currentPath = stack.map((item) => item.key)
    if (pathsEqual(currentPath, targetPath) && value !== undefined && value !== '') {
      return stripYamlQuotes(value)
    }
  }

  return undefined
}

function pathsEqual(left, right) {
  return left.length === right.length && left.every((item, index) => item === right[index])
}

function stripYamlQuotes(value) {
  const trimmed = value.trim()
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1)
  }
  return trimmed
}

function resolvePlaceholder(value) {
  if (!value) {
    return undefined
  }

  const match = value.match(/^\$\{([A-Za-z_][A-Za-z0-9_]*):(.*)\}$/)
  if (!match) {
    return value
  }

  const envName = match[1]
  const fallback = match[2]
  if (Object.prototype.hasOwnProperty.call(process.env, envName)) {
    return process.env[envName]
  }
  return fallback
}

function resolveDatabaseConfig(appConfig) {
  const jdbcUrl = appConfig.dbUrl || DEFAULT_DB_URL
  const parsedUrl = parseJdbcMysqlUrl(jdbcUrl)
  return {
    ...parsedUrl,
    jdbcUrl,
    username: appConfig.dbUsername || DEFAULT_DB_USERNAME,
    password: appConfig.dbPassword ?? DEFAULT_DB_PASSWORD,
  }
}

function parseJdbcMysqlUrl(jdbcUrl) {
  if (!jdbcUrl.startsWith('jdbc:mysql://')) {
    throw new Error(`Unsupported DB_URL. Expected jdbc:mysql://..., got: ${jdbcUrl}`)
  }

  const url = new URL(jdbcUrl.replace(/^jdbc:/, ''))
  const database = decodeURIComponent(url.pathname.replace(/^\/+/, ''))
  if (!database) {
    throw new Error('DB_URL must include a database name.')
  }

  return {
    host: url.hostname || 'localhost',
    port: url.port || '3306',
    database,
  }
}

function resolveStorageConfig(projectRoot, appConfig) {
  const publicPrefix = normalizePublicPrefix(appConfig.publicPrefix || DEFAULT_PUBLIC_PREFIX)
  const coffeeCoverDirectory = normalizePathSegment(
    appConfig.coffeeCoverDirectory || DEFAULT_COVER_DIRECTORY
  )
  const uploadPath = appConfig.uploadPath || DEFAULT_UPLOAD_PATH
  const uploadRoots = resolveUploadRoots(projectRoot, uploadPath)

  return {
    publicPrefix,
    coffeeCoverDirectory,
    uploadPath,
    uploadRoots,
  }
}

function resolveUploadRoots(projectRoot, uploadPath) {
  if (path.isAbsolute(uploadPath)) {
    return [path.normalize(uploadPath)]
  }

  const candidates = [
    path.resolve(projectRoot, uploadPath),
    path.resolve(projectRoot, 'backend', uploadPath),
  ]

  if (Object.prototype.hasOwnProperty.call(process.env, 'FILE_UPLOAD_PATH')) {
    candidates.unshift(path.resolve(process.cwd(), uploadPath))
  }

  return uniqueStrings(candidates.map((candidate) => path.normalize(candidate)))
}

function normalizePublicPrefix(value) {
  let normalized = String(value || DEFAULT_PUBLIC_PREFIX).trim()
  if (!normalized.startsWith('/')) {
    normalized = `/${normalized}`
  }
  while (normalized.length > 1 && normalized.endsWith('/')) {
    normalized = normalized.slice(0, -1)
  }
  return normalized
}

function normalizePathSegment(value) {
  let normalized = String(value || '').trim().replaceAll('\\', '/')
  while (normalized.startsWith('/')) {
    normalized = normalized.slice(1)
  }
  while (normalized.endsWith('/')) {
    normalized = normalized.slice(0, -1)
  }
  if (!normalized || normalized.includes('..') || normalized.includes('/')) {
    throw new Error(`Unsafe upload path segment: ${value}`)
  }
  return normalized
}

function findMysqlClientPath() {
  if (process.env.MYSQL_CLIENT_PATH) {
    const configuredPath = process.env.MYSQL_CLIENT_PATH
    if (!existsSync(configuredPath)) {
      throw new Error(`MYSQL_CLIENT_PATH does not exist: ${configuredPath}`)
    }
    return configuredPath
  }

  const pathCandidate = findCommandOnPath(process.platform === 'win32' ? 'mysql.exe' : 'mysql')
  if (pathCandidate) {
    return pathCandidate
  }

  const commonCandidates = [
    'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe',
    'C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe',
    'C:\\Program Files\\MariaDB 11.4\\bin\\mysql.exe',
    'C:\\Program Files\\MariaDB 10.11\\bin\\mysql.exe',
  ]

  for (const candidate of commonCandidates) {
    if (existsSync(candidate)) {
      return candidate
    }
  }

  throw new Error(
    'Cannot find mysql client. Add mysql to PATH or set MYSQL_CLIENT_PATH to mysql/mysql.exe.'
  )
}

function findCommandOnPath(commandName) {
  const lookup = process.platform === 'win32' ? 'where.exe' : 'which'
  const result = spawnSync(lookup, [commandName], {
    encoding: 'utf8',
    windowsHide: true,
  })

  if (result.status !== 0 || !result.stdout.trim()) {
    return null
  }

  return result.stdout
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && existsSync(line))
}

async function loadSmokeCoffeeBeans(mysqlClientPath, dbConfig) {
  const sql = `
SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT(
  'id', id,
  'name', name,
  'deleted', deleted,
  'coverImageUrl', cover_image_url
)), JSON_ARRAY())
FROM coffee_beans
WHERE ${smokePredicate([
    'name',
    'origin',
    'region',
    'farm',
    'variety',
    'process_method',
    'roast_level',
    'roaster',
    'notes',
  ])}
ORDER BY id;
`
  return queryJsonArray(mysqlClientPath, dbConfig, sql)
}

function smokePredicate(columns) {
  const marker = sqlString(SMOKE_MARKER)
  return columns.map((column) => `LOCATE(${marker}, COALESCE(${column}, '')) > 0`).join('\n   OR ')
}

async function loadAssociatedRows(mysqlClientPath, dbConfig, tableName, coffeeBeanIds) {
  if (coffeeBeanIds.length === 0) {
    return []
  }

  const idList = sqlNumberList(coffeeBeanIds)
  const sql = `
SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT(
  'id', id,
  'coffeeBeanId', coffee_bean_id,
  'deleted', deleted
)), JSON_ARRAY())
FROM ${tableName}
WHERE coffee_bean_id IN (${idList})
ORDER BY id;
`
  return queryJsonArray(mysqlClientPath, dbConfig, sql)
}

async function queryJsonArray(mysqlClientPath, dbConfig, sql) {
  const stdout = await runMysql(mysqlClientPath, dbConfig, sql)
  const text = stdout.trim()
  if (!text || text === 'NULL') {
    return []
  }
  const parsed = JSON.parse(text)
  return Array.isArray(parsed) ? parsed : []
}

async function deleteSmokeRows(mysqlClientPath, dbConfig, coffeeBeanIds) {
  if (coffeeBeanIds.length === 0) {
    return {
      coffeeBeans: 0,
      reviews: 0,
      brewRecords: 0,
    }
  }

  const idList = sqlNumberList(coffeeBeanIds)
  const sql = `
START TRANSACTION;
DELETE FROM coffee_reviews WHERE coffee_bean_id IN (${idList});
SET @deleted_reviews = ROW_COUNT();
DELETE FROM brew_records WHERE coffee_bean_id IN (${idList});
SET @deleted_brew_records = ROW_COUNT();
DELETE FROM coffee_beans WHERE id IN (${idList});
SET @deleted_coffee_beans = ROW_COUNT();
COMMIT;
SELECT JSON_OBJECT(
  'coffeeBeans', @deleted_coffee_beans,
  'reviews', @deleted_reviews,
  'brewRecords', @deleted_brew_records
);
`
  const stdout = await runMysql(mysqlClientPath, dbConfig, sql)
  const text = stdout.trim().split(/\r?\n/).filter(Boolean).at(-1)
  if (!text) {
    throw new Error('Database cleanup did not return affected row counts.')
  }
  return JSON.parse(text)
}

function sqlString(value) {
  return `'${String(value)
    .replaceAll('\\', '\\\\')
    .replaceAll("'", "''")}'`
}

function sqlNumberList(values) {
  return values.map((value) => {
    if (!Number.isSafeInteger(Number(value))) {
      throw new Error(`Unsafe numeric id: ${value}`)
    }
    return String(Number(value))
  }).join(', ')
}

async function runMysql(mysqlClientPath, dbConfig, sql) {
  const args = [
    '--batch',
    '--raw',
    '--silent',
    '--skip-column-names',
    '--default-character-set=utf8mb4',
    `--host=${dbConfig.host}`,
    `--port=${dbConfig.port}`,
    `--user=${dbConfig.username}`,
    `--password=${dbConfig.password ?? ''}`,
    dbConfig.database,
    '--execute',
    sql,
  ]

  return new Promise((resolve, reject) => {
    const child = spawn(mysqlClientPath, args, {
      windowsHide: true,
    })
    let stdout = ''
    let stderr = ''

    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString('utf8')
    })
    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString('utf8')
    })
    child.on('error', reject)
    child.on('close', (code) => {
      if (code !== 0) {
        reject(new Error(formatMysqlError(stderr, code)))
        return
      }
      resolve(stdout)
    })
  })
}

function formatMysqlError(stderr, code) {
  const cleaned = stderr
    .split(/\r?\n/)
    .filter((line) => !line.includes('Using a password on the command line interface can be insecure'))
    .join('\n')
    .trim()
  return cleaned || `mysql client exited with code ${code}`
}

async function scanSafeSmokeUploads(smokeCoffeeBeans, storageConfig) {
  const safeFilenames = new Set()
  const unsafeCoverUrls = []
  const missingFiles = []

  for (const coffeeBean of smokeCoffeeBeans) {
    const coverImageUrl = coffeeBean.coverImageUrl
    if (!coverImageUrl) {
      continue
    }

    const filename = extractSafeCoverFilename(coverImageUrl, storageConfig)
    if (filename) {
      safeFilenames.add(filename)
    } else {
      unsafeCoverUrls.push(coverImageUrl)
    }
  }

  const safeFiles = []
  const foundFilenames = new Set()
  const allUploadFiles = []
  const missingUploadDirectories = []

  for (const uploadRoot of storageConfig.uploadRoots) {
    const coverDirectory = path.join(uploadRoot, storageConfig.coffeeCoverDirectory)
    if (!existsSync(coverDirectory)) {
      missingUploadDirectories.push(coverDirectory)
      continue
    }

    const directoryFiles = await listRegularFiles(coverDirectory)
    allUploadFiles.push(...directoryFiles)

    for (const filename of safeFilenames) {
      const targetPath = path.normalize(path.join(coverDirectory, filename))
      if (!isPathInside(targetPath, coverDirectory)) {
        unsafeCoverUrls.push(`${storageConfig.publicPrefix}/${storageConfig.coffeeCoverDirectory}/${filename}`)
        continue
      }
      if (existsSync(targetPath)) {
        safeFiles.push(targetPath)
        foundFilenames.add(filename)
      }
    }
  }

  for (const filename of safeFilenames) {
    if (!foundFilenames.has(filename)) {
      missingFiles.push(filename)
    }
  }

  const uniqueSafeFiles = uniqueStrings(safeFiles)
  const safeFileSet = new Set(uniqueSafeFiles)
  const skippedUploadFiles = uniqueStrings(allUploadFiles).filter((filePath) => !safeFileSet.has(filePath))

  return {
    safeFiles: uniqueSafeFiles,
    skippedUploadFiles,
    missingFiles: uniqueStrings(missingFiles),
    unsafeCoverUrls: uniqueStrings(unsafeCoverUrls),
    missingUploadDirectories: uniqueStrings(missingUploadDirectories),
  }
}

function extractSafeCoverFilename(coverImageUrl, storageConfig) {
  if (typeof coverImageUrl !== 'string') {
    return null
  }

  const expectedPrefix = `${storageConfig.publicPrefix}/${storageConfig.coffeeCoverDirectory}/`
  if (!coverImageUrl.startsWith(expectedPrefix)) {
    return null
  }

  const filename = coverImageUrl.slice(expectedPrefix.length)
  if (
    !filename ||
    filename.includes('/') ||
    filename.includes('\\') ||
    filename.includes('..') ||
    path.basename(filename) !== filename
  ) {
    return null
  }

  return filename
}

async function listRegularFiles(directory) {
  const entries = await fs.readdir(directory, { withFileTypes: true })
  return entries
    .filter((entry) => entry.isFile())
    .map((entry) => path.join(directory, entry.name))
}

async function deleteUploadFiles(filePaths) {
  let deleted = 0
  for (const filePath of filePaths) {
    try {
      await fs.unlink(filePath)
      deleted += 1
    } catch (error) {
      if (error.code !== 'ENOENT') {
        throw new Error(`Failed to delete upload file ${filePath}: ${error.message}`)
      }
    }
  }
  return deleted
}

function isPathInside(targetPath, parentPath) {
  const relative = path.relative(path.resolve(parentPath), path.resolve(targetPath))
  return Boolean(relative) && !relative.startsWith('..') && !path.isAbsolute(relative)
}

function createReport(mode, projectRoot, dbConfig, storageConfig, mysqlClientPath) {
  return {
    mode,
    projectRoot,
    dbConfig,
    storageConfig,
    mysqlClientPath,
    smokeCoffeeBeans: [],
    reviews: [],
    brewRecords: [],
    uploads: {
      safeFiles: [],
      skippedUploadFiles: [],
      missingFiles: [],
      unsafeCoverUrls: [],
      missingUploadDirectories: [],
    },
    cleaned: {
      database: {
        coffeeBeans: 0,
        reviews: 0,
        brewRecords: 0,
      },
      uploadFiles: 0,
    },
    skipped: {
      dryRunProtectedTargets: 0,
    },
    errors: [],
  }
}

function printScanSummary(report) {
  console.log('Local smoke data cleanup starting...')
  console.log(`Mode: ${report.mode}`)
  console.log(`Project root: ${report.projectRoot}`)
  console.log(`Database: ${report.dbConfig.host}:${report.dbConfig.port}/${report.dbConfig.database}`)
  console.log(`mysql client: ${report.mysqlClientPath}`)
  console.log('Upload roots:')
  for (const uploadRoot of report.storageConfig.uploadRoots) {
    console.log(`- ${uploadRoot}`)
  }
  console.log('')
  console.log('Scan summary:')
  console.log(`- Smoke coffee beans: ${report.smokeCoffeeBeans.length}`)
  console.log(`  ${formatDeletedBreakdown(report.smokeCoffeeBeans)}`)
  console.log(`- Associated reviews: ${report.reviews.length}`)
  console.log(`  ${formatDeletedBreakdown(report.reviews)}`)
  console.log(`- Associated brew records: ${report.brewRecords.length}`)
  console.log(`  ${formatDeletedBreakdown(report.brewRecords)}`)
  console.log(`- Safe upload files: ${report.uploads.safeFiles.length}`)
  console.log(`- Upload files skipped as not DB-linked to smoke data: ${report.uploads.skippedUploadFiles.length}`)
  console.log(`- Smoke cover files referenced but missing: ${report.uploads.missingFiles.length}`)
  console.log(`- Smoke cover URLs skipped as unsafe/unexpected: ${report.uploads.unsafeCoverUrls.length}`)

  if (report.uploads.safeFiles.length > 0) {
    console.log('')
    console.log('Safe upload file preview:')
    for (const filePath of report.uploads.safeFiles.slice(0, 10)) {
      console.log(`- ${filePath}`)
    }
    if (report.uploads.safeFiles.length > 10) {
      console.log(`- ... ${report.uploads.safeFiles.length - 10} more`)
    }
  }

  if (report.uploads.missingUploadDirectories.length > 0) {
    console.log('')
    console.log('Missing upload directories (not fatal):')
    for (const directory of report.uploads.missingUploadDirectories) {
      console.log(`- ${directory}`)
    }
  }

  console.log('')
}

function printFinalReport(report) {
  const dbCleaned =
    report.cleaned.database.coffeeBeans +
    report.cleaned.database.reviews +
    report.cleaned.database.brewRecords
  const uploadCleaned = report.cleaned.uploadFiles
  const skipped =
    report.skipped.dryRunProtectedTargets +
    report.uploads.skippedUploadFiles.length +
    report.uploads.missingFiles.length +
    report.uploads.unsafeCoverUrls.length

  console.log('Cleanup report:')
  console.log(`- Actual cleaned coffee beans: ${report.cleaned.database.coffeeBeans}`)
  console.log(`- Actual cleaned reviews: ${report.cleaned.database.reviews}`)
  console.log(`- Actual cleaned brew records: ${report.cleaned.database.brewRecords}`)
  console.log(`- Actual cleaned upload files: ${report.cleaned.uploadFiles}`)
  console.log(`- Actual cleaned total: ${dbCleaned + uploadCleaned}`)
  console.log(`- Skipped total: ${skipped}`)
  console.log(`  Dry-run protected targets: ${report.skipped.dryRunProtectedTargets}`)
  console.log(`  Upload files not DB-linked to smoke data: ${report.uploads.skippedUploadFiles.length}`)
  console.log(`  Referenced smoke upload files missing: ${report.uploads.missingFiles.length}`)
  console.log(`  Unsafe/unexpected smoke cover URLs: ${report.uploads.unsafeCoverUrls.length}`)
  console.log(`- Error count: ${report.errors.length}`)

  if (report.errors.length > 0) {
    console.log('')
    console.log('Errors:')
    for (const error of report.errors) {
      console.log(`- ${error}`)
    }
  }

  console.log('')
  console.log(`CLEAN_SMOKE_RESULT: ${report.errors.length === 0 ? 'PASS' : 'FAIL'}`)
}

function formatDeletedBreakdown(rows) {
  const active = rows.filter((row) => Number(row.deleted) === 0).length
  const logicallyDeleted = rows.filter((row) => Number(row.deleted) === 1).length
  const other = rows.length - active - logicallyDeleted
  return `deleted=0: ${active}, deleted=1: ${logicallyDeleted}, other: ${other}`
}

function uniqueStrings(values) {
  return [...new Set(values)]
}
