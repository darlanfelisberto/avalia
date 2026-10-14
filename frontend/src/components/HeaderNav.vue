<script setup lang="ts">
import { computed } from 'vue';
import type { UsuarioContexto } from '../types/avaliacao';
import { salvarUsuarioAtivo } from '../services/storage';

const props = defineProps<{
  visaoAtual: 'responder' | 'docente' | 'gestao' | 'tecnica' | 'sincronizacao' | 'instrumentos';
  usuario: UsuarioContexto | null;
}>();

const emit = defineEmits<{
  (e: 'abrirMenuMobile'): void;
  (e: 'usuarioAlterado', usuario: UsuarioContexto): void;
  (e: 'logout'): void;
}>();

function alternarPapelAtivo(e: Event) {
  if (!props.usuario) return;
  const target = e.target as HTMLSelectElement;
  const novoPapel = target.value as any;
  const atualizado: UsuarioContexto = {
    ...props.usuario,
    papel: novoPapel,
    papelAtivo: novoPapel
  };
  salvarUsuarioAtivo(atualizado);
  emit('usuarioAlterado', atualizado);
}

const tituloVisao = computed(() => {
  switch (props.visaoAtual) {
    case 'instrumentos':
      return {
        titulo: 'Cadastro de Instrumentos',
        categoria: 'Gestão de Instrumentos',
        codigo: 'Schema avaliacao'
      };
    case 'responder':
      return { titulo: 'Responder Avaliações', categoria: 'Aplicação e Respostas', codigo: '' };
    case 'docente':
      return {
        titulo: 'Manifestação e Plano de Desenvolvimento Docente',
        categoria: 'Avaliação Mediadora',
        codigo: 'RF-AD07 (Hoffmann, 2012)'
      };
    case 'gestao':
      return {
        titulo: 'Gestão e Consolidação do Ciclo com Plano de Ação',
        categoria: 'Governança & CPA',
        codigo: 'RF14 (Cipriano Luckesi)'
      };
    case 'tecnica':
      return {
        titulo: 'Auditoria de Dados e Interoperabilidade REST',
        categoria: 'Arquitetura e API',
        codigo: 'Open Liberty & PostgreSQL'
      };
    default:
      return { titulo: 'Sistema de Avaliação', categoria: 'IFFar', codigo: '' };
  }
});

</script>

<template>
  <header class="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-xs">
    <!-- Barra de contexto da página ativa -->
    <div class="px-4 py-3 flex items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <!-- Botão hambúrguer para abrir o menu no celular/tablet -->
        <button
          type="button"
          @click="emit('abrirMenuMobile')"
          class="lg:hidden p-2 rounded-xl text-slate-600 hover:bg-slate-100 border border-slate-200"
          title="Abrir Menu Lateral"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>

        <div>
          <div class="flex items-center gap-2">
            <span class="text-[11px] font-bold uppercase tracking-wider text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">
              {{ tituloVisao.categoria }}
            </span>
            <span class="text-[11px] text-slate-400 font-mono hidden sm:inline">
              {{ tituloVisao.codigo }}
            </span>
          </div>
          <h1 class="text-base md:text-lg font-bold text-slate-900 leading-tight mt-0.5">
            {{ tituloVisao.titulo }}
          </h1>
        </div>
      </div>

      <!-- Usuário Ativo e Indicadores -->
      <div class="flex items-center gap-3 sm:gap-4 shrink-0">
        <!-- Usuário Autenticado e Papel Ativo -->
        <div v-if="usuario" class="flex items-center gap-2.5 bg-slate-50 border border-slate-200/90 rounded-xl px-3 py-1.5 shadow-2xs">
          <div class="w-7 h-7 rounded-lg bg-emerald-600 text-white font-bold text-xs flex items-center justify-center shrink-0">
            {{ usuario.nome?.charAt(0) || 'U' }}
          </div>
          <div class="flex flex-col text-left">
            <div class="flex items-center gap-1.5">
              <span class="font-bold text-xs text-slate-800 max-w-[120px] sm:max-w-[180px] truncate" :title="usuario.nome">
                {{ usuario.nome }}
              </span>
              <span class="text-[10px] text-slate-400 font-mono hidden md:inline">({{ usuario.cpfFormatado || usuario.cpf }})</span>
            </div>
            <div class="flex items-center gap-1.5">
              <!-- Se tiver mais de um papel, permite alternar -->
              <select
                v-if="usuario.papeis && usuario.papeis.length > 1"
                :value="usuario.papelAtivo || usuario.papel"
                @change="alternarPapelAtivo"
                class="bg-transparent font-semibold text-[10px] text-emerald-700 outline-hidden cursor-pointer hover:underline"
                title="Alternar Perfil Ativo"
              >
                <option v-for="p in usuario.papeis" :key="p" :value="p">
                  Perfil: {{ p }}
                </option>
              </select>
              <span v-else class="text-[10px] font-semibold text-emerald-700 uppercase tracking-wide">
                {{ usuario.papelAtivo || usuario.papel }}
              </span>
              <span class="text-[10px] text-slate-400">&bull;</span>
              <span class="text-[10px] text-slate-500 truncate max-w-[100px]">{{ usuario.campus || 'Geral' }}</span>
            </div>
          </div>

          <button
            @click="emit('logout')"
            class="ml-1 p-1 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors cursor-pointer text-xs"
            title="Sair / Desconectar"
          >
            Sair
          </button>
        </div>

      </div>
    </div>
  </header>
</template>
