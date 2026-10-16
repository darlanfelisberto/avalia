<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { buscarStatusSincronizacaoApi, executarSincronizacaoApi, type SincronizacaoStatus } from '../services/api';

const status = ref<SincronizacaoStatus | null>(null);
const carregando = ref(false);
const disparando = ref(false);
const mensagemFeedback = ref<{ texto: string; tipo: 'sucesso' | 'erro' | 'info' } | null>(null);

const anoSelecionado = ref(2026);
const periodoSelecionado = ref<number | ''>(1);
const syncEstrutura = ref(true);
const syncTurmas = ref(true);

let timerPolling: any = null;

async function carregarStatus() {
  carregando.value = true;
  try {
    const dados = await buscarStatusSincronizacaoApi();
    status.value = dados;
  } finally {
    carregando.value = false;
  }
}

async function dispararSincronizacao() {
  disparando.value = true;
  mensagemFeedback.value = { texto: 'Disparando sincronização via aplicação...', tipo: 'info' };
  try {
    const res = await executarSincronizacaoApi({
      ano: Number(anoSelecionado.value),
      periodo: periodoSelecionado.value === '' ? null : Number(periodoSelecionado.value),
      sincronizarEstrutura: syncEstrutura.value,
      sincronizarTurmasMatriculas: syncTurmas.value
    });
    if (res.sucesso) {
      mensagemFeedback.value = { texto: res.mensagem, tipo: 'sucesso' };
      await carregarStatus();
    } else {
      mensagemFeedback.value = { texto: res.mensagem, tipo: 'erro' };
    }
  } catch (e: any) {
    mensagemFeedback.value = { texto: e?.message || 'Erro ao sincronizar', tipo: 'erro' };
  } finally {
    disparando.value = false;
  }
}

onMounted(() => {
  carregarStatus();
  // Polling a cada 4 segundos para acompanhar o status
  timerPolling = setInterval(() => {
    carregarStatus();
  }, 4000);
});

onUnmounted(() => {
  if (timerPolling) clearInterval(timerPolling);
});

</script>

<template>
  <div class="space-y-6">
    <!-- Cabeçalho do Módulo -->
    <header class="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <div class="flex items-center gap-2">
          <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            Módulo Acadêmico & RH
          </span>
          <span v-if="status?.conectadoSigaa" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500 text-white">
            ● SIGAA Conectado
          </span>
          <span v-else class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500 text-white">
            ○ SIGAA Desconectado
          </span>
        </div>
        <h1 class="text-2xl font-bold text-slate-900 mt-2">Sincronização & Integração SIGAA / SIGRH</h1>
        <p class="text-sm text-slate-500 mt-1">
          Carga de estrutura organizacional, servidores, discentes, turmas e matrículas para o schema local <code class="text-emerald-700 bg-emerald-50 px-1 py-0.5 rounded font-mono text-xs">academico</code>.
        </p>
      </div>

      <div class="flex items-center gap-3">
        <button
          type="button"
          @click="carregarStatus"
          :disabled="carregando"
          class="px-4 py-2 text-xs font-semibold rounded-xl border border-slate-300 text-slate-700 bg-white hover:bg-slate-50 transition-all shadow-xs"
        >
          🔄 Atualizar Status
        </button>
      </div>
    </header>

    <!-- Feedback Banner -->
    <div
      v-if="mensagemFeedback"
      :class="[
        'p-4 rounded-xl text-xs font-medium border flex items-center justify-between',
        mensagemFeedback.tipo === 'sucesso' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' :
        mensagemFeedback.tipo === 'erro' ? 'bg-rose-50 border-rose-200 text-rose-800' :
        'bg-blue-50 border-blue-200 text-blue-800'
      ]"
    >
      <span>{{ mensagemFeedback.texto }}</span>
      <button @click="mensagemFeedback = null" class="text-slate-400 hover:text-slate-600">✕</button>
    </div>

    <!-- Cards de Status e Conexão -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
        <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-1">Origem dos Dados</div>
        <div class="text-sm font-bold text-slate-800 truncate" :title="status?.urlSigaa">
          {{ status?.urlSigaa || 'Configurando...' }}
        </div>
        <div class="text-xs text-slate-500 mt-2 flex items-center gap-2">
          <span>Usuário: <code class="bg-slate-100 px-1 py-0.5 rounded font-mono">{{ status?.usuarioSigaa }}</code></span>
        </div>
      </div>

      <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
        <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-1">Estado da Rotina</div>
        <div class="flex items-center gap-2 mt-1">
          <span
            v-if="status?.emExecucao"
            class="inline-flex items-center gap-1.5 text-amber-600 font-bold text-sm animate-pulse"
          >
            ⏳ Sincronização em Andamento...
          </span>
          <span
            v-else
            class="inline-flex items-center gap-1.5 text-emerald-600 font-bold text-sm"
          >
            ✓ Sistema Ocioso (Pronto para carga)
          </span>
        </div>
        <div class="text-[11px] text-slate-500 mt-2">
          Execução nativa no backend Java Open Liberty
        </div>
      </div>

      <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
        <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-1">Schema de Destino</div>
        <div class="text-sm font-bold text-slate-800">
          PostgreSQL <span class="text-emerald-600">academico</span> (Local)
        </div>
        <div class="text-[11px] text-slate-500 mt-2">
          Suporte à navegação hierárquica por <code class="bg-slate-100 px-1 py-0.5 rounded font-mono">ltree</code> e Closure table
        </div>
      </div>
    </div>

    <!-- Painel de Disparo de Sincronização -->
    <div class="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs space-y-4">
      <div class="flex items-center justify-between border-b border-slate-100 pb-4">
        <div>
          <h2 class="text-base font-bold text-slate-900">Configurar e Executar Carga</h2>
          <p class="text-xs text-slate-500 mt-0.5">Selecione os parâmetros e o ciclo letivo para sincronizar com o SIGAA.</p>
        </div>

        <button
          type="button"
          @click="dispararSincronizacao"
          :disabled="disparando || status?.emExecucao"
          :class="[
            'px-5 py-2.5 text-xs font-bold rounded-xl text-white shadow-sm transition-all flex items-center gap-2',
            (disparando || status?.emExecucao)
              ? 'bg-slate-400 cursor-not-allowed'
              : 'bg-emerald-600 hover:bg-emerald-700 active:scale-98 shadow-emerald-900/20'
          ]"
        >
          <span v-if="disparando || status?.emExecucao" class="animate-spin">🔄</span>
          <span v-else>🚀</span>
          <span>{{ (disparando || status?.emExecucao) ? 'Sincronizando...' : 'Iniciar Sincronização' }}</span>
        </button>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 pt-2">
        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1">Ano Letivo (Mínimo)</label>
          <input
            v-model.number="anoSelecionado"
            type="number"
            min="2020"
            max="2030"
            class="w-full text-xs px-3 py-2 border border-slate-300 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none"
          />
        </div>

        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1">Período Semestral</label>
          <select
            v-model="periodoSelecionado"
            class="w-full text-xs px-3 py-2 border border-slate-300 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none bg-white"
          >
            <option :value="''">Todos os períodos</option>
            <option :value="1">1º Semestre</option>
            <option :value="2">2º Semestre</option>
            <option :value="0">Anual (0)</option>
          </select>
        </div>

        <div class="flex items-center gap-2 pt-6">
          <input
            id="chkEstrutura"
            type="checkbox"
            v-model="syncEstrutura"
            class="rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4"
          />
          <label for="chkEstrutura" class="text-xs font-medium text-slate-700 cursor-pointer">
            Estrutura (Unidades, Pessoas, Servidores, Cursos)
          </label>
        </div>

        <div class="flex items-center gap-2 pt-6">
          <input
            id="chkTurmas"
            type="checkbox"
            v-model="syncTurmas"
            class="rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4"
          />
          <label for="chkTurmas" class="text-xs font-medium text-slate-700 cursor-pointer">
            Acadêmico (Turmas, Docentes e Matrículas)
          </label>
        </div>
      </div>
    </div>

    <!-- Contadores Locais da Base Acadêmica -->
    <div class="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs">
      <h2 class="text-base font-bold text-slate-900 mb-1">Estatísticas do Schema Local <span class="text-emerald-700 font-mono text-sm">academico</span></h2>
      <p class="text-xs text-slate-500 mb-4">Volume de dados espelhado e disponível no banco AVALIA para consultas em milissegundos.</p>

      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3">
        <div
          v-for="(qtd, tabela) in status?.contadoresLocais"
          :key="tabela"
          class="p-3 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between"
        >
          <span class="text-[11px] font-semibold text-slate-500 uppercase tracking-wider font-mono truncate" :title="tabela">
            {{ tabela }}
          </span>
          <span class="text-lg font-bold text-slate-900 mt-2 font-mono">
            {{ qtd.toLocaleString('pt-BR') }}
          </span>
        </div>
      </div>
    </div>

  </div>
</template>
