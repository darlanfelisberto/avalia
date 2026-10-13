<script setup lang="ts">
import { ref, computed } from 'vue';
import type { InstrumentoAvaliativo, ManifestacaoDocente } from '../types/avaliacao';
import { salvarManifestacaoDocente, carregarManifestacoesDocentes } from '../services/storage';

const props = defineProps<{
  instrumento: InstrumentoAvaliativo;
}>();

const componenteSelecionadoId = ref(props.instrumento.componentesMatriculados[0].id);

const componente = computed(() => {
  return props.instrumento.componentesMatriculados.find(c => c.id === componenteSelecionadoId.value)
    || props.instrumento.componentesMatriculados[0];
});

// Mock de estatísticas agregadas geradas para o docente (RF-AD04)
const estatisticasDocente = computed(() => {
  return {
    totalRespondentes: 28,
    percentualParticipacao: 87.5,
    mediaGeral: 4.65,
    mediasDimensoes: [
      { dimensao: 'Domínio de Conteúdo e Metodologia', nota: 4.8, max: 5 },
      { dimensao: 'Clareza no Plano de Ensino', nota: 4.7, max: 5 },
      { dimensao: 'Pontualidade no Feedback de Provas', nota: 4.2, max: 5 },
      { dimensao: 'Relação Ética e Dialógica', nota: 4.9, max: 5 }
    ],
    comentariosDiscentes: [
      'Excelente didática, exemplos práticos que ajudam muito na compreensão.',
      'Gostaria de mais exercícios práticos de fixação antes das avaliações formais.',
      'Muito acessível no horário de atendimento aos alunos.'
    ]
  };
});

// Estado do formulário de manifestação do docente (RF-AD07)
const manifestacoesSalvas = ref<Record<string, ManifestacaoDocente>>(carregarManifestacoesDocentes());
const comentarioDocente = ref(manifestacoesSalvas.value[componenteSelecionadoId.value]?.comentario || '');
const planoDocente = ref(manifestacoesSalvas.value[componenteSelecionadoId.value]?.planoDesenvolvimento || '');
const mensagemSucesso = ref(false);

function salvarManifestacao() {
  const novaManifestacao: ManifestacaoDocente = {
    docenteNome: componente.value.docenteNome,
    componenteId: componente.value.id,
    comentario: comentarioDocente.value,
    planoDesenvolvimento: planoDocente.value,
    dataRegistro: new Date().toISOString()
  };

  salvarManifestacaoDocente(novaManifestacao);
  manifestacoesSalvas.value = carregarManifestacoesDocentes();
  mensagemSucesso.value = true;
  setTimeout(() => {
    mensagemSucesso.value = false;
  }, 4000);
}

function aoTrocarComponente(id: string) {
  componenteSelecionadoId.value = id;
  const existente = manifestacoesSalvas.value[id];
  comentarioDocente.value = existente?.comentario || '';
  planoDocente.value = existente?.planoDesenvolvimento || '';
  mensagemSucesso.value = false;
}
</script>

<template>
  <div class="space-y-6">
    <!-- Embasamento Teórico da Dissertação (Hoffmann 2012) -->
    <div class="bg-blue-50 border border-blue-200 rounded-xl p-4 flex items-start gap-3.5 shadow-2xs">
      <div class="text-2xl text-blue-700 select-none">💬</div>
      <div class="space-y-1">
        <h2 class="text-sm font-semibold text-blue-950">
          Requisito RF-AD07: Avaliação Mediadora e Dialógica (Hoffmann, 2012)
        </h2>
        <p class="text-xs text-blue-800 leading-relaxed">
          Diferente dos sistemas convencionais onde o docente apenas recebe passivamente uma nota, este requisito 
          assegura ao professor o direito institucional de <strong>registrar sua manifestação pedagógica e plano de melhorias</strong> 
          em resposta aos dados recebidos, integrando-o formalmente ao processo de progressão (CPPD).
        </p>
      </div>
    </div>

    <!-- Seletor de Disciplina do Docente -->
    <div class="bg-white border border-slate-200 rounded-2xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-xs">
      <div>
        <label class="text-xs font-semibold uppercase text-slate-500 tracking-wider">Visualizando Relatório do Componente:</label>
        <div class="font-bold text-slate-900 text-sm mt-0.5">{{ componente.nome }} ({{ componente.codigo }})</div>
      </div>
      <div class="flex gap-2">
        <button
          v-for="comp in instrumento.componentesMatriculados"
          :key="comp.id"
          type="button"
          @click="aoTrocarComponente(comp.id)"
          :class="[
            'px-3 py-1.5 rounded-lg text-xs font-medium border transition-all',
            componenteSelecionadoId === comp.id
              ? 'bg-blue-600 text-white border-blue-600 shadow-xs'
              : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
          ]"
        >
          {{ comp.codigo }}
        </button>
      </div>
    </div>

    <!-- Painel de Resultados Agregados e Médias (RF-AD04) -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
        <div class="text-xs text-slate-500 font-medium">Média Global do Docente</div>
        <div class="text-3xl font-extrabold text-emerald-600 mt-2">
          {{ estatisticasDocente.mediaGeral }} <span class="text-xs text-slate-400 font-normal">/ 5.0</span>
        </div>
        <div class="text-[11px] text-slate-400 mt-1">Conceito: Muito Bom / Excelente</div>
      </div>

      <div class="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
        <div class="text-xs text-slate-500 font-medium">Taxa de Participação Discente</div>
        <div class="text-3xl font-extrabold text-slate-800 mt-2">
          {{ estatisticasDocente.percentualParticipacao }}%
        </div>
        <div class="text-[11px] text-slate-400 mt-1">{{ estatisticasDocente.totalRespondentes }} discentes responderam</div>
      </div>

      <div class="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
        <div class="text-xs text-slate-500 font-medium">Ciclo Avaliativo</div>
        <div class="text-base font-bold text-slate-800 mt-2">
          {{ instrumento.anoCiclo }} (Semestral)
        </div>
        <div class="text-[11px] text-slate-400 mt-1">Válido para Progressão CPPD</div>
      </div>
    </div>

    <!-- Detalhamento por Dimensão Avaliativa -->
    <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs space-y-4">
      <h3 class="text-sm font-bold text-slate-900 uppercase tracking-wide">
        Médias por Indicador Pedagógico
      </h3>
      <div class="space-y-3">
        <div
          v-for="dim in estatisticasDocente.mediasDimensoes"
          :key="dim.dimensao"
          class="space-y-1"
        >
          <div class="flex justify-between text-xs font-medium text-slate-700">
            <span>{{ dim.dimensao }}</span>
            <span class="font-bold text-slate-900">{{ dim.nota }} / {{ dim.max }}</span>
          </div>
          <div class="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
            <div
              class="bg-blue-600 h-2.5 rounded-full"
              :style="{ width: `${(dim.nota / dim.max) * 100}%` }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Comentários Textuais Anônimos dos Alunos -->
    <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs space-y-3">
      <h3 class="text-sm font-bold text-slate-900 uppercase tracking-wide">
        Apontamentos Anônimos da Turma (Questão Aberta)
      </h3>
      <div class="space-y-2">
        <blockquote
          v-for="(coment, idx) in estatisticasDocente.comentariosDiscentes"
          :key="idx"
          class="p-3 bg-slate-50 border-l-3 border-emerald-500 rounded-r-xl text-xs text-slate-700 italic"
        >
          "{{ coment }}"
        </blockquote>
      </div>
    </div>

    <!-- FORMULÁRIO DE MANIFESTAÇÃO DO DOCENTE (RF-AD07) -->
    <div class="bg-white border-2 border-blue-500/30 rounded-2xl p-5 md:p-7 shadow-xs space-y-5">
      <div class="border-b border-slate-100 pb-3">
        <span class="text-xs bg-blue-100 text-blue-900 font-bold px-2 py-0.5 rounded-full uppercase">
          Requisito Inédito RF-AD07
        </span>
        <h3 class="text-base font-bold text-slate-900 mt-2">
          Registro de Manifestação e Plano de Desenvolvimento Individual
        </h3>
        <p class="text-xs text-slate-500 mt-1 leading-relaxed">
          Registre sua reflexão sobre o feedback da turma. Este registro será vinculado ao seu processo de progressão 
          e ficará acessível à Direção de Ensino e à CPPD institucional.
        </p>
      </div>

      <div class="space-y-4">
        <div class="space-y-1.5">
          <label class="text-xs font-bold text-slate-700">
            1. Considerações e Reflexões do Docente sobre os Resultados:
          </label>
          <textarea
            v-model="comentarioDocente"
            rows="3"
            placeholder="Ex: Concordo com os apontamentos sobre a necessidade de mais tempo para devolutiva das listas práticas..."
            class="w-full text-xs md:text-sm p-3.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600 outline-hidden bg-slate-50/40"
          ></textarea>
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-bold text-slate-700">
            2. Ações ou Plano de Desenvolvimento Pessoal para o Próximo Semestre:
          </label>
          <textarea
            v-model="planoDocente"
            rows="3"
            placeholder="Ex: Vou reestruturar o cronograma para incluir plantões semanais de exercícios e utilizar o Moodle para devolutiva digital mais célere..."
            class="w-full text-xs md:text-sm p-3.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600 outline-hidden bg-slate-50/40"
          ></textarea>
        </div>

        <div v-if="mensagemSucesso" class="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 font-semibold flex items-center gap-2">
          <span>✓</span>
          <span>Manifestação e Plano salvos com sucesso e integrados ao histórico do ciclo!</span>
        </div>

        <div class="flex justify-end pt-2">
          <button
            type="button"
            @click="salvarManifestacao"
            class="bg-blue-600 hover:bg-blue-700 text-white font-bold px-6 py-2.5 rounded-xl text-xs shadow-md transition-all flex items-center gap-2"
          >
            <span>Salvar Manifestação Docente (RF-AD07)</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
