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
const input = 'form-field';

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
  <div class="instrument-form">
    <InstrumentoPreview v-if="previsualizando" :instrumento="form" :conjuntos="conjuntos" @voltar="voltarDaPrevia" />
    <template v-else>
      <p v-if="erro" class="alert alert--error instrument-form__alert">
        <span>⚠️ {{ erro }}</span><button type="button" @click="erro = null">✕</button>
      </p>
      <p v-if="sucesso" class="alert alert--success instrument-form__alert">
        <span>✓ {{ sucesso }}</span><button type="button" @click="sucesso = null">✕</button>
      </p>

      <div class="instrument-form__heading">
        <div>
          <div class="instrument-form__breadcrumb">
            <button type="button" class="text-action text-action--primary instrument-form__back" @click="voltarLista">← Voltar à lista</button>
            <span class="instrument-form__mode-badge">
              {{ form.id ? `Edição · ${form.status}` : 'Novo instrumento' }}
            </span>
          </div>
          <h2 class="instrument-form__title">{{ form.titulo || 'Cadastro de Instrumento' }}</h2>
        </div>
        <div class="instrument-form__actions">
          <button type="button" class="button button--secondary" @click="previsualizando = true">
            👁 Visualizar instrumento
          </button>
          <button v-if="!semPermissaoDeEdicao" type="button" :disabled="salvando" class="button button--primary instrument-form__save" @click="salvar()">
            💾 {{ salvando ? 'Salvando…' : 'Salvar' }}
          </button>
          <button v-if="podeEditar && !somenteLeitura" type="button" :disabled="salvando" class="button button--publish" @click="publicar">
            🚀 Salvar e publicar
          </button>
        </div>
      </div>

      <!-- 1. Dados gerais -->
      <div class="form-panel">
        <div class="form-panel__heading">
          <h3 class="form-panel__title">1. Dados Gerais</h3>
        </div>
        <div class="form-grid">
          <label class="form-label form-grid__wide">Título
            <input v-model="form.titulo" :disabled="semPermissaoDeEdicao" :class="[input, 'form-field--emphasis']" placeholder="Ex.: ADPD 2026/1 – Avaliação Docente" />
          </label>
          <label class="form-label form-grid__wide">Descrição
            <textarea v-model="form.descricao" :disabled="semPermissaoDeEdicao" rows="2" :class="input" placeholder="Orientações exibidas ao respondente"></textarea>
          </label>
          <label class="form-label">Ciclo de referência
            <input v-model="form.cicloReferencia" :disabled="semPermissaoDeEdicao" :class="input" placeholder="2026/1" />
          </label>
          <label class="form-label">Início da aplicação
            <input v-model="form.inicioAplicacao" :disabled="semPermissaoDeEdicao" type="datetime-local" :class="input" />
          </label>
          <label class="form-label">Término da aplicação
            <input v-model="form.fimAplicacao" :disabled="semPermissaoDeEdicao" type="datetime-local" :class="input" />
          </label>
          <label class="form-label form-grid__wide">O que será avaliado
            <select v-model="form.tipoEntidadeAvaliadaId" :disabled="somenteLeitura" :class="input" @change="contextoAlterado">
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
      <div class="form-panel form-panel--respondents">
        <div class="form-panel__heading">
          <h3 class="form-panel__title">2. Quem Responde</h3>
          <p class="form-panel__description">Marque os públicos e, se necessário, o vínculo exigido com o que é avaliado.</p>
        </div>
        <div class="respondent-grid">
          <div v-for="t in tiposRespondentes" :key="t.id"
            :class="['respondent-card', respondenteDe(t.id) ? 'respondent-card--selected' : '']">
            <label class="respondent-card__label">
              <input type="checkbox" class="respondent-card__checkbox" :checked="!!respondenteDe(t.id)" :disabled="somenteLeitura" @change="alternarRespondente(t.id)" />
              {{ t.nome }}
            </label>
            <p v-if="respondenteDe(t.id) && tipos.find(item => item.id === form.tipoEntidadeAvaliadaId)?.codigo === 'DOCENTE'"
              class="respondent-card__hint">Apenas alunos matriculados em uma turma do docente poderão responder.</p>
            <select v-else-if="respondenteDe(t.id)" v-model="respondenteDe(t.id)!.tipoVinculo" :disabled="somenteLeitura" :class="input">
              <option v-for="v in vinculosParaRespondente(t.id)" :key="v.valor" :value="v.valor">{{ v.rotulo }}</option>
            </select>
          </div>
        </div>
        <p v-if="!form.tipoEntidadeAvaliadaId" class="form-panel__help">Selecione o que será avaliado para ver quem pode responder.</p>
        <p v-else-if="!tiposRespondentes.length" class="form-panel__help">Nenhum público está autorizado na matriz de permissões para os tipos selecionados.</p>
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
