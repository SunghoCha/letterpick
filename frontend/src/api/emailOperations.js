import apiClient from './client'

const ENDPOINT = '/api/v1/admin/email-operations'

export async function fetchStatusSummary() {
  const { data } = await apiClient.get(`${ENDPOINT}/status-summary`)
  return data
}
