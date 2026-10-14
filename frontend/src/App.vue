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
import SincronizacaoView from './components/SincronizacaoView.vue';
import AvaliacaoInstrumentoView from './components/AvaliacaoInstrumentoView.vue';
import ResponderAvaliacoesView from './components/ResponderAvaliacoesView.vue';
import LoginView from './components/LoginView.vue';

import type { RespostaItem, EstadoPreenchimento, InstrumentoAvaliativo, UsuarioContexto } from './types/avaliacao';
import { carregarEstadoPreenchimento, salvarEstadoPreenchimento, carregarInstrumentoAtivo, carregarUsuarioAtivo, salvarUsuarioAtivo, limparUsuarioAtivo, temPermissao } from './services/storage';
import { listarInstrumentosApi } from './services/api';

const usuarioLogado = ref<UsuarioContexto | null>(carregarUsuarioAtivo());
const instrumento = ref<InstrumentoAvaliativo>(carregarInstrumentoAtivo());

// Visão inicial definida como "Cadastro de Questionário" (ou ajustada de acordo com o papel)
const visaoAtual = ref<'builder' | 'catalogo' | 'discente' | 'responder' | 'docente' | 'gestao' | 'tecnica' | 'sincronizacao' | 'instrumentos'>(obterVisaoPadraoParaUsuario(usuarioLogado.value));
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

// Calcula total de itens obrigatórios (Autoavaliação + por disciplina ou geral)
const totalQuestoesObrigatorias = computed(() => {
  let count = 0;
  // Autoavaliação discente
  for (const g of (instrumento.value.gruposAutoavaliacaoDiscente || [])) {
    for (const q of (g.questoes || [])) {
      if (q.obrigatoria) count++;
    }
  }
  const obrigGeral = (instrumento.value.gruposAvaliacaoDocente || []).reduce((acc, g) => {
    return acc + (g.questoes || []).filter(q => q.obrigatoria).length;
  }, 0);

  if (instrumento.value.componentesMatriculados && instrumento.value.componentesMatriculados.length > 0) {
    count += obrigGeral * instrumento.value.componentesMatriculados.length;
  } else {
    count += obrigGeral;
  }
  return count;
});

// Calcula total de respostas obrigatórias já preenchidas
const respostasConcluidas = computed(() => {
  let count = 0;
  // Autoavaliação
  for (const g of (instrumento.value.gruposAutoavaliacaoDiscente || [])) {
    for (const q of (g.questoes || [])) {
      if (q.obrigatoria) {
        const resp = estado.respostas[`${q.id}_geral`];
        if (resp && (resp.valorEscala !== undefined || resp.opcaoId)) count++;
      }
    }
  }
  // Por componente ou avaliação direta/geral
  if (instrumento.value.componentesMatriculados && instrumento.value.componentesMatriculados.length > 0) {
    for (const comp of instrumento.value.componentesMatriculados) {
      for (const g of (instrumento.value.gruposAvaliacaoDocente || [])) {
        for (const q of (g.questoes || [])) {
          if (q.obrigatoria) {
            const resp = estado.respostas[`${q.id}_${comp.id}`];
            if (resp && (resp.valorEscala !== undefined || resp.opcaoId)) count++;
          }
        }
      }
    }
  } else {
    for (const g of (instrumento.value.gruposAvaliacaoDocente || [])) {
      for (const q of (g.questoes || [])) {
        if (q.obrigatoria) {
          const resp = estado.respostas[`${q.id}_geral`];
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

function obterVisaoPadraoParaUsuario(u: UsuarioContexto | null): 'builder' | 'catalogo' | 'discente' | 'responder' | 'docente' | 'gestao' | 'tecnica' | 'sincronizacao' | 'instrumentos' {
  if (!u) return 'responder';
  const papel = u.papelAtivo || u.papel;
  if (papel === 'CPA' || papel === 'ADMIN') return 'builder';
  if (papel === 'DOCENTE') return 'docente';
  if (papel === 'GESTAO') return 'gestao';
  if (papel === 'DISCENTE' || papel === 'TAE') return 'responder';
  return 'responder';
}

function aoLoginSucesso(usuario: UsuarioContexto) {
  usuarioLogado.value = usuario;
  salvarUsuarioAtivo(usuario, usuario.token);
  visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
}

function aoLogout() {
  usuarioLogado.value = null;
  limparUsuarioAtivo();
}

function onUsuarioAlterado(usuario: UsuarioContexto) {
  usuarioLogado.value = usuario;
  salvarUsuarioAtivo(usuario, usuario.token);
  // Se a visão atual não for permitida para o novo papel, redireciona
  if ((visaoAtual.value === 'builder' || visaoAtual.value === 'instrumentos') && usuario.papelAtivo !== 'CPA' && usuario.papelAtivo !== 'ADMIN') {
    visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
  } else if (visaoAtual.value === 'catalogo' && !temPermissao(usuario, 'CATALOGO_GERENCIAR')) {
    visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
  } else if (visaoAtual.value === 'docente' && !temPermissao(usuario, 'DOCENTE_MANIFESTAR')) {
    visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
  } else if (visaoAtual.value === 'gestao' && !temPermissao(usuario, 'GESTAO_ENCAMINHAR')) {
    visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
  } else if (visaoAtual.value === 'sincronizacao' && !temPermissao(usuario, 'SIGAA_SINCRONIZAR')) {
    visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
  } else if (visaoAtual.value === 'tecnica' && !temPermissao(usuario, 'AUDITORIA_ACESSAR')) {
    visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
  } else if (visaoAtual.value === 'responder' && !temPermissao(usuario, 'AVALIACAO_RESPONDER')) {
    visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
  }
  statusSalvamento.value = 'salvo';
}
</script>

<template>
  <!-- Tela de Login se não estiver autenticado -->
  <LoginView
    v-if="!usuarioLogado"
    @loginSucesso="aoLoginSucesso"
  />

  <!-- Aplicação Principal Autenticada -->
  <div v-else class="min-h-screen bg-slate-50 text-slate-800 flex">
    <!-- Menu Lateral Esquerdo Institucional -->
    <Sidebar
      :visaoAtual="visaoAtual"
      :menuAbertoMobile="menuAbertoMobile"
      :usuario="usuarioLogado"
      @mudarVisao="visaoAtual = $event"
      @fecharMenuMobile="menuAbertoMobile = false"
      @logout="aoLogout"
    />

    <!-- Área de Conteúdo Principal (deslocada pela largura da Sidebar no Desktop) -->
    <div class="flex-1 flex flex-col min-h-screen lg:pl-72 w-full transition-all duration-300">
      <HeaderNav
        :visaoAtual="visaoAtual"
        :statusSalvamento="statusSalvamento"
        :ultimaSalvaHora="ultimaSalvaHora"
        :respostasConcluidas="respostasConcluidas"
        :totalQuestoesObrigatorias="totalQuestoesObrigatorias"
        :usuario="usuarioLogado"
        @abrirMenuMobile="menuAbertoMobile = true"
        @usuarioAlterado="onUsuarioAlterado"
        @logout="aoLogout"
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

        <ResponderAvaliacoesView
          v-else-if="visaoAtual === 'responder'"
          :key="`${usuarioLogado.cpf}:${usuarioLogado.papelAtivo || usuarioLogado.papel}`"
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

        <!-- Instrumentos de Avaliação (schema avaliacao): questionário, páginas, sessões e questões -->
        <AvaliacaoInstrumentoView
          v-else-if="visaoAtual === 'instrumentos'"
        />

        <!-- 6. Sincronização & Integração SIGAA / SIGRH -->
        <SincronizacaoView
          v-else-if="visaoAtual === 'sincronizacao'"
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
