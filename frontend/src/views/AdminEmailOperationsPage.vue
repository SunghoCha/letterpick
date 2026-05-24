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

export default {
  name: 'AdminEmailOperationsPage',
  data() {
    return {
      summary: null,
      loading: false,
      loaded: false,
      forbidden: false,
      loadError: false,
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
  },
  created() {
    this.fetchSummary()
  },
  methods: {
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
        :loading="loading"
        @click="fetchSummary"
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

@media (max-width: 600px) {
  .page-header {
    flex-direction: column;
  }
}
</style>
