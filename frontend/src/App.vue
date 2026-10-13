<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import Sidebar from './components/Sidebar.vue';
import HeaderNav from './components/HeaderNav.vue';
import StudentRespondentView from './components/StudentRespondentView.vue';
import InstrumentBuilderView from './components/InstrumentBuilderView.vue';
import PublicosAlvosView from './components/PublicosAlvosView.vue';
import TeacherFeedbackView from './components/TeacherFeedbackView.vue';
import ManagementActionPlanView from './components/ManagementActionPlanView.vue';
import TechnicalView from './components/TechnicalView.vue';

import type { RespostaItem, EstadoPreenchimento, InstrumentoAvaliativo } from './types/avaliacao';
import { carregarEstadoPreenchimento, salvarEstadoPreenchimento, carregarInstrumentoAtivo } from './services/storage';
import { listarInstrumentosApi } from './services/api';

const instrumento = ref<InstrumentoAvaliativo>(carregarInstrumentoAtivo());

// Visão inicial definida como "Cadastro de Questionário" (conforme solicitado)
const visaoAtual = ref<'builder' | 'catalogo' | 'discente' | 'docente' | 'gestao' | 'tecnica'>('builder');
const menuAbertoMobile = ref(false);

onMounted(async () => {
  const lista = await listarInstrumentosApi();
  if (lista && lista.length > 0) {
    // Dá preferência ao instrumento com dados completos de disciplinas/questões (ADPD) ou primeiro
    const adpd = lista.find(i => i.id === 'inst-adpd-2026-1');
    const escolhido = adpd || lista[0];
    instrumento.value = escolhido;
    const novoEstado = carregarEstadoPreenchimento(escolhido.id, escolhido.componentesMatriculados?.[0]?.id || '');
    Object.assign(estado, novoEstado);
  }
});

const estado = reactive<EstadoPreenchimento>(
  carregarEstadoPreenchimento(instrumento.value.id, instrumento.value.componentesMatriculados?.[0]?.id || '')
);

const statusSalvamento = ref<'salvo' | 'salvando' | 'erro'>('salvo');
const ultimaSalvaHora = ref(new Date().toLocaleTimeString('pt-BR'));

// Calcula total de itens obrigatórios (Autoavaliação + por disciplina)
const totalQuestoesObrigatorias = computed(() => {
  let count = 0;
  // Autoavaliação discente
  for (const g of instrumento.value.gruposAutoavaliacaoDiscente) {
    for (const q of g.questoes) {
      if (q.obrigatoria) count++;
    }
  }
  // Por cada componente curricular matriculado
  const obrigPorComp = instrumento.value.gruposAvaliacaoDocente.reduce((acc, g) => {
    return acc + g.questoes.filter(q => q.obrigatoria).length;
  }, 0);
  count += obrigPorComp * instrumento.value.componentesMatriculados.length;
  return count;
});

// Calcula total de respostas obrigatórias já preenchidas
const respostasConcluidas = computed(() => {
  let count = 0;
  // Autoavaliação
  for (const g of instrumento.value.gruposAutoavaliacaoDiscente) {
    for (const q of g.questoes) {
      if (q.obrigatoria) {
        const resp = estado.respostas[`${q.id}_geral`];
        if (resp && (resp.valorEscala !== undefined || resp.opcaoId)) count++;
      }
    }
  }
  // Docente por componente
  for (const comp of instrumento.value.componentesMatriculados) {
    for (const g of instrumento.value.gruposAvaliacaoDocente) {
      for (const q of g.questoes) {
        if (q.obrigatoria) {
          const resp = estado.respostas[`${q.id}_${comp.id}`];
          if (resp && (resp.valorEscala !== undefined || resp.opcaoId)) count++;
        }
      }
    }
  }
  return count;
});

let debounceTimer: ReturnType<typeof setTimeout> | null = null;

function registrarResposta(item: RespostaItem) {
  const chave = `${item.questaoId}_${item.componenteId || 'geral'}`;
  estado.respostas[chave] = item;

  // Feedback visual de salvamento contínuo (RF06)
  statusSalvamento.value = 'salvando';

  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    salvarEstadoPreenchimento(estado);
    statusSalvamento.value = 'salvo';
    ultimaSalvaHora.value = new Date().toLocaleTimeString('pt-BR');
  }, 350);
}

function mudarEtapa(etapa: 'autoavaliacao' | 'docentes' | 'finalizacao') {
  estado.etapaAtual = etapa;
  salvarEstadoPreenchimento(estado);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function mudarComponente(compId: string) {
  estado.componenteAtivoId = compId;
  salvarEstadoPreenchimento(estado);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function concluirEnvio() {
  estado.status = 'CONCLUIDO';
  estado.etapaAtual = 'finalizacao';
  salvarEstadoPreenchimento(estado);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function reiniciarTeste() {
  const novo = carregarEstadoPreenchimento(instrumento.value.id, instrumento.value.componentesMatriculados[0]?.id || '');
  Object.assign(estado, novo);
  estado.respostas = {};
  estado.etapaAtual = 'autoavaliacao';
  estado.status = 'EM_ANDAMENTO';
  salvarEstadoPreenchimento(estado);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function atualizarInstrumento(novo: InstrumentoAvaliativo) {
  instrumento.value = novo;
  estado.instrumentoId = novo.id;
  if (!novo.componentesMatriculados.some(c => c.id === estado.componenteAtivoId)) {
    estado.componenteAtivoId = novo.componentesMatriculados[0]?.id || '';
  }
  salvarEstadoPreenchimento(estado);
}

function testarNoRespondente() {
  visaoAtual.value = 'discente';
  estado.etapaAtual = 'autoavaliacao';
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function onUsuarioAlterado() {
  // Sincroniza estado geral se o perfil de usuario mudar
  statusSalvamento.value = 'salvo';
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-800 flex">
    <!-- Menu Lateral Esquerdo Institucional -->
    <Sidebar
      :visaoAtual="visaoAtual"
      :menuAbertoMobile="menuAbertoMobile"
      @mudarVisao="visaoAtual = $event"
      @fecharMenuMobile="menuAbertoMobile = false"
    />

    <!-- Área de Conteúdo Principal (deslocada pela largura da Sidebar no Desktop) -->
    <div class="flex-1 flex flex-col min-h-screen lg:pl-72 w-full transition-all duration-300">
      <HeaderNav
        :visaoAtual="visaoAtual"
        :statusSalvamento="statusSalvamento"
        :ultimaSalvaHora="ultimaSalvaHora"
        :respostasConcluidas="respostasConcluidas"
        :totalQuestoesObrigatorias="totalQuestoesObrigatorias"
        @abrirMenuMobile="menuAbertoMobile = true"
        @usuarioAlterado="onUsuarioAlterado"
      />

      <main class="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 py-6 md:py-8">
        <!-- 1. Cadastro de Questionário (Construtor de Instrumentos - RF01 a RF05) -->
        <InstrumentBuilderView
          v-if="visaoAtual === 'builder'"
          :instrumento="instrumento"
          @instrumentoAtualizado="atualizarInstrumento"
          @testarNoRespondente="testarNoRespondente"
        />

        <!-- 2. Catálogo de Públicos e Alvos Avaliados (Instituição, Setores, Docentes, Alunos) -->
        <PublicosAlvosView
          v-else-if="visaoAtual === 'catalogo'"
        />

        <!-- 3. Estudante Respondente (SPA Mobile-First com Auto-Save - RF06) -->
        <StudentRespondentView
          v-else-if="visaoAtual === 'discente'"
          :instrumento="instrumento"
          :estado="estado"
          @respostaAtualizada="registrarResposta"
          @mudarEtapa="mudarEtapa"
          @mudarComponente="mudarComponente"
          @concluirEnvio="concluirEnvio"
        />

        <!-- 3. Docente Avaliado (RF-AD07: Manifestação Dialógica / Hoffmann) -->
        <TeacherFeedbackView
          v-else-if="visaoAtual === 'docente'"
          :instrumento="instrumento"
        />

        <!-- 4. Gestão / CPA (RF14: Fechamento de Ciclo com Decisão / Luckesi) -->
        <ManagementActionPlanView
          v-else-if="visaoAtual === 'gestao'"
          :instrumento="instrumento"
        />

        <!-- 5. Inspeção Técnica (Payload JSON, LocalStorage, Interoperabilidade REST) -->
        <TechnicalView
          v-else-if="visaoAtual === 'tecnica'"
          :estado="estado"
          :instrumento="instrumento"
          @reiniciarTeste="reiniciarTeste"
        />
      </main>

      <!-- Rodapé Acadêmico e Institucional -->
      <footer class="bg-white border-t border-slate-200 mt-auto py-5 text-xs text-slate-500">
        <div class="max-w-6xl mx-auto px-4 sm:px-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-center sm:text-left">
          <div>
            <p class="font-medium text-slate-700">Sistema Informatizado para o Fechamento do Ciclo Avaliativo (AVALIA)</p>
            <p class="text-[11px] text-slate-400 mt-0.5">
              Dissertação de Mestrado Profissional em Tecnologias Educacionais em Rede (PPGTER / UFSM)
            </p>
          </div>
          <div class="text-[11px] text-slate-400">
            Pesquisador: Darlan E. F. da Silva • IFFar (2026/2027)
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>
