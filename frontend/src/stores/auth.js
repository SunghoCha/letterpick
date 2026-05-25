// 회원 인증 상태 store.
// 부팅 시 fetchMe로 세션 상태 확인 → store에 회원 정보를 채운다.
// 401 응답은 axios 응답 인터셉터가 clear()를 호출해 자동 정리한다.
//
// 백엔드 MemberResponse 모양:
//   { memberId, email, nickname, status, role, newsletterInboxAddress }
import { defineStore } from 'pinia'
import * as memberApi from '@/api/member'
import * as authApi from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    // 로그인한 회원 정보. 비로그인이면 null.
    member: null,
    // 부팅 시점의 fetchMe가 끝났는지. 라우터 가드가 비동기 부팅 도중에 잘못된 판단을 못 하도록 둔다.
    initialized: false,
  }),
  getters: {
    isLoggedIn: (state) => state.member !== null,
    isAdmin: (state) => state.member?.role === 'ADMIN',
  },
  actions: {
    // 부팅·OAuth redirect 복귀 시 호출.
    // 401(비로그인)은 정상 시나리오라 catch에서 member=null로 정리한다.
    // 인터셉터도 clear()를 호출하므로 catch 본문은 idempotent하게 둔 것.
    async fetchMe() {
      try {
        this.member = await memberApi.fetchMe()
      } catch {
        this.member = null
      } finally {
        this.initialized = true
      }
    },
    // 백엔드 logout 호출 후 클라이언트 상태 정리.
    // API 실패 여부와 무관하게 clear()는 진행한다 — 사용자 입장에서 어쨌든 로그아웃 의도.
    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.clear()
      }
    },
    updateNickname(nickname) {
      if (this.member) this.member.nickname = nickname
    },
    clear() {
      this.member = null
    },
  },
})
