<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import type { AlvoAvaliado, PublicoAvaliador, TipoAlvoAvaliado } from '../types/avaliacao';
import { listarAlvosApi, salvarAlvoApi, listarPublicosApi, salvarPublicoApi } from '../services/api';

const abaAtiva = ref<'alvos' | 'publicos'>('alvos');

// Estado Alvos
const alvos = ref<AlvoAvaliado[]>([]);
const filtroTipoAlvo = ref<string>('TODOS');
const carregandoAlvos = ref(false);

const modalAlvoAberto = ref(false);
const alvoEdicao = ref<Partial<AlvoAvaliado>>({
  tipo: 'DOCENTE',
  nome: '',
  codigo: '',
  descricao: '',
  campus: 'Alegrete',
  ativo: true
});

// Estado Públicos
const publicos = ref<PublicoAvaliador[]>([]);
const carregandoPublicos = ref(false);

const modalPublicoAberto = ref(false);
const publicoEdicao = ref<Partial<PublicoAvaliador>>({
  segmento: 'DISCENTE',
  nome: '',
  descricao: '',
  exigeAutenticacao: true,
  ativo: true
});

const alvosFiltrados = computed(() => {
  if (filtroTipoAlvo.value === 'TODOS') return alvos.value;
  return alvos.value.filter(a => a.tipo === filtroTipoAlvo.value);
});

async function carregarDados() {
  carregandoAlvos.value = true;
  carregandoPublicos.value = true;
  alvos.value = await listarAlvosApi();
  publicos.value = await listarPublicosApi();
  carregandoAlvos.value = false;
  carregandoPublicos.value = false;
}

onMounted(() => {
  carregarDados();
});

function novoAlvo() {
  alvoEdicao.value = {
    tipo: 'DOCENTE',
    nome: '',
    codigo: '',
    descricao: '',
    campus: 'Alegrete',
    ativo: true
  };
  modalAlvoAberto.value = true;
}

async function submeterAlvo() {
  if (!alvoEdicao.value.nome?.trim()) return;
  await salvarAlvoApi(alvoEdicao.value);
  modalAlvoAberto.value = false;
  alvos.value = await listarAlvosApi();
}

function novoPublico() {
  publicoEdicao.value = {
    segmento: 'DISCENTE',
    nome: '',
    descricao: '',
    exigeAutenticacao: true,
    ativo: true
  };
  modalPublicoAberto.value = true;
}

async function submeterPublico() {
  if (!publicoEdicao.value.nome?.trim()) return;
  await salvarPublicoApi(publicoEdicao.value);
  modalPublicoAberto.value = false;
  publicos.value = await listarPublicosApi();
}

function getTipoAlvoBadge(tipo: TipoAlvoAvaliado) {
  switch (tipo) {
    case 'INSTITUICAO':
      return 'bg-purple-100 text-purple-800 border-purple-200';
    case 'CAMPUS':
      return 'bg-blue-100 text-blue-800 border-blue-200';
    case 'SETOR':
      return 'bg-amber-100 text-amber-800 border-amber-200';
    case 'DOCENTE':
      return 'bg-emerald-100 text-emerald-800 border-emerald-200';
    case 'CURSO':
      return 'bg-indigo-100 text-indigo-800 border-indigo-200';
    case 'TURMA':
      return 'bg-cyan-100 text-cyan-800 border-cyan-200';
    default:
      return 'bg-slate-100 text-slate-800 border-slate-200';
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Cabeçalho Institucional -->
    <div class="bg-white border border-slate-200 rounded-2xl p-6 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <div class="flex items-center gap-2">
          <span class="text-xs font-bold uppercase tracking-wider bg-emerald-100 text-emerald-800 px-2.5 py-0.5 rounded-full">
            Arquitetura Parametrizável de Avaliação
          </span>
          <span class="text-xs text-slate-400">• Núcleo Reutilizável</span>
        </div>
        <h1 class="text-xl md:text-2xl font-bold text-slate-900 mt-1">
          Catálogo de Públicos e Alvos Avaliados
        </h1>
        <p class="text-xs md:text-sm text-slate-500 mt-1 max-w-2xl leading-relaxed">
          Gerencie <strong>quem avalia</strong> (públicos e segmentos acadêmicos) e <strong>o que é avaliado</strong> 
          (instituição, campi, setores, docentes e turmas) para aplicação em múltiplos instrumentos.
        </p>
      </div>

      <!-- Abas de Alternância -->
      <div class="flex items-center gap-2 bg-slate-100 p-1 rounded-xl self-start md:self-center">
        <button
          type="button"
          @click="abaAtiva = 'alvos'"
          :class="[
            'px-4 py-2 text-xs font-bold rounded-lg transition-all',
            abaAtiva === 'alvos'
              ? 'bg-white text-slate-900 shadow-xs'
              : 'text-slate-600 hover:text-slate-900'
          ]"
        >
          🎯 Alvos Avaliados ({{ alvos.length }})
        </button>
        <button
          type="button"
          @click="abaAtiva = 'publicos'"
          :class="[
            'px-4 py-2 text-xs font-bold rounded-lg transition-all',
            abaAtiva === 'publicos'
              ? 'bg-white text-slate-900 shadow-xs'
              : 'text-slate-600 hover:text-slate-900'
          ]"
        >
          👥 Públicos Avaliadores ({{ publicos.length }})
        </button>
      </div>
    </div>

    <!-- ==================== SEÇÃO 1: ALVOS AVALIADOS ==================== -->
    <div v-if="abaAtiva === 'alvos'" class="space-y-4">
      <!-- Barra de Filtros e Ação -->
      <div class="bg-white border border-slate-200 rounded-xl p-4 flex flex-col sm:flex-row items-center justify-between gap-3 shadow-2xs">
        <div class="flex items-center gap-2 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
          <span class="text-xs font-bold text-slate-500 whitespace-nowrap">Filtrar por:</span>
          <button
            type="button"
            v-for="tipo in ['TODOS', 'INSTITUICAO', 'CAMPUS', 'SETOR', 'DOCENTE', 'CURSO', 'TURMA']"
            :key="tipo"
            @click="filtroTipoAlvo = tipo"
            :class="[
              'px-2.5 py-1 text-xs rounded-lg font-medium transition-all whitespace-nowrap',
              filtroTipoAlvo === tipo
                ? 'bg-emerald-600 text-white font-bold shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            ]"
          >
            {{ tipo }}
          </button>
        </div>

        <button
          type="button"
          @click="novoAlvo"
          class="w-full sm:w-auto px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-xs transition-all flex items-center justify-center gap-1.5"
        >
          <span>＋</span>
          <span>Novo Alvo Avaliado</span>
        </button>
      </div>

      <!-- Tabela / Lista de Alvos -->
      <div class="bg-white border border-slate-200 rounded-2xl shadow-xs overflow-hidden">
        <div v-if="carregandoAlvos" class="p-8 text-center text-xs text-slate-400">
          Carregando catálogo de alvos...
        </div>
        <div v-else-if="alvosFiltrados.length === 0" class="p-8 text-center text-xs text-slate-400">
          Nenhum alvo encontrado para o filtro selecionado.
        </div>
        <table v-else class="w-full text-left text-xs border-collapse">
          <thead>
            <tr class="bg-slate-50/80 border-b border-slate-200 text-slate-600 font-bold uppercase tracking-wider text-[11px]">
              <th class="py-3 px-4">Tipo</th>
              <th class="py-3 px-4">Nome / Denominação</th>
              <th class="py-3 px-4">Código / Sigla</th>
              <th class="py-3 px-4">Campus / Unidade</th>
              <th class="py-3 px-4">Descrição</th>
              <th class="py-3 px-4 text-center">Status</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 text-slate-700">
            <tr v-for="alvo in alvosFiltrados" :key="alvo.id" class="hover:bg-slate-50/50 transition-colors">
              <td class="py-3 px-4 whitespace-nowrap">
                <span :class="['px-2 py-0.5 rounded-md text-[10px] font-bold border', getTipoAlvoBadge(alvo.tipo)]">
                  {{ alvo.tipo }}
                </span>
              </td>
              <td class="py-3 px-4 font-semibold text-slate-900">
                {{ alvo.nome }}
              </td>
              <td class="py-3 px-4 font-mono text-slate-500">
                {{ alvo.codigo || '—' }}
              </td>
              <td class="py-3 px-4 text-slate-600">
                {{ alvo.campus || 'Geral' }}
              </td>
              <td class="py-3 px-4 text-slate-500 text-[11px] max-w-xs truncate">
                {{ alvo.descricao || 'Sem descrição' }}
              </td>
              <td class="py-3 px-4 text-center">
                <span :class="['px-2 py-0.5 rounded-full text-[10px] font-bold', alvo.ativo ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700']">
                  {{ alvo.ativo ? 'Ativo' : 'Inativo' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ==================== SEÇÃO 2: PÚBLICOS AVALIADORES ==================== -->
    <div v-else class="space-y-4">
      <div class="bg-white border border-slate-200 rounded-xl p-4 flex items-center justify-between shadow-2xs">
        <div>
          <h2 class="text-sm font-bold text-slate-900">Segmentos de Usuários que Acessam e Respondem</h2>
          <p class="text-xs text-slate-500">Define as permissões, tipos de login e se exige autenticação institucional (SIGAA).</p>
        </div>
        <button
          type="button"
          @click="novoPublico"
          class="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-xs transition-all flex items-center gap-1.5"
        >
          <span>＋</span>
          <span>Novo Público</span>
        </button>
      </div>

      <!-- Grid de Públicos -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="pub in publicos"
          :key="pub.id"
          class="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between space-y-3"
        >
          <div>
            <div class="flex items-center justify-between">
              <span class="text-[10px] font-bold uppercase tracking-wider bg-slate-100 text-slate-700 px-2 py-0.5 rounded-md">
                Segmento: {{ pub.segmento }}
              </span>
              <span :class="['text-[10px] font-semibold px-2 py-0.5 rounded-full', pub.exigeAutenticacao ? 'bg-blue-50 text-blue-700 border border-blue-200' : 'bg-amber-50 text-amber-700 border border-amber-200']">
                {{ pub.exigeAutenticacao ? '🔒 Exige Login SIGAA' : '🌐 Acesso Aberto' }}
              </span>
            </div>
            <h3 class="text-sm font-bold text-slate-900 mt-2.5">
              {{ pub.nome }}
            </h3>
            <p class="text-xs text-slate-500 mt-1 leading-relaxed">
              {{ pub.descricao }}
            </p>
          </div>

          <div class="pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
            <span>Status: <strong class="text-emerald-700">Ativo</strong></span>
            <span class="font-mono text-[10px]">ID: {{ pub.id }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Cadastro de Alvo -->
    <div
      v-if="modalAlvoAberto"
      class="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center z-50 p-4"
    >
      <div class="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
        <h3 class="text-base font-bold text-slate-900">Novo Alvo Avaliado</h3>
        <div class="space-y-3 text-xs">
          <div>
            <label class="font-bold text-slate-700">Tipo de Alvo:</label>
            <select v-model="alvoEdicao.tipo" class="w-full mt-1 p-2.5 border rounded-xl bg-slate-50">
              <option value="INSTITUICAO">INSTITUICAO (Geral / PDI)</option>
              <option value="CAMPUS">CAMPUS (Unidade de Ensino)</option>
              <option value="SETOR">SETOR (Administrativo / Biblioteca / TI)</option>
              <option value="DOCENTE">DOCENTE (Professor)</option>
              <option value="CURSO">CURSO (Coordenação / Colegiado)</option>
              <option value="TURMA">TURMA (Componente específico)</option>
            </select>
          </div>

          <div>
            <label class="font-bold text-slate-700">Nome / Denominação:</label>
            <input v-model="alvoEdicao.nome" type="text" placeholder="Ex: Diretoria de Ensino ou Prof. Roberto" class="w-full mt-1 p-2.5 border rounded-xl" />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="font-bold text-slate-700">Código / Sigla:</label>
              <input v-model="alvoEdicao.codigo" type="text" placeholder="Ex: SET-DEN" class="w-full mt-1 p-2.5 border rounded-xl" />
            </div>
            <div>
              <label class="font-bold text-slate-700">Campus:</label>
              <input v-model="alvoEdicao.campus" type="text" placeholder="Ex: Alegrete" class="w-full mt-1 p-2.5 border rounded-xl" />
            </div>
          </div>

          <div>
            <label class="font-bold text-slate-700">Descrição / Escopo:</label>
            <textarea v-model="alvoEdicao.descricao" rows="2" placeholder="Descreva as atribuições ou foco" class="w-full mt-1 p-2.5 border rounded-xl"></textarea>
          </div>
        </div>

        <div class="flex justify-end gap-2 pt-2 border-t">
          <button type="button" @click="modalAlvoAberto = false" class="px-4 py-2 border rounded-xl text-xs font-bold text-slate-600">Cancelar</button>
          <button type="button" @click="submeterAlvo" class="px-4 py-2 bg-emerald-600 text-white rounded-xl text-xs font-bold">Salvar Alvo</button>
        </div>
      </div>
    </div>

    <!-- Modal Cadastro de Público -->
    <div
      v-if="modalPublicoAberto"
      class="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center z-50 p-4"
    >
      <div class="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
        <h3 class="text-base font-bold text-slate-900">Novo Público Avaliador</h3>
        <div class="space-y-3 text-xs">
          <div>
            <label class="font-bold text-slate-700">Segmento:</label>
            <select v-model="publicoEdicao.segmento" class="w-full mt-1 p-2.5 border rounded-xl bg-slate-50">
              <option value="DISCENTE">DISCENTE (Alunos matriculados)</option>
              <option value="DOCENTE">DOCENTE (Corpo docente)</option>
              <option value="TAE">TAE (Técnico Administrativo)</option>
              <option value="EGRESSO">EGRESSO (Ex-alunos)</option>
              <option value="SOCIEDADE_CIVIL">SOCIEDADE_CIVIL (Comunidade externa)</option>
              <option value="TODOS">TODOS (Comunidade Geral)</option>
            </select>
          </div>

          <div>
            <label class="font-bold text-slate-700">Nome do Público:</label>
            <input v-model="publicoEdicao.nome" type="text" placeholder="Ex: Alunos dos Cursos Técnicos Integrados" class="w-full mt-1 p-2.5 border rounded-xl" />
          </div>

          <div>
            <label class="font-bold text-slate-700">Descrição:</label>
            <textarea v-model="publicoEdicao.descricao" rows="2" placeholder="Critérios de quem participa deste segmento" class="w-full mt-1 p-2.5 border rounded-xl"></textarea>
          </div>

          <div class="flex items-center gap-2 pt-1">
            <input v-model="publicoEdicao.exigeAutenticacao" type="checkbox" id="chk-auth" class="rounded text-emerald-600" />
            <label for="chk-auth" class="font-semibold text-slate-700">Exige autenticação obrigatória (CPF / SIGAA)</label>
          </div>
        </div>

        <div class="flex justify-end gap-2 pt-2 border-t">
          <button type="button" @click="modalPublicoAberto = false" class="px-4 py-2 border rounded-xl text-xs font-bold text-slate-600">Cancelar</button>
          <button type="button" @click="submeterPublico" class="px-4 py-2 bg-emerald-600 text-white rounded-xl text-xs font-bold">Salvar Público</button>
        </div>
      </div>
    </div>
  </div>
</template>
