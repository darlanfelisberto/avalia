<script setup lang="ts">
import { ref, reactive, watch, onMounted, computed } from 'vue';
import type {
  InstrumentoAvaliativo,
  GrupoQuestoes,
  AlvoAvaliado,
  TipoPublicoAvaliador
} from '../types/avaliacao';
import { salvarInstrumentoApi, listarAlvosApi, listarInstrumentosApi } from '../services/api';
import { resetarInstrumentoParaPadrao, carregarUsuarioAtivo } from '../services/storage';
import InstrumentoGruposEditor from './InstrumentoGruposEditor.vue';
import InstrumentoListaView from './InstrumentoListaView.vue';

const props = defineProps<{
  instrumento: InstrumentoAvaliativo;
}>();

const emit = defineEmits<{
  (e: 'instrumentoAtualizado', instrumento: InstrumentoAvaliativo): void;
  (e: 'testarNoRespondente'): void;
}>();

// Modo da tela: 'lista' (ver questionários do usuário) ou 'formulario' (cadastrar/editar)
const modoVisualizacao = ref<'lista' | 'formulario'>('lista');

const catalogoAlvos = ref<AlvoAvaliado[]>([]);
const meusQuestionarios = ref<InstrumentoAvaliativo[]>([]);
const carregandoQuestionarios = ref(false);
const usuarioLogado = ref(carregarUsuarioAtivo());

async function atualizarListagem() {
  carregandoQuestionarios.value = true;
  usuarioLogado.value = carregarUsuarioAtivo();
  const todos = await listarInstrumentosApi();
  meusQuestionarios.value = todos;
  carregandoQuestionarios.value = false;
}

onMounted(async () => {
  catalogoAlvos.value = await listarAlvosApi();
  await atualizarListagem();
});

function paraDatetimeLocal(dataStr?: string, horaPadrao: string = '08:00'): string {
  if (!dataStr) return '';
  if (dataStr.includes('T')) {
    const partes = dataStr.split('T');
    const dataParte = partes[0];
    const horaParte = (partes[1] || '').substring(0, 5);
    return `${dataParte}T${horaParte || horaPadrao}`;
  }
  return dataStr;
}

function normalizarInstrumento(inst: InstrumentoAvaliativo): InstrumentoAvaliativo {
  const copia = JSON.parse(JSON.stringify(inst || {}));
  // if (!copia.tipoAlvo) copia.tipoAlvo = 'DOCENTE';
  // if (!copia.tipoPublico) copia.tipoPublico = 'DISCENTES';
  // if (!copia.publicosPermitidos || copia.publicosPermitidos.length === 0) {
  //   copia.publicosPermitidos = [copia.tipoPublico || 'DISCENTES'];
  // }
  // if (copia.restringirVinculoCampus === undefined) {
  //   copia.restringirVinculoCampus = true;
  // }
  // if (!copia.alvosEspecificosIds) {
  //   copia.alvosEspecificosIds = copia.alvoEspecificoId ? [copia.alvoEspecificoId] : [];
  // }
  // if (!copia.criadorId) copia.criadorId = usuarioLogado.value?.id || 'usr-cpa-cain';
  // if (!copia.criadorNome) copia.criadorNome = usuarioLogado.value?.nome || 'Comissão Própria de Avaliação (CPA)';
  // if (!copia.gruposAutoavaliacaoDiscente) copia.gruposAutoavaliacaoDiscente = [];
  // if (!copia.gruposAvaliacaoDocente) copia.gruposAvaliacaoDocente = [];
  // if (!copia.componentesMatriculados) copia.componentesMatriculados = [];

  copia.dataInicio = paraDatetimeLocal(copia.dataInicio,'00:00');
  

  // if (copia.dataTermino) {
    copia.dataTermino = paraDatetimeLocal(copia.dataTermino, '23:59');
  // } else {
  //   const daqui30Dias = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().substring(0, 10);
  //   copia.dataTermino = `${daqui30Dias}T23:59`;
  // }

  return copia;
}

// Cópia reativa para edição do instrumento
const form = reactive<InstrumentoAvaliativo>(normalizarInstrumento(props.instrumento));

watch(
  () => props.instrumento,
  (novo) => {
    if (novo) {
      Object.assign(form, normalizarInstrumento(novo));
    }
  },
  { deep: true }
);
const termoBuscaAlvo = ref('');

const rotuloTipoAlvo = computed(() => {
  return rotuloPorTipo(form.tipoAlvo);
});

function rotuloPorTipo(tipo?: string) {
  switch (tipo) {
    case 'CAMPUS': return { singular: 'Campus', plural: 'Campi', icone: '📍', descricao: 'Avaliação da infraestrutura, gestão e serviços da unidade.' };
    case 'DOCENTE': return { singular: 'Docente', plural: 'Docentes', icone: '👨‍🏫', descricao: 'Avaliação da prática didático-pedagógica e relação dialógica.' };
    case 'SERVIDOR': return { singular: 'Servidor / TAE', plural: 'Servidores / TAEs', icone: '💼', descricao: 'Avaliação de desempenho ou atuação de servidor técnico-administrativo.' };
    case 'TURMA': return { singular: 'Turma', plural: 'Turmas', icone: '👥', descricao: 'Avaliação de turma e componente curricular específico.' };
    case 'CURSO': return { singular: 'Curso', plural: 'Cursos', icone: '📚', descricao: 'Avaliação do projeto pedagógico e infraestrutura do curso.' };
    case 'SETOR': return { singular: 'Setor', plural: 'Setores', icone: '🛠️', descricao: 'Avaliação de atendimento e eficiência de setor administrativo.' };
    case 'INSTITUICAO': return { singular: 'Instituição', plural: 'Instituições', icone: '🏛️', descricao: 'Avaliação global institucional e PDI multicampi.' };
    case 'AUTOAVALIACAO': return { singular: 'Estudante', plural: 'Estudantes', icone: '👤', descricao: 'Autoavaliação discente individual.' };
    default: return { singular: 'Alvo', plural: 'Alvos', icone: '🎯', descricao: 'Avaliação específica.' };
  }
}

const carregandoAlvos = ref(false);

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
    if (novoTipo) {
      carregarAlvosPorTipo(novoTipo);
    }
  }
);

const instanciasAlvoDisponiveis = computed(() => {
  return catalogoAlvos.value.filter(a => a.tipo === form.tipoAlvo);
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



function ehAlvoSelecionado(id: string): boolean {
  if (!form.alvosEspecificosIds) return false;
  return form.alvosEspecificosIds.includes(id);
}

function alternarAlvoEspecifico(id: string) {
  if (!form.alvosEspecificosIds) {
    form.alvosEspecificosIds = [];
  }
  const idx = form.alvosEspecificosIds.indexOf(id);
  if (idx >= 0) {
    form.alvosEspecificosIds.splice(idx, 1);
  } else {
    form.alvosEspecificosIds.push(id);
  }
  form.alvoEspecificoId = form.alvosEspecificosIds.length > 0 ? form.alvosEspecificosIds[0] : undefined;
}

function selecionarTodosAlvosEspecificos() {
  form.alvosEspecificosIds = instanciasAlvoDisponiveis.value.map(a => a.id);
  form.alvoEspecificoId = form.alvosEspecificosIds.length > 0 ? form.alvosEspecificosIds[0] : undefined;
}

function limparAlvosEspecificos() {
  form.alvosEspecificosIds = [];
  form.alvoEspecificoId = undefined;
}

const temAlvosEspecificosSelecionados = computed(() => {
  return form.alvosEspecificosIds && form.alvosEspecificosIds.length > 0;
});

function alternarPublicoPermitido(publico: TipoPublicoAvaliador) {
  if (!form.publicosPermitidos) {
    form.publicosPermitidos = [];
  }
  const idx = form.publicosPermitidos.indexOf(publico);
  if (idx >= 0) {
    if (form.publicosPermitidos.length > 1) {
      form.publicosPermitidos.splice(idx, 1);
    }
  } else {
    form.publicosPermitidos.push(publico);
  }
  if (form.publicosPermitidos.length > 1) {
    form.tipoPublico = 'TODOS';
  } else if (form.publicosPermitidos.length === 1) {
    form.tipoPublico = form.publicosPermitidos[0];
  }
}

function temPublicoPermitido(publico: TipoPublicoAvaliador): boolean {
  return !!form.publicosPermitidos && form.publicosPermitidos.includes(publico);
}

function aoMudarTipoAlvo() {
  form.alvosEspecificosIds = [];
  form.alvoEspecificoId = undefined;
  termoBuscaAlvo.value = '';

  if (form.tipoAlvo === 'CAMPUS') {
    form.publicosPermitidos = ['DISCENTES', 'DOCENTES', 'TAES'];
    form.tipoPublico = 'TODOS';
    form.restringirVinculoCampus = true;
  } else if (form.tipoAlvo === 'DOCENTE') {
    form.publicosPermitidos = ['DISCENTES'];
    form.tipoPublico = 'DISCENTES';
    form.restringirVinculoCampus = false;
  } else if (form.tipoAlvo === 'SERVIDOR') {
    form.publicosPermitidos = ['DISCENTES', 'DOCENTES', 'TAES'];
    form.tipoPublico = 'TODOS';
    form.restringirVinculoCampus = true;
  } else if (form.tipoAlvo === 'TURMA') {
    form.publicosPermitidos = ['DISCENTES'];
    form.tipoPublico = 'DISCENTES';
    form.restringirVinculoCampus = false;
  } else if (form.tipoAlvo === 'CURSO') {
    form.publicosPermitidos = ['DISCENTES', 'DOCENTES'];
    form.tipoPublico = 'TODOS';
    form.restringirVinculoCampus = true;
  } else if (form.tipoAlvo === 'SETOR') {
    form.publicosPermitidos = ['DISCENTES', 'DOCENTES', 'TAES'];
    form.tipoPublico = 'TODOS';
    form.restringirVinculoCampus = true;
  } else {
    form.publicosPermitidos = ['TODOS'];
    form.tipoPublico = 'TODOS';
    form.restringirVinculoCampus = false;
  }
}

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

const mensagemSucesso = ref(false);
const mensagemErro = ref<string | null>(null);

// Salvar instrumento no Open Liberty Backend e localmente
async function salvar() {
  // Garante que o criador ativo fica registrado se for criação
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
    setTimeout(() => {
      mensagemSucesso.value = false;
    }, 4000);
  } else {
    mensagemErro.value = res.erro || 'Erro ao sincronizar com o servidor PostgreSQL/Liberty.';
  }
}

// Restaurar padrão
function restaurarPadrao() {
  if (confirm('Deseja restaurar as configurações e questões do modelo ADPD oficial do IFFar?')) {
    const padrao = resetarInstrumentoParaPadrao();
    Object.assign(form, padrao);
    salvar();
  }
}

// Grupos de Questões do instrumento
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
      @novo="iniciarNovoQuestionario"
      @editar="editarQuestionario"
    />

    <!-- ==================== TELA 2: FORMULÁRIO DE CADASTRO / EDIÇÃO ==================== -->
    <div v-else class="space-y-6">
      <!-- Cabeçalho com ações principais do formulário -->
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

    <!-- Alerta de Sucesso -->
    <div
      v-if="mensagemSucesso"
      class="p-4 bg-emerald-50 border border-emerald-300 rounded-xl text-xs text-emerald-900 font-semibold flex items-center justify-between shadow-2xs"
    >
      <div class="flex items-center gap-2">
        <span class="text-base">✓</span>
        <span>Instrumento avaliativo e questões atualizados com sucesso no banco de dados local!</span>
      </div>
      <button
        type="button"
        @click="emit('testarNoRespondente')"
        class="bg-emerald-700 text-white px-3 py-1 rounded-lg text-[11px] font-bold hover:bg-emerald-800"
      >
        Ir para Tela do Aluno →
      </button>
    </div>

    <!-- Alerta de Erro -->
    <div
      v-if="mensagemErro"
      class="p-4 bg-rose-50 border border-rose-300 rounded-xl text-xs text-rose-900 font-semibold flex items-center justify-between shadow-2xs"
    >
      <div class="flex items-center gap-2">
        <span class="text-base text-rose-600 font-bold">⚠️</span>
        <span>Erro ao salvar no servidor: {{ mensagemErro }}</span>
      </div>
      <button
        type="button"
        @click="mensagemErro = null"
        class="text-rose-500 hover:text-rose-700 font-bold px-2 py-1"
      >
        ✕
      </button>
    </div>

    <!-- SEÇÃO 1: Dados Gerais do Instrumento (RF01, RF04, RF05, RF08) -->
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
            placeholder=""
            class="w-full text-xs md:text-sm p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden font-semibold"
          />
        </div>

        <div class="space-y-1.5 md:col-span-2">
          <label class="text-xs font-bold text-slate-700">Subtítulo:</label>
          <input
            v-model="form.subtitulo"
            type="text"
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

        <!-- Alvo Avaliado -->
        <div class="space-y-1.5 md:col-span-2">
          <label class="text-xs font-bold text-slate-700">Foco / Alvo Avaliado (O que é Avaliado):</label>
          <select
            v-model="form.tipoAlvo"
            @change="aoMudarTipoAlvo"
            class="w-full text-xs p-3 border border-slate-200 rounded-xl bg-slate-50 font-semibold text-slate-800"
          >
            <option value="CAMPUS">📍 Campus / Unidade de Ensino Específica (Avaliação do Campus)</option>
            <option value="DOCENTE">👨‍🏫 Docente (por Componente Curricular / Turma)</option>
            <option value="SERVIDOR">💼 Servidor Técnico-Administrativo (TAE / Atendimento)</option>
            <option value="INSTITUICAO">🏛️ Instituição Geral (PDI, Infraestrutura Global)</option>
            <option value="SETOR">🛠️ Setor Administrativo / Atendimento (TI, Biblioteca, etc.)</option>
            <option value="CURSO">📚 Curso de Graduação / Técnico</option>
            <option value="TURMA">👥 Turma / Disciplina Específica</option>
            <option value="AUTOAVALIACAO">👤 Próprio Estudante (Autoavaliação Discente)</option>
          </select>
          <p class="text-[11px] text-slate-400">Define o objeto de julgamento e a agregação dos relatórios.</p>
        </div>

        <!-- PAINEL DINÂMICO GENÉRICO DE CONFIGURAÇÃO DE ALVO E ESCOPO -->
        <div
          v-if="['CAMPUS', 'DOCENTE', 'SERVIDOR', 'TURMA', 'CURSO', 'SETOR'].includes(form.tipoAlvo)"
          class="space-y-4 md:col-span-2 p-4 md:p-5 bg-purple-50/60 border border-purple-200 rounded-2xl"
        >
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

          <!-- LISTA / COMBO DE SELEÇÃO ESPECÍFICA (quando tem seleção específica ativa) -->
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

            <!-- Campo de Busca rápida de instâncias -->
            <div class="relative">
              <input
                v-model="termoBuscaAlvo"
                type="text"
                :placeholder="`Buscar ${rotuloTipoAlvo.singular.toLowerCase()} por nome ou código...`"
                class="w-full text-xs p-2.5 pl-8 border border-purple-200 rounded-xl bg-white outline-hidden focus:ring-2 focus:ring-purple-400"
              />
              <span class="absolute left-2.5 top-2.5 text-xs text-purple-400">🔍</span>
            </div>

            <!-- Grid de cartões de seleção rápida -->
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

          <!-- Filtro de Restrição por Vínculo Institucional -->
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

        <!-- Público Respondente Padrão (Quando o alvo NÃO for Campus) -->
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

    <!-- SEÇÃO 2: Estrutura de Dimensões e Grupos de Questões do Instrumento (Componente Modular) -->
    <InstrumentoGruposEditor :grupos="getGruposAtuais()" />
    </div>
  </div>
</template>
