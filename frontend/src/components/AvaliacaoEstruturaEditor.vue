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

const conjuntosReutilizaveis = computed(() => props.conjuntos.filter(c => c.reutilizavel === true));
const mapaConjuntos = computed(() => new Map(conjuntosReutilizaveis.value.map(c => [c.id, c])));

function mover<T>(lista: T[], i: number, delta: number) {
  const j = i + delta;
  if (j < 0 || j >= lista.length) return;
  [lista[i], lista[j]] = [lista[j], lista[i]];
}

const novaOpcao = () => ({ texto: '', valor: null, correta: false });
const novaQuestao = (): SessaoQuestaoDTO => ({
  enunciado: '', tipo: 'ESCALA', conjuntoOpcaoId: conjuntosReutilizaveis.value[0]?.id ?? null,
  opcoes: [], obrigatoria: true, peso: null
});
const novaSessao = (): InstrumentoSessaoDTO => ({ titulo: '', descricao: '', questoes: [novaQuestao()] });

function addPagina() {
  paginas.value.push({ titulo: `Página ${paginas.value.length + 1}`, descricao: '' });
}

function addSessao() {
  sessoes.value.push({ ...novaSessao(), titulo: `Sessão ${sessoes.value.length + 1}` });
}

function aoMudarTipo(q: SessaoQuestaoDTO) {
  if (q.tipo === 'DESCRITIVA') {
    q.conjuntoOpcaoId = null;
    q.opcoes = [];
  } else if (q.tipo === 'ESCALA') {
    q.conjuntoOpcaoId = conjuntosReutilizaveis.value.find(c => c.id === q.conjuntoOpcaoId)?.id
      ?? conjuntosReutilizaveis.value[0]?.id ?? null;
    q.opcoes = [];
  } else {
    if (!q.opcoes?.length) {
      const conjunto = mapaConjuntos.value.get(q.conjuntoOpcaoId ?? undefined);
      q.opcoes = conjunto ? conjunto.opcoes.map(o => ({ texto: o.texto, valor: o.valor ?? null, correta: o.correta }))
        : [novaOpcao(), novaOpcao()];
    }
    q.conjuntoOpcaoId = null;
  }
}

function adicionarOpcao(q: SessaoQuestaoDTO) {
  (q.opcoes ??= []).push(novaOpcao());
}

function atualizarValorOpcao(opcao: NonNullable<SessaoQuestaoDTO['opcoes']>[number], evento: Event) {
  const valor = (evento.target as HTMLInputElement).value;
  opcao.valor = valor === '' ? null : Number(valor);
}

const input = 'form-field';
const btnIcone = 'icon-button-control';
</script>

<template>
  <div class="panel panel--padded structure-editor form-panel">
    <div class="structure-editor__heading">
      <div>
        <h3 class="form-panel__title">Estrutura do Questionário</h3>
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
        <label class="form-label structure-editor__title-field"><span class="form-label__caption">Título da página <span class="required-mark" aria-hidden="true">*</span></span>
          <input v-model="pag.titulo" :disabled="somenteLeitura" required placeholder="Informe o título" :class="[input, 'structure-editor__title-input']" />
        </label>
        <template v-if="!somenteLeitura">
          <button type="button" :class="btnIcone" :disabled="pi === 0" @click="mover(paginas, pi, -1)">↑</button>
          <button type="button" :class="btnIcone" :disabled="pi === paginas.length - 1" @click="mover(paginas, pi, 1)">↓</button>
          <button type="button" :class="[btnIcone, 'structure-control--remove']" @click="paginas.splice(pi, 1)">Remover</button>
        </template>
      </header>
      <textarea v-model="pag.descricao" :disabled="somenteLeitura" rows="2" placeholder="Descrição da página" class="form-field page-editor__description" />
      </article>
    </section>

    <section class="structure-section structure-section--sessions">
      <h4 class="structure-section__title">Sessões e questões</h4>
      <p v-if="sessoes.length === 0" class="structure-section__empty">Nenhuma sessão cadastrada.</p>
      <div v-for="(ses, si) in sessoes" :key="si" class="session-editor">
          <div class="session-editor__heading">
            <span class="session-editor__badge">Sessão {{ si + 1 }}</span>
            <label class="form-label structure-editor__title-field"><span class="form-label__caption">Título da sessão <span class="required-mark" aria-hidden="true">*</span></span>
              <input v-model="ses.titulo" :disabled="somenteLeitura" required placeholder="Ex.: Didática" :class="[input, 'structure-editor__title-input']" />
            </label>
            <template v-if="!somenteLeitura">
              <button type="button" :class="btnIcone" :disabled="si === 0" @click="mover(sessoes, si, -1)">↑</button>
              <button type="button" :class="btnIcone" :disabled="si === sessoes.length - 1" @click="mover(sessoes, si, 1)">↓</button>
              <button type="button" :class="[btnIcone, 'structure-control--remove']" @click="sessoes.splice(si, 1)">Remover</button>
            </template>
          </div>
          <input v-model="ses.descricao" :disabled="somenteLeitura" placeholder="Descrição / instruções da sessão" :class="input" />

          <article v-for="(q, qi) in ses.questoes" :key="qi" class="question-editor">
            <div class="question-editor__heading">
              <span class="question-editor__number">{{ qi + 1 }}.</span>
              <label class="form-label question-editor__prompt"><span class="form-label__caption">Enunciado <span class="required-mark" aria-hidden="true">*</span></span>
                <textarea v-model="q.enunciado" :disabled="somenteLeitura" required rows="2" placeholder="Escreva a pergunta" :class="input"></textarea>
              </label>
              <div v-if="!somenteLeitura" class="question-editor__move">
                <button type="button" :class="btnIcone" :disabled="qi === 0" @click="mover(ses.questoes, qi, -1)">↑</button>
                <button type="button" :class="btnIcone" :disabled="qi === ses.questoes.length - 1" @click="mover(ses.questoes, qi, 1)">↓</button>
              </div>
            </div>

            <div class="question-editor__settings">
              <label :class="['form-label', 'question-editor__subject', q.tipo !== 'ESCALA' ? 'question-editor__subject--without-set' : '']">Assunto
                <input v-model="q.assunto" :disabled="somenteLeitura" placeholder="Ex.: Didática" :class="input" />
              </label>
              <label class="form-label question-editor__weight">Peso
                <input v-model.number="q.peso" :disabled="somenteLeitura" type="number" step="0.5" min="0" placeholder="Ex.: 1" :class="input" />
              </label>
              <label class="form-label question-editor__type"><span class="form-label__caption">Tipo de questão <span class="required-mark" aria-hidden="true">*</span></span>
                <select v-model="q.tipo" :disabled="somenteLeitura" required @change="aoMudarTipo(q)" :class="input">
                  <option v-for="t in TIPOS" :key="t.valor" :value="t.valor">{{ t.rotulo }}</option>
                </select>
              </label>
              <label v-if="q.tipo === 'ESCALA'" class="form-label question-editor__set"><span class="form-label__caption">Conjunto de opções<span class="required-mark" aria-hidden="true">*</span></span>
                <select v-model="q.conjuntoOpcaoId" :disabled="somenteLeitura" required @change="q.opcoes = []" :class="input">
                  <option :value="null" disabled>Selecione as opções…</option>
                  <option v-for="c in conjuntosReutilizaveis" :key="c.id" :value="c.id">{{ c.nome }}</option>
                </select>
              </label>

            </div>

            <p v-if="q.tipo === 'ESCALA' && conjuntosReutilizaveis.length === 0" class="question-editor__options-warning">
              Cadastre um conjunto de opções reutilizável para usar nesta escala.
            </p>

            <div v-if="q.tipo === 'ESCOLHA_UNICA' || q.tipo === 'MULTIPLA_ESCOLHA'" class="question-options">
              <p class="question-options__title">Opções desta questão</p>
              <div v-for="(opcao, oi) in q.opcoes" :key="oi" class="question-options__row">
                <label class="form-label"><span class="form-label__caption">Texto da opção {{ oi + 1 }} <span class="required-mark" aria-hidden="true">*</span></span>
                  <input v-model="opcao.texto" :disabled="somenteLeitura" required class="form-field" placeholder="Ex.: Sim" />
                </label>
                <label class="form-label">Valor
                  <input :value="opcao.valor ?? ''" @input="atualizarValorOpcao(opcao, $event)" :disabled="somenteLeitura" type="number" step="0.5" class="form-field" placeholder="Ex.: 1" />
                </label>
                <label class="question-options__correct"><input v-model="opcao.correta" :disabled="somenteLeitura" type="checkbox" /> Correta</label>
                <button v-if="!somenteLeitura" type="button" class="button button--danger question-options__remove" :disabled="(q.opcoes?.length ?? 0) <= 2" :aria-label="`Remover opção ${oi + 1}`" @click="q.opcoes?.splice(oi, 1)">×</button>
              </div>
              <button v-if="!somenteLeitura" type="button" class="button button--secondary question-options__add" @click="adicionarOpcao(q)">+ Adicionar opção</button>
            </div>

            <div class="question-editor__footer">
              <div class="question-editor__meta">
                <label class="question-editor__required-label">
                  <input v-model="q.obrigatoria" :disabled="somenteLeitura" type="checkbox" class="question-editor__checkbox" /> Obrigatória
                </label>
                <span v-if="q.tipo !== 'DESCRITIVA' && q.conjuntoOpcaoId" class="question-editor__options-preview">
                  {{ mapaConjuntos.get(q.conjuntoOpcaoId)?.opcoes.map(o => o.texto).join(' · ') }}
                </span>
                <span v-else-if="q.opcoes?.length" class="question-editor__options-preview">{{ q.opcoes.map(o => o.texto || '…').join(' · ') }}</span>
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
