export const pickFields = (source = {}, fields = []) => Object.fromEntries(
  fields
    .filter((field) => source[field] !== undefined)
    .map((field) => [field, source[field]]),
)

export const pageParams = (params = {}, extraFields = []) => {
  const filtered = pickFields(params, [...extraFields, 'page', 'size'])
  if (filtered.page !== undefined) filtered.page = Math.max(0, Number(filtered.page) || 0)
  if (filtered.size !== undefined) filtered.size = Math.min(100, Math.max(1, Number(filtered.size) || 20))
  return filtered
}
