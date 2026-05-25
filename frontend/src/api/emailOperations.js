import apiClient from './client'

const ENDPOINT = '/api/v1/admin/email-operations'

export async function fetchStatusSummary() {
  const { data } = await apiClient.get(`${ENDPOINT}/status-summary`)
  return data
}

export async function fetchActionRequiredItems(params = {}) {
  const { data } = await apiClient.get(`${ENDPOINT}/action-required`, { params })
  return data
}

export async function fetchStaleReceivedItems(params = {}) {
  const { data } = await apiClient.get(`${ENDPOINT}/stale-received`, { params })
  return data
}
