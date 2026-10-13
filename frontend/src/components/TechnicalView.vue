<script setup lang="ts">
import { computed, ref } from 'vue';
import type { EstadoPreenchimento, InstrumentoAvaliativo } from '../types/avaliacao';
import { limparEstadoPreenchimento } from '../services/storage';

const props = defineProps<{
  estado: EstadoPreenchimento;
  instrumento: InstrumentoAvaliativo;
}>();

const emit = defineEmits<{
  (e: 'reiniciarTeste'): void;
}>();

const jsonFormatado = computed(() => {
  return JSON.stringify(props.estado, null, 2);
});

const apiEndpoint = ref('POST /api/v1/avaliacao/ciclos/inst-adpd-2026-1/submeter');
const statusSimulacao = ref('');

function testarEnvioApi() {
  statusSimulacao.value = 'Enviando payload para API REST...';
  setTimeout(() => {
    statusSimulacao.value = 'HTTP 200 OK - Respostas persistidas de forma desvinculada (Anonimato garantido).';
  }, 600);
}

function resetar() {
  if (confirm('Deseja limpar as respostas salvas no localStorage para testar novamente do zero?')) {
    limparEstadoPreenchimento();
    emit('reiniciarTeste');
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="bg-slate-900 text-white rounded-2xl p-5 shadow-xs flex flex-col md:flex-row justify-between md:items-center gap-4">
      <div>
        <div class="text-xs uppercase font-mono text-emerald-400 font-semibold tracking-wider">
          Arquitetura Desacoplada (Vite + Vue 3 SPA -> Jakarta EE REST API)
        </div>
        <h3 class="text-base font-bold mt-1">Inspeção Técnica de Dados e Interoperabilidade</h3>
        <p class="text-xs text-slate-300 mt-1">
          Abaixo você pode inspecionar em tempo real o payload JSON que a SPA compõe e envia ao Core Engine.
        </p>
      </div>

      <div class="flex gap-2">
        <button
          type="button"
          @click="resetar"
          class="bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold px-4 py-2 rounded-xl transition-all"
        >
          Limpar LocalStorage e Reiniciar Teste
        </button>
      </div>
    </div>

    <!-- Simulação da Requisição REST -->
    <div class="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs space-y-3">
      <div class="flex justify-between items-center text-xs">
        <span class="font-bold text-slate-700">Endpoint REST Projetado:</span>
        <span class="font-mono bg-slate-100 text-slate-800 px-2 py-1 rounded">{{ apiEndpoint }}</span>
      </div>

      <div class="flex gap-3">
        <button
          type="button"
          @click="testarEnvioApi"
          class="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold px-4 py-2 rounded-xl transition-all"
        >
          Simular Chamada da API REST
        </button>
        <span v-if="statusSimulacao" class="text-xs text-emerald-700 font-semibold self-center">
          {{ statusSimulacao }}
        </span>
      </div>
    </div>

    <!-- Payload JSON do Estado de Resposta -->
    <div class="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs space-y-2">
      <div class="flex justify-between items-center text-xs">
        <span class="font-bold text-slate-800">Payload JSON do Estado Atual (LocalStorage / Auto-Save):</span>
        <span class="text-slate-400 font-mono">{{ Object.keys(estado.respostas).length }} respostas preenchidas</span>
      </div>
      <pre class="bg-slate-950 text-emerald-400 p-4 rounded-xl text-xs font-mono overflow-x-auto max-h-96">{{ jsonFormatado }}</pre>
    </div>
  </div>
</template>
