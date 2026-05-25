// Formats a backend timestamp into a readable local date and time.
export function formatDate(dateString: string) {
  const date = new Date(dateString);
  return date.toLocaleString();
}

// Safely parses backend timestamps and falls back to the current date if parsing fails.
export function parseBackendDate(dateString: string): Date {
  const timestamp = Date.parse(dateString);
  return isNaN(timestamp) ? new Date() : new Date(timestamp);
}

// Calculates how many seconds remain before a disappearing message expires.
export function getRemainingSeconds(expiresAt: string | null) {
  if (!expiresAt) return 0;

  const diff = parseBackendDate(expiresAt).getTime() - Date.now();
  return Math.max(Math.floor(diff / 1000), 0);
}
