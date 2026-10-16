<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { ConjuntoOpcaoDTO, InstrumentoAvaliativoDTO, PermissaoTipoEntidadeDTO, TipoEntidadeDTO } from '../types/instrumentoAvaliacao';
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

const route = useRoute();
const router = useRouter();
const previsualizando = ref(false);
const tipos = ref<TipoEntidadeDTO[]>([]);
const permissoesTipos = ref<PermissaoTipoEntidadeDTO[]>([]);
const conjuntos = ref<ConjuntoOpcaoDTO[]>([]);
const salvando = ref(false);
const modalConjunto = ref(false);
const erro = ref<string | null>(null);
const sucesso = ref<string | null>(null);
const usuario = carregarUsuarioAtivo();
const podeCriar = temPermissao(usuario, 'INSTRUMENTO_CRIAR');
const podeEditar = temPermissao(usuario, 'INSTRUMENTO_EDITAR');
const idRota = computed(() => typeof route.params.id === 'string' ? route.params.id : undefined);
const tiposAvaliaveis = computed(() => tipos.value.filter(t => t.podeSerAvaliada));
const tiposRespondentes = computed(() => {
  const tipoAvaliadoId = form.tipoEntidadeAvaliadaId;
  if (!tipoAvaliadoId) return [];
  return tipos.value.filter(tipo => tipo.podeResponder
    && permissoesTipos.value.some(permissao =>
      permissao.tipoRespondenteId === tipo.id && permissao.tipoAvaliadoId === tipoAvaliadoId));
});

const pad = (n: number) => String(n).padStart(2, '0');
function isoParaLocal(iso?: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
const localParaIso = (s: string) => (s ? new Date(s).toISOString() : '');

function novoInstrumento(): InstrumentoAvaliativoDTO {
  const hoje = new Date();
  const fim = new Date(Date.now() + 30 * 86400000);
  return {
    tipoEntidadeAvaliadaId: '',
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

function normalizarRespondentes(respondentes?: InstrumentoAvaliativoDTO['respondentes']) {
  return (respondentes || []).map(r => ({ ...r, tipoVinculo: r.tipoVinculo ?? '' }));
}

const form = reactive<InstrumentoAvaliativoDTO>(novoInstrumento());
const semPermissaoDeEdicao = computed(() => idRota.value ? (!form.id || !podeEditar) : !podeCriar);
const somenteLeitura = computed(() => (!!form.status && form.status !== 'RASCUNHO') || semPermissaoDeEdicao.value);
const input = 'w-full text-xs p-3 border border-slate-200 rounded-xl bg-white focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden disabled:bg-slate-50 disabled:text-slate-500';

function aplicar(i: InstrumentoAvaliativoDTO) {
  Object.assign(form, i, {
    inicioAplicacao: isoParaLocal(i.inicioAplicacao),
    fimAplicacao: isoParaLocal(i.fimAplicacao),
    respondentes: normalizarRespondentes(i.respondentes)
  });
}

onMounted(async () => {
  const [t, c, p] = await Promise.all([
    avaliacaoApi.tiposEntidade(),
    avaliacaoApi.conjuntos(),
    avaliacaoApi.permissoesTiposEntidade()
  ]);
  if (t.ok) tipos.value = t.dados || [];
  else erro.value = t.erro || 'Erro ao carregar os tipos de entidade.';
  if (c.ok) conjuntos.value = c.dados || [];
  else erro.value = c.erro || 'Erro ao carregar os conjuntos de opções.';
  if (p.ok) permissoesTipos.value = p.dados || [];
  else erro.value = p.erro || 'Erro ao carregar as permissões entre tipos de entidade.';

  if (!idRota.value) {
    Object.assign(form, novoInstrumento());
    if (!podeCriar) erro.value = 'Você não tem permissão para criar instrumentos.';
    return;
  }

  const resposta = await avaliacaoApi.buscar(idRota.value);
  if (resposta.ok && resposta.dados) aplicar(resposta.dados);
  else erro.value = resposta.erro || 'Instrumento não encontrado.';
});

function contextoAlterado() {
  const tiposPermitidos = new Set(tiposRespondentes.value.map(t => t.id));
  form.respondentes = form.respondentes.filter(r => tiposPermitidos.has(r.tipoEntidadeId));
  for (const respondente of form.respondentes) {
    const vinculos = vinculosParaRespondente(respondente.tipoEntidadeId).map(v => v.valor);
    if (!vinculos.includes(respondente.tipoVinculo || '')) respondente.tipoVinculo = '';
  }
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
  if (contexto === 'DOCENTE') return [{ valor: '', rotulo: 'Aluno matriculado em uma turma do docente' }];
  if (contexto === 'TURMA') {
    if (respondente === 'ALUNO') return VINCULOS.filter(v => ['', 'MATRICULADO_EM'].includes(v.valor));
    if (respondente === 'DOCENTE') return VINCULOS.filter(v => ['', 'LECIONA'].includes(v.valor));
    return VINCULOS.filter(v => !v.valor);
  }
  if (contexto === 'CAMPUS' || contexto === 'SETOR') return VINCULOS.filter(v => ['', 'PERTENCE_A'].includes(v.valor));
  if (contexto === 'INSTITUICAO') return VINCULOS.filter(v => !v.valor);
  return VINCULOS;
}

async function salvar(navegarAposCriar = true): Promise<boolean> {
  erro.value = sucesso.value = null;
  salvando.value = true;
  const payload: InstrumentoAvaliativoDTO = JSON.parse(JSON.stringify(form));
  payload.inicioAplicacao = localParaIso(form.inicioAplicacao);
  payload.fimAplicacao = localParaIso(form.fimAplicacao);
  const u = carregarUsuarioAtivo();
  const resposta = await avaliacaoApi.salvar(payload, u?.cpf || u?.id);
  salvando.value = false;
  if (!resposta.ok || !resposta.dados) {
    erro.value = resposta.erro || 'Erro ao salvar.';
    return false;
  }
  const eraNovo = !form.id;
  aplicar(resposta.dados);
  sucesso.value = 'Instrumento salvo com sucesso.';
  if (eraNovo && navegarAposCriar && resposta.dados.id) {
    await router.replace(`/instrumentos/${encodeURIComponent(resposta.dados.id)}`);
  }
  return true;
}

async function publicar() {
  if (!confirm('Após publicar, a estrutura do questionário não poderá mais ser alterada. Continuar?')) return;
  if (!(await salvar(false)) || !form.id) return;
  const resposta = await avaliacaoApi.publicar(form.id);
  if (resposta.ok && resposta.dados) {
    aplicar(resposta.dados);
    sucesso.value = 'Instrumento publicado.';
    if (!idRota.value) await router.replace(`/instrumentos/${encodeURIComponent(resposta.dados.id || form.id)}`);
  } else {
    erro.value = resposta.erro || 'Erro ao publicar.';
  }
}

function aoSalvarConjunto(conjunto: ConjuntoOpcaoDTO) {
  conjuntos.value.push(conjunto);
  conjuntos.value.sort((a, b) => a.nome.localeCompare(b.nome));
  modalConjunto.value = false;
}

function voltarLista() {
  void router.push('/instrumentos');
}

function voltarDaPrevia() {
  previsualizando.value = false;
}
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

      <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <button type="button" class="text-xs font-bold text-emerald-700 hover:underline" @click="voltarLista">← Voltar à lista</button>
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
          <button v-if="!semPermissaoDeEdicao" type="button" :disabled="salvando" class="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold px-5 py-2.5 rounded-xl disabled:opacity-50" @click="salvar()">
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
    <AvaliacaoConjuntoModal v-if="modalConjunto" @salvo="aoSalvarConjunto" @fechar="modalConjunto = false" />
  </div>
</template>
