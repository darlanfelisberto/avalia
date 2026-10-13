<script setup lang="ts">
import { ref, onMounted } from 'vue';
import type { UsuarioContexto } from '../types/avaliacao';
import { loginPorCpfApi, listarUsuariosDemonstracaoApi } from '../services/api';
import { salvarUsuarioAtivo, USUARIOS_SIMULADOS } from '../services/storage';

const emit = defineEmits<{
  (e: 'loginSucesso', usuario: UsuarioContexto): void;
}>();

const cpfInput = ref('');
const carregando = ref(false);
const mensagemErro = ref('');
const contasDemo = ref<UsuarioContexto[]>(USUARIOS_SIMULADOS);

onMounted(async () => {
  const demos = await listarUsuariosDemonstracaoApi();
  if (demos && demos.length > 0) {
    contasDemo.value = demos;
  }
});

function aplicarMascaraCpf(valor: string): string {
  const digitos = valor.replace(/\D/g, '').slice(0, 11);
  if (digitos.length <= 3) return digitos;
  if (digitos.length <= 6) return `${digitos.slice(0, 3)}.${digitos.slice(3)}`;
  if (digitos.length <= 9) return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6)}`;
  return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-${digitos.slice(9)}`;
}

function aoDigitarCpf(e: Event) {
  const target = e.target as HTMLInputElement;
  cpfInput.value = aplicarMascaraCpf(target.value);
  mensagemErro.value = '';
}

async function submeterLogin(cpfAlvo?: string) {
  const cpf = cpfAlvo || cpfInput.value;
  const digitos = cpf.replace(/\D/g, '');

  if (!digitos) {
    mensagemErro.value = 'Por favor, digite o seu CPF.';
    return;
  }

  carregando.value = true;
  mensagemErro.value = '';

  try {
    const res = await loginPorCpfApi(digitos);
    if (res.autenticado && res.usuario) {
      salvarUsuarioAtivo(res.usuario, res.token);
      emit('loginSucesso', res.usuario);
    } else {
      mensagemErro.value = res.mensagem || 'CPF não encontrado no cadastro do IFFar.';
    }
  } catch (e: any) {
    mensagemErro.value = 'Falha na comunicação com o servidor de autenticação.';
  } finally {
    carregando.value = false;
  }
}

function entrarComoDemo(conta: UsuarioContexto) {
  cpfInput.value = conta.cpfFormatado || conta.cpf;
  submeterLogin(conta.cpf);
}
</script>

<template>
  <div class="min-h-screen bg-linear-to-br from-slate-900 via-slate-800 to-emerald-950 flex flex-col justify-center items-center px-4 py-12 relative overflow-hidden">
    <!-- Efeito decorativo de fundo -->
    <div class="absolute -top-32 -right-32 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none"></div>
    <div class="absolute -bottom-32 -left-32 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl pointer-events-none"></div>

    <div class="max-w-md w-full space-y-8 z-10">
      <!-- Cabeçalho Institucional -->
      <div class="text-center space-y-3">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-emerald-600 text-white font-black text-2xl shadow-xl shadow-emerald-950 ring-4 ring-emerald-500/20">
          A
        </div>
        <div>
          <h1 class="text-2xl font-black text-white tracking-tight">IFFar AVALIA</h1>
          <p class="text-xs text-emerald-400 font-semibold tracking-wide uppercase mt-0.5">
            Autoavaliação Institucional & Devolutivas
          </p>
        </div>
        <p class="text-xs text-slate-300 max-w-sm mx-auto leading-relaxed">
          Sistema Integrado de Gestão do Ciclo Avaliativo do Instituto Federal Farroupilha
        </p>
      </div>

      <!-- Card de Autenticação -->
      <div class="bg-white/95 backdrop-blur-md rounded-3xl p-7 shadow-2xl border border-slate-100 space-y-6">
        <div>
          <h2 class="text-lg font-bold text-slate-800 tracking-tight">Acesso ao Sistema</h2>
          <div class="mt-1 flex items-center gap-1.5 text-xs text-amber-700 bg-amber-50 border border-amber-200/80 px-2.5 py-1.5 rounded-xl">
            <span>ℹ️</span>
            <span>Acesso por <strong>CPF</strong> (em fase experimental, senha desativada).</span>
          </div>
        </div>

        <!-- Alerta de erro -->
        <div
          v-if="mensagemErro"
          class="bg-red-50 border border-red-200 text-red-700 px-3.5 py-2.5 rounded-xl text-xs flex items-center gap-2 animate-shake"
        >
          <span class="text-base">⚠️</span>
          <span class="font-medium">{{ mensagemErro }}</span>
        </div>

        <!-- Formulário de Login -->
        <form @submit.prevent="() => submeterLogin()" class="space-y-4">
          <div>
            <label for="cpf" class="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
              Número do CPF
            </label>
            <div class="relative">
              <input
                id="cpf"
                type="text"
                :value="cpfInput"
                @input="aoDigitarCpf"
                placeholder="000.000.000-00"
                maxlength="14"
                class="w-full px-4 py-3 rounded-xl border border-slate-300 bg-slate-50/50 text-slate-800 text-base font-mono tracking-wider focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white transition-all shadow-inner"
                :disabled="carregando"
                autofocus
              />
              <span class="absolute right-3.5 top-3.5 text-slate-400 text-sm">🪪</span>
            </div>
            <p class="text-[11px] text-slate-600 mt-1">
              Digite os 11 dígitos do seu CPF cadastrado no IFFar.
            </p>
          </div>

          <button
            type="submit"
            :disabled="carregando"
            class="w-full py-3 px-4 rounded-xl font-bold text-sm text-white bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 shadow-lg shadow-emerald-900/30 transition-all flex items-center justify-center gap-2 disabled:opacity-60 cursor-pointer"
          >
            <span v-if="carregando" class="animate-spin text-base">⏳</span>
            <span>{{ carregando ? 'Validando identificação...' : 'Entrar no Sistema' }}</span>
            <span v-if="!carregando">→</span>
          </button>
        </form>

        <!-- Divisor para Acessos Rápidos / Demonstração -->
        <div class="relative pt-2">
          <div class="absolute inset-0 flex items-center" aria-hidden="true">
            <div class="w-full border-t border-slate-200"></div>
          </div>
          <div class="relative flex justify-center text-[10px] uppercase font-bold tracking-widest text-slate-600">
            <span class="bg-white px-2">Acesso Rápido para Testes</span>
          </div>
        </div>

        <!-- Cards de Demonstração Institucional -->
        <div class="grid grid-cols-2 gap-2 pt-1">
          <button
            v-for="conta in contasDemo"
            :key="conta.cpf"
            type="button"
            @click="entrarComoDemo(conta)"
            :disabled="carregando"
            class="p-2.5 rounded-xl border border-slate-200 hover:border-emerald-500 bg-slate-50/80 hover:bg-emerald-50/60 text-left transition-all group flex flex-col justify-between cursor-pointer"
          >
            <div class="flex items-center justify-between w-full mb-1">
              <span class="text-[10px] font-bold px-1.5 py-0.5 rounded-md uppercase"
                :class="{
                  'bg-emerald-100 text-emerald-800': conta.papel === 'CPA',
                  'bg-blue-100 text-blue-800': conta.papel === 'DOCENTE',
                  'bg-purple-100 text-purple-800': conta.papel === 'DISCENTE',
                  'bg-amber-100 text-amber-800': conta.papel === 'GESTAO',
                  'bg-slate-200 text-slate-700': conta.papel === 'TAE'
                }"
              >
                {{ conta.papel }}
              </span>
              <span class="text-xs opacity-40 group-hover:opacity-100 transition-opacity">⚡</span>
            </div>
            <div class="text-[11px] font-bold text-slate-800 truncate" :title="conta.nome">
              {{ conta.nome.split(' ')[0] }} {{ conta.nome.split(' ').slice(-1)[0] }}
            </div>
            <div class="text-[10px] text-slate-600 font-mono mt-0.5">
              {{ conta.cpfFormatado || conta.cpf }}
            </div>
          </button>
        </div>
      </div>

      <!-- Rodapé -->
      <div class="text-center text-slate-400 text-xs">
        PPGTER / UFSM &bull; Instituto Federal Farroupilha (IFFar)
      </div>
    </div>
  </div>
</template>
