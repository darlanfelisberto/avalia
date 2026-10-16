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
  <div class="min-h-screen bg-linear-to-br from-slate-900 via-slate-800 to-emerald-950 flex flex-col justify-center items-center px-4 py-12">
    <div class="max-w-md w-full space-y-8">
      <div class="text-center space-y-3">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-emerald-600 text-white font-black text-2xl shadow-xl shadow-emerald-950 ring-4 ring-emerald-500/20">A</div>
        <h1 class="text-2xl font-black text-white tracking-tight">IFFar AVALIA</h1>
        <p class="text-xs text-slate-300">Sistema de Avaliação Institucional do Instituto Federal Farroupilha</p>
      </div>

      <div class="bg-white/95 rounded-3xl p-7 shadow-2xl border border-slate-100 space-y-5">
        <h2 class="text-lg font-bold text-slate-800">Acesso ao Sistema</h2>
        <p class="text-sm text-slate-600">Entre com sua conta institucional para acessar suas avaliações.</p>
        <div v-if="erro || erroInicial" role="alert" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm">
          {{ erro || erroInicial }}
        </div>
        <button type="button" :disabled="carregando" @click="entrar"
          class="w-full py-3 px-4 rounded-xl font-bold text-sm text-white bg-emerald-600 hover:bg-emerald-700 disabled:opacity-60 cursor-pointer">
          {{ carregando ? 'Redirecionando...' : 'Entrar com conta institucional' }}
        </button>
      </div>
      <p class="text-center text-slate-400 text-xs">PPGTER / UFSM &bull; Instituto Federal Farroupilha (IFFar)</p>
    </div>
  </div>
</template>
