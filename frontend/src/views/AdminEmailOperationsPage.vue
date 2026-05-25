<script>
import * as emailOperationsApi from '@/api/emailOperations'
import { useToastStore } from '@/stores/toast'

const STATUS_META = {
  RECEIVED: {
    label: '수신됨',
    description: '수신 기록 생성 후 최종 처리 전 상태',
    color: 'warning',
    icon: 'mdi-email-fast-outline',
  },
  ISSUE_CREATED: {
    label: '이슈 생성',
    description: '사용자 보관함에 뉴스레터 이슈 생성 완료',
    color: 'success',
    icon: 'mdi-check-circle-outline',
  },
  SKIPPED_UNSUBSCRIBED: {
    label: '구독 해지 스킵',
    description: '앱 내 구독 해지 상태라 이슈 생성 제외',
    color: 'info',
    icon: 'mdi-email-minus-outline',
  },
  RECIPIENT_NOT_FOUND: {
    label: '수신자 없음',
    description: '수신 주소로 회원을 찾지 못한 상태',
    color: 'error',
    icon: 'mdi-account-alert-outline',
  },
  INVALID_RECIPIENT_ADDRESS: {
    label: '수신 주소 오류',
    description: 'letterPick 수신 주소 형식과 맞지 않는 상태',
    color: 'error',
    icon: 'mdi-email-alert-outline',
  },
  NEWSLETTER_NOT_FOUND: {
    label: '뉴스레터 없음',
    description: '발신자 이메일과 매칭되는 뉴스레터가 없는 상태',
    color: 'error',
    icon: 'mdi-book-alert-outline',
  },
}

const NUMBER_FORMATTER = new Intl.NumberFormat('ko-KR')
const ACTION_REQUIRED_PAGE_SIZE = 20

export default {
  name: 'AdminEmailOperationsPage',
  data() {
    return {
      summary: null,
      actionRequired: null,
      loading: false,
      actionRequiredLoading: false,
      loaded: false,
      forbidden: false,
      loadError: false,
      actionRequiredError: false,
      actionRequiredPage: 0,
      expandedActionItemId: null,
    }
  },
  computed: {
    toastStore() {
      return useToastStore()
    },
    statusItems() {
      return (this.summary?.statusCounts ?? []).map((item) => {
        const meta = STATUS_META[item.status] ?? {
          label: item.status,
          description: '정의되지 않은 상태',
          color: 'grey',
          icon: 'mdi-help-circle-outline',
        }
        return {
          ...item,
          ...meta,
          formattedCount: this.formatNumber(item.count),
        }
      })
    },
    totalCount() {
      return this.summary?.totalCount ?? 0
    },
    issueCreatedCount() {
      return this.findCount('ISSUE_CREATED')
    },
    receivedCount() {
      return this.findCount('RECEIVED')
    },
    actionRequiredCount() {
      return this.findCount('NEWSLETTER_NOT_FOUND')
        + this.findCount('RECIPIENT_NOT_FOUND')
        + this.findCount('INVALID_RECIPIENT_ADDRESS')
    },
    actionRequiredItems() {
      return (this.actionRequired?.items ?? []).map((item) => {
        const meta = STATUS_META[item.status] ?? {
          label: item.status,
          description: '정의되지 않은 상태',
          color: 'grey',
          icon: 'mdi-help-circle-outline',
        }
        return {
          ...item,
          ...meta,
          formattedReceivedAt: this.formatDateTime(item.receivedAt),
          expanded: this.expandedActionItemId === item.inboundEmailId,
        }
      })
    },
    hasActionRequiredItems() {
      return this.actionRequiredItems.length > 0
    },
    canMoveActionRequiredPrevious() {
      return (this.actionRequired?.page?.number ?? 0) > 0
    },
    canMoveActionRequiredNext() {
      return this.actionRequired?.page?.hasNext ?? false
    },
    isRefreshing() {
      return this.loading || this.actionRequiredLoading
    },
  },
  created() {
    this.refreshAll()
  },
  methods: {
    refreshAll() {
      this.fetchSummary()
      this.actionRequiredPage = 0
      this.expandedActionItemId = null
      this.fetchActionRequiredItems()
    },
    async fetchSummary() {
      if (this.loading) return
      this.loading = true
      this.forbidden = false
      this.loadError = false
      try {
        this.summary = await emailOperationsApi.fetchStatusSummary()
        this.loaded = true
      } catch (err) {
        if (err.response?.status === 401) {
          this.$router.push({ name: 'login' })
          return
        }
        if (err.response?.status === 403) {
          this.forbidden = true
          this.loaded = true
          return
        }
        this.loadError = true
        this.loaded = true
        this.toastStore.error('이메일 운영 상태를 불러오지 못했습니다.')
      } finally {
        this.loading = false
      }
    },
    async fetchActionRequiredItems() {
      if (this.actionRequiredLoading) return
      this.actionRequiredLoading = true
      this.actionRequiredError = false
      try {
        this.actionRequired = await emailOperationsApi.fetchActionRequiredItems({
          page: this.actionRequiredPage,
          size: ACTION_REQUIRED_PAGE_SIZE,
        })
      } catch (err) {
        if (err.response?.status === 401) {
          this.$router.push({ name: 'login' })
          return
        }
        if (err.response?.status === 403) {
          this.forbidden = true
          return
        }
        this.actionRequiredError = true
        this.toastStore.error('조치 필요 메일 목록을 불러오지 못했습니다.')
      } finally {
        this.actionRequiredLoading = false
      }
    },
    moveActionRequiredPage(delta) {
      const nextPage = this.actionRequiredPage + delta
      if (nextPage < 0) return
      this.actionRequiredPage = nextPage
      this.expandedActionItemId = null
      this.fetchActionRequiredItems()
    },
    toggleActionItem(item) {
      this.expandedActionItemId = item.expanded ? null : item.inboundEmailId
    },
    async copyTraceValue(label, value) {
      if (value == null) return
      try {
        await navigator.clipboard.writeText(String(value))
        this.toastStore.success(`${label} 복사했습니다.`)
      } catch {
        this.toastStore.error(`${label} 복사하지 못했습니다.`)
      }
    },
    findCount(status) {
      return this.summary?.statusCounts?.find((item) => item.status === status)?.count ?? 0
    },
    formatNumber(value) {
      return NUMBER_FORMATTER.format(value ?? 0)
    },
    formatDateTime(isoDate) {
      if (!isoDate) return '-'
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
  <v-container class="admin-email-page py-8" max-width="1100">
    <header class="page-header mb-6">
      <div>
        <div class="text-caption text-medium-emphasis mb-1">Admin</div>
        <h1 class="text-h5 font-weight-bold mb-2">이메일 운영 콘솔</h1>
        <p class="text-body-2 text-medium-emphasis">
          최근 24시간 인입 메일이 어떤 상태로 처리됐는지 확인합니다.
        </p>
      </div>
      <v-btn
        color="primary"
        variant="tonal"
        prepend-icon="mdi-refresh"
        :loading="isRefreshing"
        @click="refreshAll"
      >
        새로고침
      </v-btn>
    </header>

    <v-alert
      v-if="forbidden"
      type="warning"
      variant="tonal"
      class="mb-6"
    >
      관리자 권한이 필요합니다.
    </v-alert>

    <v-alert
      v-if="loadError"
      type="error"
      variant="tonal"
      class="mb-6"
    >
      이메일 운영 상태를 불러오지 못했습니다.
      <template #append>
        <v-btn variant="text" size="small" @click="fetchSummary">다시 시도</v-btn>
      </template>
    </v-alert>

    <v-sheet
      v-if="loading && !summary && !forbidden"
      class="pa-12 text-center"
      color="transparent"
    >
      <v-progress-circular indeterminate color="primary" />
      <div class="text-body-2 text-medium-emphasis mt-4">운영 상태를 불러오는 중입니다.</div>
    </v-sheet>

    <template v-if="summary && !forbidden">
      <v-sheet class="summary-band pa-4 mb-5" border rounded="lg">
        <div class="text-caption text-medium-emphasis mb-1">조회 범위</div>
        <div class="text-body-2">
          {{ formatDateTime(summary.receivedFrom) }} ~ {{ formatDateTime(summary.receivedTo) }}
        </div>
      </v-sheet>

      <v-row class="mb-5" density="comfortable">
        <v-col cols="12" sm="6" md="3">
          <v-card class="metric-card" variant="outlined">
            <v-card-text>
              <div class="metric-label">총 인입</div>
              <div class="metric-value">{{ formatNumber(totalCount) }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" sm="6" md="3">
          <v-card class="metric-card" variant="outlined">
            <v-card-text>
              <div class="metric-label">이슈 생성</div>
              <div class="metric-value text-success">{{ formatNumber(issueCreatedCount) }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" sm="6" md="3">
          <v-card class="metric-card" variant="outlined">
            <v-card-text>
              <div class="metric-label">조치 필요</div>
              <div class="metric-value text-error">{{ formatNumber(actionRequiredCount) }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" sm="6" md="3">
          <v-card class="metric-card" variant="outlined">
            <v-card-text>
              <div class="metric-label">처리 전 수신</div>
              <div class="metric-value text-warning">{{ formatNumber(receivedCount) }}</div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <v-card variant="outlined">
        <v-card-title class="text-subtitle-1 font-weight-bold">
          상태별 분포
        </v-card-title>
        <v-table>
          <thead>
            <tr>
              <th class="text-left">상태</th>
              <th class="text-left">의미</th>
              <th class="text-right">건수</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in statusItems" :key="item.status">
              <td>
                <div class="d-flex align-center ga-2">
                  <v-chip
                    :color="item.color"
                    variant="tonal"
                    size="small"
                    :prepend-icon="item.icon"
                  >
                    {{ item.label }}
                  </v-chip>
                  <span class="text-caption text-medium-emphasis">{{ item.status }}</span>
                </div>
              </td>
              <td class="text-body-2 text-medium-emphasis">
                {{ item.description }}
              </td>
              <td class="text-right font-weight-bold">
                {{ item.formattedCount }}
              </td>
            </tr>
          </tbody>
        </v-table>
      </v-card>

      <v-card class="mt-5" variant="outlined">
        <div class="section-title-row">
          <div>
            <v-card-title class="text-subtitle-1 font-weight-bold pb-1">
              최근 조치 필요 메일
            </v-card-title>
            <v-card-subtitle>
              뉴스레터 매핑, 수신자 식별, 수신 주소 문제를 최신순으로 확인합니다.
            </v-card-subtitle>
          </div>
          <v-progress-circular
            v-if="actionRequiredLoading"
            indeterminate
            color="primary"
            size="24"
          />
        </div>

        <v-alert
          v-if="actionRequiredError"
          type="error"
          variant="tonal"
          class="ma-4"
        >
          조치 필요 메일 목록을 불러오지 못했습니다.
          <template #append>
            <v-btn variant="text" size="small" @click="fetchActionRequiredItems">다시 시도</v-btn>
          </template>
        </v-alert>

        <div v-else class="table-scroll">
          <v-table class="action-table">
            <thead>
              <tr>
                <th class="text-left">수신 시각</th>
                <th class="text-left">상태</th>
                <th class="text-left">발신자</th>
                <th class="text-left">수신 주소</th>
                <th class="text-left">제목</th>
                <th class="text-right">추적</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!actionRequiredLoading && !hasActionRequiredItems">
                <td colspan="6" class="text-center text-medium-emphasis py-8">
                  최근 조치 필요 메일이 없습니다.
                </td>
              </tr>
              <template
                v-for="item in actionRequiredItems"
                :key="item.inboundEmailId"
              >
                <tr>
                  <td class="text-body-2">{{ item.formattedReceivedAt }}</td>
                  <td>
                    <v-chip
                      :color="item.color"
                      variant="tonal"
                      size="small"
                      :prepend-icon="item.icon"
                    >
                      {{ item.label }}
                    </v-chip>
                  </td>
                  <td class="text-body-2 email-cell">{{ item.senderEmail }}</td>
                  <td class="text-body-2 email-cell">{{ item.recipientAddress }}</td>
                  <td class="text-body-2 subject-cell">{{ item.subject }}</td>
                  <td class="text-right">
                    <v-btn
                      variant="text"
                      size="small"
                      :icon="item.expanded ? 'mdi-chevron-up' : 'mdi-chevron-down'"
                      :aria-label="item.expanded ? '추적 정보 닫기' : '추적 정보 열기'"
                      @click="toggleActionItem(item)"
                    />
                  </td>
                </tr>
                <tr v-if="item.expanded" class="trace-row">
                  <td colspan="6">
                    <div class="trace-grid">
                      <div>
                        <div class="trace-label">memberId</div>
                        <div class="trace-value">{{ item.memberId ?? '-' }}</div>
                      </div>
                      <div>
                        <div class="trace-label">newsletterId</div>
                        <div class="trace-value">{{ item.newsletterId ?? '-' }}</div>
                      </div>
                      <div>
                        <div class="trace-label-row">
                          <span class="trace-label">messageKey</span>
                          <v-btn
                            variant="text"
                            size="x-small"
                            icon="mdi-content-copy"
                            aria-label="messageKey 복사"
                            @click="copyTraceValue('messageKey', item.messageKey)"
                          />
                        </div>
                        <div class="trace-value">{{ item.messageKey }}</div>
                      </div>
                      <div>
                        <div class="trace-label-row">
                          <span class="trace-label">rawReference</span>
                          <v-btn
                            variant="text"
                            size="x-small"
                            icon="mdi-content-copy"
                            aria-label="rawReference 복사"
                            @click="copyTraceValue('rawReference', item.rawReference)"
                          />
                        </div>
                        <div class="trace-value">{{ item.rawReference }}</div>
                      </div>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </v-table>
        </div>

        <v-card-actions
          v-if="actionRequired"
          class="justify-end action-pagination"
        >
          <span class="text-caption text-medium-emphasis">
            {{ actionRequired.page.number + 1 }} 페이지
          </span>
          <v-btn
            variant="text"
            size="small"
            icon="mdi-chevron-left"
            aria-label="이전 페이지"
            :disabled="!canMoveActionRequiredPrevious || actionRequiredLoading"
            @click="moveActionRequiredPage(-1)"
          />
          <v-btn
            variant="text"
            size="small"
            icon="mdi-chevron-right"
            aria-label="다음 페이지"
            :disabled="!canMoveActionRequiredNext || actionRequiredLoading"
            @click="moveActionRequiredPage(1)"
          />
        </v-card-actions>
      </v-card>
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

.summary-band {
  background: #fff;
}

.metric-card {
  height: 100%;
}

.metric-label {
  color: rgba(0, 0, 0, 0.6);
  font-size: 13px;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.section-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-right: 16px;
}

.table-scroll {
  overflow-x: auto;
}

.action-table :deep(table) {
  min-width: 960px;
}

.email-cell {
  max-width: 220px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.subject-cell {
  max-width: 260px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trace-row {
  background: #fafafa;
}

.trace-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
  padding: 16px;
}

.trace-label {
  color: rgba(0, 0, 0, 0.6);
  font-size: 12px;
}

.trace-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 28px;
  margin-bottom: 4px;
}

.trace-value {
  font-family: ui-monospace, SFMono-Regular, Consolas, Liberation Mono, monospace;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.action-pagination {
  gap: 8px;
}

@media (max-width: 600px) {
  .page-header {
    flex-direction: column;
  }

  .section-title-row {
    flex-direction: column;
  }

  .trace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
