<script setup lang="ts">
defineProps<{
  visaoAtual: 'builder' | 'catalogo' | 'discente' | 'docente' | 'gestao' | 'tecnica' | 'sincronizacao';
  menuAbertoMobile: boolean;
}>();

const emit = defineEmits<{
  (e: 'mudarVisao', visao: 'builder' | 'catalogo' | 'discente' | 'docente' | 'gestao' | 'tecnica' | 'sincronizacao'): void;
  (e: 'fecharMenuMobile'): void;
}>();
</script>

<template>
  <!-- Backdrop para telas móveis -->
  <div
    v-if="menuAbertoMobile"
    @click="emit('fecharMenuMobile')"
    class="fixed inset-0 bg-slate-900/50 backdrop-blur-xs z-40 lg:hidden"
  ></div>

  <!-- Sidebar Lateral Esquerda -->
  <aside
    :class="[
      'fixed top-0 bottom-0 left-0 z-50 w-72 bg-slate-900 text-slate-200 flex flex-col border-r border-slate-800 transition-transform duration-300 ease-in-out lg:translate-x-0',
      menuAbertoMobile ? 'translate-x-0 shadow-2xl' : '-translate-x-full lg:translate-x-0'
    ]"
  >
    <!-- Topo da Sidebar: Identidade Institucional -->
    <div class="p-5 border-b border-slate-800/80 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center font-bold text-lg shadow-md shadow-emerald-950">
          A
        </div>
        <div>
          <div class="font-bold text-white text-base tracking-tight flex items-center gap-1.5">
            <span>IFFar AVALIA</span>
          </div>
          <p class="text-[11px] text-emerald-400 font-medium">Autoavaliação Institucional</p>
        </div>
      </div>

      <!-- Botão fechar no mobile -->
      <button
        @click="emit('fecharMenuMobile')"
        class="lg:hidden text-slate-400 hover:text-white p-1 rounded-lg"
      >
        ✕
      </button>
    </div>

    <!-- Navegação / Itens de Menu -->
    <nav class="flex-1 overflow-y-auto p-4 space-y-6 scrollbar-thin">
      <!-- Grupo 1: Gestão de Instrumentos (Primeiro menu!) -->
      <div>
        <div class="px-3 mb-2 text-[11px] font-bold uppercase tracking-wider text-slate-400">
          Gestão de Instrumentos
        </div>
        <div class="space-y-1">
          <!-- Primeiro Item: Cadastro de Questionário -->
          <button
            type="button"
            @click="() => { emit('mudarVisao', 'builder'); emit('fecharMenuMobile'); }"
            :class="[
              'w-full flex items-center gap-3 px-3.5 py-3 rounded-xl text-xs font-semibold transition-all text-left group',
              visaoAtual === 'builder'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/50'
                : 'text-slate-300 hover:bg-slate-800 hover:text-white'
            ]"
          >
            <span class="text-base">📝</span>
            <div class="flex-1">
              <div>Cadastro de Questionário</div>
              <div class="text-[10px] opacity-75 font-normal">Construtor de Instrumentos (RF01-RF05)</div>
            </div>
            <span
              v-if="visaoAtual === 'builder'"
              class="w-1.5 h-1.5 rounded-full bg-white"
            ></span>
          </button>

          <!-- Segundo Item: Públicos e Alvos Avaliados -->
          <button
            type="button"
            @click="() => { emit('mudarVisao', 'catalogo'); emit('fecharMenuMobile'); }"
            :class="[
              'w-full flex items-center gap-3 px-3.5 py-3 rounded-xl text-xs font-semibold transition-all text-left group',
              visaoAtual === 'catalogo'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/50'
                : 'text-slate-300 hover:bg-slate-800 hover:text-white'
            ]"
          >
            <span class="text-base">🎯</span>
            <div class="flex-1">
              <div>Públicos e Alvos</div>
              <div class="text-[10px] opacity-75 font-normal">Quem avalia e o que é avaliado</div>
            </div>
            <span
              v-if="visaoAtual === 'catalogo'"
              class="w-1.5 h-1.5 rounded-full bg-white"
            ></span>
          </button>
        </div>
      </div>

      <!-- Grupo 2: Execução Avaliativa e Ciclos -->
      <div>
        <div class="px-3 mb-2 text-[11px] font-bold uppercase tracking-wider text-slate-400">
          Aplicação e Respostas
        </div>
        <div class="space-y-1">
          <!-- Item: Estudante Responder -->
          <button
            type="button"
            @click="() => { emit('mudarVisao', 'discente'); emit('fecharMenuMobile'); }"
            :class="[
              'w-full flex items-center gap-3 px-3.5 py-3 rounded-xl text-xs font-semibold transition-all text-left',
              visaoAtual === 'discente'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/50'
                : 'text-slate-300 hover:bg-slate-800 hover:text-white'
            ]"
          >
            <span class="text-base">🎓</span>
            <div class="flex-1">
              <div>Responder Avaliação</div>
              <div class="text-[10px] opacity-75 font-normal">Visão do Estudante (ADPD)</div>
            </div>
            <span
              v-if="visaoAtual === 'discente'"
              class="w-1.5 h-1.5 rounded-full bg-white"
            ></span>
          </button>

          <!-- Item: Manifestação Docente (RF-AD07) -->
          <button
            type="button"
            @click="() => { emit('mudarVisao', 'docente'); emit('fecharMenuMobile'); }"
            :class="[
              'w-full flex items-center gap-3 px-3.5 py-3 rounded-xl text-xs font-semibold transition-all text-left',
              visaoAtual === 'docente'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/50'
                : 'text-slate-300 hover:bg-slate-800 hover:text-white'
            ]"
          >
            <span class="text-base">👨‍🏫</span>
            <div class="flex-1">
              <div>Manifestação Docente</div>
              <div class="text-[10px] opacity-75 font-normal">Plano de Desenvolvimento (RF-AD07)</div>
            </div>
            <span
              v-if="visaoAtual === 'docente'"
              class="w-1.5 h-1.5 rounded-full bg-white"
            ></span>
          </button>
        </div>
      </div>

      <!-- Grupo 3: Governança e Tomada de Decisão -->
      <div>
        <div class="px-3 mb-2 text-[11px] font-bold uppercase tracking-wider text-slate-400">
          Governança e Decisão
        </div>
        <div class="space-y-1">
          <!-- Item: Fechamento de Ciclo / Gestão (RF14) -->
          <button
            type="button"
            @click="() => { emit('mudarVisao', 'gestao'); emit('fecharMenuMobile'); }"
            :class="[
              'w-full flex items-center gap-3 px-3.5 py-3 rounded-xl text-xs font-semibold transition-all text-left',
              visaoAtual === 'gestao'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/50'
                : 'text-slate-300 hover:bg-slate-800 hover:text-white'
            ]"
          >
            <span class="text-base">🏛️</span>
            <div class="flex-1">
              <div>Gestão & Plano de Ação</div>
              <div class="text-[10px] opacity-75 font-normal">Fechamento do Ciclo (RF14)</div>
            </div>
            <span
              v-if="visaoAtual === 'gestao'"
              class="w-1.5 h-1.5 rounded-full bg-white"
            ></span>
          </button>

          <!-- Item: Sincronização SIGAA / SIGRH -->
          <button
            type="button"
            @click="() => { emit('mudarVisao', 'sincronizacao'); emit('fecharMenuMobile'); }"
            :class="[
              'w-full flex items-center gap-3 px-3.5 py-3 rounded-xl text-xs font-semibold transition-all text-left',
              visaoAtual === 'sincronizacao'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/50'
                : 'text-slate-300 hover:bg-slate-800 hover:text-white'
            ]"
          >
            <span class="text-base">🔄</span>
            <div class="flex-1">
              <div>Sincronização SIGAA</div>
              <div class="text-[10px] opacity-75 font-normal">Carga Acadêmica & RH (SIGAA)</div>
            </div>
            <span
              v-if="visaoAtual === 'sincronizacao'"
              class="w-1.5 h-1.5 rounded-full bg-white"
            ></span>
          </button>

          <!-- Item: Auditoria & API REST -->
          <button
            type="button"
            @click="() => { emit('mudarVisao', 'tecnica'); emit('fecharMenuMobile'); }"
            :class="[
              'w-full flex items-center gap-3 px-3.5 py-3 rounded-xl text-xs font-semibold transition-all text-left',
              visaoAtual === 'tecnica'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/50'
                : 'text-slate-300 hover:bg-slate-800 hover:text-white'
            ]"
          >
            <span class="text-base">⚡</span>
            <div class="flex-1">
              <div>Auditoria & API REST</div>
              <div class="text-[10px] opacity-75 font-normal">Payloads e Conectividade</div>
            </div>
            <span
              v-if="visaoAtual === 'tecnica'"
              class="w-1.5 h-1.5 rounded-full bg-white"
            ></span>
          </button>
        </div>
      </div>
    </nav>

    <!-- Rodapé da Sidebar: Identificação da Dissertação -->
    <div class="p-4 border-t border-slate-800 bg-slate-950/60 text-center">
      <p class="text-[11px] text-slate-300 font-medium leading-relaxed">
        Sistema Informatizado para o Fechamento do Ciclo Avaliativo<br>
        <span class="text-emerald-400 font-semibold text-[10px]">PPGTER / UFSM</span>
      </p>
    </div>
  </aside>
</template>
