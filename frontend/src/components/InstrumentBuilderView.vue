<script setup lang="ts">
import { ref, reactive, watch, onMounted, computed } from 'vue';
import type {
  InstrumentoAvaliativo,
  GrupoQuestoes,
  Questao,
  TipoQuestao,
  AlvoAvaliado,
  PublicoAvaliador
} from '../types/avaliacao';
import { salvarInstrumentoApi, listarAlvosApi, listarPublicosApi, listarInstrumentosApi } from '../services/api';
import { resetarInstrumentoParaPadrao, carregarUsuarioAtivo } from '../services/storage';

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
const catalogoPublicos = ref<PublicoAvaliador[]>([]);
const meusQuestionarios = ref<InstrumentoAvaliativo[]>([]);
const carregandoQuestionarios = ref(false);
const usuarioLogado = ref(carregarUsuarioAtivo());

// Filtros e Paginação da Tabela
const termoBusca = ref('');
const filtroModulo = ref('TODOS');
const paginaAtual = ref(1);
const itensPorPagina = ref(6);

const questionariosFiltrados = computed(() => {
  let list = meusQuestionarios.value;

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

async function atualizarListagem() {
  carregandoQuestionarios.value = true;
  usuarioLogado.value = carregarUsuarioAtivo();
  const todos = await listarInstrumentosApi();
  meusQuestionarios.value = todos;
  carregandoQuestionarios.value = false;
}

onMounted(async () => {
  catalogoAlvos.value = await listarAlvosApi();
  catalogoPublicos.value = await listarPublicosApi();
  await atualizarListagem();
});

function normalizarInstrumento(inst: InstrumentoAvaliativo): InstrumentoAvaliativo {
  const copia = JSON.parse(JSON.stringify(inst || {}));
  if (!copia.tipoAlvo) copia.tipoAlvo = 'DOCENTE';
  if (!copia.tipoPublico) copia.tipoPublico = 'DISCENTES';
  if (!copia.criadorId) copia.criadorId = usuarioLogado.value?.id || 'usr-cpa-cain';
  if (!copia.criadorNome) copia.criadorNome = usuarioLogado.value?.nome || 'Comissão Própria de Avaliação (CPA)';
  if (!copia.gruposAutoavaliacaoDiscente) copia.gruposAutoavaliacaoDiscente = [];
  if (!copia.gruposAvaliacaoDocente) copia.gruposAvaliacaoDocente = [];
  if (!copia.componentesMatriculados) copia.componentesMatriculados = [];
  return copia;
}

// Cópia reativa para edição do instrumento
const form = reactive<InstrumentoAvaliativo>(normalizarInstrumento(props.instrumento));

// Sincronizar form sempre que o instrumento recebido nas props mudar
watch(
  () => props.instrumento,
  (novo) => {
    if (novo) {
      Object.assign(form, normalizarInstrumento(novo));
    }
  },
  { deep: true }
);

function iniciarNovoQuestionario() {
  const u = carregarUsuarioAtivo();
  const novo: InstrumentoAvaliativo = {
    id: `inst-${Date.now().toString(16)}`,
    titulo: 'Novo Instrumento de Avaliação',
    subtitulo: `${u.campus} - Ano ${new Date().getFullYear()}`,
    modulo: u.papel === 'DOCENTE' ? 'APRENDIZAGEM' : (u.papel === 'CPA' ? 'SINAES' : 'ADPD'),
    tipoAlvo: u.papel === 'DOCENTE' ? 'TURMA' : 'INSTITUICAO',
    tipoPublico: 'DISCENTES',
    criadorId: u.id,
    criadorNome: u.nome,
    anoCiclo: `${new Date().getFullYear()}/1`,
    periodoLetivo: 'Semestre Vigente',
    dataInicio: new Date().toISOString().substring(0, 10),
    dataTermino: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().substring(0, 10),
    publicoAlvo: 'Comunidade Acadêmica / Estudantes',
    anonimo: true,
    gruposAutoavaliacaoDiscente: [],
    gruposAvaliacaoDocente: [
      {
        id: `grp-${Date.now()}`,
        titulo: 'Dimensão 1: Prática Pedagógica e Conteúdos',
        descricao: 'Avaliação dos itens de aprendizagem e métodos didáticos',
        questoes: [
          {
            id: `q-${Date.now()}`,
            enunciado: 'O planejamento e cronograma foram cumpridos com clareza.',
            tipo: 'ESCALA',
            obrigatoria: true,
            escalaMin: 1,
            escalaMax: 5,
            rotuloMin: '1 - Discordo',
            rotuloMax: '5 - Concordo'
          }
        ]
      }
    ],
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
  if (!form.criadorId) form.criadorId = u.id;
  if (!form.criadorNome) form.criadorNome = u.nome;

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

// Gerenciamento de Grupos (Estrutura única do questionário)
function getGruposAtuais(): GrupoQuestoes[] {
  // Se for autoavaliação pura e não tiver grupos na lista principal, migra ou unifica
  if (!form.gruposAvaliacaoDocente) form.gruposAvaliacaoDocente = [];
  if (form.gruposAvaliacaoDocente.length === 0 && form.gruposAutoavaliacaoDiscente?.length > 0) {
    form.gruposAvaliacaoDocente = [...form.gruposAutoavaliacaoDiscente];
    form.gruposAutoavaliacaoDiscente = [];
  }
  return form.gruposAvaliacaoDocente;
}

function adicionarGrupo() {
  const novoGrupo: GrupoQuestoes = {
    id: `grp-${Date.now()}`,
    titulo: 'Novo Grupo de Questões / Dimensão',
    descricao: 'Descreva os objetivos desta dimensão avaliativa...',
    questoes: []
  };
  getGruposAtuais().push(novoGrupo);
}

function removerGrupo(grupoIndex: number) {
  if (confirm('Tem certeza que deseja remover este grupo e todas as suas questões?')) {
    getGruposAtuais().splice(grupoIndex, 1);
  }
}

// Gerenciamento de Questões
function adicionarQuestao(grupo: GrupoQuestoes, tipo: TipoQuestao = 'ESCALA') {
  const novaQuestao: Questao = {
    id: `q-${Date.now()}`,
    enunciado: 'Novo enunciado de questão...',
    tipo,
    obrigatoria: true,
    escalaMin: 1,
    escalaMax: 5,
    rotuloMin: '1 - Não atende / Insatisfatório',
    rotuloMax: '5 - Atende plenamente / Excelente',
    opcoes: tipo === 'ESCOLHA_UNICA' || tipo === 'MULTIPLA_ESCOLHA' ? [
      { id: `opt-1-${Date.now()}`, texto: 'Opção 1' },
      { id: `opt-2-${Date.now()}`, texto: 'Opção 2' }
    ] : undefined,
    ajuda: ''
  };
  grupo.questoes.push(novaQuestao);
}

function duplicarQuestao(grupo: GrupoQuestoes, questaoIndex: number) {
  const original = grupo.questoes[questaoIndex];
  const copia: Questao = JSON.parse(JSON.stringify(original));
  copia.id = `q-${Date.now()}`;
  copia.enunciado = `${copia.enunciado} (Cópia)`;
  grupo.questoes.splice(questaoIndex + 1, 0, copia);
}

function removerQuestao(grupo: GrupoQuestoes, questaoIndex: number) {
  grupo.questoes.splice(questaoIndex, 1);
}

function moverQuestao(grupo: GrupoQuestoes, questaoIndex: number, direcao: 'cima' | 'baixo') {
  if (direcao === 'cima' && questaoIndex > 0) {
    const item = grupo.questoes.splice(questaoIndex, 1)[0];
    grupo.questoes.splice(questaoIndex - 1, 0, item);
  } else if (direcao === 'baixo' && questaoIndex < grupo.questoes.length - 1) {
    const item = grupo.questoes.splice(questaoIndex, 1)[0];
    grupo.questoes.splice(questaoIndex + 1, 0, item);
  }
}

// Gerenciamento de Opções em Questões de Escolha
function adicionarOpcao(questao: Questao) {
  if (!questao.opcoes) questao.opcoes = [];
  questao.opcoes.push({
    id: `opt-${Date.now()}`,
    texto: `Nova Opção ${questao.opcoes.length + 1}`
  });
}

function removerOpcao(questao: Questao, optIndex: number) {
  if (questao.opcoes && questao.opcoes.length > 1) {
    questao.opcoes.splice(optIndex, 1);
  }
}

// Ao mudar o tipo de questão
function mudarTipoQuestao(questao: Questao, novoTipo: TipoQuestao) {
  questao.tipo = novoTipo;
  if ((novoTipo === 'ESCOLHA_UNICA' || novoTipo === 'MULTIPLA_ESCOLHA') && (!questao.opcoes || questao.opcoes.length === 0)) {
    questao.opcoes = [
      { id: `opt-1-${Date.now()}`, texto: 'Opção 1' },
      { id: `opt-2-${Date.now()}`, texto: 'Opção 2' }
    ];
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- ==================== TELA 1: LISTAGEM DE QUESTIONÁRIOS ==================== -->
    <div v-if="modoVisualizacao === 'lista'" class="space-y-6">
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
          @click="iniciarNovoQuestionario"
          class="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2 self-start md:self-center"
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
        <div v-if="carregandoQuestionarios" class="p-12 text-center text-xs text-slate-400">
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
            @click="iniciarNovoQuestionario"
            class="px-4 py-2 bg-emerald-600 text-white text-xs font-bold rounded-xl shadow-xs"
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
                  <td class="py-3.5 px-4 whitespace-nowrap">
                    <span class="px-2 py-0.5 rounded-md text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200">
                      {{ item.tipoAlvo }}
                    </span>
                  </td>
                  <td class="py-3.5 px-4 whitespace-nowrap">
                    <span class="px-2 py-0.5 rounded-md text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                      {{ item.tipoPublico }}
                    </span>
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
                      @click="editarQuestionario(item)"
                      class="px-3 py-1.5 bg-slate-100 hover:bg-emerald-600 hover:text-white text-slate-700 font-bold rounded-lg transition-colors text-xs inline-flex items-center gap-1.5 shadow-2xs"
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
                class="px-2.5 py-1.5 border rounded-lg bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed font-bold"
                title="Primeira página"
              >
                «
              </button>
              <button
                type="button"
                @click="mudarPagina(paginaAtual - 1)"
                :disabled="paginaAtual === 1"
                class="px-2.5 py-1.5 border rounded-lg bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed font-medium"
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
                class="px-2.5 py-1.5 border rounded-lg bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed font-medium"
              >
                Próxima
              </button>
              <button
                type="button"
                @click="mudarPagina(totalPaginas)"
                :disabled="paginaAtual === totalPaginas"
                class="px-2.5 py-1.5 border rounded-lg bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed font-bold"
                title="Última página"
              >
                »
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

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
          1. Dados Gerais e Parâmetros Institucionais
        </h3>
        <p class="text-xs text-slate-500 mt-0.5">
          Identificação do instrumento e parametrização do período e público-alvo (RF04 e RF05).
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="space-y-1.5 md:col-span-2">
          <label class="text-xs font-bold text-slate-700">Título do Instrumento Avaliativo (RF01):</label>
          <input
            v-model="form.titulo"
            type="text"
            placeholder="Ex: Avaliação Docente pelo Discente (ADPD)"
            class="w-full text-xs md:text-sm p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden font-semibold"
          />
        </div>

        <div class="space-y-1.5 md:col-span-2">
          <label class="text-xs font-bold text-slate-700">Subtítulo / Fundamentação Normativa:</label>
          <input
            v-model="form.subtitulo"
            type="text"
            placeholder="Ex: Instituto Federal Farroupilha - Resolução CONSUP nº 104/2013"
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
          <label class="text-xs font-bold text-slate-700">Período de Aplicação - Início (RF04):</label>
          <input
            v-model="form.dataInicio"
            type="date"
            class="w-full text-xs p-3 border border-slate-200 rounded-xl"
          />
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-bold text-slate-700">Período de Aplicação - Término (RF04):</label>
          <input
            v-model="form.dataTermino"
            type="date"
            class="w-full text-xs p-3 border border-slate-200 rounded-xl"
          />
        </div>

        <div class="space-y-1.5">
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

        <div class="space-y-1.5">
          <label class="text-xs font-bold text-slate-700">Foco / Alvo Avaliado (O que é Avaliado):</label>
          <select
            v-model="form.tipoAlvo"
            class="w-full text-xs p-3 border border-slate-200 rounded-xl bg-slate-50 font-semibold text-slate-800"
          >
            <option value="DOCENTE">👨‍🏫 Docente (por Componente Curricular / Turma)</option>
            <option value="INSTITUICAO">🏛️ Instituição Geral (PDI, Infraestrutura Global)</option>
            <option value="CAMPUS">📍 Campus / Unidade de Ensino Específica</option>
            <option value="SETOR">🛠️ Setor Administrativo / Atendimento (TI, Biblioteca, etc.)</option>
            <option value="CURSO">📚 Curso de Graduação / Técnico</option>
            <option value="TURMA">👥 Turma / Disciplina Específica</option>
            <option value="AUTOAVALIACAO">👤 Próprio Estudante (Autoavaliação Discente)</option>
          </select>
          <p class="text-[11px] text-slate-400">Define o objeto de julgamento e agregação de relatórios.</p>
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-bold text-slate-700">Texto Informativo de Público-Alvo:</label>
          <input
            v-model="form.publicoAlvo"
            type="text"
            placeholder="Ex: Discentes com matrícula ativa nos Cursos Superiores do IFFar"
            class="w-full text-xs p-3 border border-slate-200 rounded-xl"
          />
        </div>

        <div class="space-y-1.5 flex flex-col justify-center">
          <label class="text-xs font-bold text-slate-700 mb-1">Garantia de Anonimato (RF08 / RNF03):</label>
          <label class="flex items-center gap-2 cursor-pointer text-xs font-medium text-slate-800">
            <input
              v-model="form.anonimo"
              type="checkbox"
              class="w-4 h-4 text-emerald-600 rounded-sm border-slate-300 focus:ring-emerald-500"
            />
            <span>Ativar armazenamento anônimo (Respostas desvinculadas de matrícula/CPF)</span>
          </label>
        </div>
      </div>
    </div>

    <!-- SEÇÃO 2: Estrutura de Dimensões e Grupos de Questões do Instrumento -->
    <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs space-y-6">
      <div class="flex flex-col sm:flex-row justify-between sm:items-center gap-3 border-b border-slate-100 pb-4">
        <div>
          <h3 class="text-sm font-bold text-slate-900 uppercase tracking-wide">
            2. Dimensões e Questões do Instrumento
          </h3>
          <p class="text-xs text-slate-500 mt-0.5">
            Cadastre as dimensões avaliativas e as questões direcionadas ao público selecionado ({{ form.tipoPublico }}).
          </p>
        </div>

        <button
          type="button"
          @click="adicionarGrupo"
          class="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold px-4 py-2 rounded-xl transition-all flex items-center gap-1.5 self-start sm:self-center shadow-xs"
        >
          <span>＋</span>
          <span>Nova Dimensão / Grupo de Questões</span>
        </button>
      </div>

      <!-- VISÃO: CONSTRUTOR DE GRUPOS E QUESTÕES DIRETAS -->
      <div class="space-y-6">
        <div v-if="getGruposAtuais().length === 0" class="p-8 text-center border-2 border-dashed border-slate-200 rounded-2xl space-y-3">
          <p class="text-xs text-slate-500">Este questionário ainda não possui dimensões ou questões cadastradas.</p>
          <button
            type="button"
            @click="adicionarGrupo"
            class="px-3 py-1.5 bg-emerald-600 text-white text-xs font-bold rounded-lg"
          >
            Adicionar Primeira Dimensão
          </button>
        </div>

        <!-- Lista de Grupos -->
        <div
          v-for="(grupo, gIdx) in getGruposAtuais()"
          :key="grupo.id"
          class="border-2 border-slate-200 rounded-2xl p-5 md:p-6 bg-white space-y-5 shadow-xs"
        >
          <!-- Cabeçalho do Grupo -->
          <div class="flex flex-col sm:flex-row justify-between sm:items-center gap-3 border-b border-slate-100 pb-3">
            <div class="flex-1 space-y-1">
              <input
                v-model="grupo.titulo"
                type="text"
                placeholder="Título do Grupo / Dimensão (ex: Prática Pedagógica)"
                class="w-full text-sm font-bold text-slate-900 p-1.5 border-b border-dashed border-slate-300 focus:border-emerald-600 outline-hidden"
              />
              <input
                v-model="grupo.descricao"
                type="text"
                placeholder="Descrição ou orientações gerais para este grupo de questões..."
                class="w-full text-xs text-slate-500 p-1 border-b border-transparent focus:border-slate-300 outline-hidden"
              />
            </div>

            <div class="flex items-center gap-2">
              <button
                type="button"
                @click="adicionarQuestao(grupo, 'ESCALA')"
                class="bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 text-xs font-semibold px-3 py-1.5 rounded-lg transition-all flex items-center gap-1"
              >
                <span>+</span>
                <span>Nova Questão</span>
              </button>

              <button
                type="button"
                @click="removerGrupo(gIdx)"
                title="Excluir Grupo"
                class="text-rose-500 hover:text-rose-700 text-xs px-2 py-1 font-bold"
              >
                Excluir Grupo
              </button>
            </div>
          </div>

          <!-- Lista de Questões dentro do Grupo -->
          <div class="space-y-4">
            <div
              v-if="grupo.questoes.length === 0"
              class="p-6 text-center text-xs text-slate-400 bg-slate-50 rounded-xl border border-dashed border-slate-200"
            >
              Nenhuma questão neste grupo. Clique em "+ Nova Questão" para adicionar.
            </div>

            <div
              v-for="(questao, qIdx) in grupo.questoes"
              :key="questao.id"
              class="border border-slate-200 rounded-xl p-4 bg-slate-50/40 hover:bg-white hover:border-slate-300 transition-all space-y-3"
            >
              <!-- Linha Superior da Questão: Enunciado, Tipo e Ações -->
              <div class="flex flex-col sm:flex-row gap-3 items-start justify-between">
                <div class="flex items-center gap-2 text-xs font-bold text-slate-400 shrink-0">
                  <span class="w-6 h-6 rounded-full bg-slate-200 text-slate-700 flex items-center justify-center text-[11px]">
                    {{ qIdx + 1 }}
                  </span>
                </div>

                <div class="flex-1 w-full space-y-1.5">
                  <textarea
                    v-model="questao.enunciado"
                    rows="2"
                    placeholder="Digite o enunciado da questão..."
                    class="w-full text-xs md:text-sm p-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden font-medium bg-white"
                  ></textarea>

                  <input
                    v-model="questao.ajuda"
                    type="text"
                    placeholder="Texto de ajuda ou instrução complementar (opcional)..."
                    class="w-full text-[11px] p-1.5 border border-slate-100 rounded-md text-slate-500 bg-white"
                  />
                </div>

                <div class="flex sm:flex-col items-center gap-1.5 shrink-0 self-end sm:self-auto">
                  <div class="flex gap-1">
                    <button
                      type="button"
                      @click="moverQuestao(grupo, qIdx, 'cima')"
                      :disabled="qIdx === 0"
                      title="Mover para cima"
                      class="px-2 py-1 text-xs border rounded bg-white text-slate-600 disabled:opacity-30"
                    >
                      ▲
                    </button>
                    <button
                      type="button"
                      @click="moverQuestao(grupo, qIdx, 'baixo')"
                      :disabled="qIdx === grupo.questoes.length - 1"
                      title="Mover para baixo"
                      class="px-2 py-1 text-xs border rounded bg-white text-slate-600 disabled:opacity-30"
                    >
                      ▼
                    </button>
                  </div>
                  <div class="flex gap-1">
                    <button
                      type="button"
                      @click="duplicarQuestao(grupo, qIdx)"
                      title="Duplicar questão"
                      class="px-2 py-1 text-xs border rounded bg-white text-slate-600 hover:text-slate-900"
                    >
                      📋
                    </button>
                    <button
                      type="button"
                      @click="removerQuestao(grupo, qIdx)"
                      title="Excluir questão"
                      class="px-2 py-1 text-xs border rounded bg-white text-rose-600 hover:bg-rose-50"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              </div>

              <!-- Configurações Específicas do Tipo da Questão (RF02 e RF03) -->
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-slate-100 text-xs">
                <div>
                  <label class="font-bold text-slate-600 block mb-1">Tipo de Resposta (RF02):</label>
                  <select
                    :value="questao.tipo"
                    @change="mudarTipoQuestao(questao, ($event.target as HTMLSelectElement).value as TipoQuestao)"
                    class="w-full p-2 border border-slate-200 rounded-lg bg-white font-medium"
                  >
                    <option value="ESCALA">Escala Likert (Pontuação 1 a 5)</option>
                    <option value="ESCOLHA_UNICA">Escolha Única (Radio)</option>
                    <option value="MULTIPLA_ESCOLHA">Múltipla Escolha (Caixa de Seleção)</option>
                    <option value="TEXTO_LIVRE">Texto Livre (Dissertativa / Aberta)</option>
                  </select>
                </div>

                <div class="flex items-center">
                  <label class="flex items-center gap-2 cursor-pointer font-medium text-slate-700 mt-4 sm:mt-0">
                    <input
                      v-model="questao.obrigatoria"
                      type="checkbox"
                      class="w-4 h-4 text-emerald-600 rounded-sm border-slate-300"
                    />
                    <span>Resposta Obrigatória (RF03)</span>
                  </label>
                </div>
              </div>

              <!-- Subpainel para Escala Likert -->
              <div v-if="questao.tipo === 'ESCALA'" class="bg-white p-3 rounded-lg border border-slate-200 grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <div>
                  <label class="text-[11px] font-bold text-slate-500">Rótulo Mínimo (Valor 1):</label>
                  <input
                    v-model="questao.rotuloMin"
                    type="text"
                    placeholder="Ex: 1 - Não atende"
                    class="w-full p-1.5 border border-slate-200 rounded text-xs mt-0.5"
                  />
                </div>
                <div>
                  <label class="text-[11px] font-bold text-slate-500">Rótulo Máximo (Valor 5):</label>
                  <input
                    v-model="questao.rotuloMax"
                    type="text"
                    placeholder="Ex: 5 - Atende plenamente"
                    class="w-full p-1.5 border border-slate-200 rounded text-xs mt-0.5"
                  />
                </div>
              </div>

              <!-- Subpainel para Escolha Única / Múltipla Escolha -->
              <div v-else-if="questao.tipo === 'ESCOLHA_UNICA' || questao.tipo === 'MULTIPLA_ESCOLHA'" class="bg-white p-3 rounded-lg border border-slate-200 space-y-2 text-xs">
                <div class="flex justify-between items-center font-bold text-slate-700">
                  <span>Opções de Resposta:</span>
                  <button
                    type="button"
                    @click="adicionarOpcao(questao)"
                    class="text-emerald-700 hover:text-emerald-800 text-[11px] font-bold"
                  >
                    + Adicionar Opção
                  </button>
                </div>
                <div
                  v-for="(opt, optIdx) in questao.opcoes"
                  :key="opt.id"
                  class="flex items-center gap-2"
                >
                  <span class="text-slate-400 text-[11px] font-mono">{{ optIdx + 1 }}.</span>
                  <input
                    v-model="opt.texto"
                    type="text"
                    placeholder="Texto da opção..."
                    class="flex-1 p-1.5 border border-slate-200 rounded text-xs"
                  />
                  <button
                    type="button"
                    @click="removerOpcao(questao, optIdx)"
                    class="text-slate-400 hover:text-rose-600 px-1 font-bold"
                  >
                    ✕
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>
