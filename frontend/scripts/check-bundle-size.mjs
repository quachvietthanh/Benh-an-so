import { readFile, stat } from 'node:fs/promises'
import { resolve } from 'node:path'

const budgetKb = Number(process.env.INITIAL_JS_BUDGET_KB || 900)
const indexHtml = await readFile(resolve('dist/index.html'), 'utf8')
const entryScript = indexHtml.match(/<script[^>]+src="\/?(assets\/index-[^"]+\.js)"/)

if (!entryScript) {
  throw new Error('Unable to find the Vite entry script in dist/index.html.')
}

const entrySizeKb = (await stat(resolve('dist', entryScript[1]))).size / 1024
console.log(`Initial JavaScript: ${entrySizeKb.toFixed(2)} kB (budget: ${budgetKb} kB)`)

if (entrySizeKb > budgetKb) {
  console.warn(`WARNING: Initial JavaScript exceeds the ${budgetKb} kB bundle budget.`)
}
