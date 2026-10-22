<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { InstrumentoAvaliativoDTO } from '../types/instrumentoAvaliacao';
import { responderApi } from '../services/responderApi';

const instrumentos = ref<InstrumentoAvaliativoDTO[]>([]);
const carregando = ref(false);
const erro = ref('');

async function carregar() {
  carregando.value = true;
  erro.value = '';
  try {
    instrumentos.value = await responderApi.listar();
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Não foi possível carregar as avaliações.';
  } finally {
    carregando.value = false;
  }
}

onMounted(carregar);
</script>

<template>
  <div class="survey-response">
    <div class="surface-card survey-response__intro">
      <h2 class="survey-response__title">Responder avaliações</h2>
      <p class="survey-response__description">Avaliações publicadas disponíveis para seu perfil.</p>
    </div>

    <p v-if="erro" role="alert" class="alert alert--error">{{ erro }}</p>
    <p v-if="carregando" class="loading-message">Carregando avaliações…</p>
    <p v-if="!carregando && !erro && instrumentos.length === 0" class="empty-state">
      Nenhuma avaliação disponível para seu perfil neste período.
    </p>

    <article v-for="item in instrumentos" :key="item.id" class="surface-card survey-card survey-card--summary">
      <div>
        <h3 class="survey-card__title">{{ item.titulo }}</h3>
        <p v-if="item.descricao" class="survey-card__description">{{ item.descricao }}</p>
        <p class="survey-card__hint">
          Avaliação: {{ item.tipoEntidadeAvaliadaNome || 'Institucional' }}
          <span v-if="item.cicloReferencia"> · Ciclo: {{ item.cicloReferencia }}</span>
        </p>
        <p v-if="item.inicioAplicacao || item.fimAplicacao" class="survey-card__hint">
          Período: {{ item.inicioAplicacao || '—' }} a {{ item.fimAplicacao || '—' }}
        </p>
      </div>
    </article>
  </div>
</template>