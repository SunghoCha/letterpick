<script>
import * as newsletterApi from '@/api/newsletter'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const PAGE_SIZE = 20
const LOAD_MORE_THRESHOLD_PX = 480

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
  },
  watch: {
    selectedCategory() {
      this.reloadList()
    },
  },
  async created() {
    await this.loadCategories()
  },
  mounted() {
    this.bindVirtualScroll()
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

.search-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

@media (max-width: 600px) {
  .search-row {
    flex-direction: column;
  }

  .search-row .v-btn {
    width: 100%;
  }
}
</style>
