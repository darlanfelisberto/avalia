<script setup lang="ts">
import { computed } from 'vue';
import type { UsuarioContexto } from '../types/avaliacao';
import { temPermissao } from '../services/storage';

const props = defineProps<{
  visaoAtual: 'responder' | 'sincronizacao' | 'instrumentos' | 'permissoes' | 'acesso-negado';
  menuAbertoMobile: boolean;
  usuario: UsuarioContexto | null;
}>();

const emit = defineEmits<{
  (e: 'mudarVisao', visao: 'responder' | 'sincronizacao' | 'instrumentos' | 'permissoes'): void;
  (e: 'fecharMenuMobile'): void;
  (e: 'logout'): void;
}>();

const podeGerenciarInstrumentos = computed(() => !!props.usuario?.papeis?.some(p => p === 'CPA' || p === 'ADMIN')
  && temPermissao(props.usuario, 'INSTRUMENTO_VISUALIZAR'));
const podeDiscente = computed(() => temPermissao(props.usuario, 'AVALIACAO_RESPONDER'));
const podeSincronizacao = computed(() => temPermissao(props.usuario, 'SIGAA_SINCRONIZAR'));
</script>

<template>
  <!-- Backdrop para telas móveis -->
  <div
    v-if="menuAbertoMobile"
    @click="emit('fecharMenuMobile')"
    class="sidebar-backdrop"
  ></div>

  <!-- Sidebar Lateral Esquerda -->
  <aside
    :class="['sidebar', menuAbertoMobile ? 'sidebar--open' : 'sidebar--closed']"
  >
    <!-- Topo da Sidebar: Identidade Institucional -->
    <div class="sidebar__brand-row">
      <div class="sidebar__brand">
        <div class="sidebar__brand-mark">
          A
        </div>
        <div>
          <div class="sidebar__brand-name">
            <span>AVALIA</span>
          </div>
          <p class="sidebar__brand-caption">Avaliações Institucionais</p>
        </div>
      </div>

      <!-- Botão fechar no mobile -->
      <button
        @click="emit('fecharMenuMobile')"
        class="icon-button sidebar__close"
      >
        ✕
      </button>
    </div>

    <!-- Navegação / Itens de Menu -->
    <nav class="sidebar__navigation">
      <!-- Grupo 1: Instrumentos -->
      <div v-if="podeGerenciarInstrumentos">
        <div class="sidebar__group-title">
          Instrumentos
        </div>
        <div class="sidebar__group-links">
          <!-- Instrumentos de Avaliação (schema avaliacao) -->
          <button v-if="podeGerenciarInstrumentos" type="button"
            @click="() => { emit('mudarVisao', 'instrumentos'); emit('fecharMenuMobile'); }"
            :class="['sidebar__link', visaoAtual === 'instrumentos' ? 'sidebar__link--active' : '']"
          >
            <span class="sidebar__link-icon">🧾</span>
            <div class="sidebar__link-copy">
              <div>Cadastro de Instrumentos</div>
              <div class="sidebar__link-description">Instrumentos, páginas, sessões e questões</div>
            </div>
          </button>
          <button type="button"
            @click="() => { emit('mudarVisao', 'permissoes'); emit('fecharMenuMobile'); }"
            :class="['sidebar__link', visaoAtual === 'permissoes' ? 'sidebar__link--active' : '']"
          >
            <span class="sidebar__link-icon">🔀</span>
            <div class="sidebar__link-copy">
              <div>Permissões entre Entidades</div>
              <div class="sidebar__link-description">Quem pode avaliar cada tipo</div>
            </div>
          </button>
        </div>
      </div>

      <!-- Grupo 2: Execução Avaliativa e Ciclos -->
      <div v-if="podeDiscente">
        <div class="sidebar__group-title">
          Aplicação e Respostas
        </div>
        <div class="sidebar__group-links">
          <!-- Avaliações disponíveis para o perfil ativo -->
          <button
            v-if="podeDiscente"
            type="button"
            @click="() => { emit('mudarVisao', 'responder'); emit('fecharMenuMobile'); }"
            :class="['sidebar__link', visaoAtual === 'responder' ? 'sidebar__link--active' : '']"
          >
            <span class="sidebar__link-icon">🎓</span>
            <div class="sidebar__link-copy">
              <div>Responder Avaliação</div>
              <div class="sidebar__link-description">Instrumentos disponíveis para seu perfil</div>
            </div>
          </button>

        </div>
      </div>

      <!-- Grupo 3: Integração Acadêmica -->
      <div v-if="podeSincronizacao">
        <div class="sidebar__group-title">
          Integração Acadêmica
        </div>
        <div class="sidebar__group-links">
          <!-- Item: Sincronização SIGAA / SIGRH -->
          <button
            v-if="podeSincronizacao"
            type="button"
            @click="() => { emit('mudarVisao', 'sincronizacao'); emit('fecharMenuMobile'); }"
            :class="['sidebar__link', visaoAtual === 'sincronizacao' ? 'sidebar__link--active' : '']"
          >
            <span class="sidebar__link-icon">🔄</span>
            <div class="sidebar__link-copy">
              <div>Sincronização SIGAA</div>
              <div class="sidebar__link-description">Carga Acadêmica & RH (SIGAA)</div>
            </div>
          </button>

        </div>
      </div>
    </nav>

    <!-- Rodapé da Sidebar: Usuário Autenticado e Sair -->
    <div class="sidebar__footer">
      <div v-if="usuario" class="sidebar__user-card">
        <div class="sidebar__user-avatar">
          {{ usuario.nome?.charAt(0) || 'U' }}
        </div>
        <div class="sidebar__user-copy">
          <div class="sidebar__user-name" :title="usuario.nome">
            {{ usuario.nome }}
          </div>
          <div class="sidebar__user-meta">
            <span class="sidebar__user-id">{{ usuario.cpfFormatado || usuario.cpf }}</span>
            <span>&bull;</span>
            <span class="sidebar__user-role">{{ usuario.papelAtivo || usuario.papel }}</span>
          </div>
        </div>
        <button
          @click="emit('logout')"
          class="icon-button sidebar__logout"
          title="Encerrar Sessão / Trocar Usuário"
        >
          🚪
        </button>
      </div>

      <p class="text-[10px] text-slate-500 font-medium text-center">
        PPGTER / UFSM &bull; <span class="text-emerald-500 font-semibold">IFFar AVALIA</span>
      </p>
    </div>
  </aside>
</template>
