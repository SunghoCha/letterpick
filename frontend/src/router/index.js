import { createRouter, createWebHistory } from 'vue-router'

import HomePage from '@/views/HomePage.vue'
import LoginPage from '@/views/LoginPage.vue'
import SignupPage from '@/views/SignupPage.vue'
import MyPage from '@/views/MyPage.vue'
import NewslettersPage from '@/views/NewslettersPage.vue'
import InboxPage from '@/views/InboxPage.vue'
import TodayPage from '@/views/TodayPage.vue'
import NewsletterIssuesPage from '@/views/NewsletterIssuesPage.vue'
import IssueDetailPage from '@/views/IssueDetailPage.vue'
import NotFoundPage from '@/views/NotFoundPage.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomePage,
  },
  {
    // 로그인 진입 페이지. 이미 로그인된 사용자가 들어오면 home으로 보낸다.
    path: '/login',
    name: 'login',
    component: LoginPage,
    meta: { guestOnly: true },
  },
  {
    // OAuth 콜백 후 PENDING_SIGNUP 사용자가 닉네임 입력하는 페이지.
    // ROLE_USER가 들어오면 의미 없으므로 home으로.
    // (PENDING_SIGNUP은 fetchMe가 403을 받아 store에선 비로그인으로 보임 → guestOnly 통과)
    path: '/signup',
    name: 'signup',
    component: SignupPage,
    meta: { guestOnly: true },
  },
  {
    // 내 정보 페이지: 회원 정보 표시 + 닉네임 변경 + 탈퇴.
    path: '/me',
    name: 'my',
    component: MyPage,
    meta: { requiresAuth: true },
  },
  {
    path: '/newsletters',
    name: 'newsletters',
    component: NewslettersPage,
  },
  {
    // 보관함: 회원이 구독·수신한 뉴스레터 모음.
    path: '/inbox',
    name: 'inbox',
    component: InboxPage,
    meta: { requiresAuth: true },
  },
  {
    // 투데이: 오늘 도착한 뉴스레터.
    path: '/today',
    name: 'today',
    component: TodayPage,
    meta: { requiresAuth: true },
  },
  {
    // 한 뉴스레터의 이슈 목록. 백엔드의 뉴스레터별 이슈 API가 정해지기 전까지 직접 진입만 가능하다.
    path: '/newsletters/:newsletterId/issues',
    name: 'newsletter-issues',
    component: NewsletterIssuesPage,
    // route param이 string으로 들어오므로 props로 변환할 때 number로 캐스팅.
    props: (route) => ({ newsletterId: Number(route.params.newsletterId) }),
    meta: { requiresAuth: true },
  },
  {
    // 이슈 단건 상세 (투데이·뉴스레터별 목록의 항목 클릭으로 진입).
    path: '/issues/:issueId',
    name: 'issue-detail',
    component: IssueDetailPage,
    props: (route) => ({ issueId: Number(route.params.issueId) }),
    meta: { requiresAuth: true },
  },
  // 어느 라우트와도 매칭되지 않는 모든 경로는 404로 처리한다.
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundPage,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 보호 라우트 가드.
// - meta.requiresAuth: 비로그인이면 /login으로.
// - meta.guestOnly: 이미 로그인된 사용자는 / 로.
// main.js가 mount 전에 fetchMe를 끝내므로 이 시점엔 store가 정확한 상태를 가진다.
router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    const toastStore = useToastStore()
    toastStore.info('로그인이 필요합니다.')
    return { name: 'login' }
  }
  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return { name: 'home' }
  }
  return true
})

export default router
