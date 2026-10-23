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
  <div class="page-stack">
    <!-- Cabeçalho do Módulo -->
    <header class="panel panel--spacious sync-header">
      <div>
        <div class="sync-header__badges">
          <span class="sync-badge sync-badge--module">
            Módulo Acadêmico & RH
          </span>
          <span v-if="status?.conectadoSigaa" class="sync-badge sync-badge--connected">
            ● SIGAA Conectado
          </span>
          <span v-else class="sync-badge sync-badge--disconnected">
            ○ SIGAA Desconectado
          </span>
        </div>
        <h1 class="sync-header__title">Sincronização & Integração SIGAA / SIGRH</h1>
        <p class="sync-header__description">
          Carga de estrutura organizacional, servidores, discentes, turmas e matrículas para o schema local <code class="sync-code sync-code--brand">academico</code>.
        </p>
      </div>

      <div class="sync-header__actions">
        <button
          type="button"
          @click="carregarStatus"
          :disabled="carregando"
          class="button button--secondary sync-refresh"
        >
          🔄 Atualizar Status
        </button>
      </div>
    </header>

    <!-- Feedback Banner -->
    <div
      v-if="mensagemFeedback"
      :class="[
        'sync-feedback',
        mensagemFeedback.tipo === 'sucesso' ? 'sync-feedback--success' :
        mensagemFeedback.tipo === 'erro' ? 'sync-feedback--error' :
        'sync-feedback--info'
      ]"
    >
      <span>{{ mensagemFeedback.texto }}</span>
      <button @click="mensagemFeedback = null" class="sync-feedback__close">✕</button>
    </div>

    <!-- Cards de Status e Conexão -->
    <div class="sync-status-grid">
      <div class="panel panel--padded sync-status-card">
        <div class="sync-status-card__label">Origem dos Dados</div>
        <div class="sync-status-card__value sync-status-card__value--truncate" :title="status?.urlSigaa">
          {{ status?.urlSigaa || 'Configurando...' }}
        </div>
        <div class="sync-status-card__detail">
          <span>Usuário: <code class="sync-code">{{ status?.usuarioSigaa }}</code></span>
        </div>
      </div>

      <div class="panel panel--padded sync-status-card">
        <div class="sync-status-card__label">Estado da Rotina</div>
        <div class="sync-status-card__state">
          <span
            v-if="status?.emExecucao"
            class="sync-state sync-state--running"
          >
            ⏳ Sincronização em Andamento...
          </span>
          <span
            v-else
            class="sync-state sync-state--idle"
          >
            ✓ Sistema Ocioso (Pronto para carga)
          </span>
        </div>
        <div class="sync-status-card__detail">
          Execução nativa no backend Java Open Liberty
        </div>
      </div>

      <div class="panel panel--padded sync-status-card">
        <div class="sync-status-card__label">Schema de Destino</div>
        <div class="sync-status-card__value">
          PostgreSQL <span class="sync-highlight">academico</span> (Local)
        </div>
        <div class="sync-status-card__detail">
          Suporte à navegação hierárquica por <code class="sync-code">ltree</code> e Closure table
        </div>
      </div>
    </div>

    <!-- Painel de Disparo de Sincronização -->
    <div class="panel panel--spacious sync-panel">
      <div class="sync-panel__header">
        <div>
          <h2 class="sync-panel__title">Configurar e Executar Carga</h2>
          <p class="sync-panel__description">Selecione os parâmetros e o ciclo letivo para sincronizar com o SIGAA.</p>
        </div>

        <button
          type="button"
          @click="dispararSincronizacao"
          :disabled="disparando || status?.emExecucao"
          :class="[
            'button sync-run-button',
            (disparando || status?.emExecucao)
              ? 'sync-run-button--disabled'
              : 'sync-run-button--ready'
          ]"
        >
          <span v-if="disparando || status?.emExecucao" class="sync-run-button__icon">🔄</span>
          <span v-else>🚀</span>
          <span>{{ (disparando || status?.emExecucao) ? 'Sincronizando...' : 'Iniciar Sincronização' }}</span>
        </button>
      </div>

      <div class="sync-form-grid">
        <div>
          <label class="sync-form__label">Ano Letivo (Mínimo)</label>
          <input
            v-model.number="anoSelecionado"
            type="number"
            min="2020"
            max="2030"
            class="form-field sync-form__control"
          />
        </div>

        <div>
          <label class="sync-form__label">Período Semestral</label>
          <select
            v-model="periodoSelecionado"
            class="form-field sync-form__control"
          >
            <option :value="''">Todos os períodos</option>
            <option :value="1">1º Semestre</option>
            <option :value="2">2º Semestre</option>
            <option :value="0">Anual (0)</option>
          </select>
        </div>

        <div class="sync-form__checkbox-row">
          <input
            id="chkEstrutura"
            type="checkbox"
            v-model="syncEstrutura"
            class="sync-form__checkbox"
          />
          <label for="chkEstrutura" class="sync-form__checkbox-label">
            Estrutura (Unidades, Pessoas, Servidores, Cursos)
          </label>
        </div>

        <div class="sync-form__checkbox-row">
          <input
            id="chkTurmas"
            type="checkbox"
            v-model="syncTurmas"
            class="sync-form__checkbox"
          />
          <label for="chkTurmas" class="sync-form__checkbox-label">
            Acadêmico (Turmas, Docentes e Matrículas)
          </label>
        </div>
      </div>
    </div>

    <!-- Contadores Locais da Base Acadêmica -->
    <div class="panel panel--spacious sync-panel">
      <h2 class="sync-panel__title sync-panel__title--spaced">Estatísticas do Schema Local <span class="sync-highlight sync-highlight--code">academico</span></h2>
      <p class="sync-panel__description sync-panel__description--spaced">Volume de dados espelhado e disponível no banco AVALIA para consultas em milissegundos.</p>

      <div class="sync-stats-grid">
        <div
          v-for="(qtd, tabela) in status?.contadoresLocais"
          :key="tabela"
          class="sync-stat"
        >
          <span class="sync-stat__name" :title="tabela">
            {{ tabela }}
          </span>
          <span class="sync-stat__value">
            {{ qtd.toLocaleString('pt-BR') }}
          </span>
        </div>
      </div>
    </div>

  </div>
</template>
