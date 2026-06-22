<script>
import * as newsletterApi from '@/api/newsletter'
import { useToastStore } from '@/stores/toast'

export default {
  name: 'PublicIssueDetailPage',
  props: {
    issueId: {
      type: Number,
      required: true,
    },
  },
  data() {
    return {
      issue: null,
      loading: true,
    }
  },
  computed: {
    toastStore() {
      return useToastStore()
    },
  },
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
      try {
        this.issue = await newsletterApi.fetchPublicIssueDetail(this.issueId)
        this.recordIssueView()
      } catch {
        this.issue = null
        this.toastStore.error('이슈를 불러오지 못했습니다.')
      } finally {
        this.loading = false
      }
    },
    async recordIssueView() {
      try {
        await newsletterApi.recordPublicIssueView(this.issueId)
      } catch {
        // 조회수 기록 실패는 상세 조회 경험을 막지 않는다.
      }
    },
    goBack() {
      if (window.history.length > 1) {
        this.$router.back()
      } else {
        this.$router.push({ name: 'home' })
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
  <v-container class="py-8" max-width="800">
    <v-sheet
      v-if="loading"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-progress-circular indeterminate />
    </v-sheet>

    <v-sheet
      v-else-if="!issue"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-icon size="48" class="mb-3 text-medium-emphasis">mdi-email-remove-outline</v-icon>
      <div class="text-body-1 font-weight-medium mb-2">이슈를 찾을 수 없어요</div>
      <div class="text-body-2 text-medium-emphasis mb-4">
        공개 피드에서 삭제되었거나 존재하지 않는 이슈일 수 있어요.
      </div>
      <v-btn color="primary" @click="goBack">돌아가기</v-btn>
    </v-sheet>

    <template v-else>
      <header class="mb-4 d-flex align-center">
        <v-btn
          icon="mdi-arrow-left"
          variant="text"
          size="small"
          class="mr-2"
          @click="goBack"
        />
      </header>

      <div class="d-flex align-center mb-3">
        <v-avatar size="44" rounded="md" class="mr-3">
          <v-img :src="issue.newsletterImageUrl" :alt="issue.newsletterName">
            <template #error>
              <div class="image-fallback">
                {{ issue.newsletterName.slice(0, 2) }}
              </div>
            </template>
          </v-img>
        </v-avatar>
        <div>
          <div class="text-body-2 font-weight-medium">
            {{ issue.newsletterName }}
          </div>
          <div class="text-caption text-medium-emphasis">
            {{ formatDateTime(issue.receivedAt) }}
          </div>
        </div>
      </div>

      <h1 class="text-h5 font-weight-bold mb-4">{{ issue.subject }}</h1>

      <v-divider class="mb-6" />

      <div class="issue-contents" v-html="issue.content" />
    </template>
  </v-container>
</template>

<style scoped>
.issue-contents {
  line-height: 1.7;
  color: rgba(0, 0, 0, 0.85);
}

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
