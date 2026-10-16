<script setup lang="ts">
import { ref } from 'vue';
import type { ConjuntoOpcao, InstrumentoAvaliativoDTO, QuestaoAv } from '../types/instrumentoAvaliacao';

const props = defineProps<{
  instrumento: InstrumentoAvaliativoDTO;
  conjuntos: ConjuntoOpcao[];
}>();

const emit = defineEmits<{ voltar: [] }>();

const escolhas = ref<Record<string, string>>({});
const multiplas = ref<Record<string, string[]>>({});
const textos = ref<Record<string, string>>({});
const erro = ref('');
const concluida = ref(false);

function chaveQuestao(q: QuestaoAv, sessaoIndex: number, questaoIndex: number): string {
  return q.sessaoQuestaoId || q.questaoId || `sessao-${sessaoIndex}-questao-${questaoIndex}`;
}

function opcoes(q: QuestaoAv) {
  return props.conjuntos.find(c => c.id === q.conjuntoOpcaoId)?.opcoes || [];
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
  <div class="space-y-5">
    <div class="bg-white border border-slate-200 rounded-xl p-5">
      <button type="button" @click="emit('voltar')" class="text-xs font-bold text-emerald-700 hover:underline">
        ← Voltar à edição
      </button>
      <p class="mt-4 inline-flex rounded-full bg-amber-100 px-3 py-1 text-[10px] font-bold uppercase text-amber-800">
        Modo de visualização · respostas não serão salvas
      </p>
      <h2 class="text-xl font-bold text-slate-900 mt-3">{{ instrumento.titulo || 'Prévia do instrumento' }}</h2>
      <p v-if="instrumento.descricao" class="text-sm text-slate-600 mt-1">{{ instrumento.descricao }}</p>
      <p v-if="concluida" role="status" class="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">
        Prévia concluída. Nenhuma resposta foi salva.
      </p>
    </div>

    <p v-if="erro" role="alert" class="bg-rose-50 border border-rose-200 rounded-xl p-4 text-sm text-rose-800">{{ erro }}</p>

    <section v-for="(pagina, pi) in instrumento.paginas || []" :key="pagina.id || pi"
      class="bg-slate-100 border border-slate-200 rounded-xl p-5">
      <p class="text-[10px] uppercase font-bold text-slate-500">Página {{ pi + 1 }}</p>
      <h3 class="font-bold text-slate-900 mt-1">{{ pagina.titulo }}</h3>
      <p v-if="pagina.descricao" class="text-sm text-slate-600 mt-1">{{ pagina.descricao }}</p>
    </section>

    <section v-for="(sessao, si) in instrumento.sessoes || []" :key="sessao.id || si"
      class="bg-white border border-slate-200 rounded-xl p-5 space-y-5">
      <div>
        <h3 class="font-bold text-slate-900">{{ sessao.titulo }}</h3>
        <p v-if="sessao.descricao" class="text-sm text-slate-500">{{ sessao.descricao }}</p>
      </div>
      <fieldset v-for="(q, qi) in sessao.questoes" :key="chaveQuestao(q, si, qi)"
        class="border-t border-slate-100 pt-4 space-y-2">
        <legend class="font-semibold text-sm text-slate-800">
          {{ qi + 1 }}. {{ q.enunciado }} <span v-if="q.obrigatoria" class="text-rose-600">*</span>
        </legend>
        <textarea v-if="q.tipo === 'DESCRITIVA'" v-model="textos[chaveQuestao(q, si, qi)]" rows="4"
          class="w-full border border-slate-200 rounded-lg p-3 text-sm" placeholder="Escreva sua resposta"></textarea>
        <template v-else>
          <label v-for="op in opcoes(q)" :key="op.id || op.texto" class="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
            <input v-if="q.tipo === 'MULTIPLA_ESCOLHA'" v-model="multiplas[chaveQuestao(q, si, qi)]" type="checkbox" :value="op.id || op.texto" />
            <input v-else v-model="escolhas[chaveQuestao(q, si, qi)]" type="radio" :name="chaveQuestao(q, si, qi)" :value="op.id || op.texto" />
            {{ op.texto }}
          </label>
          <p v-if="opcoes(q).length === 0" class="text-xs text-amber-700">Esta questão ainda não tem opções cadastradas.</p>
        </template>
      </fieldset>
    </section>

    <p v-if="instrumento.sessoes.length === 0" class="bg-white border border-slate-200 rounded-xl p-6 text-sm text-slate-500">
      Adicione sessões e questões para visualizar as respostas.
    </p>

    <button type="button" :disabled="concluida" @click="finalizar"
      class="bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-bold text-sm px-5 py-3 rounded-xl">
      {{ concluida ? 'Prévia concluída' : 'Concluir prévia' }}
    </button>
  </div>
</template>
