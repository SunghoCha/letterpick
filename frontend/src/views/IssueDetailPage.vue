<script>
import * as newsletterApi from '@/api/newsletter'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

export default {
  name: 'IssueDetailPage',
  props: {
    // route param. router에서 number로 캐스팅된 값.
    issueId: {
      type: Number,
      required: true,
    },
  },
  data() {
    return {
      issue: null,
      loading: true,
      deleting: false,
      deleteDialogOpen: false,
    }
  },
  computed: {
    authStore() {
      return useAuthStore()
    },
    toastStore() {
      return useToastStore()
    },
    isLoggedIn() {
      return this.authStore.isLoggedIn
    },
  },
  // 라우트 param이 바뀌어도 같은 컴포넌트가 재사용될 수 있으므로
  // mounted 대신 watcher + immediate로 fetch.
  watch: {
    issueId: {
      immediate: true,
      handler() {
        this.loadIssue()
      },
    },
  },
  methods: {
    async loadIssue() {
      this.loading = true
      if (!this.isLoggedIn) {
        this.issue = null
        this.loading = false
        return
      }
      try {
        this.issue = await newsletterApi.fetchIssueDetail(this.issueId)
      } catch {
        this.issue = null
        this.toastStore.error('이슈를 불러오지 못했습니다.')
      } finally {
        this.loading = false
      }
    },
    onDeleteClick() {
      this.deleteDialogOpen = true
    },
    async confirmDelete() {
      this.deleting = true
      try {
        await newsletterApi.deleteIssue(this.issueId)
        this.deleteDialogOpen = false
        this.toastStore.success('이슈를 삭제했어요.')
        this.goBack()
      } catch {
        this.toastStore.error('이슈를 삭제하지 못했습니다.')
      } finally {
        this.deleting = false
      }
    },
    goBack() {
      if (window.history.length > 1) {
        this.$router.back()
      } else {
        this.$router.push({ name: 'today' })
      }
    },
    goToLogin() {
      this.$router.push({ name: 'login' })
    },
    formatDateTime(isoDate) {
      const d = new Date(isoDate)
      const yyyy = d.getFullYear()
      const mm = String(d.getMonth() + 1).padStart(2, '0')
      const dd = String(d.getDate()).padStart(2, '0')
      const hh = String(d.getHours()).padStart(2, '0')
      const mi = String(d.getMinutes()).padStart(2, '0')
      return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
    },
  },
}
</script>

<template>
  <v-container class="py-8" max-width="800">
    <!-- 비로그인: 로그인 안내 -->
    <v-sheet
      v-if="!isLoggedIn"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-icon size="48" class="mb-3 text-medium-emphasis">mdi-lock-outline</v-icon>
      <div class="text-body-1 font-weight-medium mb-2">로그인이 필요합니다</div>
      <div class="text-body-2 text-medium-emphasis mb-4">
        이슈 상세는 회원 본인의 자료라 로그인 후 확인할 수 있어요.
      </div>
      <v-btn color="primary" @click="goToLogin">로그인하기</v-btn>
    </v-sheet>

    <!-- 로딩 -->
    <v-sheet
      v-else-if="loading"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-progress-circular indeterminate />
    </v-sheet>

    <!-- 이슈 없음 (백엔드 시점엔 404) -->
    <v-sheet
      v-else-if="!issue"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-icon size="48" class="mb-3 text-medium-emphasis">mdi-email-remove-outline</v-icon>
      <div class="text-body-1 font-weight-medium mb-2">이슈를 찾을 수 없어요</div>
      <div class="text-body-2 text-medium-emphasis mb-4">
        이미 삭제되었거나, 본인의 이슈가 아닐 수 있어요.
      </div>
      <v-btn color="primary" @click="goBack">돌아가기</v-btn>
    </v-sheet>

    <!-- 정상 -->
    <template v-else>
      <!-- 상단: 뒤로가기 + 우측 삭제 -->
      <header class="mb-4 d-flex align-center">
        <v-btn
          icon="mdi-arrow-left"
          variant="text"
          size="small"
          class="mr-2"
          @click="goBack"
        />
        <div class="flex-grow-1" />
        <v-btn
          icon="mdi-delete-outline"
          variant="text"
          size="small"
          @click="onDeleteClick"
        />
      </header>

      <!-- 메타: 뉴스레터 이름 + 발송 시각 -->
      <div class="text-caption text-medium-emphasis mb-2">
        {{ issue.newsletterName }} · {{ formatDateTime(issue.receivedAt) }}
      </div>

      <!-- 제목 -->
      <h1 class="text-h5 font-weight-bold mb-4">{{ issue.subject }}</h1>

      <v-divider class="mb-6" />

      <!-- 본문: 백엔드가 sanitize한 HTML 문자열을 v-html로 렌더 -->
      <div class="issue-contents" v-html="issue.content" />
    </template>

    <!-- 이슈 삭제 확인 다이얼로그 -->
    <v-dialog v-model="deleteDialogOpen" max-width="420">
      <v-card rounded="lg">
        <v-card-title class="text-subtitle-1 font-weight-bold">
          이슈를 삭제할까요?
        </v-card-title>
        <v-card-text class="text-body-2">
          삭제한 이슈는 보관함과 투데이에서 사라지고 복원할 수 없어요.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteDialogOpen = false">취소</v-btn>
          <v-btn
            color="error"
            variant="flat"
            :loading="deleting"
            @click="confirmDelete"
          >
            삭제
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<style scoped>
.issue-contents {
  line-height: 1.7;
  color: rgba(0, 0, 0, 0.85);
}

/* v-html로 들어온 자식 요소에 마진을 주려면 :deep 필요 (scoped 경계 통과) */
.issue-contents :deep(h2) {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 1.5rem 0 0.75rem;
}

.issue-contents :deep(p) {
  margin-bottom: 1rem;
}

.issue-contents :deep(ul) {
  margin: 0.5rem 0 1rem 1.25rem;
}

.issue-contents :deep(li) {
  margin-bottom: 0.25rem;
}

.issue-contents :deep(code) {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
}
</style>
