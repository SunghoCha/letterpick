<script>
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

export default {
  name: 'App',
  data() {
    return {
      logoutSubmitting: false,
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
    isAdmin() {
      return this.authStore.isAdmin
    },
    memberNickname() {
      return this.authStore.member?.nickname ?? ''
    },
    // v-model로 store 상태와 양방향 바인딩.
    // 사용자가 X 버튼·바깥 클릭으로 닫을 때 store의 show가 false로 갱신됨.
    toastShow: {
      get() {
        return this.toastStore.show
      },
      set(value) {
        this.toastStore.show = value
      },
    },
  },
  methods: {
    async onLogoutClick() {
      if (this.logoutSubmitting) return
      this.logoutSubmitting = true
      try {
        await this.authStore.logout()
        this.toastStore.success('로그아웃되었습니다.')
        this.$router.push({ name: 'home' })
      } catch {
        // store.logout은 finally에서 clear까지 마치고 throw할 수 있다.
        // (ensureCsrfToken/POST 자체 실패 등). 사용자 화면에는 그래도 로그아웃된 것처럼 보임.
        this.toastStore.error('로그아웃 처리 중 오류가 발생했지만 세션은 정리되었습니다.')
        this.$router.push({ name: 'home' })
      } finally {
        this.logoutSubmitting = false
      }
    },
  },
}
</script>

<template>
  <v-app>
    <v-app-bar color="white" elevation="1" density="compact">
      <v-app-bar-title>
        <router-link to="/" class="brand">letterPick</router-link>
      </v-app-bar-title>

      <template #append>
        <v-btn :to="{ name: 'home' }" variant="text">홈</v-btn>
        <v-btn :to="{ name: 'newsletters' }" variant="text">뉴스레터</v-btn>
        <v-btn :to="{ name: 'today' }" variant="text">투데이</v-btn>
        <v-btn :to="{ name: 'inbox' }" variant="text">보관함</v-btn>

        <template v-if="isLoggedIn">
          <v-btn
            v-if="isAdmin"
            :to="{ name: 'admin-email-operations' }"
            variant="text"
            prepend-icon="mdi-shield-account-outline"
          >
            운영
          </v-btn>
          <v-btn
            :to="{ name: 'my' }"
            variant="text"
            prepend-icon="mdi-account-circle"
          >
            {{ memberNickname }}
          </v-btn>
          <v-btn
            variant="tonal"
            size="small"
            color="grey-darken-2"
            class="ml-2"
            :loading="logoutSubmitting"
            @click="onLogoutClick"
          >
            로그아웃
          </v-btn>
        </template>
        <v-btn v-else :to="{ name: 'login' }" variant="text">
          로그인
        </v-btn>
      </template>
    </v-app-bar>

    <v-main>
      <router-view />
    </v-main>

    <!-- 전역 토스트(스낵바). useToastStore().info/success/error()로 트리거. -->
    <v-snackbar
      v-model="toastShow"
      :color="toastStore.color"
      :timeout="toastStore.timeout"
      location="bottom"
    >
      {{ toastStore.text }}
      <template #actions>
        <v-btn variant="text" @click="toastStore.dismiss">닫기</v-btn>
      </template>
    </v-snackbar>
  </v-app>
</template>

<style scoped>
.brand {
  font-weight: 600;
  color: inherit;
  text-decoration: none;
}
</style>
