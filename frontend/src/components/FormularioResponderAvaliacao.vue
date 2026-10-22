<script setup lang="ts">
import type { InstrumentoAvaliativoDTO } from '../types/instrumentoAvaliacao';

defineProps<{
  instrumento: InstrumentoAvaliativoDTO;
}>();

defineEmits<{
  voltar: [];
}>();

const respostas = defineModel<Record<string, any>>('respostas', { required: true });
</script>

<template>
  <div class="surface-card survey-response__instrument">
    <button type="button" class="text-action text-action--primary" @click="$emit('voltar')">← Voltar às avaliações</button>
    <h3 class="survey-response__instrument-title">{{ instrumento.titulo }}</h3>
    <p v-if="instrumento.descricao" class="survey-card__description">{{ instrumento.descricao }}</p>
  </div>

  <section v-for="(pagina, pi) in instrumento.paginas || []" :key="pagina.id || pi"
    class="surface-card surface-card--subtle survey-page">
    <h4 class="survey-page__title">{{ pagina.titulo }}</h4>
    <p v-if="pagina.descricao" class="survey-page__description">{{ pagina.descricao }}</p>
  </section>

  <section v-for="(sessao, si) in instrumento.sessoes || []" :key="sessao.id || si"
    class="surface-card survey-session">
    <h4 class="survey-session__title">{{ sessao.titulo }}</h4>
    <p v-if="sessao.descricao" class="survey-session__description">{{ sessao.descricao }}</p>
    <fieldset v-for="(questao, qi) in sessao.questoes" :key="questao.sessaoQuestaoId || qi" class="survey-question">
      <legend class="survey-question__title">
        {{ qi + 1 }}. {{ questao.enunciado }}
        <span v-if="questao.obrigatoria" class="survey-question__required" aria-hidden="true">*</span>
      </legend>
      <textarea v-if="questao.tipo === 'DESCRITIVA'" v-model="respostas[questao.sessaoQuestaoId!]"
        rows="4" class="form-field survey-answer" placeholder="Escreva sua resposta"></textarea>
      <template v-else>
        <label v-for="opcao in questao.opcoes || []" :key="opcao.id" class="survey-option">
          <input v-if="questao.tipo === 'MULTIPLA_ESCOLHA'" v-model="respostas[questao.sessaoQuestaoId!]"
            type="checkbox" :value="opcao.id" />
          <input v-else v-model="respostas[questao.sessaoQuestaoId!]"
            type="radio" :name="questao.sessaoQuestaoId" :value="opcao.id" />
          {{ opcao.texto }}
        </label>
      </template>
    </fieldset>
  </section>
  <p class="survey-card__hint">As respostas podem ser preenchidas, mas o envio ainda não está disponível neste fluxo do backend.</p>
</template>
