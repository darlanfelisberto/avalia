<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { ConjuntoOpcaoDTO, InstrumentoAvaliativoDTO, SessaoQuestaoDTO } from '../types/instrumentoAvaliacao';
import { responderApi, type AvaliacaoDisponivel, type RespostaEnvio } from '../services/responderApi';

const disponiveis = ref<AvaliacaoDisponivel[]>([]);
const instrumento = ref<InstrumentoAvaliativoDTO | null>(null);
const selecionada = ref<AvaliacaoDisponivel | null>(null);
const conjuntos = ref<ConjuntoOpcaoDTO[]>([]);
const escolhas = ref<Record<string, string>>({});
const multiplas = ref<Record<string, string[]>>({});
const textos = ref<Record<string, string>>({});
const carregando = ref(false);
const enviando = ref(false);
const erro = ref('');
const sucesso = ref('');
const grupoSelecionado = ref<string | null>(null);
const alvosMarcados = ref<string[]>([]);
const filaAlvos = ref<AvaliacaoDisponivel[]>([]);

const grupos = computed(() => {
  const mapa = new Map<string, { chave: string; instrumentoId: string; titulo: string; descricao?: string | null; contextoNome?: string | null; alvos: AvaliacaoDisponivel[] }>();
  for (const item of disponiveis.value) {
    const chave = `${item.instrumentoId}:${item.contextoId || 'geral'}`;
    let grupo = mapa.get(chave);
    if (!grupo) {
      grupo = { chave, instrumentoId: item.instrumentoId, titulo: item.titulo, descricao: item.descricao, contextoNome: item.contextoNome, alvos: [] };
      mapa.set(chave, grupo);
    }
    grupo.alvos.push(item);
  }
  return [...mapa.values()];
});

function opcoes(q: SessaoQuestaoDTO) {
  return q.opcoes?.length ? q.opcoes : conjuntos.value.find(c => c.id === q.conjuntoOpcaoId)?.opcoes || [];
}

async function carregar() {
  carregando.value = true;
  erro.value = '';
  try {
    disponiveis.value = await responderApi.listar();
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Não foi possível carregar as avaliações.';
  } finally {
    carregando.value = false;
  }
}

onMounted(carregar);

async function abrir(item: AvaliacaoDisponivel, manterSucesso = false) {
  erro.value = '';
  if (!manterSucesso) sucesso.value = '';
  carregando.value = true;
  try {
    const [instrumentoCarregado, conjuntosCarregados] = await Promise.all([
      responderApi.buscar(item.instrumentoId, item.alvoId, item.contextoId),
      responderApi.conjuntos(item.instrumentoId, item.alvoId, item.contextoId)
    ]);
    instrumento.value = instrumentoCarregado;
    conjuntos.value = conjuntosCarregados;
    selecionada.value = item;
    escolhas.value = {};
    multiplas.value = {};
    textos.value = {};
    for (const s of instrumento.value.sessoes || []) {
      for (const q of s.questoes) {
        if (q.sessaoQuestaoId) multiplas.value[q.sessaoQuestaoId] = [];
      }
    }
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Não foi possível abrir a avaliação.';
  } finally {
    carregando.value = false;
  }
}

async function enviar() {
  if (!instrumento.value || !selecionada.value) return;
  erro.value = '';
  const respostas: RespostaEnvio['respostas'] = [];
  for (const sessao of instrumento.value.sessoes || []) {
    for (const q of sessao.questoes) {
      const id = q.sessaoQuestaoId;
      if (!id) {
        erro.value = 'Questão sem identificação. Atualize a página e tente novamente.';
        return;
      }
      const opcaoIds = q.tipo === 'MULTIPLA_ESCOLHA'
        ? (multiplas.value[id] || [])
        : escolhas.value[id] ? [escolhas.value[id]] : [];
      const texto = (textos.value[id] || '').trim();
      if (q.obrigatoria && (q.tipo === 'DESCRITIVA' ? !texto : opcaoIds.length === 0)) {
        erro.value = `Responda a questão obrigatória: ${q.enunciado}`;
        return;
      }
      if (texto || opcaoIds.length) respostas.push({ sessaoQuestaoId: id, opcaoIds, texto });
    }
  }
  enviando.value = true;
  try {
    await responderApi.enviar(selecionada.value.instrumentoId, {
      alvoId: selecionada.value.alvoId, contextoId: selecionada.value.contextoId, respostas
    });
    instrumento.value = null;
    selecionada.value = null;
    sucesso.value = 'Avaliação enviada com sucesso.';
    await carregar();
    const proximo = filaAlvos.value.shift();
    if (proximo) await abrir(proximo, true);
    else {
      grupoSelecionado.value = null;
      alvosMarcados.value = [];
    }
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Não foi possível enviar a avaliação.';
  } finally {
    enviando.value = false;
  }
}

function selecionarGrupo(chave: string) {
  grupoSelecionado.value = chave;
  alvosMarcados.value = [];
  erro.value = sucesso.value = '';
}

function alternarAlvo(chave: string) {
  const idx = alvosMarcados.value.indexOf(chave);
  if (idx >= 0) alvosMarcados.value.splice(idx, 1);
  else alvosMarcados.value.push(chave);
}

function responderAlvosSelecionados() {
  const grupo = grupos.value.find(g => g.chave === grupoSelecionado.value);
  if (!grupo) return;
  const escolhidos = grupo.alvos.filter(a => alvosMarcados.value.includes(`${a.alvoId}:${a.contextoId || ''}`) && !a.respondido);
  if (!escolhidos.length) return;
  filaAlvos.value = escolhidos.slice(1);
  void abrir(escolhidos[0]);
}

function voltarASelecao() {
  instrumento.value = null;
  selecionada.value = null;
  filaAlvos.value = [];
  erro.value = '';
}
</script>

<template>
  <div class="survey-response">
    <div class="surface-card survey-response__intro">
      <h2 class="survey-response__title">Responder avaliações</h2>
      <p class="survey-response__description">Avaliações publicadas para seu perfil durante o período de aplicação.</p>
    </div>

    <p v-if="erro" role="alert" class="alert alert--error">{{ erro }}</p>
    <p v-if="sucesso" role="status" class="alert alert--success">{{ sucesso }}</p>
    <p v-if="carregando" class="loading-message">Carregando avaliações…</p>

    <template v-if="!instrumento">
      <p v-if="!carregando && grupos.length === 0" class="empty-state">
        Nenhuma avaliação disponível para seu perfil neste período.
      </p>
      <div v-if="grupoSelecionado" class="surface-card survey-selection">
        <button type="button" @click="grupoSelecionado = null; alvosMarcados = []" class="text-action text-action--primary">← Voltar às avaliações</button>
        <div>
          <h3 class="survey-card__title">{{ grupos.find(g => g.chave === grupoSelecionado)?.titulo }}</h3>
          <p class="survey-card__hint">{{ grupos.find(g => g.chave === grupoSelecionado)?.contextoNome || 'Avaliação institucional' }} · selecione os alvos antes de responder.</p>
        </div>
        <label v-for="item in grupos.find(g => g.chave === grupoSelecionado)?.alvos" :key="`${item.alvoId}:${item.contextoId || ''}`"
          class="survey-selection__target">
          <span class="survey-selection__target-name">
            <input v-if="!item.respondido" type="checkbox" :checked="alvosMarcados.includes(`${item.alvoId}:${item.contextoId || ''}`)"
              @change="alternarAlvo(`${item.alvoId}:${item.contextoId || ''}`)" />
            <span>{{ item.alvoNome }}<span v-if="item.alvoTipoCodigo" class="survey-selection__target-type"> · {{ item.alvoTipoCodigo }}</span></span>
          </span>
          <span v-if="item.respondido" class="survey-selection__answered">Respondida</span>
        </label>
        <button type="button" :disabled="!alvosMarcados.length || carregando" @click="responderAlvosSelecionados"
          class="button button--primary survey-response__button">
          Responder aos alvos selecionados
        </button>
      </div>
      <div v-else v-for="grupo in grupos" :key="grupo.chave"
        class="surface-card survey-card survey-card--summary">
        <div>
          <h3 class="survey-card__title">{{ grupo.titulo }}</h3>
          <p v-if="grupo.descricao" class="survey-card__description">{{ grupo.descricao }}</p>
          <p v-if="grupo.contextoNome" class="survey-card__hint">Turma: {{ grupo.contextoNome }}</p>
          <p class="survey-card__hint">{{ grupo.alvos.filter(a => !a.respondido).length }} alvo(s) disponíveis</p>
        </div>
        <button type="button" :disabled="carregando || grupo.alvos.every(a => a.respondido)" @click="selecionarGrupo(grupo.chave)"
          class="button button--primary survey-response__button">
          Selecionar alvos
        </button>
      </div>
    </template>

    <template v-else>
      <div class="surface-card survey-response__instrument">
        <button type="button" @click="voltarASelecao"
          class="text-action text-action--primary">← Voltar às avaliações</button>
        <h3 class="survey-response__instrument-title">{{ instrumento.titulo }}</h3>
        <p v-if="instrumento.descricao" class="survey-card__description">{{ instrumento.descricao }}</p>
        <p v-if="selecionada" class="survey-response__target-caption">
          Avaliando: {{ selecionada.alvoNome }}<span v-if="selecionada.contextoNome"> · Turma: {{ selecionada.contextoNome }}</span>
        </p>
      </div>
      <section v-for="(pagina, pi) in instrumento.paginas || []" :key="pagina.id || pi"
        class="surface-card surface-card--subtle survey-page">
        <p class="survey-page__number">Página {{ pi + 1 }}</p>
        <h4 class="survey-page__title">{{ pagina.titulo }}</h4>
        <p v-if="pagina.descricao" class="survey-page__description">{{ pagina.descricao }}</p>
      </section>
      <section v-for="(sessao, si) in instrumento.sessoes || []" :key="sessao.id || si"
        class="surface-card survey-session">
        <div>
          <h4 class="survey-session__title">{{ sessao.titulo }}</h4>
          <p v-if="sessao.descricao" class="survey-session__description">{{ sessao.descricao }}</p>
        </div>
        <fieldset v-for="(q, qi) in sessao.questoes" :key="q.sessaoQuestaoId || qi" class="survey-question">
          <legend class="survey-question__title">{{ qi + 1 }}. {{ q.enunciado }} <span v-if="q.obrigatoria" class="survey-question__required">*</span></legend>
          <textarea v-if="q.tipo === 'DESCRITIVA'" v-model="textos[q.sessaoQuestaoId!]" rows="4"
            class="form-field survey-answer" placeholder="Escreva sua resposta"></textarea>
          <template v-else>
            <label v-for="op in opcoes(q)" :key="op.id" class="survey-option">
              <input v-if="q.tipo === 'MULTIPLA_ESCOLHA'" v-model="multiplas[q.sessaoQuestaoId!]" type="checkbox" :value="op.id" />
              <input v-else v-model="escolhas[q.sessaoQuestaoId!]" type="radio" :name="q.sessaoQuestaoId" :value="op.id" />
              {{ op.texto }}
            </label>
          </template>
        </fieldset>
      </section>
      <button type="button" :disabled="enviando || carregando" @click="enviar"
        class="button button--primary survey-response__submit">
        {{ enviando ? 'Enviando…' : 'Enviar avaliação' }}
      </button>
    </template>
  </div>
</template>
