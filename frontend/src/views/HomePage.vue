<script>
import * as newsletterApi from '@/api/newsletter'
import { useToastStore } from '@/stores/toast'

const PAGE_SIZE = 20

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

      categories: [{ code: 'ALL', label: '전체' }],
      selectedCategory: 'ALL',
    }
  },
  computed: {
    toastStore() {
      return useToastStore()
    },
    isEmpty() {
      return this.loaded && this.issues.length === 0
    },
  },
  watch: {
    selectedCategory() {
      this.resetList()
    },
  },
  async created() {
    await this.loadCategories()
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
      this.requestToken += 1
      this.issues = []
      this.nextPage = 0
      this.hasNext = true
      this.loading = false
      this.loaded = false
      this.listKey += 1
    },
    openIssue(issue) {
      this.$router.push({
        name: 'public-issue-detail',
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

    <v-chip-group
      v-model="selectedCategory"
      mandatory
      column
      selected-class="bg-grey-darken-4 text-white"
      class="mb-5"
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

    <v-sheet
      v-if="isEmpty"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-icon size="48" class="mb-3 text-medium-emphasis">mdi-email-outline</v-icon>
      <div class="text-body-1 font-weight-medium mb-2">표시할 이슈가 없습니다</div>
      <div class="text-body-2 text-medium-emphasis">
        선택한 카테고리에 아직 공개된 뉴스레터 이슈가 없어요.
      </div>
    </v-sheet>

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
</style>
