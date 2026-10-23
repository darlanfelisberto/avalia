<script setup lang="ts">
import { ref } from 'vue';
import type { ConjuntoOpcaoDTO, InstrumentoAvaliativoDTO, SessaoQuestaoDTO } from '../types/instrumentoAvaliacao';

const props = defineProps<{
  instrumento: InstrumentoAvaliativoDTO;
  conjuntos: ConjuntoOpcaoDTO[];
}>();

const emit = defineEmits<{ voltar: [] }>();

const escolhas = ref<Record<string, string>>({});
const multiplas = ref<Record<string, string[]>>({});
const textos = ref<Record<string, string>>({});
const erro = ref('');
const concluida = ref(false);

function chaveQuestao(q: SessaoQuestaoDTO, sessaoIndex: number, questaoIndex: number): string {
  return q.sessaoQuestaoId || q.questaoId || `sessao-${sessaoIndex}-questao-${questaoIndex}`;
}

function opcoes(q: SessaoQuestaoDTO) {
  return q.opcoes?.length ? q.opcoes : props.conjuntos.find(c => c.id === q.conjuntoOpcaoId)?.opcoes || [];
}

function finalizar() {
  erro.value = '';
  for (const [si, sessao] of (props.instrumento.sessoes || []).entries()) {
    for (const [qi, q] of sessao.questoes.entries()) {
      if (!q.obrigatoria) continue;
      const chave = chaveQuestao(q, si, qi);
      const respondida = q.tipo === 'DESCRITIVA'
        ? !!textos.value[chave]?.trim()
        : q.tipo === 'MULTIPLA_ESCOLHA'
          ? (multiplas.value[chave]?.length || 0) > 0
          : !!escolhas.value[chave];
      if (!respondida) {
        erro.value = `Responda a questão obrigatória: ${q.enunciado}`;
        return;
      }
    }
  }
  concluida.value = true;
}
</script>

<template>
  <div class="page-stack page-stack--compact">
    <div class="panel panel--padded">
      <button type="button" @click="emit('voltar')" class="text-action text-action--primary">
        ← Voltar à edição
      </button>
      <p class="survey-preview__badge">
        Modo de visualização · respostas não serão salvas
      </p>
      <h2 class="survey-preview__title">{{ instrumento.titulo || 'Prévia do instrumento' }}</h2>
      <p v-if="instrumento.descricao" class="survey-preview__description">{{ instrumento.descricao }}</p>
      <p v-if="concluida" role="status" class="alert alert--success survey-preview__complete">
        Prévia concluída. Nenhuma resposta foi salva.
      </p>
    </div>

    <p v-if="erro" role="alert" class="alert alert--error">{{ erro }}</p>

    <section v-for="(pagina, pi) in instrumento.paginas || []" :key="pagina.id || pi"
      class="panel panel--subtle panel--padded">
      <p class="survey-page__number">Página {{ pi + 1 }}</p>
      <h3 class="survey-page__title">{{ pagina.titulo }}</h3>
      <p v-if="pagina.descricao" class="survey-page__description">{{ pagina.descricao }}</p>
    </section>

    <section v-for="(sessao, si) in instrumento.sessoes || []" :key="sessao.id || si"
      class="panel panel--padded survey-session">
      <div>
        <h3 class="survey-session__title">{{ sessao.titulo }}</h3>
        <p v-if="sessao.descricao" class="survey-session__description">{{ sessao.descricao }}</p>
      </div>
      <fieldset v-for="(q, qi) in sessao.questoes" :key="chaveQuestao(q, si, qi)"
        class="survey-question">
        <legend class="survey-question__title">
          {{ qi + 1 }}. {{ q.enunciado }} <span v-if="q.obrigatoria" class="survey-question__required">*</span>
        </legend>
        <textarea v-if="q.tipo === 'DESCRITIVA'" v-model="textos[chaveQuestao(q, si, qi)]" rows="4"
          class="form-field survey-answer" placeholder="Escreva sua resposta"></textarea>
        <template v-else>
          <label v-for="op in opcoes(q)" :key="op.id || op.texto" class="survey-option">
            <input v-if="q.tipo === 'MULTIPLA_ESCOLHA'" v-model="multiplas[chaveQuestao(q, si, qi)]" type="checkbox" :value="op.id || op.texto" />
            <input v-else v-model="escolhas[chaveQuestao(q, si, qi)]" type="radio" :name="chaveQuestao(q, si, qi)" :value="op.id || op.texto" />
            {{ op.texto }}
          </label>
          <p v-if="opcoes(q).length === 0" class="survey-question__warning">Esta questão ainda não tem opções cadastradas.</p>
        </template>
      </fieldset>
    </section>

    <p v-if="instrumento.sessoes.length === 0" class="panel empty-state">
      Adicione sessões e questões para visualizar as respostas.
    </p>

    <button type="button" :disabled="concluida" @click="finalizar"
      class="button button--primary survey-preview__submit">
      {{ concluida ? 'Prévia concluída' : 'Concluir prévia' }}
    </button>
  </div>
</template>
