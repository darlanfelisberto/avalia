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
import { carregarTokenAtivo, carregarUsuarioAtivo, salvarUsuarioAtivo, limparUsuarioAtivo, temPermissao } from './services/storage';
import { concluirLoginOidc, encerrarLoginOidc, recebeuRetornoOidc, validarSessaoAtual } from './services/oidc';

const usuarioLogado = ref<UsuarioContexto | null>(carregarUsuarioAtivo());

const visaoAtual = ref<'responder' | 'sincronizacao' | 'instrumentos' | 'permissoes'>(obterVisaoPadraoParaUsuario(usuarioLogado.value));
const menuAbertoMobile = ref(false);
const processandoRetorno = ref(recebeuRetornoOidc() || !!usuarioLogado.value);
const erroLogin = ref('');

onMounted(async () => {
  if (!processandoRetorno.value) return;
  try {
    if (recebeuRetornoOidc()) {
      const resposta = await concluirLoginOidc();
      aoLoginSucesso({ ...resposta.usuario!, papel: resposta.usuario!.papelAtivo || resposta.usuario!.papel, token: resposta.token });
    } else if (usuarioLogado.value) {
      const token = carregarTokenAtivo();
      const usuario = token ? await validarSessaoAtual(token, usuarioLogado.value.papelAtivo || usuarioLogado.value.papel) : null;
      if (usuario) {
        usuarioLogado.value = { ...usuario, papel: usuario.papelAtivo || usuario.papel, token: token! };
        salvarUsuarioAtivo(usuarioLogado.value, token!);
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
  if (papel === 'CPA' || papel === 'ADMIN') return 'instrumentos';
  if (papel === 'DOCENTE' || papel === 'DISCENTE' || papel === 'TAE') return 'responder';
  return 'responder';
}

function aoLoginSucesso(usuario: UsuarioContexto) {
  usuarioLogado.value = usuario;
  salvarUsuarioAtivo(usuario, usuario.token);
  visaoAtual.value = obterVisaoPadraoParaUsuario(usuario);
}

async function aoLogout() {
  const token = carregarTokenAtivo();
  usuarioLogado.value = null;
  limparUsuarioAtivo();
  await encerrarLoginOidc(token || undefined);
}

function onUsuarioAlterado(usuario: UsuarioContexto) {
  usuarioLogado.value = usuario;
  salvarUsuarioAtivo(usuario, usuario.token);
  // Se a visão atual não for permitida para o novo papel, redireciona
  if ((visaoAtual.value === 'instrumentos' || visaoAtual.value === 'permissoes') && usuario.papelAtivo !== 'CPA' && usuario.papelAtivo !== 'ADMIN') {
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
