<script>
import * as emailOperationsApi from '@/api/emailOperations'
import InboundEmailOperationsTable from '@/components/InboundEmailOperationsTable.vue'
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
const EMAIL_OPERATIONS_PAGE_SIZE = 20
const RANGE_PRESETS = [
  { value: '1h', label: '1시간', hours: 1 },
  { value: '6h', label: '6시간', hours: 6 },
  { value: '24h', label: '24시간', hours: 24 },
  { value: '7d', label: '7일', hours: 24 * 7 },
]

export default {
  name: 'AdminEmailOperationsPage',
  components: {
    InboundEmailOperationsTable,
  },
  data() {
    return {
      summary: null,
      actionRequired: null,
      staleReceived: null,
      queueStatus: null,
      loading: false,
      actionRequiredLoading: false,
      staleReceivedLoading: false,
      queueStatusLoading: false,
      loaded: false,
      forbidden: false,
      loadError: false,
      actionRequiredError: false,
      staleReceivedError: false,
      queueStatusError: false,
      actionRequiredPage: 0,
      staleReceivedPage: 0,
      expandedActionItemId: null,
      expandedStaleReceivedItemId: null,
      selectedRangePreset: '24h',
      receivedRangeParams: null,
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
      return this.toInboundEmailViewItems(this.actionRequired?.items ?? [], this.expandedActionItemId)
    },
    staleReceivedItems() {
      return this.toInboundEmailViewItems(this.staleReceived?.items ?? [], this.expandedStaleReceivedItemId)
    },
    queueStatusAvailable() {
      return this.queueStatus?.status === 'AVAILABLE'
    },
    queueStatusLabel() {
      if (!this.queueStatus) return '조회 전'
      return this.queueStatusAvailable ? '조회 가능' : '조회 불가'
    },
    queueStatusColor() {
      if (!this.queueStatus) return 'grey'
      return this.queueStatusAvailable ? 'success' : 'warning'
    },
    queueMetricItems() {
      if (!this.queueStatusAvailable) return []
      return [
        {
          label: '메인 큐 대기',
          description: 'worker가 아직 가져가지 않은 메시지',
          value: this.queueStatus.mainQueue?.availableMessageCount,
          color: 'primary',
        },
        {
          label: '메인 큐 처리 중',
          description: 'worker가 가져간 뒤 처리 중인 메시지',
          value: this.queueStatus.mainQueue?.inFlightMessageCount,
          color: 'warning',
        },
        {
          label: '메인 큐 지연',
          description: '지연 설정으로 아직 처리 가능하지 않은 메시지',
          value: this.queueStatus.mainQueue?.delayedMessageCount,
          color: 'info',
        },
        {
          label: 'DLQ 대기',
          description: '반복 실패 후 DLQ에 남은 메시지',
          value: this.queueStatus.deadLetterQueue?.availableMessageCount,
          color: 'error',
        },
      ]
    },
    canMoveActionRequiredPrevious() {
      return (this.actionRequired?.page?.number ?? 0) > 0
    },
    canMoveActionRequiredNext() {
      return this.actionRequired?.page?.hasNext ?? false
    },
    canMoveStaleReceivedPrevious() {
      return (this.staleReceived?.page?.number ?? 0) > 0
    },
    canMoveStaleReceivedNext() {
      return this.staleReceived?.page?.hasNext ?? false
    },
    isRefreshing() {
      return this.loading || this.actionRequiredLoading || this.staleReceivedLoading || this.queueStatusLoading
    },
    rangePresets() {
      return RANGE_PRESETS
    },
    selectedRangePresetItem() {
      return this.rangePresets.find((preset) => preset.value === this.selectedRangePreset) ?? this.rangePresets[2]
    },
  },
  created() {
    this.refreshAll()
  },
  methods: {
    refreshAll() {
      this.receivedRangeParams = this.createReceivedRangeParams()
      this.fetchSummary()
      this.actionRequiredPage = 0
      this.staleReceivedPage = 0
      this.expandedActionItemId = null
      this.expandedStaleReceivedItemId = null
      this.fetchActionRequiredItems()
      this.fetchStaleReceivedItems()
      this.fetchQueueStatus()
    },
    changeRangePreset(value) {
      if (!value || value === this.selectedRangePreset) return
      this.selectedRangePreset = value
      this.refreshAll()
    },
    async fetchSummary() {
      if (this.loading) return
      this.loading = true
      this.forbidden = false
      this.loadError = false
      try {
        this.summary = await emailOperationsApi.fetchStatusSummary(this.receivedRangeParams ?? {})
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
          ...(this.receivedRangeParams ?? {}),
          page: this.actionRequiredPage,
          size: EMAIL_OPERATIONS_PAGE_SIZE,
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
    async fetchStaleReceivedItems() {
      if (this.staleReceivedLoading) return
      this.staleReceivedLoading = true
      this.staleReceivedError = false
      try {
        this.staleReceived = await emailOperationsApi.fetchStaleReceivedItems({
          page: this.staleReceivedPage,
          size: EMAIL_OPERATIONS_PAGE_SIZE,
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
        this.staleReceivedError = true
        this.toastStore.error('처리 지연 메일 목록을 불러오지 못했습니다.')
      } finally {
        this.staleReceivedLoading = false
      }
    },
    async fetchQueueStatus() {
      if (this.queueStatusLoading) return
      this.queueStatusLoading = true
      this.queueStatusError = false
      try {
        this.queueStatus = await emailOperationsApi.fetchQueueStatus()
      } catch (err) {
        if (err.response?.status === 401) {
          this.$router.push({ name: 'login' })
          return
        }
        if (err.response?.status === 403) {
          this.forbidden = true
          return
        }
        this.queueStatusError = true
        this.toastStore.error('메일 수신 큐 상태를 불러오지 못했습니다.')
      } finally {
        this.queueStatusLoading = false
      }
    },
    createReceivedRangeParams() {
      const receivedTo = new Date()
      const receivedFrom = new Date(
        receivedTo.getTime() - this.selectedRangePresetItem.hours * 60 * 60 * 1000,
      )

      return {
        receivedFrom: receivedFrom.toISOString(),
        receivedTo: receivedTo.toISOString(),
      }
    },
    moveActionRequiredPage(delta) {
      const nextPage = this.actionRequiredPage + delta
      if (nextPage < 0) return
      this.actionRequiredPage = nextPage
      this.expandedActionItemId = null
      this.fetchActionRequiredItems()
    },
    moveStaleReceivedPage(delta) {
      const nextPage = this.staleReceivedPage + delta
      if (nextPage < 0) return
      this.staleReceivedPage = nextPage
      this.expandedStaleReceivedItemId = null
      this.fetchStaleReceivedItems()
    },
    toggleActionItem(item) {
      this.expandedActionItemId = item.expanded ? null : item.inboundEmailId
    },
    toggleStaleReceivedItem(item) {
      this.expandedStaleReceivedItemId = item.expanded ? null : item.inboundEmailId
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
    toInboundEmailViewItems(items, expandedItemId) {
      return items.map((item) => {
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
          expanded: expandedItemId === item.inboundEmailId,
        }
      })
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
          선택한 조회 범위의 인입 메일이 어떤 상태로 처리됐는지 확인합니다.
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
        <div class="range-toolbar">
          <div>
            <div class="text-caption text-medium-emphasis mb-1">조회 범위</div>
            <div class="text-body-2">
              {{ formatDateTime(summary.receivedFrom) }} ~ {{ formatDateTime(summary.receivedTo) }}
            </div>
          </div>
          <v-btn-toggle
            :model-value="selectedRangePreset"
            mandatory
            divided
            density="compact"
            variant="outlined"
            @update:model-value="changeRangePreset"
          >
            <v-btn
              v-for="preset in rangePresets"
              :key="preset.value"
              :value="preset.value"
              :disabled="isRefreshing"
            >
              {{ preset.label }}
            </v-btn>
          </v-btn-toggle>
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

      <v-card class="mb-5" variant="outlined">
        <v-card-title class="queue-card-title">
          <span class="text-subtitle-1 font-weight-bold">메일 수신 큐 상태</span>
          <div class="d-flex align-center ga-2">
            <v-progress-circular
              v-if="queueStatusLoading"
              indeterminate
              color="primary"
              size="20"
              width="2"
            />
            <v-chip
              :color="queueStatusColor"
              variant="tonal"
              size="small"
            >
              {{ queueStatusLabel }}
            </v-chip>
          </div>
        </v-card-title>
        <v-card-subtitle>
          SQS 메인 큐와 DLQ에 남은 메시지 수를 현재 시점 기준으로 확인합니다.
          <span v-if="queueStatus?.checkedAt" class="ml-2">
            조회 시각 {{ formatDateTime(queueStatus.checkedAt) }}
          </span>
        </v-card-subtitle>
        <v-card-text>
          <v-alert
            v-if="queueStatusError"
            type="error"
            variant="tonal"
            density="comfortable"
          >
            메일 수신 큐 상태를 불러오지 못했습니다.
            <template #append>
              <v-btn variant="text" size="small" @click="fetchQueueStatus">다시 시도</v-btn>
            </template>
          </v-alert>

          <v-alert
            v-else-if="queueStatus && !queueStatusAvailable"
            type="warning"
            variant="tonal"
            density="comfortable"
          >
            큐 상태를 조회할 수 없습니다.
            <span v-if="queueStatus.failureReason" class="ml-1">
              {{ queueStatus.failureReason }}
            </span>
          </v-alert>

          <v-row v-else-if="queueStatusAvailable" density="comfortable">
            <v-col
              v-for="item in queueMetricItems"
              :key="item.label"
              cols="12"
              sm="6"
              md="3"
            >
              <v-sheet class="queue-metric pa-3" border rounded="lg">
                <div class="metric-label">{{ item.label }}</div>
                <div class="metric-value" :class="`text-${item.color}`">
                  {{ formatNumber(item.value) }}
                </div>
                <div class="text-caption text-medium-emphasis mt-1">
                  {{ item.description }}
                </div>
              </v-sheet>
            </v-col>
          </v-row>

          <div v-else class="text-body-2 text-medium-emphasis">
            큐 상태를 불러오는 중입니다.
          </div>
        </v-card-text>
      </v-card>

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

      <InboundEmailOperationsTable
        class="mt-5"
        title="처리 지연 메일"
        subtitle="10분 이상 RECEIVED 상태로 남은 인입 메일을 오래된 순으로 확인합니다."
        :items="staleReceivedItems"
        :loading="staleReceivedLoading"
        :error="staleReceivedError"
        error-message="처리 지연 메일 목록을 불러오지 못했습니다."
        empty-message="처리 지연 메일이 없습니다."
        :page="staleReceived?.page"
        :can-move-previous="canMoveStaleReceivedPrevious"
        :can-move-next="canMoveStaleReceivedNext"
        @retry="fetchStaleReceivedItems"
        @move-page="moveStaleReceivedPage"
        @toggle-item="toggleStaleReceivedItem"
        @copy-trace="copyTraceValue"
      />

      <InboundEmailOperationsTable
        class="mt-5"
        title="최근 조치 필요 메일"
        subtitle="뉴스레터 매핑, 수신자 식별, 수신 주소 문제를 최신순으로 확인합니다."
        :items="actionRequiredItems"
        :loading="actionRequiredLoading"
        :error="actionRequiredError"
        error-message="조치 필요 메일 목록을 불러오지 못했습니다."
        empty-message="최근 조치 필요 메일이 없습니다."
        :page="actionRequired?.page"
        :can-move-previous="canMoveActionRequiredPrevious"
        :can-move-next="canMoveActionRequiredNext"
        @retry="fetchActionRequiredItems"
        @move-page="moveActionRequiredPage"
        @toggle-item="toggleActionItem"
        @copy-trace="copyTraceValue"
      />
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

.range-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.metric-card {
  height: 100%;
}

.queue-card-title {
  align-items: center;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.queue-metric {
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

@media (max-width: 600px) {
  .page-header {
    flex-direction: column;
  }

  .range-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
