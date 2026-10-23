<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { InstrumentoAvaliativoDTO } from '../types/instrumentoAvaliacao';
import { responderApi } from '../services/responderApi';
import InstrumentosDisponiveisView from './InstrumentosDisponiveisView.vue';

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
  <div class="page-stack page-stack--compact">
    <div class="panel panel--padded">
      <h2 class="survey-response__title">Responder avaliações</h2>
      <p class="survey-response__description">Avaliações publicadas disponíveis para seu perfil.</p>
    </div>
    <p v-if="erro" role="alert" class="alert alert--error">{{ erro }}</p>
    <p v-if="carregando" class="loading-message">Carregando avaliações…</p>
    <InstrumentosDisponiveisView v-if="!erro" :instrumentos="instrumentos" :carregando="carregando" />
  </div>
</template>
