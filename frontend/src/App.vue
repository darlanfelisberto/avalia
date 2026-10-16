<script setup lang="ts">
import { onMounted, ref } from 'vue';
import Sidebar from './components/Sidebar.vue';
import HeaderNav from './components/HeaderNav.vue';
import SincronizacaoView from './components/SincronizacaoView.vue';
import AvaliacaoInstrumentoView from './components/AvaliacaoInstrumentoView.vue';
import PermissoesTiposEntidadeView from './components/PermissoesTiposEntidadeView.vue';
import ResponderAvaliacoesView from './components/ResponderAvaliacoesView.vue';
import LoginView from './components/LoginView.vue';

import type { UsuarioContexto } from './types/avaliacao';
import { carregarUsuarioAtivo, salvarUsuarioAtivo, limparUsuarioAtivo, temPermissao } from './services/storage';
import { concluirLoginOidc, encerrarLoginOidc, recebeuRetornoOidc, validarSessaoAtual } from './services/oidc';

const usuarioLogado = ref<UsuarioContexto | null>(carregarUsuarioAtivo());

const visaoAtual = ref<'responder' | 'sincronizacao' | 'instrumentos' | 'permissoes'>(obterVisaoPadraoParaUsuario(usuarioLogado.value));
const menuAbertoMobile = ref(false);
const processandoRetorno = ref(true);
const erroLogin = ref('');
const erroLogout = ref('');

onMounted(async () => {
  try {
    if (recebeuRetornoOidc()) {
      const resposta = await concluirLoginOidc();
      aoLoginSucesso({ ...resposta.usuario!, papel: resposta.usuario!.papelAtivo || resposta.usuario!.papel });
    } else {
      const usuario = await validarSessaoAtual(usuarioLogado.value?.papelAtivo || usuarioLogado.value?.papel);
      if (usuario) {
        aoLoginSucesso({ ...usuario, papel: usuario.papelAtivo || usuario.papel });
      } else {
        usuarioLogado.value = null;
        limparUsuarioAtivo();
      }
    }
  } catch (e) {
    usuarioLogado.value = null;
    limparUsuarioAtivo();
    erroLogin.value = e instanceof Error ? e.message : 'Falha ao concluir a autenticação.';
  } finally {
    processandoRetorno.value = false;
  }
});

function obterVisaoPadraoParaUsuario(u: UsuarioContexto | null): 'responder' | 'sincronizacao' | 'instrumentos' | 'permissoes' {
  if (!u) return 'responder';
  const papel = u.papelAtivo || u.papel;
  if ((papel === 'CPA' || papel === 'ADMIN') && temPermissao(u, 'INSTRUMENTO_VISUALIZAR')) return 'instrumentos';
  if (temPermissao(u, 'AVALIACAO_RESPONDER')) return 'responder';
  if (temPermissao(u, 'SIGAA_SINCRONIZAR')) return 'sincronizacao';
  return 'responder';
}

function aoLoginSucesso(usuario: UsuarioContexto) {
  usuarioLogado.value = usuario;
  salvarUsuarioAtivo(usuario);
  visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
}

async function aoLogout() {
  erroLogout.value = '';
  try {
    await encerrarLoginOidc();
    usuarioLogado.value = null;
    limparUsuarioAtivo();
  } catch (e) {
    erroLogout.value = e instanceof Error ? e.message : 'Não foi possível encerrar a sessão. Tente novamente.';
  }
}

function onUsuarioAlterado(usuario: UsuarioContexto) {
  usuarioLogado.value = usuario;
  salvarUsuarioAtivo(usuario);
  // Se a visão atual não for permitida para o novo papel, redireciona
  if ((visaoAtual.value === 'instrumentos' || visaoAtual.value === 'permissoes')
      && ((usuario.papelAtivo !== 'CPA' && usuario.papelAtivo !== 'ADMIN') || !temPermissao(usuario, 'INSTRUMENTO_VISUALIZAR'))) {
    visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
  } else if (visaoAtual.value === 'sincronizacao' && !temPermissao(usuario, 'SIGAA_SINCRONIZAR')) {
    visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
  } else if (visaoAtual.value === 'responder' && !temPermissao(usuario, 'AVALIACAO_RESPONDER')) {
    visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
  }
}
</script>

<template>
  <div v-if="processandoRetorno" class="min-h-screen flex items-center justify-center text-slate-600">Concluindo autenticação...</div>
  <LoginView v-else-if="!usuarioLogado" :erroInicial="erroLogin" />

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
        <p v-if="erroLogout" role="alert" class="mb-4 rounded bg-red-50 p-3 text-red-700">{{ erroLogout }}</p>
        <ResponderAvaliacoesView
          v-if="visaoAtual === 'responder'"
          :key="`${usuarioLogado.cpf}:${usuarioLogado.papelAtivo || usuarioLogado.papel}`"
        />

        <!-- Instrumentos de Avaliação (schema avaliacao): questionário, páginas, sessões e questões -->
        <AvaliacaoInstrumentoView
          v-else-if="visaoAtual === 'instrumentos'"
        />

        <PermissoesTiposEntidadeView
          v-else-if="visaoAtual === 'permissoes'"
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
