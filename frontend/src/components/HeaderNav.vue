<script setup lang="ts">
import { computed } from 'vue';
import type { UsuarioContexto } from '../types/avaliacao';

const props = defineProps<{
  visaoAtual: 'responder' | 'sincronizacao' | 'instrumentos' | 'permissoes' | 'acesso-negado';
  usuario: UsuarioContexto | null;
}>();

const emit = defineEmits<{
  (e: 'abrirMenuMobile'): void;
  (e: 'logout'): void;
}>();

const tituloVisao = computed(() => {
  switch (props.visaoAtual) {
    case 'instrumentos':
      return {
        titulo: 'Cadastro de Instrumentos',
        categoria: 'Instrumentos',
        codigo: 'Schema avaliacao'
      };
    case 'permissoes':
      return {
        titulo: 'Permissões entre Tipos de Entidade',
        categoria: 'Instrumentos',
        codigo: 'Regras de participação'
      };
    case 'responder':
      return { titulo: 'Responder Avaliações', categoria: 'Aplicação e Respostas', codigo: '' };
    case 'sincronizacao':
      return { titulo: 'Sincronização SIGAA', categoria: 'Integração Acadêmica', codigo: '' };
    case 'acesso-negado':
      return { titulo: 'Acesso não autorizado', categoria: 'Conta', codigo: '' };
    default:
      return { titulo: 'Sistema de Avaliação', categoria: 'IFFar', codigo: '' };
  }
});

</script>

<template>
  <header class="app-header">
    <!-- Barra de contexto da página ativa -->
    <div class="app-header__bar">
      <div class="app-header__context">
        <!-- Botão hambúrguer para abrir o menu no celular/tablet -->
        <button
          type="button"
          @click="emit('abrirMenuMobile')"
          class="icon-button app-header__menu-button"
          title="Abrir Menu Lateral"
        >
          <svg class="app-header__menu-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>

        <div>
          <div class="app-header__labels">
            <span class="app-header__category">
              {{ tituloVisao.categoria }}
            </span>
            <span class="app-header__code">
              {{ tituloVisao.codigo }}
            </span>
          </div>
          <h1 class="app-header__title">
            {{ tituloVisao.titulo }}
          </h1>
        </div>
      </div>

      <!-- Usuário Ativo e Indicadores -->
      <div class="app-header__user-area">
        <!-- Usuário Autenticado e Papel Ativo -->
        <div v-if="usuario" class="app-header__user-card">
          <div class="app-header__avatar">
            {{ usuario.nome?.charAt(0) || 'U' }}
          </div>
          <div class="app-header__user-copy">
            <div class="app-header__user-line">
              <span class="app-header__user-name" :title="usuario.nome">
                {{ usuario.nome }}
              </span>
              <span class="app-header__user-id">({{ usuario.cpfFormatado || usuario.cpf }})</span>
            </div>
            <div class="app-header__user-line">
              <span class="app-header__roles">
                {{ usuario.papeis?.join(' / ') || usuario.papel }}
              </span>
              <span class="app-header__separator">&bull;</span>
              <span class="app-header__campus">{{ usuario.campus || 'Geral' }}</span>
            </div>
          </div>

          <button
            @click="emit('logout')"
            class="app-header__logout"
            title="Sair / Desconectar"
          >
            Sair
          </button>
        </div>

      </div>
    </div>
  </header>
</template>
