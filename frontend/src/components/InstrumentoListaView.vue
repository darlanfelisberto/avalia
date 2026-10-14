<script setup lang="ts">
import { ref, computed } from 'vue';
import type { InstrumentoAvaliativo, AlvoAvaliado, TipoPublico } from '../types/avaliacao';

const props = defineProps<{
  instrumentos: InstrumentoAvaliativo[];
  carregando: boolean;
  catalogoAlvos: AlvoAvaliado[];
  tiposPublico?: TipoPublico[];
}>();

const emit = defineEmits<{
  (e: 'novo'): void;
  (e: 'editar', instrumento: InstrumentoAvaliativo): void;
}>();

// Filtros e Paginação da Tabela
const termoBusca = ref('');
const filtroModulo = ref('TODOS');
const paginaAtual = ref(1);
const itensPorPagina = ref(6);

function rotuloPorTipo(tipo?: string) {
  if (tipo && props.tiposPublico && props.tiposPublico.length > 0) {
    const encontrado = props.tiposPublico.find(t => t.codigo === tipo);
    if (encontrado) {
      return { singular: encontrado.singular, plural: encontrado.plural, icone: encontrado.icone };
    }
  }
  switch (tipo) {
    case 'CAMPUS': return { singular: 'Campus', plural: 'Campi', icone: '📍' };
    case 'DOCENTE': return { singular: 'Docente', plural: 'Docentes', icone: '👨‍🏫' };
    case 'SERVIDOR': return { singular: 'Servidor / TAE', plural: 'Servidores / TAEs', icone: '💼' };
    case 'TURMA': return { singular: 'Turma', plural: 'Turmas', icone: '👥' };
    case 'CURSO': return { singular: 'Curso', plural: 'Cursos', icone: '📚' };
    case 'SETOR': return { singular: 'Setor', plural: 'Setores', icone: '🛠️' };
    case 'INSTITUICAO': return { singular: 'Instituição', plural: 'Instituições', icone: '🏛️' };
    case 'AUTOAVALIACAO': return { singular: 'Estudante', plural: 'Estudantes', icone: '👤' };
    default: return { singular: 'Alvo', plural: 'Alvos', icone: '🎯' };
  }
}

function obterNomeAlvo(alvoId?: string) {
  if (!alvoId) return '';
  const alvo = props.catalogoAlvos.find(a => a.id === alvoId);
  return alvo ? alvo.nome : alvoId;
}

function obterResumoAlvo(item: InstrumentoAvaliativo): string {
  if (item.tipoAlvo === 'INSTITUICAO' || item.tipoAlvo === 'AUTOAVALIACAO') {
    return rotuloPorTipo(item.tipoAlvo).singular;
  }
  const ids = item.alvosEspecificosIds && item.alvosEspecificosIds.length > 0
    ? item.alvosEspecificosIds
    : (item.alvoEspecificoId ? [item.alvoEspecificoId] : []);

  if (ids.length === 0) {
    return `Todos(as) os(as) ${rotuloPorTipo(item.tipoAlvo).plural} (Geral)`;
  }
  if (ids.length === 1) {
    return obterNomeAlvo(ids[0]);
  }
  const primeiro = obterNomeAlvo(ids[0]);
  return `${ids.length} selecionados (${primeiro} +${ids.length - 1})`;
}

function formatarDataPtBr(dataStr?: string): string {
  if (!dataStr) return '—';
  try {
    if (dataStr.includes('T')) {
      const partes = dataStr.split('T');
      const dataParte = partes[0];
      const horaMin = (partes[1] || '').substring(0, 5);
      if (dataParte.includes('-')) {
        const [ano, mes, dia] = dataParte.split('-');
        return `${dia}/${mes}/${ano}${horaMin ? ` às ${horaMin}` : ''}`;
      }
    }
    if (dataStr.length === 10 && dataStr.includes('-')) {
      const [ano, mes, dia] = dataStr.split('-');
      return `${dia}/${mes}/${ano}`;
    }
    const d = new Date(dataStr);
    if (!isNaN(d.getTime())) {
      const dia = String(d.getDate()).padStart(2, '0');
      const mes = String(d.getMonth() + 1).padStart(2, '0');
      const ano = d.getFullYear();
      const hora = String(d.getHours()).padStart(2, '0');
      const min = String(d.getMinutes()).padStart(2, '0');
      return `${dia}/${mes}/${ano} às ${hora}:${min}`;
    }
    return dataStr;
  } catch {
    return dataStr;
  }
}

const questionariosFiltrados = computed(() => {
  let list = props.instrumentos || [];

  if (filtroModulo.value !== 'TODOS') {
    list = list.filter(q => q.modulo === filtroModulo.value);
  }

  if (termoBusca.value.trim()) {
    const t = termoBusca.value.toLowerCase();
    list = list.filter(q =>
      q.titulo.toLowerCase().includes(t) ||
      (q.subtitulo && q.subtitulo.toLowerCase().includes(t)) ||
      (q.criadorNome && q.criadorNome.toLowerCase().includes(t)) ||
      q.tipoAlvo.toLowerCase().includes(t) ||
      q.tipoPublico.toLowerCase().includes(t)
    );
  }

  return list;
});

const totalPaginas = computed(() => {
  return Math.ceil(questionariosFiltrados.value.length / itensPorPagina.value) || 1;
});

const questionariosPaginados = computed(() => {
  const inicio = (paginaAtual.value - 1) * itensPorPagina.value;
  return questionariosFiltrados.value.slice(inicio, inicio + itensPorPagina.value);
});

function mudarPagina(nova: number) {
  if (nova >= 1 && nova <= totalPaginas.value) {
    paginaAtual.value = nova;
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Topo da Listagem -->
    <div class="bg-white border border-slate-200 rounded-2xl p-6 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <div class="flex items-center gap-2">
          <span class="text-xs bg-emerald-100 text-emerald-800 font-bold px-2.5 py-0.5 rounded-full uppercase">
            Gestão de Instrumentos
          </span>
          <span class="text-xs text-slate-400">• Painel do Criador</span>
        </div>
        <h2 class="text-xl md:text-2xl font-bold text-slate-900 mt-1">
          Questionários e Instrumentos Avaliativos
        </h2>
        <p class="text-xs md:text-sm text-slate-500 mt-1 max-w-2xl leading-relaxed">
          Consulte a lista de questionários cadastrados, veja o público e o alvo avaliado de cada um,
          edite suas questões ou crie um novo instrumento associado ao seu usuário.
        </p>
      </div>

      <button
        type="button"
        @click="emit('novo')"
        class="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2 self-start md:self-center cursor-pointer"
      >
        <span class="text-base font-bold">＋</span>
        <span>Cadastrar Novo Questionário</span>
      </button>
    </div>

    <!-- Barra de Filtros, Pesquisa e Paginação -->
    <div class="bg-white border border-slate-200 rounded-xl p-4 flex flex-col md:flex-row items-center justify-between gap-3 shadow-2xs">
      <div class="flex flex-col sm:flex-row items-center gap-3 w-full md:w-auto">
        <!-- Campo de Busca Textual -->
        <div class="relative w-full sm:w-64">
          <span class="absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">🔍</span>
          <input
            v-model="termoBusca"
            type="text"
            placeholder="Buscar por título, autor, alvo..."
            class="w-full pl-9 pr-3 py-2 text-xs border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden bg-slate-50/50"
          />
        </div>

        <!-- Filtro por Módulo -->
        <div class="flex items-center gap-2 w-full sm:w-auto">
          <span class="text-xs font-bold text-slate-500 whitespace-nowrap">Módulo:</span>
          <select
            v-model="filtroModulo"
            class="p-2 text-xs border border-slate-200 rounded-xl bg-slate-50 font-medium text-slate-700"
          >
            <option value="TODOS">Todos os Módulos</option>
            <option value="ADPD">ADPD (Docente pelo Discente)</option>
            <option value="SINAES">SINAES (Autoavaliação Geral)</option>
            <option value="APRENDIZAGEM">Aprendizagem (Docente)</option>
          </select>
        </div>
      </div>

      <div class="flex items-center gap-2 text-xs text-slate-500 self-end md:self-center">
        <span>Total: <strong>{{ questionariosFiltrados.length }}</strong> instrumentos</span>
      </div>
    </div>

    <!-- Tabela Paginada de Questionários -->
    <div class="bg-white border border-slate-200 rounded-2xl shadow-xs overflow-hidden">
      <div v-if="carregando" class="p-12 text-center text-xs text-slate-400">
        Carregando questionários cadastrados...
      </div>
      <div v-else-if="questionariosFiltrados.length === 0" class="p-12 text-center space-y-3">
        <div class="text-3xl">📋</div>
        <h3 class="text-base font-bold text-slate-800">Nenhum questionário encontrado</h3>
        <p class="text-xs text-slate-500 max-w-md mx-auto">
          Tente ajustar os termos de busca ou crie um novo questionário.
        </p>
        <button
          type="button"
          @click="emit('novo')"
          class="px-4 py-2 bg-emerald-600 text-white text-xs font-bold rounded-xl shadow-xs cursor-pointer"
        >
          Cadastrar Novo Questionário
        </button>
      </div>
      <div v-else>
        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs border-collapse">
            <thead>
              <tr class="bg-slate-50/80 border-b border-slate-200 text-slate-600 font-bold uppercase tracking-wider text-[11px]">
                <th class="py-3 px-4">Instrumento / Título</th>
                <th class="py-3 px-4">Módulo</th>
                <th class="py-3 px-4">Ciclo</th>
                <th class="py-3 px-4">Período de Aplicação</th>
                <th class="py-3 px-4">Alvo Avaliado</th>
                <th class="py-3 px-4">Público Respondente</th>
                <th class="py-3 px-4">Autor / Criador</th>
                <th class="py-3 px-4 text-center">Status</th>
                <th class="py-3 px-4 text-center">Ações</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 text-slate-700">
              <tr
                v-for="item in questionariosPaginados"
                :key="item.id"
                class="hover:bg-slate-50/70 transition-colors"
              >
                <td class="py-3.5 px-4 font-semibold text-slate-900 max-w-xs">
                  <div class="text-sm font-bold text-slate-900">{{ item.titulo }}</div>
                  <div class="text-[11px] text-slate-500 font-normal truncate mt-0.5" :title="item.subtitulo">
                    {{ item.subtitulo || 'Sem subtítulo normativo' }}
                  </div>
                </td>
                <td class="py-3.5 px-4 whitespace-nowrap">
                  <span class="px-2 py-0.5 rounded-md text-[10px] font-bold bg-slate-100 text-slate-700 border">
                    {{ item.modulo }}
                  </span>
                </td>
                <td class="py-3.5 px-4 whitespace-nowrap font-medium text-slate-600">
                  {{ item.anoCiclo }}
                </td>
                <td class="py-3.5 px-4 whitespace-nowrap text-slate-600 font-mono text-[11px]">
                  <div class="flex items-center gap-1.5 text-slate-700">
                    <span>📅</span>
                    <span>{{ formatarDataPtBr(item.dataInicio) }}</span>
                  </div>
                  <div class="text-[10px] text-slate-400 pl-5">
                    até {{ formatarDataPtBr(item.dataTermino) }}
                  </div>
                </td>
                <td class="py-3.5 px-4 whitespace-nowrap">
                  <div class="flex flex-col items-start gap-1">
                    <span class="px-2 py-0.5 rounded-md text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200 w-fit">
                      {{ rotuloPorTipo(item.tipoAlvo).icone }} {{ item.tipoAlvo }}
                    </span>
                    <span class="text-[11px] font-bold text-slate-800">
                      {{ obterResumoAlvo(item) }}
                    </span>
                  </div>
                </td>
                <td class="py-3.5 px-4 whitespace-nowrap">
                  <div v-if="item.publicosPermitidos && item.publicosPermitidos.length > 1" class="flex flex-wrap gap-1 max-w-[200px]">
                    <span
                      v-for="pub in item.publicosPermitidos"
                      :key="pub"
                      class="px-1.5 py-0.5 rounded-md text-[9px] font-bold bg-blue-50 text-blue-700 border border-blue-200"
                    >
                      {{ pub }}
                    </span>
                  </div>
                  <span v-else class="px-2 py-0.5 rounded-md text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                    {{ item.tipoPublico }}
                  </span>
                  <div v-if="item.tipoAlvo === 'CAMPUS' && item.restringirVinculoCampus" class="text-[10px] text-emerald-700 font-semibold mt-1 flex items-center gap-1">
                    <span>🔒</span>
                    <span>Restrito ao Campus</span>
                  </div>
                </td>
                <td class="py-3.5 px-4 text-slate-600">
                  <div class="font-medium text-slate-800">{{ item.criadorNome || 'CPA' }}</div>
                  <div class="text-[10px] text-slate-400 font-mono">{{ item.criadorId || '—' }}</div>
                </td>
                <td class="py-3.5 px-4 text-center whitespace-nowrap">
                  <span :class="['px-2 py-0.5 rounded-full text-[10px] font-bold', item.status === 'ABERTO' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-100 text-slate-600']">
                    {{ item.status || 'ABERTO' }}
                  </span>
                </td>
                <td class="py-3.5 px-4 text-center whitespace-nowrap">
                  <button
                    type="button"
                    @click="emit('editar', item)"
                    class="px-3 py-1.5 bg-slate-100 hover:bg-emerald-600 hover:text-white text-slate-700 font-bold rounded-lg transition-colors text-xs inline-flex items-center gap-1.5 shadow-2xs cursor-pointer"
                  >
                    <span>✏️</span>
                    <span>Editar</span>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Rodapé com Paginação -->
        <div class="p-4 bg-slate-50/50 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-600">
          <div>
            Mostrando página <strong>{{ paginaAtual }}</strong> de <strong>{{ totalPaginas }}</strong>
            (itens {{ (paginaAtual - 1) * itensPorPagina + 1 }} a {{ Math.min(paginaAtual * itensPorPagina, questionariosFiltrados.length) }})
          </div>

          <div class="flex items-center gap-1">
            <button
              type="button"
              @click="mudarPagina(1)"
              :disabled="paginaAtual === 1"
              class="px-2.5 py-1.5 border rounded-lg bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed font-bold cursor-pointer"
              title="Primeira página"
            >
              «
            </button>
            <button
              type="button"
              @click="mudarPagina(paginaAtual - 1)"
              :disabled="paginaAtual === 1"
              class="px-2.5 py-1.5 border rounded-lg bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed font-medium cursor-pointer"
            >
              Anterior
            </button>

            <span class="px-3 py-1 font-bold text-slate-900 bg-white border rounded-lg shadow-2xs">
              {{ paginaAtual }}
            </span>

            <button
              type="button"
              @click="mudarPagina(paginaAtual + 1)"
              :disabled="paginaAtual === totalPaginas"
              class="px-2.5 py-1.5 border rounded-lg bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed font-medium cursor-pointer"
            >
              Próxima
            </button>
            <button
              type="button"
              @click="mudarPagina(totalPaginas)"
              :disabled="paginaAtual === totalPaginas"
              class="px-2.5 py-1.5 border rounded-lg bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed font-bold cursor-pointer"
              title="Última página"
            >
              »
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
