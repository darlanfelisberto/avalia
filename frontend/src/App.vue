<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import Sidebar from './components/Sidebar.vue';
import HeaderNav from './components/HeaderNav.vue';
import LoginView from './components/LoginView.vue';
import { RouterView, useRoute, useRouter } from 'vue-router';

import type { UsuarioContexto } from './types/avaliacao';
import { carregarUsuarioAtivo, salvarUsuarioAtivo, limparUsuarioAtivo } from './services/storage';
import { concluirLoginOidc, encerrarLoginOidc, recebeuRetornoOidc, validarSessaoAtual } from './services/oidc';
import { rotaInicial } from './router';

const usuarioLogado = ref<UsuarioContexto | null>(carregarUsuarioAtivo());
const route = useRoute();
const router = useRouter();
type VisaoMenu = 'responder' | 'sincronizacao' | 'instrumentos' | 'permissoes';
const visaoAtual = computed<VisaoMenu | 'acesso-negado'>(() => {
  if (route.name === 'instrumento-novo' || route.name === 'instrumento-editar') return 'instrumentos';
  return route.name === 'instrumentos' || route.name === 'permissoes'
    || route.name === 'sincronizacao' || route.name === 'acesso-negado'
    ? route.name
    : 'responder';
});
const recargaVisao = ref(0);
const menuAbertoMobile = ref(false);
const processandoRetorno = ref(true);
const erroLogin = ref('');
const erroLogout = ref('');

onMounted(async () => {
  try {
    if (recebeuRetornoOidc()) {
      const resposta = await concluirLoginOidc();
      aoLoginSucesso({ ...resposta.usuario!, papel: resposta.usuario!.papelAtivo || resposta.usuario!.papel }, true);
    } else {
      const usuario = await validarSessaoAtual();
      if (usuario) {
        aoLoginSucesso({ ...usuario, papel: usuario.papelAtivo || usuario.papel }, !usuarioLogado.value && route.path === '/');
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

function aoLoginSucesso(usuario: UsuarioContexto, irParaRotaInicial = false) {
  usuarioLogado.value = usuario;
  salvarUsuarioAtivo(usuario);
  if (irParaRotaInicial) void router.replace(rotaInicial(usuario));
}

async function aoMudarVisao(visao: VisaoMenu) {
  const caminho = `/${visao}`;
  if (route.path === caminho) {
    recargaVisao.value++;
    return;
  }
  await router.push(caminho);
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

</script>

<template>
  <div v-if="processandoRetorno" class="auth-loading">Concluindo autenticação...</div>
  <LoginView v-else-if="!usuarioLogado" :erroInicial="erroLogin" />

  <!-- Aplicação Principal Autenticada -->
  <div v-else class="app-shell">
    <!-- Menu Lateral Esquerdo Institucional -->
    <Sidebar
      :visaoAtual="visaoAtual"
      :menuAbertoMobile="menuAbertoMobile"
      :usuario="usuarioLogado"
      @mudarVisao="aoMudarVisao"
      @fecharMenuMobile="menuAbertoMobile = false"
      @logout="aoLogout"
    />

    <!-- Área de Conteúdo Principal (deslocada pela largura da Sidebar no Desktop) -->
    <div class="app-workspace">
      <HeaderNav
        :visaoAtual="visaoAtual"
        :usuario="usuarioLogado"
        @abrirMenuMobile="menuAbertoMobile = true"
        @logout="aoLogout"
      />

      <main class="app-main-content">
        <p v-if="erroLogout" role="alert" class="alert alert--error app-logout-error">{{ erroLogout }}</p>
        <RouterView v-slot="{ Component }">
          <component :is="Component" :key="`${route.fullPath}-${usuarioLogado.cpf}-${recargaVisao}`" />
        </RouterView>
      </main>

      <!-- Rodapé Acadêmico e Institucional -->
      <footer class="app-footer">
        <div class="app-footer__content">
          <div>
            <p class="app-footer__title">Sistema Informatizado para o Fechamento do Ciclo Avaliativo (AVALIA)</p>
            <p class="app-footer__subtitle">
              Dissertação de Mestrado Profissional em Tecnologias Educacionais em Rede (PPGTER / UFSM)
            </p>
          </div>
          <div class="app-footer__researcher">
            Pesquisador: Darlan E. F. da Silva • IFFar (2026/2027)
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>
