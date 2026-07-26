export function getErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    if (axiosErr.response?.data?.message) return axiosErr.response.data.message;
  }
  return err instanceof Error ? err.message : fallback;
}
