<script>
import * as memberApi from '@/api/member'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

// 백엔드 MemberNicknameChangeRequest 제약과 동일.
// @NotBlank, @Size(min=2,max=20), @Pattern(^[가-힣a-zA-Z0-9]+$)
const NICKNAME_PATTERN = /^[가-힣a-zA-Z0-9]+$/

export default {
  name: 'MyPage',
  data() {
    return {
      // 닉네임 변경 폼.
      nicknameInput: '',
      nicknameSubmitting: false,
      nicknameError: '',
      nicknameMin: 2,
      nicknameMax: 20,
      // 탈퇴 다이얼로그.
      withdrawDialog: false,
      withdrawSubmitting: false,
    }
  },
  computed: {
    authStore() {
      return useAuthStore()
    },
    toastStore() {
      return useToastStore()
    },
    member() {
      return this.authStore.member
    },
    roleLabel() {
      if (this.member?.role === 'ADMIN') return '관리자'
      if (this.member?.role === 'USER') return '회원'
      return '-'
    },
    isNicknameValid() {
      const v = this.nicknameInput.trim()
      return (
        v.length >= this.nicknameMin
        && v.length <= this.nicknameMax
        && NICKNAME_PATTERN.test(v)
        && v !== this.member?.nickname
      )
    },
  },
  created() {
    if (this.member) this.nicknameInput = this.member.nickname
  },
  methods: {
    async submitNickname() {
      if (!this.isNicknameValid || this.nicknameSubmitting) return
      const next = this.nicknameInput.trim()
      this.nicknameSubmitting = true
      this.nicknameError = ''
      try {
        await memberApi.changeNickname(next)
        this.authStore.updateNickname(next)
        this.toastStore.success('닉네임을 변경했어요.')
      } catch (err) {
        // 401은 인터셉터가 store를 비웠고, 라우터 가드도 정합 처리하지만
        // 이 페이지는 이미 mount된 상태라 명시 redirect를 한 번 더 둔다.
        if (err.response?.status === 401) {
          this.$router.push({ name: 'login' })
          return
        }
        const message = err.response?.data?.message
        this.nicknameError = message ?? '닉네임 변경 중 오류가 발생했습니다.'
      } finally {
        this.nicknameSubmitting = false
      }
    },
    openWithdrawDialog() {
      this.withdrawDialog = true
    },
    closeWithdrawDialog() {
      if (!this.withdrawSubmitting) this.withdrawDialog = false
    },
    async submitWithdraw() {
      if (this.withdrawSubmitting) return
      this.withdrawSubmitting = true
      try {
        await memberApi.withdraw()
        this.authStore.clear()
        this.toastStore.success('탈퇴 처리되었습니다.')
        this.$router.push({ name: 'home' })
      } catch (err) {
        if (err.response?.status === 401) {
          this.$router.push({ name: 'login' })
          return
        }
        const message = err.response?.data?.message
        this.toastStore.error(message ?? '탈퇴 처리 중 오류가 발생했습니다.')
      } finally {
        this.withdrawSubmitting = false
        this.withdrawDialog = false
      }
    },
  },
}
</script>

<template>
  <v-container class="my-wrap" max-width="640">
    <h1 class="text-h5 font-weight-bold mb-6">내 정보</h1>

    <v-card v-if="member" class="pa-6 mb-6" variant="outlined">
      <v-list density="comfortable" class="pa-0">
        <v-list-item>
          <v-list-item-title class="text-medium-emphasis text-body-2">
            이메일
          </v-list-item-title>
          <v-list-item-subtitle class="text-body-1 text-high-emphasis">
            {{ member.email }}
          </v-list-item-subtitle>
        </v-list-item>
        <v-list-item>
          <v-list-item-title class="text-medium-emphasis text-body-2">
            권한
          </v-list-item-title>
          <v-list-item-subtitle class="text-body-1 text-high-emphasis">
            {{ roleLabel }}
          </v-list-item-subtitle>
        </v-list-item>
        <v-list-item>
          <v-list-item-title class="text-medium-emphasis text-body-2">
            뉴스레터 수신 주소
          </v-list-item-title>
          <v-list-item-subtitle class="text-body-1 text-high-emphasis">
            {{ member.newsletterInboxAddress }}
          </v-list-item-subtitle>
        </v-list-item>
      </v-list>
    </v-card>

    <v-card class="pa-6 mb-6" variant="outlined">
      <v-card-title class="pa-0 mb-4 text-h6">닉네임 변경</v-card-title>
      <v-form @submit.prevent="submitNickname">
        <v-text-field
          v-model="nicknameInput"
          label="닉네임"
          :counter="nicknameMax"
          :hint="`${nicknameMin}~${nicknameMax}자, 한글·영문·숫자만`"
          persistent-hint
        />
        <v-alert
          v-if="nicknameError"
          type="error"
          variant="tonal"
          class="mt-4"
        >
          {{ nicknameError }}
        </v-alert>
        <v-btn
          type="submit"
          :disabled="!isNicknameValid || nicknameSubmitting"
          :loading="nicknameSubmitting"
          color="primary"
          class="mt-4"
        >
          변경
        </v-btn>
      </v-form>
    </v-card>

    <v-card class="pa-6" variant="outlined">
      <v-card-title class="pa-0 mb-2 text-h6">회원 탈퇴</v-card-title>
      <p class="text-body-2 text-medium-emphasis mb-4">
        탈퇴하면 보관된 뉴스레터·구독 내역에 더 이상 접근할 수 없습니다.
      </p>
      <v-btn variant="outlined" color="error" @click="openWithdrawDialog">
        탈퇴하기
      </v-btn>
    </v-card>

    <v-dialog
      v-model="withdrawDialog"
      max-width="420"
      :persistent="withdrawSubmitting"
    >
      <v-card>
        <v-card-title>정말 탈퇴하시겠습니까?</v-card-title>
        <v-card-text class="text-body-2">
          이 작업은 되돌릴 수 없습니다.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            variant="text"
            :disabled="withdrawSubmitting"
            @click="closeWithdrawDialog"
          >
            취소
          </v-btn>
          <v-btn
            color="error"
            variant="elevated"
            :loading="withdrawSubmitting"
            @click="submitWithdraw"
          >
            탈퇴
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<style scoped>
.my-wrap {
  padding-top: 32px;
  padding-bottom: 64px;
}
</style>
