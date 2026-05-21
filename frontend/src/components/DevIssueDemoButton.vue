<script>
import * as newsletterApi from '@/api/newsletter'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

export default {
  name: 'DevIssueDemoButton',
  emits: ['created'],
  data() {
    return {
      loading: false,
    }
  },
  computed: {
    authStore() {
      return useAuthStore()
    },
    toastStore() {
      return useToastStore()
    },
    isVisible() {
      const hostname = window.location.hostname
      return import.meta.env.DEV
        || hostname === 'localhost'
        || hostname === '127.0.0.1'
        || hostname.startsWith('dev.')
    },
    canCreate() {
      return this.isVisible && this.authStore.isLoggedIn
    },
  },
  methods: {
    async createDemoIssues() {
      this.loading = true
      try {
        const result = await newsletterApi.createDemoIssues()
        this.toastStore.success(
          `데모 이슈 ${result.createdIssueCount}개 생성, ${result.skippedIssueCount}개 건너뜀`
        )
        this.$emit('created')
      } catch {
        this.toastStore.error('데모 이슈를 생성하지 못했습니다.')
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<template>
  <v-btn
    v-if="canCreate"
    color="primary"
    variant="tonal"
    size="small"
    prepend-icon="mdi-flask-outline"
    :loading="loading"
    @click="createDemoIssues"
  >
    데모 이슈 생성
  </v-btn>
</template>
