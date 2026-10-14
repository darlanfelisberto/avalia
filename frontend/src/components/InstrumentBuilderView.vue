<script setup lang="ts">
import { ref, reactive, watch, onMounted, computed } from 'vue';
import type {
  InstrumentoAvaliativo,
  GrupoQuestoes,
  AlvoAvaliado,
  TipoPublico
} from '../types/avaliacao';
import { salvarInstrumentoApi, listarAlvosApi, listarInstrumentosApi, listarTiposPublicoApi } from '../services/api';
import { resetarInstrumentoParaPadrao, carregarUsuarioAtivo } from '../services/storage';
import InstrumentoListaView from './InstrumentoListaView.vue';
import InstrumentoEscopoAlvoEditor from './InstrumentoEscopoAlvoEditor.vue';
import InstrumentoGruposEditor from './InstrumentoGruposEditor.vue';

const props = defineProps<{
  instrumento: InstrumentoAvaliativo;
}>();

const emit = defineEmits<{
  (e: 'instrumentoAtualizado', instrumento: InstrumentoAvaliativo): void;
  (e: 'testarNoRespondente'): void;
}>();

// Modo da tela: 'lista' (gerenciar questionários) ou 'formulario' (cadastrar/editar)
const modoVisualizacao = ref<'lista' | 'formulario'>('lista');

// Catálogos e estado global
const catalogoAlvos = ref<AlvoAvaliado[]>([]);
const tiposPublico = ref<TipoPublico[]>([]);
const meusQuestionarios = ref<InstrumentoAvaliativo[]>([]);
const carregandoQuestionarios = ref(false);
const carregandoAlvos = ref(false);
const usuarioLogado = ref(carregarUsuarioAtivo());

const mensagemSucesso = ref(false);
const mensagemErro = ref<string | null>(null);

// Formatação e normalização de datas
function paraDatetimeLocal(dataStr?: string, horaPadrao: string = '08:00'): string {
  if (!dataStr) return '';
  if (dataStr.includes('T')) {
    const partes = dataStr.split('T');
    return `${partes[0]}T${(partes[1] || '').substring(0, 5) || horaPadrao}`;
  }
  return dataStr;
}

function normalizarInstrumento(inst: InstrumentoAvaliativo): InstrumentoAvaliativo {
  const copia = JSON.parse(JSON.stringify(inst || {}));
  copia.dataInicio = paraDatetimeLocal(copia.dataInicio, '00:00');
  copia.dataTermino = paraDatetimeLocal(copia.dataTermino, '23:59');
  return copia;
}

const form = reactive<InstrumentoAvaliativo>(normalizarInstrumento(props.instrumento));

watch(
  () => props.instrumento,
  (novo) => {
    if (novo) Object.assign(form, normalizarInstrumento(novo));
  },
  { deep: true }
);

// Obter rótulo do tipo a partir da tabela tipo_publico
function rotuloPorTipo(tipo?: string) {
  if (tipo && tiposPublico.value.length > 0) {
    const encontrado = tiposPublico.value.find(t => t.codigo === tipo);
    if (encontrado) {
      return {
        singular: encontrado.singular,
        plural: encontrado.plural,
        icone: encontrado.icone,
        descricao: encontrado.descricao || ''
      };
    }
  }
  return { singular: 'Alvo', plural: 'Alvos', icone: '🎯', descricao: 'Avaliação específica.' };
}

const rotuloTipoAlvo = computed(() => rotuloPorTipo(form.tipoAlvo));
const precisaEscopoDinamico = computed(() =>
  ['CAMPUS', 'DOCENTE', 'SERVIDOR', 'TURMA', 'CURSO', 'SETOR'].includes(form.tipoAlvo)
);

// Carregamento dinâmico de instâncias por tipo
async function carregarAlvosPorTipo(tipo: string) {
  carregandoAlvos.value = true;
  try {
    catalogoAlvos.value = await listarAlvosApi(tipo);
  } finally {
    carregandoAlvos.value = false;
  }
}

watch(
  () => form.tipoAlvo,
  (novoTipo) => {
    if (novoTipo) carregarAlvosPorTipo(novoTipo);
  }
);

function aoMudarTipoAlvo() {
  form.alvosEspecificosIds = [];
  form.alvoEspecificoId = undefined;

  const regrasAlvo: Record<string, { publicos: any[]; tipo: any; restringir: boolean }> = {
    CAMPUS: { publicos: ['DISCENTES', 'DOCENTES', 'TAES'], tipo: 'TODOS', restringir: true },
    DOCENTE: { publicos: ['DISCENTES'], tipo: 'DISCENTES', restringir: false },
    SERVIDOR: { publicos: ['DISCENTES', 'DOCENTES', 'TAES'], tipo: 'TODOS', restringir: true },
    TURMA: { publicos: ['DISCENTES'], tipo: 'DISCENTES', restringir: false },
    CURSO: { publicos: ['DISCENTES', 'DOCENTES'], tipo: 'TODOS', restringir: true },
    SETOR: { publicos: ['DISCENTES', 'DOCENTES', 'TAES'], tipo: 'TODOS', restringir: true }
  };

  const regra = regrasAlvo[form.tipoAlvo] || { publicos: ['TODOS'], tipo: 'TODOS', restringir: false };
  form.publicosPermitidos = regra.publicos;
  form.tipoPublico = regra.tipo;
  form.restringirVinculoCampus = regra.restringir;
}

// Ações do questionário
async function atualizarListagem() {
  carregandoQuestionarios.value = true;
  usuarioLogado.value = carregarUsuarioAtivo();
  meusQuestionarios.value = await listarInstrumentosApi();
  carregandoQuestionarios.value = false;
}

onMounted(async () => {
  catalogoAlvos.value = await listarAlvosApi();
  tiposPublico.value = await listarTiposPublicoApi();
  await atualizarListagem();
});

function iniciarNovoQuestionario() {
  const u = carregarUsuarioAtivo();
  const papel = u?.papelAtivo || u?.papel || 'CPA';
  const novo: InstrumentoAvaliativo = {
    id: crypto.randomUUID(),
    titulo: '',
    subtitulo: '',
    modulo: papel === 'DOCENTE' ? 'APRENDIZAGEM' : (papel === 'CPA' ? 'SINAES' : 'ADPD'),
    tipoAlvo: papel === 'DOCENTE' ? 'TURMA' : 'INSTITUICAO',
    tipoPublico: 'DISCENTES',
    publicosPermitidos: ['DISCENTES'],
    restringirVinculoCampus: true,
    alvosEspecificosIds: [],
    alvoEspecificoId: undefined,
    criadorId: u?.id || u?.cpf || '00000000000',
    criadorNome: u?.nome || 'Comissão Própria de Avaliação',
    anoCiclo: `${new Date().getFullYear()}/1`,
    periodoLetivo: '',
    dataInicio: `${new Date().toISOString().substring(0, 10)}T08:00`,
    dataTermino: `${new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().substring(0, 10)}T23:59`,
    anonimo: true,
    gruposAutoavaliacaoDiscente: [],
    gruposAvaliacaoDocente: [],
    componentesMatriculados: []
  };

  Object.assign(form, novo);
  modoVisualizacao.value = 'formulario';
}

function editarQuestionario(inst: InstrumentoAvaliativo) {
  Object.assign(form, normalizarInstrumento(inst));
  emit('instrumentoAtualizado', JSON.parse(JSON.stringify(inst)));
  modoVisualizacao.value = 'formulario';
}

async function salvar() {
  const u = carregarUsuarioAtivo();
  if (!form.criadorId) form.criadorId = u?.id || u?.cpf || '00000000000';
  if (!form.criadorNome) form.criadorNome = u?.nome || 'Comissão Própria de Avaliação';

  mensagemErro.value = null;
  const res = await salvarInstrumentoApi(form);
  if (res.sucesso) {
    Object.assign(form, res.instrumento);
    emit('instrumentoAtualizado', JSON.parse(JSON.stringify(res.instrumento)));
    await atualizarListagem();
    mensagemSucesso.value = true;
    setTimeout(() => { mensagemSucesso.value = false; }, 4000);
  } else {
    mensagemErro.value = res.erro || 'Erro ao sincronizar com o servidor PostgreSQL/Liberty.';
  }
}

function restaurarPadrao() {
  if (confirm('Deseja restaurar as configurações e questões do modelo ADPD oficial do IFFar?')) {
    const padrao = resetarInstrumentoParaPadrao();
    Object.assign(form, padrao);
    salvar();
  }
}

function getGruposAtuais(): GrupoQuestoes[] {
  if (!form.gruposAvaliacaoDocente) form.gruposAvaliacaoDocente = [];
  if (form.gruposAvaliacaoDocente.length === 0 && form.gruposAutoavaliacaoDiscente?.length > 0) {
    form.gruposAvaliacaoDocente = [...form.gruposAutoavaliacaoDiscente];
    form.gruposAutoavaliacaoDiscente = [];
  }
  return form.gruposAvaliacaoDocente;
}
</script>

<template>
  <div class="space-y-6">
    <!-- ==================== TELA 1: LISTAGEM DE QUESTIONÁRIOS ==================== -->
    <InstrumentoListaView
      v-if="modoVisualizacao === 'lista'"
      :instrumentos="meusQuestionarios"
      :carregando="carregandoQuestionarios"
      :catalogoAlvos="catalogoAlvos"
      :tiposPublico="tiposPublico"
      @novo="iniciarNovoQuestionario"
      @editar="editarQuestionario"
    />

    <!-- ==================== TELA 2: FORMULÁRIO DE CADASTRO / EDIÇÃO ==================== -->
    <div v-else class="space-y-6">
      <!-- Cabeçalho com ações principais -->
      <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs flex flex-col md:flex-row justify-between md:items-center gap-4">
        <div>
          <div class="flex items-center gap-2">
            <button
              type="button"
              @click="modoVisualizacao = 'lista'"
              class="text-xs font-bold text-emerald-700 hover:underline flex items-center gap-1"
            >
              <span>← Voltar à Lista</span>
            </button>
            <span class="text-slate-300">•</span>
            <span class="text-xs bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded-full uppercase">
              {{ form.id ? 'Edição de Instrumento' : 'Novo Instrumento' }}
            </span>
          </div>
          <h2 class="text-xl font-bold text-slate-900 mt-1">
            {{ form.titulo || 'Cadastro de Questionário' }}
          </h2>
          <p class="text-xs text-slate-500 mt-0.5">
            Vinculado ao usuário criador: <strong>{{ form.criadorNome }}</strong>
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            @click="modoVisualizacao = 'lista'"
            class="border border-slate-200 hover:bg-slate-50 text-slate-600 text-xs font-semibold px-3.5 py-2.5 rounded-xl transition-all"
          >
            Ver Meus Questionários
          </button>
          <button
            type="button"
            @click="restaurarPadrao"
            class="border border-slate-200 hover:bg-slate-50 text-slate-600 text-xs font-semibold px-3.5 py-2.5 rounded-xl transition-all"
          >
            Restaurar Padrão Oficial
          </button>
          <button
            type="button"
            @click="salvar"
            class="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold px-5 py-2.5 rounded-xl shadow-md transition-all flex items-center gap-1.5"
          >
            <span>💾</span>
            <span>Salvar Alterações</span>
          </button>
          <button
            type="button"
            @click="() => { salvar(); emit('testarNoRespondente'); }"
            class="bg-slate-900 hover:bg-black text-white text-xs font-bold px-4 py-2.5 rounded-xl transition-all flex items-center gap-1.5"
          >
            <span>👁️</span>
            <span>Testar no Modo Respondente</span>
          </button>
        </div>
      </div>

      <!-- Alertas de Feedback -->
      <div
        v-if="mensagemSucesso"
        class="p-4 bg-emerald-50 border border-emerald-300 rounded-xl text-xs text-emerald-900 font-semibold flex items-center justify-between shadow-2xs"
      >
        <div class="flex items-center gap-2">
          <span class="text-base">✓</span>
          <span>Instrumento salvo e sincronizado com sucesso!</span>
        </div>
        <button
          type="button"
          @click="emit('testarNoRespondente')"
          class="bg-emerald-700 text-white px-3 py-1 rounded-lg text-[11px] font-bold hover:bg-emerald-800"
        >
          Ir para Tela do Aluno →
        </button>
      </div>

      <div
        v-if="mensagemErro"
        class="p-4 bg-rose-50 border border-rose-300 rounded-xl text-xs text-rose-900 font-semibold flex items-center justify-between shadow-2xs"
      >
        <div class="flex items-center gap-2">
          <span class="text-base text-rose-600 font-bold">⚠️</span>
          <span>Erro ao salvar no servidor: {{ mensagemErro }}</span>
        </div>
        <button type="button" @click="mensagemErro = null" class="text-rose-500 hover:text-rose-700 font-bold px-2 py-1">
          ✕
        </button>
      </div>

      <!-- SEÇÃO 1: Dados Gerais do Instrumento -->
      <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs space-y-5">
        <div class="border-b border-slate-100 pb-3">
          <h3 class="text-sm font-bold text-slate-900 uppercase tracking-wide">
            1. Dados Gerais
          </h3>
          <p class="text-xs text-slate-500 mt-0.5">
            Identificação do instrumento e parametrização do período e público-alvo.
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="space-y-1.5 md:col-span-2">
            <label class="text-xs font-bold text-slate-700">Título do Instrumento:</label>
            <input
              v-model="form.titulo"
              type="text"
              placeholder="Ex: Autoavaliação Institucional 2026/1"
              class="w-full text-xs md:text-sm p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden font-semibold"
            />
          </div>

          <div class="space-y-1.5 md:col-span-2">
            <label class="text-xs font-bold text-slate-700">Subtítulo:</label>
            <input
              v-model="form.subtitulo"
              type="text"
              placeholder="Ex: Avaliação de infraestrutura, gestão e disciplinas"
              class="w-full text-xs md:text-sm p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden text-slate-600"
            />
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-bold text-slate-700">Módulo de Aplicação:</label>
            <select
              v-model="form.modulo"
              class="w-full text-xs p-3 border border-slate-200 rounded-xl bg-slate-50/50 font-medium"
            >
              <option value="ADPD">Módulo 1: Avaliação Docente pelo Discente (ADPD)</option>
              <option value="SINAES">Módulo 2: Autoavaliação Institucional (SINAES / CPA)</option>
              <option value="APRENDIZAGEM">Módulo 3: Avaliação de Aprendizagem (Provas / Enquetes)</option>
            </select>
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-bold text-slate-700">Ano / Ciclo Avaliativo:</label>
            <input
              v-model="form.anoCiclo"
              type="text"
              placeholder="Ex: 2026/1"
              class="w-full text-xs p-3 border border-slate-200 rounded-xl"
            />
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-bold text-slate-700">Início:</label>
            <input
              v-model="form.dataInicio"
              type="datetime-local"
              class="w-full text-xs p-3 border border-slate-200 rounded-xl bg-white font-medium"
            />
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-bold text-slate-700">Término:</label>
            <input
              v-model="form.dataTermino"
              type="datetime-local"
              class="w-full text-xs p-3 border border-slate-200 rounded-xl bg-white font-medium"
            />
          </div>

          <!-- Alvo Avaliado (Dados vindos da tabela tipo_publico) -->
          <div class="space-y-1.5 md:col-span-2">
            <label class="text-xs font-bold text-slate-700">Foco / Alvo Avaliado (O que é Avaliado):</label>
            <select
              v-model="form.tipoAlvo"
              @change="aoMudarTipoAlvo"
              class="w-full text-xs p-3 border border-slate-200 rounded-xl bg-slate-50 font-semibold text-slate-800"
            >
              <template v-if="tiposPublico.length > 0">
                <option v-for="tp in tiposPublico" :key="tp.codigo" :value="tp.codigo">
                  {{ tp.icone }} {{ tp.singular }} {{ tp.descricao ? `— ${tp.descricao}` : '' }}
                </option>
              </template>
              <template v-else>
                <option value="CAMPUS">📍 Campus</option>
                <option value="DOCENTE">👨‍🏫 Docente</option>
                <option value="SERVIDOR">💼 Servidor / TAE</option>
                <option value="INSTITUICAO">🏛️ Instituição Geral</option>
                <option value="SETOR">🛠️ Setor</option>
                <option value="CURSO">📚 Curso</option>
                <option value="TURMA">👥 Turma</option>
                <option value="AUTOAVALIACAO">👤 Próprio Estudante</option>
              </template>
            </select>
            <p class="text-[11px] text-slate-400">Define o objeto de julgamento e a agregação dos relatórios.</p>
          </div>

          <!-- Componente Modular: Painel de Escopo e Alvos Específicos -->
          <InstrumentoEscopoAlvoEditor
            v-if="precisaEscopoDinamico"
            :form="form"
            :catalogoAlvos="catalogoAlvos"
            :tiposPublico="tiposPublico"
            :rotuloTipoAlvo="rotuloTipoAlvo"
            :carregandoAlvos="carregandoAlvos"
          />

          <!-- Público Respondente Padrão (quando não requer escopo dinâmico) -->
          <div v-else class="space-y-1.5 md:col-span-2">
            <label class="text-xs font-bold text-slate-700">Público Respondente (Quem Avalia):</label>
            <select
              v-model="form.tipoPublico"
              class="w-full text-xs p-3 border border-slate-200 rounded-xl bg-slate-50 font-semibold text-slate-800"
            >
              <option value="DISCENTES">🎓 Discentes Matriculados (Alunos)</option>
              <option value="DOCENTES">👨‍🏫 Corpo Docente (Professores)</option>
              <option value="TAES">🏢 Técnicos-Administrativos (TAEs)</option>
              <option value="EGRESSOS">📜 Egressos e Ex-alunos</option>
              <option value="SOCIEDADE_CIVIL">🤝 Sociedade Civil / Comunidade Regional</option>
              <option value="TODOS">🌐 Toda a Comunidade Acadêmica Integrada</option>
            </select>
            <p class="text-[11px] text-slate-400">Define permissão e credencial necessária para acessar.</p>
          </div>

          <div class="space-y-1.5 flex flex-col justify-center md:col-span-2">
            <label class="text-xs font-bold text-slate-700 mb-1">Respostas Anônimas:</label>
            <label class="flex items-center gap-2.5 cursor-pointer text-xs font-medium text-slate-800">
              <input
                v-model="form.anonimo"
                type="checkbox"
                class="w-4 h-4 text-emerald-600 rounded-sm border-slate-300 focus:ring-emerald-500"
              />
              <span class="text-xs text-slate-600">Não registrar a identificação nominal do respondente no instrumento</span>
            </label>
          </div>
        </div>
      </div>

      <!-- SEÇÃO 2: Estrutura de Dimensões e Grupos de Questões (Componente Modular) -->
      <InstrumentoGruposEditor :grupos="getGruposAtuais()" />
    </div>
  </div>
</template>
