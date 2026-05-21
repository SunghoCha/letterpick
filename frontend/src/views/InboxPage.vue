<script>
import * as newsletterApi from '@/api/newsletter'
import DevIssueDemoButton from '@/components/DevIssueDemoButton.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const PAGE_SIZE = 20

export default {
  name: 'InboxPage',
  components: {
    DevIssueDemoButton,
  },
  data() {
    return {
      issues: [],
      nextPage: 0,
      hasNext: true,
      loading: false,
      loaded: false,
      keyword: '',
      listKey: 0,
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
  },
  methods: {
    async fetchPage(pageNum) {
      if (this.loading) return
      this.loading = true
      try {
        const data = await newsletterApi.fetchIssues({
          keyword: this.keyword,
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
        this.toastStore.error('보관함 이슈를 불러오지 못했습니다.')
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
    resetList() {
      this.issues = []
      this.nextPage = 0
      this.hasNext = true
      this.loaded = false
      this.listKey += 1
    },
    applySearch() {
      this.resetList()
    },
    clearSearch() {
      this.keyword = ''
      this.resetList()
    },
    onItemClick(issue) {
      this.$router.push({
        name: 'issue-detail',
        params: { issueId: issue.issueId },
      })
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
    <header class="page-header mb-6">
      <div>
        <h1 class="text-h5 font-weight-bold mb-2">보관함</h1>
        <p class="text-body-2 text-medium-emphasis">
          내가 구독한 뉴스레터에서 도착한 전체 이슈를 최신순으로 모아 봅니다.
        </p>
      </div>
      <DevIssueDemoButton @created="resetList" />
    </header>

    <!-- 비로그인: 로그인 안내 카드 -->
    <v-sheet
      v-if="!isLoggedIn"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-icon size="48" class="mb-3 text-medium-emphasis">mdi-lock-outline</v-icon>
      <div class="text-body-1 font-weight-medium mb-2">로그인이 필요합니다</div>
      <div class="text-body-2 text-medium-emphasis mb-4">
        보관함은 회원 본인의 이슈 목록이라 로그인 후 확인할 수 있어요.
      </div>
      <v-btn color="primary" @click="goToLogin">로그인하기</v-btn>
    </v-sheet>

    <template v-else>
      <div class="d-flex ga-2 mb-4">
        <v-text-field
          v-model="keyword"
          prepend-inner-icon="mdi-magnify"
          placeholder="제목·본문 검색"
          variant="outlined"
          density="compact"
          hide-details
          clearable
          @keydown.enter="applySearch"
          @click:clear="clearSearch"
        />
        <v-btn
          color="primary"
          variant="flat"
          height="40"
          @click="applySearch"
        >
          검색
        </v-btn>
      </div>

      <!-- 로그인 + 빈 보관함 -->
      <v-sheet
        v-if="isEmpty"
        class="pa-12 text-center"
        color="transparent"
      >
        <v-icon size="48" class="mb-3 text-medium-emphasis">mdi-inbox-outline</v-icon>
        <div class="text-body-1 font-weight-medium mb-2">아직 보관된 이슈가 없어요</div>
        <div class="text-body-2 text-medium-emphasis mb-4">
          뉴스레터를 구독하고 메일이 도착하면 이곳에 쌓입니다.
        </div>
        <v-btn color="primary" @click="goToNewsletters">뉴스레터 둘러보기</v-btn>
      </v-sheet>

      <!-- 로그인 + 전체 이슈 목록 -->
      <v-infinite-scroll
        v-else
        :key="listKey"
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
              <v-avatar size="40" rounded="md" class="mr-2">
                <v-img :src="issue.newsletterImageUrl" :alt="issue.newsletterName">
                  <template #error>
                    <div class="image-fallback">
                      {{ issue.newsletterName.slice(0, 2) }}
                    </div>
                  </template>
                </v-img>
              </v-avatar>
            </template>

            <div class="d-flex align-center mb-1">
              <span class="text-caption text-medium-emphasis mr-2">
                {{ issue.newsletterName }}
              </span>
              <span class="text-caption text-medium-emphasis">
                · {{ formatDateTime(issue.receivedAt) }}
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
    </template>
  </v-container>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.issue-scroll {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  overflow: hidden;
}

.issue-list {
  background: transparent;
}

.issue-item {
  cursor: pointer;
  transition: background-color 0.15s;
  padding: 12px 16px;
}

.issue-item:hover {
  background: #fafafa;
}

.issue-item--read :deep(.text-subtitle-2),
.issue-item--read .issue-summary {
  color: rgba(0, 0, 0, 0.55);
}

.issue-summary {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.image-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #757575;
  font-weight: 600;
  font-size: 13px;
}

@media (max-width: 600px) {
  .page-header {
    flex-direction: column;
  }
}
</style>
