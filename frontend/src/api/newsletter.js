// 뉴스레터 도메인 API 호출.
//
// 카탈로그 (비로그인 OK):
//   - fetchNewsletters: GET /api/v1/newsletters?category=...&page=...&size=...
//   - fetchCategories:  GET /api/v1/newsletters/categories
//
// 회원-뉴스레터 구독 (로그인 필수, /api/v1/me/newsletter-subscriptions/{id}):
//   - fetchSubscriptionInfo: GET — { status, externalSubscribeUrl }
//       status=NONE이면 externalSubscribeUrl 채워짐, 그 외엔 null
//   - resubscribe:           PATCH — UNSUBSCRIBED → ACTIVE, 204
//   - 처음 구독은 별도 API 없음 — fetchSubscriptionInfo에서 받은 externalSubscribeUrl로 외부 이동
//   - 구독 해지(DELETE)는 다음 사이클 (티켓정제 03)
//
// 회원 뉴스레터 이슈 (로그인 필수, /api/v1/me/newsletter-issues):
//   - fetchTodayIssues: GET /today — 오늘 도착한 이슈 목록
//   - fetchIssues:      GET        — 보관함 전체 이슈 목록
//   - fetchIssueDetail: GET /{id}  — 이슈 상세 + 읽음 처리
//   - deleteIssue:      DELETE /{id}
//   - createDemoIssues: POST /demo — dev 환경 데모 이슈 생성
import apiClient, { ensureCsrfToken } from './client'

const SUBSCRIPTION_BASE = '/api/v1/me/newsletter-subscriptions'
const ISSUE_BASE = '/api/v1/me/newsletter-issues'

export async function fetchNewsletters({ category, page = 0, size = 20 } = {}) {
  const params = { page, size }
  // 'ALL'은 프론트 표시용. 백엔드엔 보내지 않음.
  if (category && category !== 'ALL') {
    params.category = category
  }
  const { data } = await apiClient.get('/api/v1/newsletters', { params })
  return data
}

export async function fetchCategories() {
  const { data } = await apiClient.get('/api/v1/newsletters/categories')
  return data
}

export async function fetchSubscriptionInfo(newsletterId) {
  const { data } = await apiClient.get(`${SUBSCRIPTION_BASE}/${newsletterId}`)
  return data
}

export async function resubscribe(newsletterId) {
  await ensureCsrfToken()
  await apiClient.patch(`${SUBSCRIPTION_BASE}/${newsletterId}`)
}

export async function fetchTodayIssues({ page = 0, size = 20 } = {}) {
  const { data } = await apiClient.get(`${ISSUE_BASE}/today`, {
    params: { page, size },
  })
  return data
}

export async function fetchIssues({ keyword, page = 0, size = 20 } = {}) {
  const params = { page, size }
  const trimmedKeyword = keyword?.trim()
  if (trimmedKeyword) {
    params.keyword = trimmedKeyword
  }
  const { data } = await apiClient.get(ISSUE_BASE, { params })
  return data
}

export async function fetchIssueDetail(issueId) {
  const { data } = await apiClient.get(`${ISSUE_BASE}/${issueId}`)
  return data
}

export async function deleteIssue(issueId) {
  await ensureCsrfToken()
  await apiClient.delete(`${ISSUE_BASE}/${issueId}`)
}

export async function createDemoIssues() {
  await ensureCsrfToken()
  const { data } = await apiClient.post(`${ISSUE_BASE}/demo`)
  return data
}
