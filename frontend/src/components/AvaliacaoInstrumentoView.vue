<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { ConjuntoOpcao, InstrumentoAv, PermissaoTipoEntidadeAv, TipoEntidade } from '../types/instrumentoAvaliacao';
import { avaliacaoApi } from '../services/avaliacaoApi';
import { carregarUsuarioAtivo, temPermissao } from '../services/storage';
import AvaliacaoEstruturaEditor from './AvaliacaoEstruturaEditor.vue';
import AvaliacaoConjuntoModal from './AvaliacaoConjuntoModal.vue';
import InstrumentoPreview from './InstrumentoPreview.vue';

const VINCULOS = [
  { valor: '', rotulo: 'Sem vínculo exigido (qualquer pessoa do público)' },
  { valor: 'MATRICULADO_EM', rotulo: 'Matriculado no que é avaliado' },
  { valor: 'LECIONA', rotulo: 'Leciona no que é avaliado' },
  { valor: 'PERTENCE_A', rotulo: 'Pertence ao que é avaliado' }
];

// ---------------------------------------------------------------- estado
const modo = ref<'lista' | 'form'>('lista');
const previsualizando = ref(false);
const instrumentos = ref<InstrumentoAv[]>([]);
const tipos = ref<TipoEntidade[]>([]);
const permissoesTipos = ref<PermissaoTipoEntidadeAv[]>([]);
const conjuntos = ref<ConjuntoOpcao[]>([]);
const carregando = ref(false);
const salvando = ref(false);
const modalConjunto = ref(false);
const erro = ref<string | null>(null);
const sucesso = ref<string | null>(null);
const usuario = carregarUsuarioAtivo();
const podeCriar = temPermissao(usuario, 'INSTRUMENTO_CRIAR');
const podeEditar = temPermissao(usuario, 'INSTRUMENTO_EDITAR');
const podeExcluir = temPermissao(usuario, 'INSTRUMENTO_EXCLUIR');

const tiposAvaliaveis = computed(() => tipos.value.filter(t => t.podeSerAvaliada));
const tiposRespondentes = computed(() => {
  const tipoAvaliadoId = form.tipoEntidadeAvaliadaId;
  const alvosSelecionados = form.tiposEntidadeAlvoIds || [];
  if (!tipoAvaliadoId || !alvosSelecionados.length) return [];
  return tipos.value.filter(tipo => tipo.podeResponder
    && permissoesTipos.value.some(permissao =>
      permissao.tipoRespondenteId === tipo.id && permissao.tipoAvaliadoId === tipoAvaliadoId)
    && alvosSelecionados.every(alvoId => permissoesTipos.value.some(permissao =>
      permissao.tipoRespondenteId === tipo.id && permissao.tipoAvaliadoId === alvoId)));
});
const tiposAlvoDisponiveis = computed(() => {
  const contexto = tipos.value.find(t => t.id === form.tipoEntidadeAvaliadaId);
  return contexto?.codigo === 'TURMA'
    ? tiposAvaliaveis.value.filter(t => ['DOCENTE', 'ALUNO'].includes(t.codigo))
    : tiposAvaliaveis.value.filter(t => t.id === form.tipoEntidadeAvaliadaId);
});

// datas ISO <-> input datetime-local
const pad = (n: number) => String(n).padStart(2, '0');
function isoParaLocal(iso?: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
const localParaIso = (s: string) => (s ? new Date(s).toISOString() : '');

function novoInstrumento(): InstrumentoAv {
  const hoje = new Date();
  const fim = new Date(Date.now() + 30 * 86400000);
  return {
    tipoEntidadeAvaliadaId: '',
    tiposEntidadeAlvoIds: [],
    titulo: '',
    descricao: '',
    cicloReferencia: `${hoje.getFullYear()}/${hoje.getMonth() < 6 ? 1 : 2}`,
    inicioAplicacao: isoParaLocal(hoje.toISOString()),
    fimAplicacao: isoParaLocal(fim.toISOString()),
    anonimo: true,
    permiteEdicao: true,
    respondentes: [],
    paginas: [],
    sessoes: []
  };
}

function normalizarRespondentes(respondentes?: InstrumentoAv['respondentes']) {
  // O banco persiste o vínculo sem exigência como NULL. O select usa string vazia
  // para essa opção, então convertemos o valor ao carregar o instrumento.
  return (respondentes || []).map(r => ({ ...r, tipoVinculo: r.tipoVinculo ?? '' }));
}

function normalizarTiposAlvo(i: InstrumentoAv) {
  return i.tiposEntidadeAlvoIds?.length ? [...i.tiposEntidadeAlvoIds] : (i.tipoEntidadeAvaliadaId ? [i.tipoEntidadeAvaliadaId] : []);
}

function contextoAlterado() {
  const contexto = tipos.value.find(t => t.id === form.tipoEntidadeAvaliadaId);
  form.tiposEntidadeAlvoIds = contexto?.codigo === 'TURMA'
    ? tiposAlvoDisponiveis.value.map(t => t.id)
    : (form.tipoEntidadeAvaliadaId ? [form.tipoEntidadeAvaliadaId] : []);
  ajustarRespondentesPermitidos();
  for (const respondente of form.respondentes) {
    const vinculos = vinculosParaRespondente(respondente.tipoEntidadeId).map(v => v.valor);
    if (!vinculos.includes(respondente.tipoVinculo || '')) respondente.tipoVinculo = '';
  }
}

function alternarTipoAlvo(tipoId: string) {
  form.tiposEntidadeAlvoIds ||= [];
  const idx = form.tiposEntidadeAlvoIds?.indexOf(tipoId) ?? -1;
  if (idx >= 0) form.tiposEntidadeAlvoIds!.splice(idx, 1);
  else form.tiposEntidadeAlvoIds!.push(tipoId);
  ajustarRespondentesPermitidos();
}

function ajustarRespondentesPermitidos() {
  const tiposPermitidos = new Set(tiposRespondentes.value.map(t => t.id));
  form.respondentes = form.respondentes.filter(r => tiposPermitidos.has(r.tipoEntidadeId));
}

const form = reactive<InstrumentoAv>(novoInstrumento());
const semPermissaoDeEdicao = computed(() => form.id ? !podeEditar : !podeCriar);
const somenteLeitura = computed(() => (!!form.status && form.status !== 'RASCUNHO') || semPermissaoDeEdicao.value);

// ---------------------------------------------------------------- carga
async function carregarLista() {
  carregando.value = true;
  const res = await avaliacaoApi.listar();
  carregando.value = false;
  if (res.ok) instrumentos.value = res.dados || [];
  else erro.value = res.erro || 'Erro ao carregar instrumentos.';
}

onMounted(async () => {
  const [t, c, p] = await Promise.all([
    avaliacaoApi.tiposEntidade(),
    avaliacaoApi.conjuntos(),
    avaliacaoApi.permissoesTiposEntidade()
  ]);
  if (t.ok) tipos.value = t.dados || [];
  if (c.ok) conjuntos.value = c.dados || [];
  if (p.ok) permissoesTipos.value = p.dados || [];
  if (!t.ok) erro.value = t.erro || 'Erro ao carregar os tipos de entidade.';
  if (!p.ok) erro.value = p.erro || 'Erro ao carregar as permissões entre tipos de entidade.';
  await carregarLista();
});

// ---------------------------------------------------------------- ações
function novo() {
  Object.assign(form, novoInstrumento(), { id: undefined, status: undefined });
  erro.value = sucesso.value = null;
  modo.value = 'form';
}

async function editar(id: string) {
  erro.value = sucesso.value = null;
  const res = await avaliacaoApi.buscar(id);
  if (!res.ok || !res.dados) {
    erro.value = res.erro || 'Instrumento não encontrado.';
    return;
  }
  Object.assign(form, res.dados, {
    inicioAplicacao: isoParaLocal(res.dados.inicioAplicacao),
    fimAplicacao: isoParaLocal(res.dados.fimAplicacao),
    respondentes: normalizarRespondentes(res.dados.respondentes),
    tiposEntidadeAlvoIds: normalizarTiposAlvo(res.dados)
  });
  modo.value = 'form';
}

function aplicar(i: InstrumentoAv) {
  Object.assign(form, i, {
    inicioAplicacao: isoParaLocal(i.inicioAplicacao),
    fimAplicacao: isoParaLocal(i.fimAplicacao),
    respondentes: normalizarRespondentes(i.respondentes),
    tiposEntidadeAlvoIds: normalizarTiposAlvo(i)
  });
}

async function salvar(): Promise<boolean> {
  erro.value = sucesso.value = null;
  salvando.value = true;
  const payload: InstrumentoAv = JSON.parse(JSON.stringify(form));
  payload.inicioAplicacao = localParaIso(form.inicioAplicacao);
  payload.fimAplicacao = localParaIso(form.fimAplicacao);
  const u = carregarUsuarioAtivo();
  const res = await avaliacaoApi.salvar(payload, u?.cpf || u?.id);
  salvando.value = false;
  if (!res.ok || !res.dados) {
    erro.value = res.erro || 'Erro ao salvar.';
    return false;
  }
  aplicar(res.dados);
  sucesso.value = 'Instrumento salvo com sucesso.';
  return true;
}

async function publicar() {
  if (!confirm('Após publicar, a estrutura do questionário não poderá mais ser alterada. Continuar?')) return;
  if (!(await salvar())) return;
  const res = await avaliacaoApi.publicar(form.id!);
  if (res.ok && res.dados) {
    aplicar(res.dados);
    sucesso.value = 'Instrumento publicado.';
  } else {
    erro.value = res.erro || 'Erro ao publicar.';
  }
}

async function excluir(id: string, titulo: string) {
  if (!confirm(`Excluir o rascunho "${titulo}"?`)) return;
  const res = await avaliacaoApi.excluir(id);
  if (res.ok) await carregarLista();
  else erro.value = res.erro || 'Erro ao excluir.';
}

async function voltar() {
  modo.value = 'lista';
  await carregarLista();
}

function respondenteDe(tipoId: string) {
  return form.respondentes.find(r => r.tipoEntidadeId === tipoId);
}
function alternarRespondente(tipoId: string) {
  const idx = form.respondentes.findIndex(r => r.tipoEntidadeId === tipoId);
  if (idx >= 0) form.respondentes.splice(idx, 1);
  else form.respondentes.push({ tipoEntidadeId: tipoId, tipoVinculo: '' });
}

function vinculosParaRespondente(tipoId: string) {
  const contexto = tipos.value.find(t => t.id === form.tipoEntidadeAvaliadaId)?.codigo;
  const respondente = tipos.value.find(t => t.id === tipoId)?.codigo;
  if (contexto === 'DOCENTE')
    return [{ valor: '', rotulo: 'Aluno matriculado em uma turma do docente' }];
  if (contexto === 'TURMA') {
    if (respondente === 'ALUNO') return VINCULOS.filter(v => ['', 'MATRICULADO_EM'].includes(v.valor));
    if (respondente === 'DOCENTE') return VINCULOS.filter(v => ['', 'LECIONA'].includes(v.valor));
    return VINCULOS.filter(v => !v.valor);
  }
  if (contexto === 'CAMPUS' || contexto === 'SETOR')
    return VINCULOS.filter(v => ['', 'PERTENCE_A'].includes(v.valor));
  if (contexto === 'INSTITUICAO') return VINCULOS.filter(v => !v.valor);
  return VINCULOS;
}

function aoSalvarConjunto(c: ConjuntoOpcao) {
  conjuntos.value.push(c);
  conjuntos.value.sort((a, b) => a.nome.localeCompare(b.nome));
  modalConjunto.value = false;
}

function voltarDaPrevia() {
  previsualizando.value = false;
}

const fmtData = (iso?: string) => (iso ? new Date(iso).toLocaleDateString('pt-BR') : '—');
const corStatus: Record<string, string> = {
  RASCUNHO: 'bg-slate-100 text-slate-700',
  PUBLICADO: 'bg-emerald-100 text-emerald-800',
  ENCERRADO: 'bg-amber-100 text-amber-800',
  CONSOLIDADO: 'bg-purple-100 text-purple-800'
};

const input = 'w-full text-xs p-3 border border-slate-200 rounded-xl bg-white focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden disabled:bg-slate-50 disabled:text-slate-500';
</script>

<template>
  <div class="space-y-6">
    <InstrumentoPreview v-if="previsualizando" :instrumento="form" :conjuntos="conjuntos" @voltar="voltarDaPrevia" />
    <template v-else>
    <p v-if="erro" class="p-4 bg-rose-50 border border-rose-300 rounded-xl text-xs text-rose-900 font-semibold flex justify-between gap-3">
      <span>⚠️ {{ erro }}</span><button type="button" @click="erro = null">✕</button>
    </p>
    <p v-if="sucesso" class="p-4 bg-emerald-50 border border-emerald-300 rounded-xl text-xs text-emerald-900 font-semibold flex justify-between gap-3">
      <span>✓ {{ sucesso }}</span><button type="button" @click="sucesso = null">✕</button>
    </p>

    <!-- ===================== LISTA ===================== -->
    <div v-if="modo === 'lista'" class="bg-white border border-slate-200 rounded-2xl shadow-xs overflow-hidden">
      <div class="p-5 md:p-6 flex flex-wrap items-center justify-between gap-3 border-b border-slate-100">
        <div>
          <h2 class="text-lg font-bold text-slate-900">Instrumentos de Avaliação</h2>
          <p class="text-xs text-slate-500">Questionários cadastrados no schema <code>avaliacao</code>.</p>
        </div>
        <button v-if="podeCriar" type="button" @click="novo" class="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold px-4 py-2.5 rounded-xl">
          + Novo instrumento
        </button>
      </div>

      <p v-if="carregando" class="p-8 text-center text-xs text-slate-400 animate-pulse">Carregando…</p>
      <p v-else-if="instrumentos.length === 0" class="p-8 text-center text-xs text-slate-400">Nenhum instrumento cadastrado.</p>
      <table v-else class="w-full text-xs">
        <thead class="bg-slate-50 text-[10px] uppercase text-slate-500">
          <tr>
            <th class="text-left p-3">Título</th><th class="text-left p-3">Ciclo</th><th class="text-left p-3">Avalia</th>
            <th class="text-left p-3">Período</th><th class="text-center p-3">Questões</th><th class="text-left p-3">Status</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="i in instrumentos" :key="i.id" class="border-t border-slate-100 hover:bg-slate-50/60">
            <td class="p-3 font-semibold text-slate-900">{{ i.titulo }}</td>
            <td class="p-3 text-slate-600">{{ i.cicloReferencia }}</td>
            <td class="p-3 text-slate-600">{{ i.tiposEntidadeAlvoNomes?.join(', ') || i.tipoEntidadeAvaliadaNome }}</td>
            <td class="p-3 text-slate-600">{{ fmtData(i.inicioAplicacao) }} – {{ fmtData(i.fimAplicacao) }}</td>
            <td class="p-3 text-center">{{ i.totalQuestoes }}</td>
            <td class="p-3"><span :class="['px-2 py-0.5 rounded-full font-bold text-[10px]', corStatus[i.status || 'RASCUNHO']]">{{ i.status }}</span></td>
            <td class="p-3 text-right whitespace-nowrap">
              <button type="button" class="font-bold text-emerald-700 hover:underline mr-3" @click="editar(i.id!)">{{ i.status === 'RASCUNHO' ? 'Editar' : 'Abrir' }}</button>
              <button v-if="podeExcluir && i.status === 'RASCUNHO'" type="button" class="font-bold text-rose-600 hover:underline" @click="excluir(i.id!, i.titulo)">Excluir</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ===================== FORMULÁRIO ===================== -->
    <template v-else>
      <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <button type="button" class="text-xs font-bold text-emerald-700 hover:underline" @click="voltar">← Voltar à lista</button>
            <span class="text-xs bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded-full uppercase">
              {{ form.id ? `Edição · ${form.status}` : 'Novo instrumento' }}
            </span>
          </div>
          <h2 class="text-xl font-bold text-slate-900 mt-1">{{ form.titulo || 'Cadastro de Instrumento' }}</h2>
        </div>
        <div class="flex gap-2.5">
          <button type="button" class="bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-bold px-4 py-2.5 rounded-xl" @click="previsualizando = true">
            👁 Visualizar instrumento
          </button>
          <button v-if="!semPermissaoDeEdicao" type="button" :disabled="salvando" class="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold px-5 py-2.5 rounded-xl disabled:opacity-50" @click="salvar">
            💾 {{ salvando ? 'Salvando…' : 'Salvar' }}
          </button>
          <button v-if="podeEditar && !somenteLeitura" type="button" :disabled="salvando" class="bg-slate-900 hover:bg-black text-white text-xs font-bold px-4 py-2.5 rounded-xl disabled:opacity-50" @click="publicar">
            🚀 Salvar e publicar
          </button>
        </div>
      </div>

      <!-- 1. Dados gerais -->
      <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs space-y-5">
        <div class="border-b border-slate-100 pb-3">
          <h3 class="text-sm font-bold text-slate-900 uppercase tracking-wide">1. Dados Gerais</h3>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <label class="md:col-span-2 space-y-1.5 text-xs font-bold text-slate-700">Título
            <input v-model="form.titulo" :disabled="semPermissaoDeEdicao" :class="[input, 'font-semibold']" placeholder="Ex.: ADPD 2026/1 – Avaliação Docente" />
          </label>
          <label class="md:col-span-2 space-y-1.5 text-xs font-bold text-slate-700">Descrição
            <textarea v-model="form.descricao" :disabled="semPermissaoDeEdicao" rows="2" :class="input" placeholder="Orientações exibidas ao respondente"></textarea>
          </label>
          <label class="space-y-1.5 text-xs font-bold text-slate-700">Ciclo de referência
            <input v-model="form.cicloReferencia" :disabled="semPermissaoDeEdicao" :class="input" placeholder="2026/1" />
          </label>
          <label class="space-y-1.5 text-xs font-bold text-slate-700">Início da aplicação
            <input v-model="form.inicioAplicacao" :disabled="semPermissaoDeEdicao" type="datetime-local" :class="input" />
          </label>
          <label class="space-y-1.5 text-xs font-bold text-slate-700">Término da aplicação
            <input v-model="form.fimAplicacao" :disabled="semPermissaoDeEdicao" type="datetime-local" :class="input" />
          </label>
          <label class="md:col-span-2 space-y-1.5 text-xs font-bold text-slate-700">O que será avaliado
            <select v-model="form.tipoEntidadeAvaliadaId" :disabled="somenteLeitura" :class="input" @change="contextoAlterado">
              <option value="" disabled>Selecione…</option>
              <option v-for="t in tiposAvaliaveis" :key="t.id" :value="t.id">{{ t.nome }}</option>
            </select>
          </label>
          <div v-if="tiposAlvoDisponiveis.length" class="md:col-span-2 space-y-2">
            <p class="text-xs font-bold text-slate-700">Alvos deste instrumento</p>
            <p v-if="tipos.find(t => t.id === form.tipoEntidadeAvaliadaId)?.codigo === 'TURMA'" class="text-xs text-slate-500">
              Ao selecionar uma turma, o respondente poderá avaliar docentes que lecionam nela e alunos vinculados a ela.
            </p>
            <div class="flex flex-wrap gap-3">
              <label v-for="t in tiposAlvoDisponiveis" :key="t.id" class="flex items-center gap-2 text-xs text-slate-700">
                <input type="checkbox" :checked="form.tiposEntidadeAlvoIds?.includes(t.id)" :disabled="somenteLeitura"
                  @change="alternarTipoAlvo(t.id)" /> {{ t.nome }}
              </label>
            </div>
          </div>
          <div class="md:col-span-2 flex flex-wrap gap-6">
            <label class="flex items-center gap-2 text-xs font-medium text-slate-800 cursor-pointer">
              <input v-model="form.anonimo" type="checkbox" class="w-4 h-4" /> Respostas anônimas (não vincular o respondente à resposta)
            </label>
            <label class="flex items-center gap-2 text-xs font-medium text-slate-800 cursor-pointer">
              <input v-model="form.permiteEdicao" type="checkbox" class="w-4 h-4" /> Permitir editar a resposta até o término
            </label>
          </div>
        </div>
      </div>

      <!-- 2. Respondentes -->
      <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs space-y-4">
        <div class="border-b border-slate-100 pb-3">
          <h3 class="text-sm font-bold text-slate-900 uppercase tracking-wide">2. Quem Responde</h3>
          <p class="text-xs text-slate-500 mt-0.5">Marque os públicos e, se necessário, o vínculo exigido com o que é avaliado.</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
          <div v-for="t in tiposRespondentes" :key="t.id"
            :class="['border rounded-xl p-3 space-y-2 transition-all', respondenteDe(t.id) ? 'border-emerald-500 bg-emerald-50/40' : 'border-slate-200']">
            <label class="flex items-center gap-2 text-xs font-bold text-slate-800 cursor-pointer">
              <input type="checkbox" class="w-4 h-4" :checked="!!respondenteDe(t.id)" :disabled="somenteLeitura" @change="alternarRespondente(t.id)" />
              {{ t.nome }}
            </label>
            <p v-if="respondenteDe(t.id) && tipos.find(item => item.id === form.tipoEntidadeAvaliadaId)?.codigo === 'DOCENTE'"
              class="text-xs text-slate-600">Apenas alunos matriculados em uma turma do docente poderão responder.</p>
            <select v-else-if="respondenteDe(t.id)" v-model="respondenteDe(t.id)!.tipoVinculo" :disabled="somenteLeitura" :class="input">
              <option v-for="v in vinculosParaRespondente(t.id)" :key="v.valor" :value="v.valor">{{ v.rotulo }}</option>
            </select>
          </div>
        </div>
        <p v-if="!form.tipoEntidadeAvaliadaId" class="text-xs text-slate-500">Selecione o que será avaliado para ver quem pode responder.</p>
        <p v-else-if="!tiposRespondentes.length" class="text-xs text-slate-500">Nenhum público está autorizado na matriz de permissões para os tipos selecionados.</p>
      </div>

      <!-- 3. Estrutura -->
      <AvaliacaoEstruturaEditor
        v-model:paginas="form.paginas"
        v-model:sessoes="form.sessoes"
        :conjuntos="conjuntos"
        :somente-leitura="somenteLeitura"
        :pode-criar-conjunto="podeCriar"
        @novo-conjunto="modalConjunto = true"
      />
    </template>

    </template>
    <AvaliacaoConjuntoModal v-if="modalConjunto" @salvo="aoSalvarConjunto" @fechar="modalConjunto = false" />
  </div>
</template>
