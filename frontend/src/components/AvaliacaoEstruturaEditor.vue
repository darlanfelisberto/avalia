<script setup lang="ts">
import { computed } from 'vue';
import type { ConjuntoOpcao, ModuloInstrumento, PaginaAv, QuestaoAv, SessaoAv, TipoQuestaoAv } from '../types/instrumentoAvaliacao';

const paginas = defineModel<PaginaAv[]>('paginas', { required: true });

const props = defineProps<{
  conjuntos: ConjuntoOpcao[];
  modulo: ModuloInstrumento;
  somenteLeitura?: boolean;
}>();

const emit = defineEmits<{ (e: 'novoConjunto'): void }>();

const TIPOS: { valor: TipoQuestaoAv; rotulo: string }[] = [
  { valor: 'ESCALA', rotulo: 'Escala (ex.: concordância)' },
  { valor: 'ESCOLHA_UNICA', rotulo: 'Escolha única' },
  { valor: 'MULTIPLA_ESCOLHA', rotulo: 'Múltipla escolha' },
  { valor: 'DESCRITIVA', rotulo: 'Resposta descritiva' }
];

const mostrarPeso = computed(() => props.modulo === 'APRENDIZAGEM');
const mapaConjuntos = computed(() => new Map(props.conjuntos.map(c => [c.id, c])));

function mover<T>(lista: T[], i: number, delta: number) {
  const j = i + delta;
  if (j < 0 || j >= lista.length) return;
  [lista[i], lista[j]] = [lista[j], lista[i]];
}

const novaQuestao = (): QuestaoAv => ({ enunciado: '', tipo: 'ESCALA', conjuntoOpcaoId: props.conjuntos[0]?.id ?? null, obrigatoria: true, peso: null });
const novaSessao = (): SessaoAv => ({ titulo: '', descricao: '', questoes: [novaQuestao()] });

function addPagina() {
  paginas.value.push({ titulo: `Página ${paginas.value.length + 1}`, descricao: '', sessoes: [novaSessao()] });
}

function aoMudarTipo(q: QuestaoAv) {
  if (q.tipo === 'DESCRITIVA') q.conjuntoOpcaoId = null;
  else if (!q.conjuntoOpcaoId) q.conjuntoOpcaoId = props.conjuntos[0]?.id ?? null;
}

const input = 'w-full text-xs p-2.5 border border-slate-200 rounded-lg bg-white focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden disabled:bg-slate-50';
const btnIcone = 'text-[11px] px-1.5 py-0.5 rounded border border-slate-200 text-slate-500 hover:bg-slate-100 disabled:opacity-30';
</script>

<template>
  <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs space-y-5">
    <div class="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 pb-3">
      <div>
        <h3 class="text-sm font-bold text-slate-900 uppercase tracking-wide">3. Estrutura do Questionário</h3>
        <p class="text-xs text-slate-500 mt-0.5">Páginas contêm sessões; sessões agrupam as questões.</p>
      </div>
      <button v-if="!somenteLeitura" type="button" @click="addPagina"
        class="text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white px-3.5 py-2 rounded-xl">
        + Nova página
      </button>
    </div>

    <p v-if="somenteLeitura" class="text-xs bg-amber-50 border border-amber-200 text-amber-900 rounded-lg p-3">
      Instrumento já publicado: a estrutura está bloqueada para edição. Apenas os dados gerais podem ser ajustados.
    </p>
    <p v-if="paginas.length === 0" class="text-xs text-center text-slate-400 py-6">
      Nenhuma página ainda. Clique em “Nova página” para começar.
    </p>

    <section v-for="(pag, pi) in paginas" :key="pi" class="border border-slate-200 rounded-xl overflow-hidden">
      <header class="bg-slate-50 px-4 py-3 flex flex-wrap items-center gap-2 border-b border-slate-200">
        <span class="text-[10px] font-bold uppercase bg-slate-800 text-white px-2 py-0.5 rounded-full">Página {{ pi + 1 }}</span>
        <input v-model="pag.titulo" :disabled="somenteLeitura" placeholder="Título da página" :class="[input, 'flex-1 min-w-48 font-semibold']" />
        <template v-if="!somenteLeitura">
          <button type="button" :class="btnIcone" :disabled="pi === 0" @click="mover(paginas, pi, -1)">↑</button>
          <button type="button" :class="btnIcone" :disabled="pi === paginas.length - 1" @click="mover(paginas, pi, 1)">↓</button>
          <button type="button" :class="btnIcone" class="text-rose-600!" @click="paginas.splice(pi, 1)">Remover</button>
        </template>
      </header>

      <div class="p-4 space-y-4">
        <div v-for="(ses, si) in pag.sessoes" :key="si" class="border border-purple-200 bg-purple-50/40 rounded-xl p-3.5 space-y-3">
          <div class="flex flex-wrap items-center gap-2">
            <span class="text-[10px] font-bold uppercase bg-purple-700 text-white px-2 py-0.5 rounded-full">Sessão {{ si + 1 }}</span>
            <input v-model="ses.titulo" :disabled="somenteLeitura" placeholder="Título da sessão (ex.: Didática)" :class="[input, 'flex-1 min-w-48 font-semibold']" />
            <template v-if="!somenteLeitura">
              <button type="button" :class="btnIcone" :disabled="si === 0" @click="mover(pag.sessoes, si, -1)">↑</button>
              <button type="button" :class="btnIcone" :disabled="si === pag.sessoes.length - 1" @click="mover(pag.sessoes, si, 1)">↓</button>
              <button type="button" :class="btnIcone" class="text-rose-600!" @click="pag.sessoes.splice(si, 1)">Remover</button>
            </template>
          </div>
          <input v-model="ses.descricao" :disabled="somenteLeitura" placeholder="Descrição / instruções da sessão (opcional)" :class="input" />

          <article v-for="(q, qi) in ses.questoes" :key="qi" class="bg-white border border-slate-200 rounded-lg p-3 space-y-2.5">
            <div class="flex items-start gap-2">
              <span class="text-xs font-bold text-slate-400 pt-2.5 w-6">{{ qi + 1 }}.</span>
              <textarea v-model="q.enunciado" :disabled="somenteLeitura" rows="2" placeholder="Enunciado da questão" :class="input"></textarea>
              <div v-if="!somenteLeitura" class="flex flex-col gap-1">
                <button type="button" :class="btnIcone" :disabled="qi === 0" @click="mover(ses.questoes, qi, -1)">↑</button>
                <button type="button" :class="btnIcone" :disabled="qi === ses.questoes.length - 1" @click="mover(ses.questoes, qi, 1)">↓</button>
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-12 gap-2 pl-8">
              <select v-model="q.tipo" :disabled="somenteLeitura" @change="aoMudarTipo(q)" :class="[input, 'sm:col-span-4']">
                <option v-for="t in TIPOS" :key="t.valor" :value="t.valor">{{ t.rotulo }}</option>
              </select>
              <select v-if="q.tipo !== 'DESCRITIVA'" v-model="q.conjuntoOpcaoId" :disabled="somenteLeitura" :class="[input, 'sm:col-span-5']">
                <option :value="null" disabled>Selecione as opções…</option>
                <option v-for="c in conjuntos" :key="c.id" :value="c.id">{{ c.nome }}</option>
              </select>
              <input v-model="q.assunto" :disabled="somenteLeitura" placeholder="Assunto (opcional)"
                :class="[input, q.tipo === 'DESCRITIVA' ? 'sm:col-span-5' : 'sm:col-span-3']" />
              <input v-if="mostrarPeso" v-model.number="q.peso" :disabled="somenteLeitura" type="number" step="0.5" min="0" placeholder="Peso" :class="[input, 'sm:col-span-2']" />
            </div>

            <div class="flex flex-wrap items-center justify-between gap-2 pl-8">
              <div class="flex flex-wrap items-center gap-3">
                <label class="flex items-center gap-1.5 text-[11px] font-semibold text-slate-600 cursor-pointer">
                  <input v-model="q.obrigatoria" :disabled="somenteLeitura" type="checkbox" class="w-3.5 h-3.5 rounded-sm" /> Obrigatória
                </label>
                <span v-if="q.tipo !== 'DESCRITIVA' && q.conjuntoOpcaoId" class="text-[10px] text-slate-400">
                  {{ mapaConjuntos.get(q.conjuntoOpcaoId)?.opcoes.map(o => o.texto).join(' · ') }}
                </span>
              </div>
              <button v-if="!somenteLeitura" type="button" class="text-[11px] font-bold text-rose-600 hover:underline" @click="ses.questoes.splice(qi, 1)">
                Remover questão
              </button>
            </div>
          </article>

          <div v-if="!somenteLeitura" class="flex flex-wrap gap-2">
            <button type="button" class="text-[11px] font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-3 py-1.5 rounded-lg" @click="ses.questoes.push(novaQuestao())">
              + Questão
            </button>
            <button type="button" class="text-[11px] font-bold text-purple-700 bg-white border border-purple-200 px-3 py-1.5 rounded-lg" @click="emit('novoConjunto')">
              + Novo conjunto de opções
            </button>
          </div>
        </div>

        <button v-if="!somenteLeitura" type="button" class="text-[11px] font-bold text-purple-700 border border-dashed border-purple-300 rounded-lg px-3 py-2 w-full hover:bg-purple-50"
          @click="pag.sessoes.push(novaSessao())">
          + Nova sessão nesta página
        </button>
      </div>
    </section>
  </div>
</template>
