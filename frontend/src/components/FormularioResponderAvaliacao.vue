<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import type { InstrumentoAvaliativoDTO } from '../types/instrumentoAvaliacao';
import { responderApi } from '../services/responderApi';

const route = useRoute();
const instrumento = ref<InstrumentoAvaliativoDTO | null>(null);
const respostas = ref<Record<string, any>>({});
const carregando = ref(false);
const erro = ref('');
let ultimaRequisicao = 0;

watch(() => route.params.instrumentoId, async parametro => {
  const requisicao = ++ultimaRequisicao;
  instrumento.value = null;
  respostas.value = {};
  erro.value = '';
  if (typeof parametro !== 'string' || !parametro) {
    erro.value = 'Instrumento inválido.';
    return;
  }

  carregando.value = true;
  try {
    const [detalhe, conjuntos] = await Promise.all([
      responderApi.formulario(parametro),
      responderApi.conjuntosFormulario(parametro)
    ]);
    if (requisicao !== ultimaRequisicao) return;

    const porId = new Map(conjuntos.map(conjunto => [conjunto.id, conjunto.opcoes]));
    for (const sessao of detalhe.sessoes || []) {
      for (const questao of sessao.questoes) {
        if (questao.conjuntoOpcaoId) {
          const opcoes = porId.get(questao.conjuntoOpcaoId);
          if (!opcoes) throw new Error(`Opções indisponíveis para a questão: ${questao.enunciado}`);
          questao.opcoes = opcoes;
        }
      }
    }
    instrumento.value = detalhe;
  } catch (e) {
    if (requisicao === ultimaRequisicao) {
      erro.value = e instanceof Error ? e.message : 'Não foi possível carregar o formulário.';
    }
  } finally {
    if (requisicao === ultimaRequisicao) carregando.value = false;
  }
}, { immediate: true });
</script>

<template>
  <div class="survey-response">
    <div class="surface-card survey-response__instrument">
      <RouterLink to="/responder" class="text-action text-action--primary">← Voltar às avaliações</RouterLink>
      <template v-if="instrumento">
        <h2 class="survey-response__instrument-title">{{ instrumento.titulo }}</h2>
        <p v-if="instrumento.descricao" class="survey-card__description">{{ instrumento.descricao }}</p>
      </template>
    </div>

    <p v-if="carregando" class="loading-message">Carregando formulário…</p>
    <p v-if="erro" role="alert" class="alert alert--error">{{ erro }}</p>

    <template v-if="instrumento">
      <section v-for="(pagina, pi) in instrumento.paginas || []" :key="pagina.id || pi"
        class="surface-card surface-card--subtle survey-page">
        <h3 class="survey-page__title">{{ pagina.titulo }}</h3>
        <p v-if="pagina.descricao" class="survey-page__description">{{ pagina.descricao }}</p>
      </section>

      <section v-for="(sessao, si) in instrumento.sessoes || []" :key="sessao.id || si"
        class="surface-card survey-session">
        <h3 class="survey-session__title">{{ sessao.titulo }}</h3>
        <p v-if="sessao.descricao" class="survey-session__description">{{ sessao.descricao }}</p>
        <fieldset v-for="(questao, qi) in sessao.questoes" :key="questao.sessaoQuestaoId || qi" class="survey-question">
          <legend class="survey-question__title">
            {{ qi + 1 }}. {{ questao.enunciado }}<span v-if="questao.obrigatoria"
              class="survey-question__required" aria-hidden="true">*</span>
          </legend>
          <textarea v-if="questao.tipo === 'DESCRITIVA'" v-model="respostas[questao.sessaoQuestaoId!]"
            :required="questao.obrigatoria" rows="4" class="form-field survey-answer"
            placeholder="Escreva sua resposta"></textarea>
          <template v-else>
            <label v-for="opcao in questao.opcoes || []" :key="opcao.id" class="survey-option">
              <input v-if="questao.tipo === 'MULTIPLA_ESCOLHA'" v-model="respostas[questao.sessaoQuestaoId!]"
                type="checkbox" :value="opcao.id" />
              <input v-else v-model="respostas[questao.sessaoQuestaoId!]"
                type="radio" :name="questao.sessaoQuestaoId" :value="opcao.id" :required="questao.obrigatoria" />
              {{ opcao.texto }}
            </label>
          </template>
        </fieldset>
      </section>
      <p class="survey-card__hint">O envio das respostas ainda não está disponível.</p>
    </template>
  </div>
</template>
