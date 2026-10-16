<script setup lang="ts">
import { computed } from 'vue';
import type { ConjuntoOpcaoDTO, InstrumentoSessaoPaginaDTO, SessaoQuestaoDTO, InstrumentoSessaoDTO, TipoQuestaoDTO } from '../types/instrumentoAvaliacao';

const paginas = defineModel<InstrumentoSessaoPaginaDTO[]>('paginas', { required: true });
const sessoes = defineModel<InstrumentoSessaoDTO[]>('sessoes', { required: true });

const props = defineProps<{
  conjuntos: ConjuntoOpcaoDTO[];
  somenteLeitura?: boolean;
  podeCriarConjunto?: boolean;
}>();

const emit = defineEmits<{ (e: 'novoConjunto'): void }>();

const TIPOS: { valor: TipoQuestaoDTO; rotulo: string }[] = [
  { valor: 'ESCALA', rotulo: 'Escala (ex.: concordância)' },
  { valor: 'ESCOLHA_UNICA', rotulo: 'Escolha única' },
  { valor: 'MULTIPLA_ESCOLHA', rotulo: 'Múltipla escolha' },
  { valor: 'DESCRITIVA', rotulo: 'Resposta descritiva' }
];

const mapaConjuntos = computed(() => new Map(props.conjuntos.map(c => [c.id, c])));

function mover<T>(lista: T[], i: number, delta: number) {
  const j = i + delta;
  if (j < 0 || j >= lista.length) return;
  [lista[i], lista[j]] = [lista[j], lista[i]];
}

const novaQuestao = (): SessaoQuestaoDTO => ({ enunciado: '', tipo: 'ESCALA', conjuntoOpcaoId: props.conjuntos[0]?.id ?? null, obrigatoria: true, peso: null });
const novaSessao = (): InstrumentoSessaoDTO => ({ titulo: '', descricao: '', questoes: [novaQuestao()] });

function addPagina() {
  paginas.value.push({ titulo: `Página ${paginas.value.length + 1}`, descricao: '' });
}

function addSessao() {
  sessoes.value.push({ ...novaSessao(), titulo: `Sessão ${sessoes.value.length + 1}` });
}

function aoMudarTipo(q: SessaoQuestaoDTO) {
  if (q.tipo === 'DESCRITIVA') q.conjuntoOpcaoId = null;
  else if (!q.conjuntoOpcaoId) q.conjuntoOpcaoId = props.conjuntos[0]?.id ?? null;
}

const input = 'form-field';
const btnIcone = 'icon-button-control';
</script>

<template>
  <div class="structure-editor form-panel">
    <div class="structure-editor__heading">
      <div>
        <h3 class="form-panel__title">3. Estrutura do Questionário</h3>
        <p class="form-panel__description">Páginas e sessões são tipos independentes do instrumento; sessões agrupam as questões.</p>
      </div>
      <div v-if="!somenteLeitura" class="structure-editor__actions">
        <button type="button" @click="addPagina" class="button button--primary structure-editor__add-page">+ Nova página</button>
        <button type="button" @click="addSessao" class="button structure-editor__add-session">+ Nova sessão</button>
      </div>
    </div>

    <p v-if="somenteLeitura" class="structure-editor__readonly">
      A estrutura deste instrumento está disponível somente para leitura.
    </p>
    <section class="structure-section">
      <h4 class="structure-section__title">Páginas</h4>
      <p v-if="paginas.length === 0" class="structure-section__empty">Nenhuma página cadastrada.</p>
      <article v-for="(pag, pi) in paginas" :key="pi" class="page-editor">
      <header class="page-editor__heading">
        <span class="page-editor__badge">Página {{ pi + 1 }}</span>
        <input v-model="pag.titulo" :disabled="somenteLeitura" placeholder="Título da página" :class="[input, 'structure-editor__title-input']" />
        <template v-if="!somenteLeitura">
          <button type="button" :class="btnIcone" :disabled="pi === 0" @click="mover(paginas, pi, -1)">↑</button>
          <button type="button" :class="btnIcone" :disabled="pi === paginas.length - 1" @click="mover(paginas, pi, 1)">↓</button>
          <button type="button" :class="[btnIcone, 'structure-control--remove']" @click="paginas.splice(pi, 1)">Remover</button>
        </template>
      </header>
      <textarea v-model="pag.descricao" :disabled="somenteLeitura" rows="2" placeholder="Descrição da página (opcional)" class="form-field page-editor__description" />
      </article>
    </section>

    <section class="structure-section structure-section--sessions">
      <h4 class="structure-section__title">Sessões e questões</h4>
      <p v-if="sessoes.length === 0" class="structure-section__empty">Nenhuma sessão cadastrada.</p>
      <div v-for="(ses, si) in sessoes" :key="si" class="session-editor">
          <div class="session-editor__heading">
            <span class="session-editor__badge">Sessão {{ si + 1 }}</span>
            <input v-model="ses.titulo" :disabled="somenteLeitura" placeholder="Título da sessão (ex.: Didática)" :class="[input, 'structure-editor__title-input']" />
            <template v-if="!somenteLeitura">
              <button type="button" :class="btnIcone" :disabled="si === 0" @click="mover(sessoes, si, -1)">↑</button>
              <button type="button" :class="btnIcone" :disabled="si === sessoes.length - 1" @click="mover(sessoes, si, 1)">↓</button>
              <button type="button" :class="[btnIcone, 'structure-control--remove']" @click="sessoes.splice(si, 1)">Remover</button>
            </template>
          </div>
          <input v-model="ses.descricao" :disabled="somenteLeitura" placeholder="Descrição / instruções da sessão (opcional)" :class="input" />

          <article v-for="(q, qi) in ses.questoes" :key="qi" class="question-editor">
            <div class="question-editor__heading">
              <span class="question-editor__number">{{ qi + 1 }}.</span>
              <textarea v-model="q.enunciado" :disabled="somenteLeitura" rows="2" placeholder="Enunciado da questão" :class="input"></textarea>
              <div v-if="!somenteLeitura" class="question-editor__move">
                <button type="button" :class="btnIcone" :disabled="qi === 0" @click="mover(ses.questoes, qi, -1)">↑</button>
                <button type="button" :class="btnIcone" :disabled="qi === ses.questoes.length - 1" @click="mover(ses.questoes, qi, 1)">↓</button>
              </div>
            </div>

            <div class="question-editor__settings">
              <select v-model="q.tipo" :disabled="somenteLeitura" @change="aoMudarTipo(q)" :class="[input, 'question-editor__type']">
                <option v-for="t in TIPOS" :key="t.valor" :value="t.valor">{{ t.rotulo }}</option>
              </select>
              <select v-if="q.tipo !== 'DESCRITIVA'" v-model="q.conjuntoOpcaoId" :disabled="somenteLeitura" :class="[input, 'question-editor__options']">
                <option :value="null" disabled>Selecione as opções…</option>
                <option v-for="c in conjuntos" :key="c.id" :value="c.id">{{ c.nome }}</option>
              </select>
              <input v-model="q.assunto" :disabled="somenteLeitura" placeholder="Assunto (opcional)"
                :class="[input, 'question-editor__subject', q.tipo === 'DESCRITIVA' ? 'question-editor__subject--descriptive' : '']" />
              <input v-model.number="q.peso" :disabled="somenteLeitura" type="number" step="0.5" min="0" placeholder="Peso (opcional)" :class="[input, 'question-editor__weight']" />
            </div>

            <div class="question-editor__footer">
              <div class="question-editor__meta">
                <label class="question-editor__required-label">
                  <input v-model="q.obrigatoria" :disabled="somenteLeitura" type="checkbox" class="question-editor__checkbox" /> Obrigatória
                </label>
                <span v-if="q.tipo !== 'DESCRITIVA' && q.conjuntoOpcaoId" class="question-editor__options-preview">
                  {{ mapaConjuntos.get(q.conjuntoOpcaoId)?.opcoes.map(o => o.texto).join(' · ') }}
                </span>
              </div>
              <button v-if="!somenteLeitura" type="button" class="button button--danger question-editor__remove" @click="ses.questoes.splice(qi, 1)">
                <span aria-hidden="true">×</span> Remover questão
              </button>
            </div>
          </article>

          <div v-if="!somenteLeitura" class="session-editor__actions">
            <button type="button" class="button button--add-question" @click="ses.questoes.push(novaQuestao())">
              + Questão
            </button>
            <button v-if="podeCriarConjunto" type="button" class="button button--add-options" @click="emit('novoConjunto')">
              + Novo conjunto de opções
            </button>
          </div>
      </div>
    </section>
  </div>
</template>
