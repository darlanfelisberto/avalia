<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { InstrumentoAvaliativoDTO } from '../types/instrumentoAvaliacao';
import { responderApi } from '../services/responderApi';
import InstrumentosDisponiveisView from './InstrumentosDisponiveisView.vue';
import FormularioResponderAvaliacao from './FormularioResponderAvaliacao.vue';

const instrumentos = ref<InstrumentoAvaliativoDTO[]>([]);
const instrumentoAberto = ref<InstrumentoAvaliativoDTO | null>(null);
const carregando = ref(false);
const erro = ref('');
const respostas = ref<Record<string, any>>({});

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

async function abrirInstrumento(item: InstrumentoAvaliativoDTO) {
  if (!item.id) return;
  carregando.value = true;
  erro.value = '';
  try {
    instrumentoAberto.value = await responderApi.formulario(item.id);
    respostas.value = {};
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Não foi possível abrir a avaliação.';
  } finally {
    carregando.value = false;
  }
}

function voltarALista() {
  instrumentoAberto.value = null;
  respostas.value = {};
  erro.value = '';
}
</script>

<template>
  <div class="survey-response">
    <div class="surface-card survey-response__intro">
      <h2 class="survey-response__title">Responder avaliações</h2>
      <p class="survey-response__description">Avaliações publicadas disponíveis para seu perfil.</p>
    </div>

    <p v-if="erro" role="alert" class="alert alert--error">{{ erro }}</p>
    <p v-if="carregando" class="loading-message">
      {{ instrumentoAberto ? 'Carregando formulário…' : 'Carregando avaliações…' }}
    </p>

    <FormularioResponderAvaliacao v-if="instrumentoAberto" :instrumento="instrumentoAberto"
      v-model:respostas="respostas" @voltar="voltarALista" />
    <InstrumentosDisponiveisView v-else :instrumentos="instrumentos" :carregando="carregando"
      @selecionar="abrirInstrumento" />
  </div>
</template>
