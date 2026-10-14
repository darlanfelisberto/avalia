<script setup lang="ts">
import { ref, reactive } from 'vue';
import Sidebar from './components/Sidebar.vue';
import HeaderNav from './components/HeaderNav.vue';
import TeacherFeedbackView from './components/TeacherFeedbackView.vue';
import ManagementActionPlanView from './components/ManagementActionPlanView.vue';
import TechnicalView from './components/TechnicalView.vue';
import SincronizacaoView from './components/SincronizacaoView.vue';
import AvaliacaoInstrumentoView from './components/AvaliacaoInstrumentoView.vue';
import ResponderAvaliacoesView from './components/ResponderAvaliacoesView.vue';
import LoginView from './components/LoginView.vue';

import type { EstadoPreenchimento, InstrumentoAvaliativo, UsuarioContexto } from './types/avaliacao';
import { carregarEstadoPreenchimento, salvarEstadoPreenchimento, carregarInstrumentoAtivo, carregarUsuarioAtivo, salvarUsuarioAtivo, limparUsuarioAtivo, temPermissao } from './services/storage';

const usuarioLogado = ref<UsuarioContexto | null>(carregarUsuarioAtivo());
const instrumento = ref<InstrumentoAvaliativo>(carregarInstrumentoAtivo());

const visaoAtual = ref<'responder' | 'docente' | 'gestao' | 'tecnica' | 'sincronizacao' | 'instrumentos'>(obterVisaoPadraoParaUsuario(usuarioLogado.value));
const menuAbertoMobile = ref(false);

const estado = reactive<EstadoPreenchimento>(carregarEstadoPreenchimento(instrumento.value.id, ''));

function reiniciarTeste() {
  const novo = carregarEstadoPreenchimento(instrumento.value.id, '');
  Object.assign(estado, novo);
  estado.respostas = {};
  estado.etapaAtual = 'autoavaliacao';
  estado.status = 'EM_ANDAMENTO';
  salvarEstadoPreenchimento(estado);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function obterVisaoPadraoParaUsuario(u: UsuarioContexto | null): 'responder' | 'docente' | 'gestao' | 'tecnica' | 'sincronizacao' | 'instrumentos' {
  if (!u) return 'responder';
  const papel = u.papelAtivo || u.papel;
  if (papel === 'CPA' || papel === 'ADMIN') return 'instrumentos';
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
  if (visaoAtual.value === 'instrumentos' && usuario.papelAtivo !== 'CPA' && usuario.papelAtivo !== 'ADMIN') {
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
        :usuario="usuarioLogado"
        @abrirMenuMobile="menuAbertoMobile = true"
        @usuarioAlterado="onUsuarioAlterado"
        @logout="aoLogout"
      />

      <main class="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 py-6 md:py-8">
        <ResponderAvaliacoesView
          v-if="visaoAtual === 'responder'"
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
