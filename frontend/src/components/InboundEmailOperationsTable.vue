<script>
export default {
  name: 'InboundEmailOperationsTable',
  props: {
    title: {
      type: String,
      required: true,
    },
    subtitle: {
      type: String,
      required: true,
    },
    items: {
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
    error: {
      type: Boolean,
      default: false,
    },
    errorMessage: {
      type: String,
      required: true,
    },
    emptyMessage: {
      type: String,
      required: true,
    },
    page: {
      type: Object,
      default: null,
    },
    canMovePrevious: {
      type: Boolean,
      default: false,
    },
    canMoveNext: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['retry', 'move-page', 'toggle-item', 'copy-trace'],
  computed: {
    hasItems() {
      return this.items.length > 0
    },
  },
}
</script>

<template>
  <v-card variant="outlined">
    <div class="section-title-row">
      <div>
        <v-card-title class="text-subtitle-1 font-weight-bold pb-1">
          {{ title }}
        </v-card-title>
        <v-card-subtitle>
          {{ subtitle }}
        </v-card-subtitle>
      </div>
      <v-progress-circular
        v-if="loading"
        indeterminate
        color="primary"
        size="24"
      />
    </div>

    <v-alert
      v-if="error"
      type="error"
      variant="tonal"
      class="ma-4"
    >
      {{ errorMessage }}
      <template #append>
        <v-btn variant="text" size="small" @click="$emit('retry')">다시 시도</v-btn>
      </template>
    </v-alert>

    <div v-else class="table-scroll">
      <v-table class="inbound-email-table">
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
          <tr v-if="!loading && !hasItems">
            <td colspan="6" class="text-center text-medium-emphasis py-8">
              {{ emptyMessage }}
            </td>
          </tr>
          <template
            v-for="item in items"
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
                  @click="$emit('toggle-item', item)"
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
                        @click="$emit('copy-trace', 'messageKey', item.messageKey)"
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
                        @click="$emit('copy-trace', 'rawReference', item.rawReference)"
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
      v-if="page"
      class="justify-end table-pagination"
    >
      <span class="text-caption text-medium-emphasis">
        {{ page.number + 1 }} 페이지
      </span>
      <v-btn
        variant="text"
        size="small"
        icon="mdi-chevron-left"
        aria-label="이전 페이지"
        :disabled="!canMovePrevious || loading"
        @click="$emit('move-page', -1)"
      />
      <v-btn
        variant="text"
        size="small"
        icon="mdi-chevron-right"
        aria-label="다음 페이지"
        :disabled="!canMoveNext || loading"
        @click="$emit('move-page', 1)"
      />
    </v-card-actions>
  </v-card>
</template>

<style scoped>
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

.inbound-email-table :deep(table) {
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

.table-pagination {
  gap: 8px;
}

@media (max-width: 600px) {
  .section-title-row {
    flex-direction: column;
  }

  .trace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
