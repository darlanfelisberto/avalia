<script setup lang="ts">
import { ref } from 'vue';
import type { InstrumentoAvaliativo, EncaminhamentoGestao } from '../types/avaliacao';
import { salvarEncaminhamentoGestao, carregarEncaminhamentosGestao } from '../services/storage';

const props = defineProps<{
  instrumento: InstrumentoAvaliativo;
}>();

const encaminhamentos = ref<EncaminhamentoGestao[]>(carregarEncaminhamentosGestao());

const responsavelNome = ref('Profa. Coordenadora de Curso ADS');
const papel = ref<'COORDENADOR' | 'DIRECAO_ENSINO' | 'CPA'>('COORDENADOR');
const tipoDecisao = ref<'PLANO_DE_ACAO' | 'ORIENTACAO_PEDAGOGICA' | 'SEM_ACAO_NECESSARIA' | 'ENCAMINHAMENTO_CPPD'>('PLANO_DE_ACAO');
const detalhamento = ref('');
const prazoRevisao = ref('2026-12-15');
const mensagemSucesso = ref(false);

const cicloConsolidado = ref(encaminhamentos.value.length > 0);

function registrarEncaminhamento() {
  if (!detalhamento.value.trim()) return;

  const novo: EncaminhamentoGestao = {
    cicloId: props.instrumento.id,
    responsavelNome: responsavelNome.value,
    papel: papel.value,
    tipoDecisao: tipoDecisao.value,
    detalhamento: detalhamento.value,
    prazoRevisao: prazoRevisao.value,
    dataRegistro: new Date().toISOString()
  };

  salvarEncaminhamentoGestao(novo);
  encaminhamentos.value = carregarEncaminhamentosGestao();
  cicloConsolidado.value = true;
  detalhamento.value = '';
  mensagemSucesso.value = true;
  setTimeout(() => {
    mensagemSucesso.value = false;
  }, 4000);
}
</script>

<template>
  <div class="space-y-6">
    <!-- Fundamentação Teórica da Dissertação (Luckesi 2011) -->
    <div class="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start gap-3.5 shadow-2xs">
      <div class="text-2xl text-amber-700 select-none">⚖️</div>
      <div class="space-y-1">
        <h2 class="text-sm font-semibold text-amber-950">
          Requisito Transversal RF14: Fechamento do Ciclo Avaliativo (Luckesi, 2011)
        </h2>
        <p class="text-xs text-amber-900 leading-relaxed">
          Para superar o diagnóstico de relatórios que terminam na gaveta (mera verificação cartorial), 
          <strong>o sistema bloqueia a consolidação do ciclo avaliativo</strong> até que o gestor responsável registre 
          formalmente uma tomada de decisão: plano de ação de melhoria, orientação pedagógica ou justificativa motivada de não ação.
        </p>
      </div>
    </div>

    <!-- Status de Consolidação do Ciclo (Regra do RF14) -->
    <div class="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <div class="text-xs font-semibold uppercase text-slate-400 tracking-wider">Estado do Ciclo Institucional</div>
        <div class="text-base font-bold text-slate-900 flex items-center gap-2 mt-1">
          <span>{{ instrumento.titulo }} ({{ instrumento.anoCiclo }})</span>
          <span
            :class="[
              'text-xs px-2.5 py-0.5 rounded-full font-bold',
              cicloConsolidado ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800 animate-pulse'
            ]"
          >
            {{ cicloConsolidado ? 'CICLO CONSOLIDADO COM DECISÃO' : 'BLOQUEADO: AGUARDANDO ENCAMINHAMENTO (RF14)' }}
          </span>
        </div>
      </div>

      <div class="text-xs text-slate-500 bg-slate-50 p-3 rounded-xl border border-slate-200">
        <span class="font-bold text-slate-700">Regra de Negócio RF14:</span>
        Nenhum ciclo pode ser arquivado sem plano de ação registrado no banco.
      </div>
    </div>

    <!-- Histórico de Encaminhamentos Registrados -->
    <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs space-y-4">
      <h3 class="text-sm font-bold text-slate-900 uppercase tracking-wide">
        Encaminhamentos e Decisões Registradas neste Ciclo
      </h3>

      <div v-if="encaminhamentos.length === 0" class="p-6 text-center text-xs text-slate-400 bg-slate-50 rounded-xl border border-dashed border-slate-300">
        Nenhum encaminhamento registrado até o momento. O ciclo permanece em aberto.
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="(enc, idx) in encaminhamentos"
          :key="idx"
          class="border border-slate-200 bg-slate-50/50 p-4 rounded-xl space-y-2 text-xs"
        >
          <div class="flex justify-between items-center">
            <span class="font-bold text-slate-900">{{ enc.responsavelNome }} ({{ enc.papel }})</span>
            <span class="text-slate-400">{{ new Date(enc.dataRegistro).toLocaleString('pt-BR') }}</span>
          </div>
          <div class="flex items-center gap-2">
            <span class="bg-emerald-100 text-emerald-900 font-bold px-2 py-0.5 rounded text-[10px]">
              {{ enc.tipoDecisao }}
            </span>
            <span v-if="enc.prazoRevisao" class="text-slate-500 text-[11px]">
              Prazo de execução: {{ enc.prazoRevisao }}
            </span>
          </div>
          <p class="text-slate-700 leading-relaxed font-serif text-sm bg-white p-3 rounded-lg border border-slate-100">
            "{{ enc.detalhamento }}"
          </p>
        </div>
      </div>
    </div>

    <!-- FORMULÁRIO DE REGISTRO DO ENCAMINHAMENTO (RF14) -->
    <div class="bg-white border-2 border-amber-500/30 rounded-2xl p-5 md:p-7 shadow-xs space-y-5">
      <div class="border-b border-slate-100 pb-3">
        <span class="text-xs bg-amber-100 text-amber-900 font-bold px-2 py-0.5 rounded-full uppercase">
          Procedimento Decisório Obrigatório
        </span>
        <h3 class="text-base font-bold text-slate-900 mt-2">
          Registrar Plano de Ação ou Encaminhamento Institucional
        </h3>
        <p class="text-xs text-slate-500 mt-1">
          Ao registrar uma deliberação, os dados avaliativos são formalmente convertidos em instrumentos de governança.
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="space-y-1.5">
          <label class="text-xs font-bold text-slate-700">Responsável pelo Registro:</label>
          <input
            v-model="responsavelNome"
            type="text"
            class="w-full text-xs p-3 border border-slate-200 rounded-xl bg-slate-50/50 focus:bg-white"
          />
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-bold text-slate-700">Tipo de Decisão / Encaminhamento:</label>
          <select
            v-model="tipoDecisao"
            class="w-full text-xs p-3 border border-slate-200 rounded-xl bg-slate-50/50 focus:bg-white font-medium"
          >
            <option value="PLANO_DE_ACAO">Plano de Ação e Melhorias (PDI/Ensino)</option>
            <option value="ORIENTACAO_PEDAGOGICA">Orientação e Apoio Pedagógico Agendado</option>
            <option value="ENCAMINHAMENTO_CPPD">Encaminhamento Formal à CPPD</option>
            <option value="SEM_ACAO_NECESSARIA">Justificativa Formal de Não Ação</option>
          </select>
        </div>
      </div>

      <div class="space-y-1.5">
        <label class="text-xs font-bold text-slate-700">
          Detalhamento da Decisão / Plano de Trabalho (Exigência Luckesi):
        </label>
        <textarea
          v-model="detalhamento"
          rows="4"
          placeholder="Descreva as medidas pactuadas (ex: aquisição de novos equipamentos de laboratório citados no feedback, reorganização do cronograma de avaliações, etc)..."
          class="w-full text-xs md:text-sm p-3.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-amber-500/20 focus:border-amber-600 outline-hidden bg-slate-50/40"
        ></textarea>
      </div>

      <div v-if="mensagemSucesso" class="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 font-semibold flex items-center gap-2">
        <span>✓</span>
        <span>Encaminhamento registrado com sucesso! Ciclo consolidado em conformidade com o RF14.</span>
      </div>

      <div class="flex justify-end pt-2">
        <button
          type="button"
          @click="registrarEncaminhamento"
          :disabled="!detalhamento.trim()"
          class="bg-amber-600 hover:bg-amber-700 disabled:bg-slate-300 text-white font-bold px-6 py-2.5 rounded-xl text-xs shadow-md transition-all flex items-center gap-2"
        >
          <span>Consolidar Ciclo com Encaminhamento (RF14)</span>
        </button>
      </div>
    </div>
  </div>
</template>
