<script setup lang="ts">
import { computed, ref } from 'vue';
import type { UsuarioContexto } from '../types/avaliacao';
import { carregarUsuarioAtivo, salvarUsuarioAtivo, USUARIOS_SIMULADOS } from '../services/storage';

const props = defineProps<{
  visaoAtual: 'builder' | 'catalogo' | 'discente' | 'docente' | 'gestao' | 'tecnica';
  statusSalvamento: 'salvo' | 'salvando' | 'erro';
  ultimaSalvaHora: string;
  respostasConcluidas: number;
  totalQuestoesObrigatorias: number;
}>();

const emit = defineEmits<{
  (e: 'abrirMenuMobile'): void;
  (e: 'usuarioAlterado', usuario: UsuarioContexto): void;
}>();

const usuarioAtivo = ref<UsuarioContexto>(carregarUsuarioAtivo());
const listaUsuarios = USUARIOS_SIMULADOS as UsuarioContexto[];

function trocarUsuario(novo: UsuarioContexto) {
  usuarioAtivo.value = novo;
  salvarUsuarioAtivo(novo);
  emit('usuarioAlterado', novo);
}

function aoMudarUsuario(e: Event) {
  const target = e.target as HTMLSelectElement;
  const encontrado = listaUsuarios.find(u => u.id === target.value);
  if (encontrado) {
    trocarUsuario(encontrado);
  }
}

const tituloVisao = computed(() => {
  switch (props.visaoAtual) {
    case 'builder':
      return {
        titulo: 'Cadastro de Questionário',
        categoria: 'Gestão de Instrumentos',
        codigo: 'RF01 a RF05'
      };
    case 'catalogo':
      return {
        titulo: 'Catálogo de Públicos e Alvos Avaliados',
        categoria: 'Gestão de Instrumentos',
        codigo: 'Núcleo Parametrizável'
      };
    case 'discente':
      return {
        titulo: 'Avaliação Docente pelo Discente (ADPD)',
        categoria: 'Preenchimento do Estudante',
        codigo: 'Resolução CONSUP nº 104/2013'
      };
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

const percentual = computed(() => {
  if (props.totalQuestoesObrigatorias === 0) return 0;
  return Math.min(100, Math.round((props.respostasConcluidas / props.totalQuestoesObrigatorias) * 100));
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
        <!-- Seletor Rápido de Usuário (Simulação de Autenticação) -->
        <div class="flex items-center gap-2 bg-slate-50 border border-slate-200/80 rounded-xl px-2.5 py-1.5">
          <span class="text-base">👤</span>
          <div class="flex flex-col text-left">
            <span class="text-[9px] uppercase tracking-wider font-bold text-slate-400">Usuário Ativo:</span>
            <select
              :value="usuarioAtivo.id"
              @change="aoMudarUsuario"
              class="bg-transparent font-bold text-xs text-slate-800 outline-hidden cursor-pointer"
            >
              <option v-for="u in listaUsuarios" :key="u.id" :value="u.id">
                {{ u.nome }} ({{ u.papel }})
              </option>
            </select>
          </div>
        </div>

        <!-- Indicador de Auto-Save e Progresso -->
        <div class="flex flex-col items-end">
          <div class="flex items-center gap-1.5 text-xs">
            <span v-if="statusSalvamento === 'salvando'" class="inline-block w-2 h-2 rounded-full bg-amber-500 animate-ping"></span>
            <span v-else-if="statusSalvamento === 'salvo'" class="inline-block w-2 h-2 rounded-full bg-emerald-500"></span>
            <span v-else class="inline-block w-2 h-2 rounded-full bg-rose-500"></span>

            <span class="font-medium text-slate-700 text-[11px] md:text-xs">
              {{ statusSalvamento === 'salvando' ? 'Salvando...' : 'Salvo' }}
            </span>
          </div>
          <span class="text-[10px] text-slate-400" v-if="ultimaSalvaHora">
            às {{ ultimaSalvaHora }}
          </span>
        </div>

        <div v-if="visaoAtual === 'discente'" class="border-l border-slate-200 pl-4 hidden md:block min-w-32">
          <div class="flex justify-between text-xs mb-1 font-medium text-slate-600">
            <span>Progresso</span>
            <span>{{ percentual }}%</span>
          </div>
          <div class="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
            <div
              class="bg-emerald-600 h-2 rounded-full transition-all duration-300"
              :style="{ width: `${percentual}%` }"
            ></div>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>
