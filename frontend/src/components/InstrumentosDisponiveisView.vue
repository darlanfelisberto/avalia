<script setup lang="ts">
import type { InstrumentoAvaliativoDTO } from '../types/instrumentoAvaliacao';

defineProps<{
  instrumentos: InstrumentoAvaliativoDTO[];
  carregando?: boolean;
}>();

function formatarData(data?: string | null) {
  if (!data) return '—';
  const valor = new Date(data);
  if (Number.isNaN(valor.getTime())) return data;
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(valor);
}
</script>

<template>
  <p v-if="!carregando && instrumentos.length === 0" class="empty-state">
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
        Período: {{ formatarData(item.inicioAplicacao) }} a {{ formatarData(item.fimAplicacao) }}
      </p>
    </div>
    <RouterLink v-if="item.id" :to="{ name: 'responder-formulario', params: { instrumentoId: item.id } }"
      class="button button--primary survey-response__button">Responder avaliação</RouterLink>
  </article>
</template>
