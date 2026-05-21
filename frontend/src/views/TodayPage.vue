<script>
import * as newsletterApi from '@/api/newsletter'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const PAGE_SIZE = 20

export default {
  name: 'TodayPage',
  data() {
    return {
      issues: [],
      nextPage: 0,
      hasNext: true,
      loading: false,
      loaded: false,
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
    isEmpty() {
      return this.loaded && this.issues.length === 0
    },
    unreadCount() {
      return this.issues.filter((i) => !i.read).length
    },
  },
  methods: {
    async fetchPage(pageNum) {
      if (this.loading) return
      this.loading = true
      try {
        const data = await newsletterApi.fetchTodayIssues({
          page: pageNum,
          size: PAGE_SIZE,
        })
        if (pageNum === 0) {
          this.issues = data.items
        } else {
          this.issues.push(...data.items)
        }
        this.hasNext = data.page.hasNext
        this.nextPage = data.page.number + 1
        this.loaded = true
      } catch {
        this.toastStore.error('오늘 이슈를 불러오지 못했습니다.')
        this.loaded = true
        this.hasNext = false
      } finally {
        this.loading = false
      }
    },
    onLoad({ done }) {
      if (!this.hasNext && this.issues.length > 0) {
        done('empty')
        return
      }
      this.fetchPage(this.nextPage).then(() => {
        done(this.hasNext ? 'ok' : 'empty')
      })
    },
    onItemClick(issue) {
      this.$router.push({
        name: 'issue-detail',
        params: { issueId: issue.issueId },
      })
    },
    // 발송 시각을 시:분 형식으로. "오늘"이라는 컨텍스트 안이라 시각만 보여줘도 충분.
    formatTime(isoDate) {
      const d = new Date(isoDate)
      const hh = String(d.getHours()).padStart(2, '0')
      const mm = String(d.getMinutes()).padStart(2, '0')
      return `${hh}:${mm}`
    },
    goToLogin() {
      this.$router.push({ name: 'login' })
    },
    goToNewsletters() {
      this.$router.push({ name: 'newsletters' })
    },
  },
}
</script>

<template>
  <v-container class="py-8" max-width="900">
    <header class="mb-6">
      <h1 class="text-h5 font-weight-bold mb-2">투데이</h1>
      <p class="text-body-2 text-medium-emphasis">
        오늘 도착한 뉴스레터 이슈를 한 곳에서 모아 봅니다.
        <span v-if="isLoggedIn && !isEmpty">
          · 미열람 <strong>{{ unreadCount }}</strong>개
        </span>
      </p>
    </header>

    <!-- 비로그인: 로그인 안내 -->
    <v-sheet
      v-if="!isLoggedIn"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-icon size="48" class="mb-3 text-medium-emphasis">mdi-lock-outline</v-icon>
      <div class="text-body-1 font-weight-medium mb-2">로그인이 필요합니다</div>
      <div class="text-body-2 text-medium-emphasis mb-4">
        투데이는 회원 본인의 이슈 목록이라 로그인 후 확인할 수 있어요.
      </div>
      <v-btn color="primary" @click="goToLogin">로그인하기</v-btn>
    </v-sheet>

    <!-- 로그인 + 빈 상태 -->
    <v-sheet
      v-else-if="isEmpty"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-icon size="48" class="mb-3 text-medium-emphasis">mdi-email-outline</v-icon>
      <div class="text-body-1 font-weight-medium mb-2">오늘 도착한 이슈가 없어요</div>
      <div class="text-body-2 text-medium-emphasis mb-4">
        뉴스레터를 더 구독하면 매일 새로운 이슈를 받아볼 수 있어요.
      </div>
      <v-btn color="primary" @click="goToNewsletters">뉴스레터 둘러보기</v-btn>
    </v-sheet>

    <!-- 로그인 + 이슈 리스트 -->
    <v-infinite-scroll
      v-else
      mode="intersect"
      empty-text=""
      class="issue-scroll"
      @load="onLoad"
    >
      <v-list class="issue-list pa-0" lines="three">
        <v-list-item
          v-for="(issue, index) in issues"
          :key="issue.issueId"
          :class="['issue-item', { 'issue-item--read': issue.read }]"
          :border="index < issues.length - 1 ? 'b' : ''"
          @click="onItemClick(issue)"
        >
          <template #prepend>
            <!-- 미열람 표시: 좌측 dot -->
            <div class="unread-dot-wrap">
              <span v-if="!issue.read" class="unread-dot" />
            </div>
          </template>

          <div class="d-flex align-center mb-1">
            <span class="text-caption text-medium-emphasis mr-2">
              {{ issue.newsletterName }}
            </span>
            <span class="text-caption text-medium-emphasis">
              · {{ formatTime(issue.receivedAt) }}
            </span>
          </div>
          <div :class="['text-subtitle-2', issue.read ? 'font-weight-regular' : 'font-weight-bold']">
            {{ issue.subject }}
          </div>
          <div class="text-body-2 text-medium-emphasis issue-summary">
            {{ issue.previewText }}
          </div>
        </v-list-item>
      </v-list>
    </v-infinite-scroll>
  </v-container>
</template>

<style scoped>
.issue-scroll,
.issue-list {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  overflow: hidden;
}

.issue-list {
  border: 0;
  border-radius: 0;
}

.issue-item {
  cursor: pointer;
  transition: background-color 0.15s;
  padding: 12px 16px;
}

.issue-item:hover {
  background: #fafafa;
}

/* 읽음 상태는 흐리게 */
.issue-item--read :deep(.text-subtitle-2),
.issue-item--read .issue-summary {
  color: rgba(0, 0, 0, 0.55);
}

.unread-dot-wrap {
  width: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgb(var(--v-theme-primary));
}

/* 요약 한 줄 ellipsis */
.issue-summary {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}
</style>
