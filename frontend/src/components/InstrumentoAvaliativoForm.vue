<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { InstrumentoAvaliativoDTO, PermissaoTipoEntidadeDTO, TipoEntidadeDTO } from '../types/instrumentoAvaliacao';
import { avaliacaoApi } from '../services/avaliacaoApi';
import { carregarUsuarioAtivo, temPermissao } from '../services/storage';

const VINCULOS = [
  { valor: '', rotulo: 'Sem vínculo exigido (qualquer pessoa do público)' },
  { valor: 'MATRICULADO_EM', rotulo: 'Matriculado no que é avaliado' },
  { valor: 'LECIONA', rotulo: 'Leciona no que é avaliado' },
  { valor: 'PERTENCE_A', rotulo: 'Pertence ao que é avaliado' }
];

const route = useRoute();
const router = useRouter();
const tipos = ref<TipoEntidadeDTO[]>([]);
const permissoesTipos = ref<PermissaoTipoEntidadeDTO[]>([]);
const salvando = ref(false);
const erro = ref<string | null>(null);
const sucesso = ref<string | null>(null);
const usuario = carregarUsuarioAtivo();
const podeCriar = temPermissao(usuario, 'INSTRUMENTO_CRIAR');
const podeEditar = temPermissao(usuario, 'INSTRUMENTO_EDITAR');
const idRota = computed(() => typeof route.params.id === 'string' ? route.params.id : undefined);
const tiposAvaliaveis = computed(() => tipos.value.filter(t => t.podeSerAvaliada));
const tiposRespondentes = computed(() => {
  const tipoAvaliadoId = form.tipoEntidadeAvaliada.tipoEntidadeAvaliadaId;
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
    tipoEntidadeAvaliada: { tipoEntidadeAvaliadaId: '', codigo: '', nome: '', podeSerAvaliada: false, podeResponder: false },
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
const input = 'form-field';

function aplicar(i: InstrumentoAvaliativoDTO) {
  Object.assign(form, i, {
    inicioAplicacao: isoParaLocal(i.inicioAplicacao),
    fimAplicacao: isoParaLocal(i.fimAplicacao),
    respondentes: normalizarRespondentes(i.respondentes)
  });
}

onMounted(async () => {
  const [t, p] = await Promise.all([
    avaliacaoApi.tiposEntidade(),
    avaliacaoApi.permissoesTiposEntidade()
  ]);
  if (t.ok) tipos.value = t.dados || [];
  else erro.value = t.erro || 'Erro ao carregar os tipos de entidade.';
  if (p.ok) permissoesTipos.value = p.dados || [];
  else erro.value = p.erro || 'Erro ao carregar as permissões entre tipos de entidade.';

  if (!idRota.value) {
    Object.assign(form, novoInstrumento());
    if (!podeCriar) erro.value = 'Você não tem permissão para criar instrumentos.';
    return;
  }

  const resposta = await avaliacaoApi.buscarCadastro(idRota.value);
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
  const contexto = tipos.value.find(t => t.id === form.tipoEntidadeAvaliada.tipoEntidadeAvaliadaId)?.codigo;
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
  payload.paginas = [];
  payload.sessoes = [];
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
    await router.replace({ name: 'instrumento-editar', params: { id: resposta.dados.id } });
  }
  return true;
}

</script>

<template>
  <div class="">
      <p v-if="erro" class="alert alert--error instrument-form__alert">
        <span>⚠️ {{ erro }}</span><button type="button" @click="erro = null">✕</button>
      </p>
      <p v-if="sucesso" class="alert alert--success instrument-form__alert">
        <span>✓ {{ sucesso }}</span><button type="button" @click="sucesso = null">✕</button>
      </p>

      <div class="panel panel--padded instrument-form__heading">
        <div>
          <div class="instrument-form__breadcrumb">
            <RouterLink :to="{ name: 'instrumentos' }" class="text-action text-action--primary instrument-form__back">← Voltar à lista</RouterLink>
            <span class="instrument-form__mode-badge">
              {{ form.id ? `Edição · ${form.status}` : 'Novo instrumento' }}
            </span>
          </div>
          <h2 class="instrument-form__title">{{ form.titulo || 'Cadastro de Instrumento' }}</h2>
        </div>
        <div class="instrument-form__actions">
          <button v-if="!semPermissaoDeEdicao" type="button" :disabled="salvando" class="button button--primary instrument-form__save" @click="salvar()">
            💾 {{ salvando ? 'Salvando…' : 'Salvar' }}
          </button>
        </div>
      </div>

      <!-- 1. Dados gerais -->
      <div class="panel panel--padded form-panel">
        <div class="form-panel__heading">
          <h3 class="form-panel__title">1. Dados Gerais</h3>
        </div>
        <div class="form-grid">
          <label class="form-label form-grid__wide"><span class="form-label__caption">Título <span class="required-mark" aria-hidden="true">*</span></span>
            <input v-model="form.titulo" :disabled="semPermissaoDeEdicao" required :class="[input, 'form-field--emphasis']" placeholder="Ex.: ADPD 2026/1 – Avaliação Docente" />
          </label>
          <label class="form-label form-grid__wide">Descrição
            <textarea v-model="form.descricao" :disabled="semPermissaoDeEdicao" rows="2" :class="input" placeholder="Orientações exibidas ao respondente"></textarea>
          </label>
          <label class="form-label"><span class="form-label__caption">Ciclo de referência <span class="required-mark" aria-hidden="true">*</span></span>
            <input v-model="form.cicloReferencia" :disabled="semPermissaoDeEdicao" required :class="input" placeholder="2026/1" />
          </label>
          <label class="form-label"><span class="form-label__caption">Início da aplicação <span class="required-mark" aria-hidden="true">*</span></span>
            <input v-model="form.inicioAplicacao" :disabled="semPermissaoDeEdicao" type="datetime-local" required :class="input" />
          </label>
          <label class="form-label"><span class="form-label__caption">Término da aplicação <span class="required-mark" aria-hidden="true">*</span></span>
            <input v-model="form.fimAplicacao" :disabled="semPermissaoDeEdicao" type="datetime-local" required :class="input" />
          </label>
          <label class="form-label form-grid__wide"><span class="form-label__caption">O que será avaliado <span class="required-mark" aria-hidden="true">*</span></span>
            <select v-model="form.tipoEntidadeAvaliada.tipoEntidadeAvaliadaId" :disabled="somenteLeitura" required :class="input" @change="contextoAlterado">
              <option value="" disabled>Selecione…</option>
              <option v-for="t in tiposAvaliaveis" :key="t.id" :value="t.id">{{ t.nome }}</option>
            </select>
          </label>
          <div class="form-grid__wide form-checkboxes">
            <label class="form-checkbox-label">
              <input v-model="form.anonimo" type="checkbox" class="form-checkbox" /> Respostas anônimas (não vincular o respondente à resposta)
            </label>
            <label class="form-checkbox-label">
              <input v-model="form.permiteEdicao" type="checkbox" class="form-checkbox" /> Permitir editar a resposta até o término
            </label>
          </div>
        </div>
      </div>

      <!-- 2. Respondentes -->
      <div class="panel panel--padded form-panel">
        <div class="form-panel__heading">
          <h3 class="form-panel__title">2. Quem Responde <span class="required-mark" aria-hidden="true">*</span></h3>
          <p class="form-panel__description">Marque os públicos autorizados a responder ao instrumento.</p>
        </div>
        <div class="respondent-grid">
          <div v-for="t in tiposRespondentes" :key="t.id"
            :class="['respondent-card', respondenteDe(t.id) ? 'respondent-card--selected' : '']">
            <label class="respondent-card__label">
              <input type="checkbox" class="respondent-card__checkbox" :checked="!!respondenteDe(t.id)" :disabled="somenteLeitura" @change="alternarRespondente(t.id)" />
              {{ t.nome }}
            </label>
            <p v-if="respondenteDe(t.id) && tipos.find(item => item.id === form.tipoEntidadeAvaliada.tipoEntidadeAvaliadaId)?.codigo === 'DOCENTE'"
              class="respondent-card__hint">Apenas alunos matriculados em uma turma do docente poderão responder.</p>
          </div>
        </div>
        <p v-if="!form.tipoEntidadeAvaliada.tipoEntidadeAvaliadaId" class="form-panel__help">Selecione o que será avaliado para ver quem pode responder.</p>
        <p v-else-if="!tiposRespondentes.length" class="form-panel__help">Nenhum público está autorizado na matriz de permissões para os tipos selecionados.</p>
      </div>

  </div>
</template>
