import dayjs from 'dayjs'

const DEFAULT_FORMAT = 'YYYY-MM-DD HH:mm'

export function formatDateTime(value, pattern = DEFAULT_FORMAT, fallback = '-') {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  const d = dayjs(value)
  if (!d.isValid()) {
    return fallback
  }
  return d.format(pattern)
}

export function formatDate(value, pattern = 'YYYY-MM-DD', fallback = '-') {
  return formatDateTime(value, pattern, fallback)
}
