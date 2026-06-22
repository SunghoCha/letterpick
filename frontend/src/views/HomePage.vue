<script>
import * as newsletterApi from '@/api/newsletter'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const PAGE_SIZE = 20
const LOAD_MORE_THRESHOLD_PX = 480
const RANKING_LIMIT = 10

export default {
  name: 'HomePage',
  data() {
    return {
      issues: [],
      nextPage: 0,
      hasNext: true,
      loading: false,
      loaded: false,
      listKey: 0,
      requestToken: 0,
      deleteDialogOpen: false,
      issueToDelete: null,
      deleting: false,
      virtualScrollElement: null,

      rankings: [],
      rankingWindowType: 'WEEKLY',
      rankingLoading: false,
      rankingLoaded: false,
      rankingRequestToken: 0,

      categories: [{ code: 'ALL', label: '전체' }],
      selectedCategory: 'ALL',
      searchKeyword: '',
      appliedKeyword: '',
    }
  },
  computed: {
    authStore() {
      return useAuthStore()
    },
    toastStore() {
      return useToastStore()
    },
    isAdmin() {
      return this.authStore.isAdmin
    },
    isEmpty() {
      return this.loaded && this.issues.length === 0
    },
    isRankingEmpty() {
      return this.rankingLoaded && this.rankings.length === 0
    },
  },
  watch: {
    selectedCategory() {
      this.reloadList()
    },
    rankingWindowType() {
      this.fetchRankings()
    },
  },
  async created() {
    await this.loadCategories()
  },
  mounted() {
    this.bindVirtualScroll()
    this.fetchRankings()
    this.fetchPage(0)
  },
  updated() {
    this.bindVirtualScroll()
  },
  beforeUnmount() {
    this.unbindVirtualScroll()
  },
  methods: {
    async loadCategories() {
      try {
        const data = await newsletterApi.fetchCategories()
        this.categories = [
          { code: 'ALL', label: '전체' },
          ...data.categories,
        ]
      } catch {
        this.toastStore.error('카테고리를 불러오지 못했습니다.')
      }
    },
    async fetchRankings() {
      this.rankingRequestToken += 1
      const requestToken = this.rankingRequestToken
      this.rankingLoading = true
      this.rankingLoaded = false
      this.rankings = []
      try {
        const data = await newsletterApi.fetchPublicIssueRankings({
          windowType: this.rankingWindowType,
          limit: RANKING_LIMIT,
        })
        if (requestToken !== this.rankingRequestToken) return

        this.rankings = data.items
        this.rankingLoaded = true
      } catch {
        if (requestToken !== this.rankingRequestToken) return

        this.rankings = []
        this.rankingLoaded = true
        this.toastStore.error('인기글을 불러오지 못했습니다.')
      } finally {
        if (requestToken === this.rankingRequestToken) {
          this.rankingLoading = false
        }
      }
    },
    async fetchPage(pageNum) {
      if (this.loading) return
      this.loading = true
      const requestToken = this.requestToken
      try {
        const data = await newsletterApi.fetchPublicIssues({
          category: this.selectedCategory,
          keyword: this.appliedKeyword,
          page: pageNum,
          size: PAGE_SIZE,
        })
        if (requestToken !== this.requestToken) return

        if (pageNum === 0) {
          this.issues = data.items
        } else {
          this.issues.push(...data.items)
        }
        this.hasNext = data.page.hasNext
        this.nextPage = data.page.number + 1
        this.loaded = true
      } catch {
        if (requestToken !== this.requestToken) return

        this.toastStore.error('공개 피드를 불러오지 못했습니다.')
        this.loaded = true
        this.hasNext = false
      } finally {
        if (requestToken === this.requestToken) {
          this.loading = false
        }
      }
    },
    getVirtualScrollElement() {
      return this.$refs.virtualScroll?.$el ?? null
    },
    bindVirtualScroll() {
      const nextElement = this.getVirtualScrollElement()
      if (!nextElement || nextElement === this.virtualScrollElement) return

      this.unbindVirtualScroll()
      nextElement.addEventListener('scroll', this.onVirtualScroll, { passive: true })
      this.virtualScrollElement = nextElement
    },
    unbindVirtualScroll() {
      if (!this.virtualScrollElement) return

      this.virtualScrollElement.removeEventListener('scroll', this.onVirtualScroll)
      this.virtualScrollElement = null
    },
    onVirtualScroll() {
      const element = this.virtualScrollElement
      if (!element || this.loading || !this.hasNext) return

      const remainingPx = element.scrollHeight - element.scrollTop - element.clientHeight
      if (remainingPx <= LOAD_MORE_THRESHOLD_PX) {
        this.fetchPage(this.nextPage)
      }
    },
    scrollListToTop() {
      const element = this.getVirtualScrollElement()
      if (element) {
        element.scrollTop = 0
      }
    },
    resetList() {
      this.requestToken += 1
      this.issues = []
      this.nextPage = 0
      this.hasNext = true
      this.loading = false
      this.loaded = false
      this.listKey += 1
    },
    reloadList() {
      this.resetList()
      this.$nextTick(() => {
        this.bindVirtualScroll()
        this.scrollListToTop()
        this.fetchPage(0)
      })
    },
    applySearch() {
      this.appliedKeyword = this.searchKeyword.trim()
      this.reloadList()
    },
    clearSearch() {
      this.searchKeyword = ''
      this.appliedKeyword = ''
      this.reloadList()
    },
    openIssue(issue) {
      this.$router.push({
        name: 'public-issue-detail',
        params: { issueId: issue.issueId },
      })
    },
    onDeleteClick(issue) {
      this.issueToDelete = issue
      this.deleteDialogOpen = true
    },
    async confirmDelete() {
      if (!this.issueToDelete || this.deleting) return

      this.deleting = true
      try {
        await newsletterApi.deletePublicIssueAsAdmin(this.issueToDelete.issueId)
        this.issues = this.issues.filter((issue) => issue.issueId !== this.issueToDelete.issueId)
        this.toastStore.success('공개 피드에서 이슈를 삭제했어요.')
        this.deleteDialogOpen = false
        this.issueToDelete = null
      } catch {
        this.toastStore.error('공개 피드 이슈를 삭제하지 못했습니다.')
      } finally {
        this.deleting = false
      }
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
  <v-container class="py-8" max-width="960">
    <header class="mb-6">
      <h1 class="text-h5 font-weight-bold mb-2">최신 뉴스레터 피드</h1>
      <p class="text-body-2 text-medium-emphasis">
        수집된 뉴스레터 이슈를 최신순으로 둘러보세요.
      </p>
    </header>

    <section class="ranking-section mb-7">
      <div class="ranking-header">
        <div>
          <h2 class="text-subtitle-1 font-weight-bold">인기글</h2>
        </div>

        <v-btn-toggle
          v-model="rankingWindowType"
          mandatory
          density="compact"
          divided
          class="ranking-toggle"
        >
          <v-btn value="DAILY" size="small">오늘</v-btn>
          <v-btn value="WEEKLY" size="small">이번 주</v-btn>
        </v-btn-toggle>
      </div>

      <div
        v-if="rankingLoading && rankings.length === 0"
        class="ranking-loading"
      >
        <v-skeleton-loader
          v-for="n in 3"
          :key="n"
          type="list-item-avatar-two-line"
          class="ranking-skeleton"
        />
      </div>

      <div
        v-else-if="isRankingEmpty"
        class="ranking-empty text-body-2 text-medium-emphasis"
      >
        표시할 인기글이 없습니다.
      </div>

      <v-slide-group
        v-else
        show-arrows
        class="ranking-list"
      >
        <v-slide-group-item
          v-for="(ranking, index) in rankings"
          :key="ranking.issueId"
        >
          <button
            type="button"
            class="ranking-item"
            @click="openIssue(ranking)"
          >
            <span class="ranking-number">{{ index + 1 }}</span>
            <v-avatar size="36" rounded="md" class="ranking-avatar">
              <v-img :src="ranking.newsletterImageUrl" :alt="ranking.newsletterName">
                <template #error>
                  <div class="image-fallback">
                    {{ ranking.newsletterName.slice(0, 2) }}
                  </div>
                </template>
              </v-img>
            </v-avatar>
            <span class="ranking-content">
              <span class="ranking-meta">
                <span class="ranking-newsletter">{{ ranking.newsletterName }}</span>
                <span class="ranking-score">
                  <v-icon size="14" icon="mdi-trending-up" />
                  {{ ranking.score }}
                </span>
              </span>
              <span class="ranking-title">{{ ranking.subject }}</span>
            </span>
          </button>
        </v-slide-group-item>
      </v-slide-group>
    </section>

    <div class="filters mb-5">
      <v-chip-group
        v-model="selectedCategory"
        mandatory
        column
        selected-class="bg-grey-darken-4 text-white"
      >
        <v-chip
          v-for="category in categories"
          :key="category.code"
          :value="category.code"
          variant="outlined"
          size="small"
        >
          {{ category.label }}
        </v-chip>
      </v-chip-group>

      <div class="search-row mt-4">
        <v-text-field
          v-model="searchKeyword"
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
    </div>

    <v-sheet
      v-if="isEmpty"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-icon size="48" class="mb-3 text-medium-emphasis">mdi-email-outline</v-icon>
      <div class="text-body-1 font-weight-medium mb-2">표시할 이슈가 없습니다</div>
      <div class="text-body-2 text-medium-emphasis">
        선택한 조건에 아직 공개된 뉴스레터 이슈가 없어요.
      </div>
    </v-sheet>

    <div
      v-else
      :key="listKey"
      class="issue-scroll-shell"
    >
      <v-virtual-scroll
        ref="virtualScroll"
        :items="issues"
        item-key="issueId"
        :item-height="104"
        height="720"
        class="issue-scroll"
      >
        <template #default="{ item: issue, index }">
          <v-list-item
            :key="issue.issueId"
            class="issue-item"
            :border="index < issues.length - 1 ? 'b' : ''"
            @click="openIssue(issue)"
          >
            <template #prepend>
              <v-avatar size="44" rounded="md" class="mr-2">
                <v-img :src="issue.newsletterImageUrl" :alt="issue.newsletterName">
                  <template #error>
                    <div class="image-fallback">
                      {{ issue.newsletterName.slice(0, 2) }}
                    </div>
                  </template>
                </v-img>
              </v-avatar>
            </template>

            <template #append>
              <v-btn
                v-if="isAdmin"
                icon="mdi-delete-outline"
                variant="text"
                size="small"
                color="error"
                :aria-label="`${issue.subject} 삭제`"
                @click.stop="onDeleteClick(issue)"
              />
            </template>

            <div class="d-flex align-center flex-wrap mb-1">
              <span class="text-caption text-medium-emphasis mr-2">
                {{ issue.newsletterName }}
              </span>
              <v-chip
                v-if="issue.newsletterCategory"
                size="x-small"
                variant="tonal"
                class="mr-2"
              >
                {{ issue.newsletterCategory.label }}
              </v-chip>
              <span class="text-caption text-medium-emphasis">
                {{ formatDateTime(issue.receivedAt) }}
              </span>
            </div>
            <div class="text-subtitle-2 font-weight-bold">
              {{ issue.subject }}
            </div>
            <div class="text-body-2 text-medium-emphasis issue-summary">
              {{ issue.previewText }}
            </div>
          </v-list-item>
        </template>
      </v-virtual-scroll>

      <div
        v-if="loading"
        class="issue-scroll-status text-caption text-medium-emphasis"
      >
        불러오는 중...
      </div>
    </div>

    <v-dialog v-model="deleteDialogOpen" max-width="420">
      <v-card rounded="lg">
        <v-card-title class="text-subtitle-1 font-weight-bold">
          공개 피드에서 삭제할까요?
        </v-card-title>
        <v-card-text class="text-body-2">
          삭제하면 홈 공개 피드에서 사라집니다.
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
.issue-scroll-shell,
.issue-scroll {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
}

.issue-scroll-shell {
  overflow: hidden;
}

.issue-scroll {
  border: 0;
  border-radius: 0;
  height: 720px;
  max-height: calc(100vh - 240px);
  min-height: 420px;
}

.issue-item {
  cursor: pointer;
  transition: background-color 0.15s;
  padding: 12px 16px;
}

.issue-item:hover {
  background: #fafafa;
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

.issue-scroll-status {
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  padding: 12px;
  text-align: center;
}

.ranking-section {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  padding: 16px;
}

.ranking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.ranking-toggle {
  flex: 0 0 auto;
}

.ranking-loading {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.ranking-skeleton {
  border-radius: 8px;
}

.ranking-empty {
  padding: 28px 0;
  text-align: center;
}

.ranking-list {
  margin: 0 -4px;
}

.ranking-item {
  width: 280px;
  min-height: 88px;
  margin: 0 4px;
  padding: 12px;
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s, border-color 0.15s;
}

.ranking-item:hover {
  background: #fafafa;
  border-color: rgba(0, 0, 0, 0.14);
}

.ranking-number {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #111827;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.ranking-avatar {
  align-self: center;
}

.ranking-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ranking-meta {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.2;
}

.ranking-score {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: #374151;
  font-weight: 600;
}

.ranking-newsletter {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-title {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
}

.search-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

@media (max-width: 600px) {
  .ranking-section {
    padding: 14px;
  }

  .ranking-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .ranking-toggle,
  .ranking-toggle :deep(.v-btn) {
    width: 100%;
  }

  .ranking-loading {
    grid-template-columns: 1fr;
  }

  .ranking-item {
    width: 248px;
  }

  .search-row {
    flex-direction: column;
  }

  .search-row .v-btn {
    width: 100%;
  }
}
</style>
