<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { ConjuntoOpcaoDTO } from '../types/instrumentoAvaliacao';
import { avaliacaoApi } from '../services/avaliacaoApi';

const emit = defineEmits<{
  (e: 'salvo', conjunto: ConjuntoOpcaoDTO): void;
  (e: 'fechar'): void;
}>();

const conjunto = reactive<ConjuntoOpcaoDTO>({
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

const input = 'form-field';
</script>

<template>
  <div class="dialog-backdrop" @click.self="emit('fechar')">
    <div class="panel panel--spacious dialog-card">
      <div>
        <h3 class="dialog-card__title">Novo conjunto de opções</h3>
        <p class="dialog-card__description">Fica disponível para reutilização em qualquer questão.</p>
      </div>

      <label class="form-label">
        <span class="form-label__caption">Nome do conjunto <span class="required-mark" aria-hidden="true">*</span></span>
        <input v-model="conjunto.nome" required placeholder="Ex.: Frequência: Nunca … Sempre" :class="[input, 'form-field--emphasis']" />
      </label>

      <div class="dialog-options">
        <div class="dialog-options__header">
          <span class="dialog-options__text">Texto <span class="required-mark" aria-hidden="true">*</span></span><span class="dialog-options__value">Valor</span><span class="dialog-options__correct">Correta</span><span></span>
        </div>
        <div v-for="(o, i) in conjunto.opcoes" :key="i" class="dialog-options__row">
          <input v-model="o.texto" required :aria-label="`Texto da opção ${i + 1} (obrigatório)`" placeholder="Texto da opção" class="form-field dialog-options__text" />
          <input v-model.number="o.valor" type="number" step="0.5" class="form-field dialog-options__value" />
          <input v-model="o.correta" type="checkbox" class="dialog-options__checkbox" />
          <button type="button" class="dialog-options__remove" :disabled="conjunto.opcoes.length <= 2" @click="conjunto.opcoes.splice(i, 1)">✕</button>
        </div>
        <button type="button" class="dialog-options__add" @click="addOpcao">+ Adicionar opção</button>
      </div>

      <p v-if="erro" class="alert alert--error dialog-card__error">{{ erro }}</p>

      <div class="dialog-card__actions">
        <button type="button" class="button button--secondary" @click="emit('fechar')">Cancelar</button>
        <button type="button" :disabled="salvando" class="button button--primary dialog-card__save" @click="salvar">
          {{ salvando ? 'Salvando…' : 'Salvar conjunto' }}
        </button>
      </div>
    </div>
  </div>
</template>
