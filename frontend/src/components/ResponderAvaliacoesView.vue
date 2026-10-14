<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { ConjuntoOpcao, InstrumentoAv, QuestaoAv } from '../types/instrumentoAvaliacao';
import { avaliacaoApi } from '../services/avaliacaoApi';
import { responderApi, type AvaliacaoDisponivel, type RespostaEnvio } from '../services/responderApi';

const disponiveis = ref<AvaliacaoDisponivel[]>([]);
const instrumento = ref<InstrumentoAv | null>(null);
const selecionada = ref<AvaliacaoDisponivel | null>(null);
const conjuntos = ref<ConjuntoOpcao[]>([]);
const escolhas = ref<Record<string, string>>({});
const multiplas = ref<Record<string, string[]>>({});
const textos = ref<Record<string, string>>({});
const carregando = ref(false);
const enviando = ref(false);
const erro = ref('');
const sucesso = ref('');

function opcoes(q: QuestaoAv) {
  return conjuntos.value.find(c => c.id === q.conjuntoOpcaoId)?.opcoes || [];
}

async function carregar() {
  carregando.value = true;
  erro.value = '';
  try {
    const [lista, catalogo] = await Promise.all([responderApi.listar(), avaliacaoApi.conjuntos()]);
    disponiveis.value = lista;
    if (catalogo.ok) conjuntos.value = catalogo.dados || [];
    else erro.value = catalogo.erro || 'Não foi possível carregar as opções das questões.';
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Não foi possível carregar as avaliações.';
  } finally {
    carregando.value = false;
  }
}

onMounted(carregar);

async function abrir(item: AvaliacaoDisponivel) {
  erro.value = '';
  sucesso.value = '';
  carregando.value = true;
  try {
    instrumento.value = await responderApi.buscar(item.instrumentoId, item.alvoId);
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
      alvoId: selecionada.value.alvoId, respostas
    });
    instrumento.value = null;
    selecionada.value = null;
    sucesso.value = 'Avaliação enviada com sucesso.';
    await carregar();
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Não foi possível enviar a avaliação.';
  } finally {
    enviando.value = false;
  }
}
</script>

<template>
  <div class="space-y-5">
    <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6">
      <h2 class="text-lg font-bold text-slate-900">Responder avaliações</h2>
      <p class="text-xs text-slate-500 mt-1">Avaliações publicadas para seu perfil durante o período de aplicação.</p>
    </div>

    <p v-if="erro" role="alert" class="bg-rose-50 border border-rose-200 rounded-xl p-4 text-sm text-rose-800">{{ erro }}</p>
    <p v-if="sucesso" role="status" class="bg-emerald-50 border border-emerald-200 rounded-xl p-4 text-sm text-emerald-800">{{ sucesso }}</p>
    <p v-if="carregando" class="text-sm text-slate-500">Carregando avaliações…</p>

    <template v-if="!instrumento">
      <p v-if="!carregando && disponiveis.length === 0" class="bg-white border border-slate-200 rounded-xl p-6 text-sm text-slate-500">
        Nenhuma avaliação disponível para seu perfil neste período.
      </p>
      <div v-for="item in disponiveis" :key="`${item.instrumentoId}:${item.alvoId}`"
        class="bg-white border border-slate-200 rounded-xl p-5 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h3 class="font-bold text-slate-900">{{ item.titulo }}</h3>
          <p v-if="item.descricao" class="text-sm text-slate-600 mt-1">{{ item.descricao }}</p>
          <p class="text-xs text-slate-500 mt-1">{{ item.modulo }} · {{ item.alvoNome }}</p>
        </div>
        <span v-if="item.respondido" class="text-xs font-bold text-emerald-700">Respondida</span>
        <button v-else type="button" :disabled="carregando" @click="abrir(item)"
          class="bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white text-xs font-bold px-4 py-2.5 rounded-xl">
          Responder
        </button>
      </div>
    </template>

    <template v-else>
      <div class="bg-white border border-slate-200 rounded-xl p-5">
        <button type="button" @click="instrumento = null; selecionada = null; erro = ''"
          class="text-xs font-bold text-emerald-700 hover:underline">← Voltar às avaliações</button>
        <h3 class="text-lg font-bold text-slate-900 mt-3">{{ instrumento.titulo }}</h3>
        <p v-if="instrumento.descricao" class="text-sm text-slate-600 mt-1">{{ instrumento.descricao }}</p>
      </div>
      <section v-for="(pagina, pi) in instrumento.paginas || []" :key="pagina.id || pi"
        class="bg-slate-100 border border-slate-200 rounded-xl p-5">
        <p class="text-[10px] uppercase font-bold text-slate-500">Página {{ pi + 1 }}</p>
        <h4 class="font-bold text-slate-900 mt-1">{{ pagina.titulo }}</h4>
        <p v-if="pagina.descricao" class="text-sm text-slate-600 mt-1">{{ pagina.descricao }}</p>
      </section>
      <section v-for="(sessao, si) in instrumento.sessoes || []" :key="sessao.id || si"
        class="bg-white border border-slate-200 rounded-xl p-5 space-y-5">
        <div>
          <h4 class="font-bold text-slate-900">{{ sessao.titulo }}</h4>
          <p v-if="sessao.descricao" class="text-sm text-slate-500">{{ sessao.descricao }}</p>
        </div>
        <fieldset v-for="(q, qi) in sessao.questoes" :key="q.sessaoQuestaoId || qi" class="border-t border-slate-100 pt-4 space-y-2">
          <legend class="font-semibold text-sm text-slate-800">{{ qi + 1 }}. {{ q.enunciado }} <span v-if="q.obrigatoria" class="text-rose-600">*</span></legend>
          <textarea v-if="q.tipo === 'DESCRITIVA'" v-model="textos[q.sessaoQuestaoId!]" rows="4"
            class="w-full border border-slate-200 rounded-lg p-3 text-sm" placeholder="Escreva sua resposta"></textarea>
          <template v-else>
            <label v-for="op in opcoes(q)" :key="op.id" class="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
              <input v-if="q.tipo === 'MULTIPLA_ESCOLHA'" v-model="multiplas[q.sessaoQuestaoId!]" type="checkbox" :value="op.id" />
              <input v-else v-model="escolhas[q.sessaoQuestaoId!]" type="radio" :name="q.sessaoQuestaoId" :value="op.id" />
              {{ op.texto }}
            </label>
          </template>
        </fieldset>
      </section>
      <button type="button" :disabled="enviando || carregando" @click="enviar"
        class="bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-bold text-sm px-5 py-3 rounded-xl">
        {{ enviando ? 'Enviando…' : 'Enviar avaliação' }}
      </button>
    </template>
  </div>
</template>
