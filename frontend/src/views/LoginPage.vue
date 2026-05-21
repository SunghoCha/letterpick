<script>
export default {
  name: 'LoginPage',
  data() {
    // OAuth2 진입은 브라우저 navigate (window.location 이동)이라 CORS 적용 X.
    // VITE_API_BASE_URL이 비어 있으면 현재 origin의 /oauth2 경로를 사용한다.
    // axios도 같은 변수를 baseURL로 사용한다.
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''
    return {
      providers: [
        {
          name: 'Google',
          href: `${apiBaseUrl}/oauth2/authorization/google`,
          icon: 'mdi-google',
        },
        {
          name: 'Naver',
          href: `${apiBaseUrl}/oauth2/authorization/naver`,
          icon: 'mdi-alpha-n-box',
        },
      ],
    }
  },
}
</script>

<template>
  <v-container class="login-wrap" max-width="420">
    <div class="text-center mb-10">
      <h1 class="text-h3 font-weight-bold mb-2">letterPick</h1>
      <p class="text-body-1 text-medium-emphasis">
        흩어진 뉴스레터를 한곳에서.
      </p>
    </div>

    <v-card class="pa-6" variant="outlined">
      <v-card-title class="text-center text-h6 pa-0 mb-6">
        시작하기
      </v-card-title>

      <div class="d-flex flex-column ga-3">
        <v-btn
          v-for="provider in providers"
          :key="provider.name"
          :href="provider.href"
          :prepend-icon="provider.icon"
          variant="outlined"
          size="large"
          block
        >
          {{ provider.name }}로 시작하기
        </v-btn>
      </div>
    </v-card>

    <p class="text-center text-caption text-medium-emphasis mt-6">
      소셜 계정으로 로그인하면 letterPick 약관에 동의한 것으로 간주합니다.
    </p>
  </v-container>
</template>

<style scoped>
.login-wrap {
  padding-top: 64px;
  padding-bottom: 64px;
}
</style>
