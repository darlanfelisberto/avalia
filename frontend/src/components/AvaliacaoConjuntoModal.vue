<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { ConjuntoOpcao } from '../types/instrumentoAvaliacao';
import { avaliacaoApi } from '../services/avaliacaoApi';

const emit = defineEmits<{
  (e: 'salvo', conjunto: ConjuntoOpcao): void;
  (e: 'fechar'): void;
}>();

const conjunto = reactive<ConjuntoOpcao>({
  nome: '',
  opcoes: [
    { texto: '', valor: 1, correta: false },
    { texto: '', valor: 2, correta: false }
  ]
});
const erro = ref<string | null>(null);
const salvando = ref(false);

function addOpcao() {
  conjunto.opcoes.push({ texto: '', valor: conjunto.opcoes.length + 1, correta: false });
}

async function salvar() {
  erro.value = null;
  salvando.value = true;
  const res = await avaliacaoApi.salvarConjunto(JSON.parse(JSON.stringify(conjunto)));
  salvando.value = false;
  if (res.ok && res.dados) emit('salvo', res.dados);
  else erro.value = res.erro || 'Erro ao salvar o conjunto.';
}

const input = 'w-full text-xs p-2.5 border border-slate-200 rounded-lg outline-hidden focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600';
</script>

<template>
  <div class="fixed inset-0 z-50 bg-slate-900/50 flex items-center justify-center p-4" @click.self="emit('fechar')">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg p-6 space-y-4 max-h-[90vh] overflow-y-auto">
      <div>
        <h3 class="text-sm font-bold text-slate-900">Novo conjunto de opções</h3>
        <p class="text-xs text-slate-500 mt-0.5">Fica disponível para reutilização em qualquer questão.</p>
      </div>

      <input v-model="conjunto.nome" placeholder="Nome (ex.: Frequência: Nunca … Sempre)" :class="[input, 'font-semibold']" />

      <div class="space-y-2">
        <div class="grid grid-cols-12 gap-2 text-[10px] font-bold uppercase text-slate-400 px-1">
          <span class="col-span-7">Texto</span><span class="col-span-2">Valor</span><span class="col-span-2 text-center">Correta</span><span></span>
        </div>
        <div v-for="(o, i) in conjunto.opcoes" :key="i" class="grid grid-cols-12 gap-2 items-center">
          <input v-model="o.texto" placeholder="Texto da opção" :class="[input, 'col-span-7']" />
          <input v-model.number="o.valor" type="number" step="0.5" :class="[input, 'col-span-2']" />
          <input v-model="o.correta" type="checkbox" class="col-span-2 mx-auto w-4 h-4" />
          <button type="button" class="text-rose-500 text-sm disabled:opacity-30" :disabled="conjunto.opcoes.length <= 2" @click="conjunto.opcoes.splice(i, 1)">✕</button>
        </div>
        <button type="button" class="text-[11px] font-bold text-emerald-700" @click="addOpcao">+ Adicionar opção</button>
      </div>

      <p v-if="erro" class="text-xs bg-rose-50 border border-rose-200 text-rose-800 rounded-lg p-2.5">{{ erro }}</p>

      <div class="flex justify-end gap-2 pt-2">
        <button type="button" class="text-xs font-semibold border border-slate-200 rounded-xl px-4 py-2 hover:bg-slate-50" @click="emit('fechar')">Cancelar</button>
        <button type="button" :disabled="salvando" class="text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl px-4 py-2 disabled:opacity-50" @click="salvar">
          {{ salvando ? 'Salvando…' : 'Salvar conjunto' }}
        </button>
      </div>
    </div>
  </div>
</template>
