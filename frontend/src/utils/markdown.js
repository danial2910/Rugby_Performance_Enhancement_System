/**
 * markdown.js — renderer and parsers for AI-generated plan text.
 *
 * Shared by MealPlannerView and WorkoutView, which previously carried
 * byte-identical copies of this logic.
 *
 * The renderer walks lines into discrete blocks rather than chaining global
 * regexes. The old pipeline wrapped the whole document in a single <p> *after*
 * substituting headings and list items, emitting `<p><h2>..</h2><ul>..</ul></p>`
 * — invalid nesting that browsers silently repaired, which is what made
 * paragraph spacing around headings inconsistent.
 */

const DAY_SEPARATOR = ' – '

/** Escape HTML-significant characters before any markup is introduced. */
function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

/** Apply inline bold/italic formatting to an already-escaped fragment. */
export function mdInlineFormat(s) {
  return s
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
}

function isSeparatorRow(cells) {
  return cells.length > 0 && cells.every(c => /^:?-+:?$/.test(c))
}

/** Build one <table> from a run of consecutive pipe-table lines. */
function renderTableBlock(block) {
  const rows = block.trim().split('\n').map(line =>
    line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(c => c.trim())
  )

  let headerRow = null
  let bodyRows = rows
  if (rows.length > 1 && isSeparatorRow(rows[1])) {
    headerRow = rows[0]
    bodyRows = rows.slice(2)
  }
  bodyRows = bodyRows.filter(r => !isSeparatorRow(r))

  let html = '<div class="md-table-wrap"><table class="md-table">'
  if (headerRow) {
    html += '<thead><tr>' +
      headerRow.map(c => `<th scope="col">${mdInlineFormat(c)}</th>`).join('') +
      '</tr></thead>'
  }
  html += '<tbody>' + bodyRows.map(r =>
    '<tr>' + r.map(c => `<td>${mdInlineFormat(c)}</td>`).join('') + '</tr>'
  ).join('') + '</tbody></table></div>'

  return html
}

/**
 * Render plan markdown to HTML.
 *
 * Supports ## / ### / #### headings, bold, italic, unordered lists,
 * horizontal rules, paragraphs, and pipe tables.
 */
export function renderMarkdown(text) {
  if (!text) return ''

  // Tables are lifted out first so their internal newlines never reach the
  // line walker. Placeholders contain no markdown characters.
  const tables = []
  const withTokens = escapeHtml(text).replace(/(^\|.+\|[ \t]*$\n?)+/gm, (block) => {
    tables.push(renderTableBlock(block))
    return `\n@@TABLE_${tables.length - 1}@@\n`
  })

  const out = []
  let paragraph = []
  let list = []

  const flushParagraph = () => {
    if (!paragraph.length) return
    out.push(`<p class="md-p">${paragraph.map(mdInlineFormat).join('<br>')}</p>`)
    paragraph = []
  }

  const flushList = () => {
    if (!list.length) return
    out.push(
      '<ul class="md-ul">' +
      list.map(item => `<li>${mdInlineFormat(item)}</li>`).join('') +
      '</ul>'
    )
    list = []
  }

  const flushAll = () => { flushParagraph(); flushList() }

  for (const raw of withTokens.split('\n')) {
    const line = raw.trim()

    if (!line) { flushAll(); continue }

    const tableToken = line.match(/^@@TABLE_(\d+)@@$/)
    if (tableToken) {
      flushAll()
      // Pushed, never string-replaced: String.replace would read a `$&` or
      // `$'` inside cell text as a substitution pattern.
      out.push(tables[Number(tableToken[1])])
      continue
    }

    const heading = line.match(/^(#{2,4})\s+(.+)$/)
    if (heading) {
      flushAll()
      const level = heading[1].length
      out.push(
        `<h${level} class="md-h${level}">${mdInlineFormat(heading[2].trim())}</h${level}>`
      )
      continue
    }

    if (/^---+$/.test(line)) {
      flushAll()
      out.push('<hr class="md-hr">')
      continue
    }

    const listItem = line.match(/^[-*]\s+(.+)$/)
    if (listItem) {
      flushParagraph()
      list.push(listItem[1])
      continue
    }

    flushList()
    paragraph.push(line)
  }

  flushAll()
  return out.join('')
}

/**
 * Extract one checklist key per meal from plan markdown.
 *
 * Handles the output shapes the model actually produces:
 *   "## Day 1 (Monday)"        classic format
 *   "## Monday (Pre-Season)"   day-name format
 *   "### Meal 1: Breakfast"    meal heading
 *   "**Meal 1: Breakfast**"    bold meal heading (fallback)
 */
export function extractMealKeys(text) {
  if (!text) return []

  const DAYS = 'Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday'
  const dayPattern = new RegExp(
    `^##\\s+\\**\\s*((?:Day\\s+\\d+|${DAYS})\\b[^\n]*)\\**\\s*$`, 'i'
  )

  const keys = []
  let currentDay = ''

  for (const line of text.split('\n')) {
    const dayMatch = line.match(dayPattern)
    const mealMatch = line.match(/^###\s+(.+)/) ||
                      line.match(/^\*\*(Meal\s+\d+[^*]+)\*\*\s*$/)

    if (dayMatch) {
      currentDay = dayMatch[1].replace(/\*\*/g, '').trim()
      continue
    }

    if (!mealMatch || !currentDay) continue

    const mealName = mealMatch[1].replace(/\*\*/g, '').trim()
    const lower = mealName.toLowerCase()
    if (lower.includes('summary') ||
        lower.includes('table') ||
        lower.includes('nutrition overview')) continue

    const key = `${currentDay}${DAY_SEPARATOR}${mealName}`
    if (!keys.includes(key)) keys.push(key)
  }

  return keys
}

/** Group meal keys under their day heading. */
export function groupMealKeys(keys) {
  const map = new Map()
  for (const key of keys) {
    const [day] = key.split(DAY_SEPARATOR)
    if (!map.has(day)) map.set(day, [])
    map.get(day).push(key)
  }
  return Array.from(map.entries()).map(([label, meals]) => ({ label, meals }))
}

/**
 * Parse a "Weekly Nutrition Summary" table into rows of
 * { day, calories, protein, carbs, fat, isTotal }.
 *
 * Falls back to scanning any day-shaped 5-column table when the model omits
 * the section heading.
 */
export function nutritionTableRows(text) {
  if (!text) return []

  const lines = text.split('\n')
  const rows = []

  const toCells = line =>
    line.split('|').map(c => c.replace(/\*\*/g, '').trim()).filter(Boolean)

  const isHeaderCell = first => first === 'day' || first.includes('calorie')

  let inSummarySection = false

  for (const raw of lines) {
    const line = raw.trim()

    if (/(weekly|daily)\s+nutrition\s+summ/i.test(line)) {
      inSummarySection = true
      continue
    }
    if (!inSummarySection) continue
    if (/^##\s/.test(line)) break
    if (!line.startsWith('|')) continue
    if (/^\|[\s\-:|]+\|/.test(line)) continue

    const cells = toCells(line)
    if (cells.length < 2) continue
    if (isHeaderCell(cells[0].toLowerCase())) continue

    rows.push({
      day: cells[0] || '–',
      calories: cells[1] || '–',
      protein: cells[2] || '–',
      carbs: cells[3] || '–',
      fat: cells[4] || '–',
      isTotal: /total|average|avg/i.test(cells[0])
    })
  }

  if (rows.length) return rows

  // Fallback: any table whose first column names a day or a total.
  for (const raw of lines) {
    const line = raw.trim()
    if (!line.startsWith('|')) continue
    if (/^\|[\s\-:|]+\|/.test(line)) continue

    const cells = toCells(line)
    if (cells.length < 5) continue

    const first = cells[0].toLowerCase()
    if (isHeaderCell(first)) continue
    if (!(/day\s*\d/i.test(first) ||
          /mon|tue|wed|thu|fri|sat|sun/i.test(first) ||
          /total|avg/i.test(first))) continue

    rows.push({
      day: cells[0],
      calories: cells[1],
      protein: cells[2],
      carbs: cells[3],
      fat: cells[4],
      isTotal: /total|average|avg/i.test(cells[0])
    })
  }

  return rows
}

/**
 * Parse the "Weekly Structure Summary" table into one key per exercise per day,
 * plus each day's Focus label.
 *
 * Expected shape:
 *   | Day | Focus | Volume | Key Exercises |
 *   | 1   | Lower Body Strength | High | Squats, Romanian Deadlifts |
 */
export function parseWorkoutStructure(text) {
  if (!text) return { keys: [], focusByDay: {} }

  const keys = []
  const focusByDay = {}

  let inSummary = false
  let headerDone = false
  let dayCol = -1
  let focusCol = -1
  let exCol = -1

  for (const raw of text.split('\n')) {
    const line = raw.trim()

    if (/weekly.*(structure.*summary|summary)/i.test(line)) {
      inSummary = true
      headerDone = false
      dayCol = focusCol = exCol = -1
      continue
    }
    if (!inSummary) continue
    if (line.startsWith('##') && headerDone) break
    if (!line.startsWith('|')) continue

    const cells = line.split('|').map(c => c.trim()).slice(1, -1)
    if (!cells.length) continue
    if (cells.every(c => /^[-: ]+$/.test(c))) continue

    if (!headerDone) {
      dayCol = cells.findIndex(c => /^day$/i.test(c))
      focusCol = cells.findIndex(c => /focus/i.test(c))
      exCol = cells.findIndex(c => /key\s*exercise|exercise/i.test(c))
      headerDone = true
      continue
    }

    if (dayCol < 0) continue

    // The model writes the Day column as either "1" or "Day 1"; normalise so
    // the label is never doubled up.
    const dayNum = cells[dayCol] ? cells[dayCol].replace(/^day\s*/i, '').trim() : ''
    if (!dayNum) continue

    const dayLabel = `Day ${dayNum}`
    if (focusCol >= 0) focusByDay[dayLabel] = cells[focusCol] ? cells[focusCol].trim() : ''

    if (exCol < 0 || !cells[exCol]) continue

    cells[exCol].split(',').forEach(part => {
      const clean = part.replace(/\*\*/g, '').trim()
      // Skip rest-day placeholders.
      if (!clean || /^[-–—\s]+$/.test(clean)) return
      keys.push(`${dayLabel}${DAY_SEPARATOR}${clean}`)
    })
  }

  return { keys, focusByDay }
}

/** Group exercise keys by day, attaching each day's Focus label. */
export function groupExerciseKeys(keys, focusByDay = {}) {
  const map = new Map()
  for (const key of keys) {
    const sepIdx = key.indexOf(DAY_SEPARATOR)
    const label = sepIdx >= 0 ? key.slice(0, sepIdx) : 'Exercises'
    if (!map.has(label)) map.set(label, [])
    map.get(label).push(key)
  }
  return Array.from(map.entries()).map(([label, exercises]) => ({
    label,
    focus: focusByDay[label] || '',
    exercises
  }))
}

/** Strip the day prefix, leaving just the meal or exercise name. */
export function itemNameOnly(key) {
  return key.includes(DAY_SEPARATOR)
    ? key.split(DAY_SEPARATOR).slice(1).join(DAY_SEPARATOR)
    : key
}

/**
 * Completion stats for a plan.
 *
 * Counts only logged items that still match a heading in the current plan
 * text. Plan content is user-editable, so a stale completedItems entry would
 * otherwise be counted against a smaller total and drive the percentage past
 * 100 — which rendered the progress ring with a negative dash offset.
 */
export function completionStats(completedItems, keys) {
  const total = keys.length
  const valid = new Set(keys)
  const logged = (completedItems || []).filter(key => valid.has(key))
  const done = logged.length

  return {
    total,
    done,
    orphaned: (completedItems || []).length - done,
    percent: total ? Math.min(100, Math.round((done / total) * 100)) : 0
  }
}
