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
          className: 'provider-google',
        },
        {
          name: 'Naver',
          href: `${apiBaseUrl}/oauth2/authorization/naver`,
          icon: 'mdi-alpha-n-box',
          className: 'provider-naver',
        },
      ],
    }
  },
}
</script>

<template>
  <v-container class="login-wrap" max-width="480">
    <div class="login-heading">
      <div class="login-brand">letterPick</div>
      <h1>로그인</h1>
      <p>
        흩어진 뉴스레터를 한곳에서 읽고 정리하세요.
      </p>
    </div>

    <v-card class="login-card" elevation="0">
      <div class="login-card-header">
        <div class="login-card-title">소셜 계정으로 시작하기</div>
      </div>

      <div class="provider-list">
        <v-btn
          v-for="provider in providers"
          :key="provider.name"
          :href="provider.href"
          :prepend-icon="provider.icon"
          :class="['provider-button', provider.className]"
          variant="flat"
          size="large"
          block
        >
          {{ provider.name }}로 시작하기
        </v-btn>
      </div>

      <p class="login-terms">
        소셜 계정으로 로그인하면 letterPick 약관에 동의한 것으로 간주합니다.
      </p>
    </v-card>
  </v-container>
</template>

<style scoped>
.login-wrap {
  padding-top: 72px;
  padding-bottom: 64px;
}

.login-heading {
  margin-bottom: 32px;
  text-align: center;
}

.login-brand {
  margin-bottom: 14px;
  color: #2563eb;
  font-size: 0.875rem;
  font-weight: 700;
  letter-spacing: 0;
}

.login-heading h1 {
  margin: 0 0 10px;
  color: #111827;
  font-size: 2.25rem;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.2;
}

.login-heading p {
  margin: 0;
  color: #64748b;
  font-size: 1rem;
  line-height: 1.6;
}

.login-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 28px;
  background: #ffffff;
}

.login-card-header {
  margin-bottom: 20px;
}

.login-card-title {
  color: #111827;
  font-size: 1.125rem;
  font-weight: 700;
  letter-spacing: 0;
  text-align: center;
}

.provider-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.provider-button {
  min-height: 54px;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: none;
}

.provider-google {
  border: 1px solid #d1d5db;
  background: #ffffff !important;
  color: #111827 !important;
}

.provider-naver {
  background: #03c75a !important;
  color: #ffffff !important;
}

.login-terms {
  margin: 18px 0 0;
  color: #64748b;
  font-size: 0.8125rem;
  line-height: 1.55;
  text-align: center;
}

@media (max-width: 600px) {
  .login-wrap {
    padding-top: 48px;
  }

  .login-heading {
    margin-bottom: 24px;
  }

  .login-heading h1 {
    font-size: 1.875rem;
  }

  .login-card {
    padding: 22px;
  }
}
</style>
