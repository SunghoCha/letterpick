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
    memberInitial() {
      return this.member?.nickname?.slice(0, 1) ?? 'L'
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
  watch: {
    member: {
      immediate: true,
      handler(member) {
        this.nicknameInput = member?.nickname ?? ''
      },
    },
  },
  methods: {
    async copyInboxAddress() {
      const address = this.member?.newsletterInboxAddress
      if (!address) {
        this.toastStore.error('복사할 수신 주소가 없습니다.')
        return
      }
      try {
        await navigator.clipboard.writeText(address)
        this.toastStore.success('수신 주소를 복사했어요.')
      } catch {
        this.toastStore.error('수신 주소를 복사하지 못했습니다.')
      }
    },
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
  <v-container class="my-wrap" max-width="920">
    <header class="page-header">
      <div class="page-eyebrow">계정 설정</div>
      <h1>내 정보</h1>
      <p>
        계정 정보와 뉴스레터 수신 주소를 확인하고, 표시되는 닉네임을 관리합니다.
      </p>
    </header>

    <section v-if="member" class="account-panel">
      <div class="account-main">
        <div class="member-avatar" aria-hidden="true">
          {{ memberInitial }}
        </div>
        <div class="member-copy">
          <div class="member-label">letterPick 계정</div>
          <h2>{{ member.nickname }}</h2>
          <p>{{ member.email }}</p>
        </div>
        <span class="role-pill">{{ roleLabel }}</span>
      </div>

      <div class="account-details">
        <div class="detail-row">
          <span class="detail-label">이메일</span>
          <span class="detail-value">{{ member.email }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">권한</span>
          <span class="detail-value">{{ roleLabel }}</span>
        </div>
        <div class="detail-row detail-row-address">
          <span class="detail-label">뉴스레터 수신 주소</span>
          <div class="inbox-address">
            <span>{{ member.newsletterInboxAddress }}</span>
            <v-btn
              icon="mdi-content-copy"
              variant="text"
              size="small"
              :disabled="!member.newsletterInboxAddress"
              aria-label="뉴스레터 수신 주소 복사"
              @click="copyInboxAddress"
            />
          </div>
        </div>
      </div>
    </section>

    <div v-if="member" class="settings-grid">
      <v-card class="settings-panel" elevation="0">
        <div class="panel-heading">
          <v-icon icon="mdi-account-edit-outline" size="22" />
          <div>
            <h2>닉네임 변경</h2>
            <p>서비스 안에서 표시되는 이름입니다.</p>
          </div>
        </div>

        <v-form @submit.prevent="submitNickname">
          <v-text-field
            v-model="nicknameInput"
            label="닉네임"
            variant="outlined"
            density="comfortable"
            :counter="nicknameMax"
            :hint="`${nicknameMin}~${nicknameMax}자, 한글·영문·숫자만`"
            persistent-hint
            class="nickname-field"
          />
          <v-alert
            v-if="nicknameError"
            type="error"
            variant="tonal"
            class="nickname-alert"
          >
            {{ nicknameError }}
          </v-alert>
          <v-btn
            type="submit"
            prepend-icon="mdi-check"
            :disabled="!isNicknameValid || nicknameSubmitting"
            :loading="nicknameSubmitting"
            color="primary"
            class="submit-button"
          >
            변경
          </v-btn>
        </v-form>
      </v-card>

      <v-card class="settings-panel danger-panel" elevation="0">
        <div class="panel-heading">
          <v-icon icon="mdi-account-remove-outline" size="22" />
          <div>
            <h2>회원 탈퇴</h2>
            <p>탈퇴하면 보관된 뉴스레터와 구독 내역에 접근할 수 없습니다.</p>
          </div>
        </div>

        <v-btn
          variant="outlined"
          color="error"
          prepend-icon="mdi-alert-outline"
          class="danger-button"
          @click="openWithdrawDialog"
        >
          탈퇴하기
        </v-btn>
      </v-card>
    </div>

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
  padding-top: 56px;
  padding-bottom: 64px;
}

.page-header {
  margin-bottom: 28px;
}

.page-eyebrow {
  margin-bottom: 10px;
  color: #2563eb;
  font-size: 0.875rem;
  font-weight: 700;
  letter-spacing: 0;
}

.page-header h1 {
  margin: 0 0 10px;
  color: #111827;
  font-size: 2.125rem;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.2;
}

.page-header p {
  max-width: 560px;
  margin: 0;
  color: #64748b;
  font-size: 1rem;
  line-height: 1.65;
}

.account-panel,
.settings-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.account-panel {
  margin-bottom: 24px;
  overflow: hidden;
}

.account-main {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 26px;
  border-bottom: 1px solid #eef2f7;
  background: #f8fafc;
}

.member-avatar {
  display: grid;
  flex: 0 0 auto;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 50%;
  background: #111827;
  color: #ffffff;
  font-size: 1.375rem;
  font-weight: 800;
}

.member-copy {
  min-width: 0;
}

.member-label {
  margin-bottom: 5px;
  color: #64748b;
  font-size: 0.8125rem;
  font-weight: 700;
}

.member-copy h2 {
  margin: 0 0 5px;
  color: #111827;
  font-size: 1.25rem;
  font-weight: 800;
  letter-spacing: 0;
}

.member-copy p {
  margin: 0;
  color: #64748b;
  font-size: 0.9375rem;
  overflow-wrap: anywhere;
}

.role-pill {
  flex: 0 0 auto;
  margin-left: auto;
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  padding: 6px 12px;
  background: #ffffff;
  color: #334155;
  font-size: 0.8125rem;
  font-weight: 700;
}

.account-details {
  display: grid;
  gap: 0;
}

.detail-row {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 20px;
  padding: 18px 26px;
  border-bottom: 1px solid #f1f5f9;
}

.detail-row:last-child {
  border-bottom: 0;
}

.detail-label {
  color: #64748b;
  font-size: 0.875rem;
  font-weight: 700;
}

.detail-value {
  color: #111827;
  font-size: 0.9375rem;
  overflow-wrap: anywhere;
}

.inbox-address {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  padding: 10px 12px 10px 14px;
  background: #f8fafc;
  color: #111827;
  font-size: 0.875rem;
  overflow-wrap: anywhere;
}

.inbox-address span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(260px, 0.7fr);
  gap: 24px;
}

.settings-panel {
  padding: 26px;
}

.panel-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 22px;
  color: #111827;
}

.panel-heading h2 {
  margin: 0 0 5px;
  font-size: 1.125rem;
  font-weight: 800;
  letter-spacing: 0;
}

.panel-heading p {
  margin: 0;
  color: #64748b;
  font-size: 0.875rem;
  line-height: 1.55;
}

.nickname-field {
  margin-bottom: 10px;
}

.nickname-alert {
  margin-top: 14px;
}

.submit-button,
.danger-button {
  min-height: 40px;
  border-radius: 8px;
  font-weight: 700;
  letter-spacing: 0;
}

.submit-button {
  margin-top: 18px;
}

.danger-panel {
  border-color: #fecdd3;
  background: #fffafa;
}

.danger-panel .panel-heading {
  color: #be123c;
}

@media (max-width: 760px) {
  .my-wrap {
    padding-top: 40px;
  }

  .page-header h1 {
    font-size: 1.875rem;
  }

  .account-main {
    align-items: flex-start;
    padding: 22px;
  }

  .role-pill {
    margin-left: 0;
  }

  .detail-row {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 16px 22px;
  }

  .detail-row-address {
    gap: 10px;
  }

  .settings-grid {
    grid-template-columns: 1fr;
  }

  .settings-panel {
    padding: 22px;
  }
}

@media (max-width: 480px) {
  .account-main {
    flex-wrap: wrap;
  }

  .member-avatar {
    width: 50px;
    height: 50px;
    font-size: 1.125rem;
  }

  .role-pill {
    width: fit-content;
  }

  .inbox-address {
    align-items: flex-start;
  }
}
</style>
