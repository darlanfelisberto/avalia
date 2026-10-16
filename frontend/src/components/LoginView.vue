<script setup lang="ts">
import { ref } from 'vue';
import { iniciarLoginOidc } from '../services/oidc';

defineProps<{ erroInicial?: string }>();

const carregando = ref(false);
const erro = ref('');

async function entrar() {
  carregando.value = true;
  erro.value = '';
  try {
    await iniciarLoginOidc();
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Falha ao iniciar autenticação.';
    carregando.value = false;
  }
}
</script>

<template>
  <div class="login-screen">
    <div class="login-screen__content">
      <div class="login-brand">
        <div class="login-brand__mark">A</div>
        <h1 class="login-brand__title">IFFar AVALIA</h1>
        <p class="login-brand__description">Sistema de Avaliação Institucional do Instituto Federal Farroupilha</p>
      </div>

      <div class="login-card">
        <h2 class="login-card__title">Acesso ao Sistema</h2>
        <p class="login-card__description">Entre com sua conta institucional para acessar suas avaliações.</p>
        <div v-if="erro || erroInicial" role="alert" class="alert alert--error">
          {{ erro || erroInicial }}
        </div>
        <button type="button" :disabled="carregando" @click="entrar"
          class="button button--primary login-card__submit">
          {{ carregando ? 'Redirecionando...' : 'Entrar com conta institucional' }}
        </button>
      </div>
      <p class="login-screen__footer">PPGTER / UFSM &bull; Instituto Federal Farroupilha (IFFar)</p>
    </div>
  </div>
</template>
