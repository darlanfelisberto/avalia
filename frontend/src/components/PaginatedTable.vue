<script setup lang="ts" generic="T extends object">
import { computed } from 'vue';

interface Column {
  key: string;
  label: string;
  class?: string;
  headerClass?: string;
}

const props = withDefaults(defineProps<{
  rows: T[];
  columns?: Column[];
  pageSize?: number;
  rowKey?: string;
  page: number;
  hasNext: boolean;
}>(), {
  columns: () => [],
  pageSize: 10,
  rowKey: 'id'
});

const emit = defineEmits<{ (e: 'page-change', page: number): void }>();
const primeiroItem = computed(() => props.rows.length ? (props.page - 1) * props.pageSize + 1 : 0);
const ultimoItem = computed(() => props.rows.length ? primeiroItem.value + props.rows.length - 1 : 0);

function valor(linha: T, chave: string): unknown {
  return chave.split('.').reduce<unknown>((atual, segmento) => {
    if (atual === null || typeof atual !== 'object') return undefined;
    return (atual as Record<string, unknown>)[segmento];
  }, linha);
}
</script>

<template>
  <div class="data-table-wrap">
    <div class="data-table-scroll">
      <table class="data-table">
        <thead class="data-table__head">
          <slot name="header" :columns="columns">
            <tr>
              <th v-for="coluna in columns" :key="coluna.key" :class="['data-table__heading', coluna.headerClass]">
                {{ coluna.label }}
              </th>
            </tr>
          </slot>
        </thead>
        <tbody>
          <template v-for="(linha, indice) in rows" :key="String(valor(linha, rowKey) ?? indice)">
            <slot name="body" :row="linha" :index="indice">
              <tr class="data-table__row">
                <td v-for="coluna in columns" :key="coluna.key" :class="['data-table__cell', coluna.class]">
                  <slot :name="`cell-${coluna.key}`" :row="linha" :column="coluna.key" :value="valor(linha, coluna.key)">
                    <slot name="cell" :row="linha" :column="coluna.key" :value="valor(linha, coluna.key)">
                      {{ valor(linha, coluna.key) }}
                    </slot>
                  </slot>
                </td>
              </tr>
            </slot>
          </template>
        </tbody>
      </table>
    </div>

    <div class="data-table__pagination">
      <span>{{ rows.length ? `Exibindo ${primeiroItem}–${ultimoItem} nesta página` : 'Nenhum instrumento nesta página' }}</span>
      <nav class="data-table__page-controls" aria-label="Paginação da tabela">
        <button type="button" class="button button--secondary data-table__page-button" :disabled="page <= 1" aria-label="Página anterior" @click="emit('page-change', page - 1)">Anterior</button>
        <span class="data-table__current-page">Página {{ page }}</span>
        <button type="button" class="button button--secondary data-table__page-button" :disabled="!hasNext" aria-label="Próxima página" @click="emit('page-change', page + 1)">Próxima</button>
      </nav>
    </div>
  </div>
</template>
