<script setup lang="ts">
import { ref, computed } from 'vue';
import type { InstrumentoAvaliativo, AlvoAvaliado, TipoPublicoAvaliador, TipoPublico } from '../types/avaliacao';

const props = defineProps<{
  form: InstrumentoAvaliativo;
  catalogoAlvos: AlvoAvaliado[];
  tiposPublico: TipoPublico[];
  rotuloTipoAlvo: {
    singular: string;
    plural: string;
    icone: string;
    descricao: string;
  };
  carregandoAlvos: boolean;
}>();

const termoBuscaAlvo = ref('');

const instanciasAlvoDisponiveis = computed(() => {
  return props.catalogoAlvos.filter(a => a.tipo === props.form.tipoAlvo);
});

const instanciasAlvoFiltradas = computed(() => {
  const lista = instanciasAlvoDisponiveis.value;
  if (!termoBuscaAlvo.value.trim()) return lista;
  const t = termoBuscaAlvo.value.toLowerCase();
  return lista.filter(a =>
    a.nome.toLowerCase().includes(t) ||
    (a.codigo && a.codigo.toLowerCase().includes(t)) ||
    (a.campus && a.campus.toLowerCase().includes(t))
  );
});

const temAlvosEspecificosSelecionados = computed(() => {
  return !!props.form.alvosEspecificosIds && props.form.alvosEspecificosIds.length > 0;
});

function ehAlvoSelecionado(id: string): boolean {
  return !!props.form.alvosEspecificosIds && props.form.alvosEspecificosIds.includes(id);
}

function alternarAlvoEspecifico(id: string) {
  if (!props.form.alvosEspecificosIds) {
    props.form.alvosEspecificosIds = [];
  }
  const idx = props.form.alvosEspecificosIds.indexOf(id);
  if (idx >= 0) {
    props.form.alvosEspecificosIds.splice(idx, 1);
  } else {
    props.form.alvosEspecificosIds.push(id);
  }
  props.form.alvoEspecificoId = props.form.alvosEspecificosIds.length > 0 ? props.form.alvosEspecificosIds[0] : undefined;
}

function selecionarTodosAlvosEspecificos() {
  props.form.alvosEspecificosIds = instanciasAlvoDisponiveis.value.map(a => a.id);
  props.form.alvoEspecificoId = props.form.alvosEspecificosIds.length > 0 ? props.form.alvosEspecificosIds[0] : undefined;
}

function limparAlvosEspecificos() {
  props.form.alvosEspecificosIds = [];
  props.form.alvoEspecificoId = undefined;
}

function alternarPublicoPermitido(publico: TipoPublicoAvaliador) {
  if (!props.form.publicosPermitidos) {
    props.form.publicosPermitidos = [];
  }
  const idx = props.form.publicosPermitidos.indexOf(publico);
  if (idx >= 0) {
    if (props.form.publicosPermitidos.length > 1) {
      props.form.publicosPermitidos.splice(idx, 1);
    }
  } else {
    props.form.publicosPermitidos.push(publico);
  }
  if (props.form.publicosPermitidos.length > 1) {
    props.form.tipoPublico = 'TODOS';
  } else if (props.form.publicosPermitidos.length === 1) {
    props.form.tipoPublico = props.form.publicosPermitidos[0];
  }
}

function temPublicoPermitido(publico: TipoPublicoAvaliador): boolean {
  return !!props.form.publicosPermitidos && props.form.publicosPermitidos.includes(publico);
}
</script>

<template>
  <div class="space-y-4 md:col-span-2 p-4 md:p-5 bg-purple-50/60 border border-purple-200 rounded-2xl">
    <!-- Cabeçalho do Escopo -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-1 border-b border-purple-100 pb-3">
      <div>
        <div class="flex items-center gap-1.5 text-xs font-bold text-purple-950 uppercase tracking-wide">
          <span>{{ rotuloTipoAlvo.icone }}</span>
          <span>Configuração de Avaliação de {{ rotuloTipoAlvo.singular }}</span>
        </div>
        <p class="text-[11px] text-purple-700 mt-0.5">
          {{ rotuloTipoAlvo.descricao }}
        </p>
      </div>
      <span class="text-[10px] text-purple-800 bg-purple-100/80 px-2.5 py-1 rounded-full font-bold border border-purple-200 w-fit">
        Escopo Parametrizável
      </span>
    </div>

    <!-- Controle de Escopo: Todos vs Seleção Específica -->
    <div class="space-y-2">
      <label class="text-xs font-bold text-purple-950">
        Escopo dos(as) {{ rotuloTipoAlvo.plural }} a serem avaliados(as):
      </label>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
        <button
          type="button"
          @click="limparAlvosEspecificos"
          :class="[
            'p-3 rounded-xl border text-left text-xs font-semibold transition-all flex items-start gap-2.5',
            !temAlvosEspecificosSelecionados
              ? 'bg-white border-purple-500 text-purple-950 shadow-2xs ring-2 ring-purple-400/40'
              : 'bg-white/60 border-purple-200 text-slate-500 hover:border-purple-300'
          ]"
        >
          <span class="text-base">🌐</span>
          <div>
            <div class="font-bold">Todos(as) os(as) {{ rotuloTipoAlvo.plural }} (Geral)</div>
            <div class="text-[11px] font-normal text-purple-700 mt-0.5">
              Cada respondente avaliará o seu respectivo vínculo institucional no período.
            </div>
          </div>
        </button>

        <button
          type="button"
          @click="() => { if (!temAlvosEspecificosSelecionados && instanciasAlvoDisponiveis.length > 0) alternarAlvoEspecifico(instanciasAlvoDisponiveis[0].id); }"
          :class="[
            'p-3 rounded-xl border text-left text-xs font-semibold transition-all flex items-start gap-2.5',
            temAlvosEspecificosSelecionados
              ? 'bg-white border-purple-500 text-purple-950 shadow-2xs ring-2 ring-purple-400/40'
              : 'bg-white/60 border-purple-200 text-slate-500 hover:border-purple-300'
          ]"
        >
          <span class="text-base">🎯</span>
          <div>
            <div class="font-bold">Selecionar {{ rotuloTipoAlvo.plural }} Específicos(as)</div>
            <div class="text-[11px] font-normal text-purple-700 mt-0.5">
              {{ temAlvosEspecificosSelecionados ? `${form.alvosEspecificosIds?.length} selecionado(s)` : 'Marque um ou mais alvos da lista abaixo.' }}
            </div>
          </div>
        </button>
      </div>
    </div>

    <!-- Lista com busca de instâncias específicas -->
    <div v-if="temAlvosEspecificosSelecionados" class="space-y-2 pt-2 border-t border-purple-100">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div class="text-xs font-bold text-purple-900">
          Selecione os(as) {{ rotuloTipoAlvo.plural }} da Avaliação:
        </div>
        <div class="flex items-center gap-2">
          <button
            type="button"
            @click="selecionarTodosAlvosEspecificos"
            class="text-[11px] font-bold text-purple-700 hover:text-purple-950 bg-purple-100/80 px-2 py-0.5 rounded-md transition-colors"
          >
            Marcar Todos ({{ instanciasAlvoDisponiveis.length }})
          </button>
          <button
            type="button"
            @click="limparAlvosEspecificos"
            class="text-[11px] font-bold text-rose-600 hover:text-rose-800 bg-rose-50 px-2 py-0.5 rounded-md transition-colors"
          >
            Limpar Seleção
          </button>
        </div>
      </div>

      <!-- Busca rápida -->
      <div class="relative">
        <input
          v-model="termoBuscaAlvo"
          type="text"
          :placeholder="`Buscar ${rotuloTipoAlvo.singular.toLowerCase()} por nome ou código...`"
          class="w-full text-xs p-2.5 pl-8 border border-purple-200 rounded-xl bg-white outline-hidden focus:ring-2 focus:ring-purple-400"
        />
        <span class="absolute left-2.5 top-2.5 text-xs text-purple-400">🔍</span>
      </div>

      <!-- Grid com scroll de instâncias -->
      <div class="max-h-52 overflow-y-auto space-y-1.5 p-2 bg-white/70 border border-purple-200 rounded-xl">
        <div v-if="carregandoAlvos" class="text-xs text-center py-4 text-purple-600 font-semibold animate-pulse">
          Carregando alvos disponíveis...
        </div>
        <div v-else-if="instanciasAlvoFiltradas.length === 0" class="text-xs text-center py-4 text-purple-400">
          Nenhum {{ rotuloTipoAlvo.singular.toLowerCase() }} encontrado com o filtro aplicado.
        </div>
        <label
          v-for="alvo in instanciasAlvoFiltradas"
          :key="alvo.id"
          :class="[
            'flex items-center justify-between p-2.5 rounded-lg border text-xs cursor-pointer transition-all',
            ehAlvoSelecionado(alvo.id)
              ? 'bg-purple-50 border-purple-400 text-purple-950 shadow-2xs font-bold'
              : 'bg-white border-slate-200 text-slate-700 hover:border-purple-200'
          ]"
        >
          <div class="flex items-center gap-2.5">
            <input
              type="checkbox"
              :checked="ehAlvoSelecionado(alvo.id)"
              @change="alternarAlvoEspecifico(alvo.id)"
              class="w-4 h-4 text-purple-600 rounded-sm border-purple-300 focus:ring-purple-500"
            />
            <div>
              <div class="text-slate-900">{{ alvo.nome }}</div>
              <div class="text-[10px] text-slate-400 font-normal">
                {{ alvo.codigo || alvo.campus || '' }}
              </div>
            </div>
          </div>
          <span v-if="alvo.campus" class="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded-full font-normal">
            {{ alvo.campus }}
          </span>
        </label>
      </div>
    </div>

    <!-- Seleção Múltipla de Públicos Respondentes -->
    <div class="space-y-2 pt-2 border-t border-purple-100">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-1">
        <label class="text-xs font-bold text-purple-950 flex items-center gap-1.5">
          <span>👥</span>
          <span>Públicos Elegíveis para Responder (Quem Avalia):</span>
        </label>
        <span class="text-[10px] text-purple-700 font-bold bg-purple-100 px-2 py-0.5 rounded-md">
          Segmentos Permitidos
        </span>
      </div>
      <p class="text-[11px] text-purple-700">
        Marque quais categorias de respondentes terão acesso ao questionário:
      </p>

      <div class="grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-1">
        <label
          :class="[
            'flex items-center gap-2.5 p-3 rounded-xl border text-xs cursor-pointer transition-all',
            temPublicoPermitido('DISCENTES')
              ? 'bg-white border-purple-500 text-purple-950 shadow-2xs ring-1 ring-purple-300'
              : 'bg-white/60 border-purple-200 text-slate-500 hover:border-purple-300'
          ]"
        >
          <input
            type="checkbox"
            :checked="temPublicoPermitido('DISCENTES')"
            @change="alternarPublicoPermitido('DISCENTES')"
            class="w-4 h-4 text-purple-600 rounded-sm border-purple-300 focus:ring-purple-500"
          />
          <div>
            <div class="font-bold">🎓 Discentes (Alunos)</div>
            <div class="text-[10px] text-slate-500">Matrícula ativa no alvo</div>
          </div>
        </label>

        <label
          :class="[
            'flex items-center gap-2.5 p-3 rounded-xl border text-xs cursor-pointer transition-all',
            temPublicoPermitido('DOCENTES')
              ? 'bg-white border-purple-500 text-purple-950 shadow-2xs ring-1 ring-purple-300'
              : 'bg-white/60 border-purple-200 text-slate-500 hover:border-purple-300'
          ]"
        >
          <input
            type="checkbox"
            :checked="temPublicoPermitido('DOCENTES')"
            @change="alternarPublicoPermitido('DOCENTES')"
            class="w-4 h-4 text-purple-600 rounded-sm border-purple-300 focus:ring-purple-500"
          />
          <div>
            <div class="font-bold">👨‍🏫 Corpo Docente</div>
            <div class="text-[10px] text-slate-500">Professores em exercício</div>
          </div>
        </label>

        <label
          :class="[
            'flex items-center gap-2.5 p-3 rounded-xl border text-xs cursor-pointer transition-all',
            temPublicoPermitido('TAES')
              ? 'bg-white border-purple-500 text-purple-950 shadow-2xs ring-1 ring-purple-300'
              : 'bg-white/60 border-purple-200 text-slate-500 hover:border-purple-300'
          ]"
        >
          <input
            type="checkbox"
            :checked="temPublicoPermitido('TAES')"
            @change="alternarPublicoPermitido('TAES')"
            class="w-4 h-4 text-purple-600 rounded-sm border-purple-300 focus:ring-purple-500"
          />
          <div>
            <div class="font-bold">🏢 Técnicos-Administrativos</div>
            <div class="text-[10px] text-slate-500">TAEs lotados no alvo</div>
          </div>
        </label>
      </div>
    </div>

    <!-- Validação SIGAA -->
    <div class="pt-3 border-t border-purple-200/70 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
      <label class="flex items-center gap-2 cursor-pointer text-xs font-semibold text-purple-950">
        <input
          v-model="form.restringirVinculoCampus"
          type="checkbox"
          class="w-4 h-4 text-purple-600 rounded-sm border-purple-300 focus:ring-purple-500"
        />
        <span>Validar vínculo ativo do respondente no alvo avaliado (SIGAA / SIGRH)</span>
      </label>
      <span class="text-[10px] text-purple-800 bg-purple-100/90 px-2 py-0.5 rounded-md font-bold self-start sm:self-auto">
        Validação via SIGAA
      </span>
    </div>
  </div>
</template>
