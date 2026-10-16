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
  columns: Column[];
  pageSize?: number;
  rowKey?: string;
  page: number;
  hasNext: boolean;
}>(), {
  pageSize: 10,
  rowKey: 'id'
});

const emit = defineEmits<{ (e: 'page-change', page: number): void }>();
const primeiroItem = computed(() => props.rows.length ? (props.page - 1) * props.pageSize + 1 : 0);
const ultimoItem = computed(() => props.rows.length ? primeiroItem.value + props.rows.length - 1 : 0);

function valor(linha: T, chave: string): unknown {
  return (linha as Record<string, unknown>)[chave];
}
</script>

<template>
  <div class="w-full">
    <div class="overflow-x-auto">
      <table class="w-full text-xs">
        <thead class="bg-slate-50 text-[10px] uppercase text-slate-500">
          <tr>
            <th v-for="coluna in columns" :key="coluna.key" :class="['p-3', coluna.headerClass || 'text-left']">
              {{ coluna.label }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(linha, indice) in rows" :key="String(valor(linha, rowKey) ?? indice)" class="border-t border-slate-100 hover:bg-slate-50/60">
            <td v-for="coluna in columns" :key="coluna.key" :class="['p-3', coluna.class]">
              <slot name="cell" :row="linha" :column="coluna.key">
                {{ valor(linha, coluna.key) }}
              </slot>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 px-4 py-3 text-xs text-slate-500">
      <span>{{ rows.length ? `Exibindo ${primeiroItem}–${ultimoItem} nesta página` : 'Nenhum instrumento nesta página' }}</span>
      <nav class="flex items-center gap-1" aria-label="Paginação da tabela">
        <button type="button" class="rounded-lg border border-slate-200 px-2.5 py-1.5 hover:bg-slate-50 disabled:opacity-40" :disabled="page <= 1" aria-label="Página anterior" @click="emit('page-change', page - 1)">Anterior</button>
        <span class="px-2">Página {{ page }}</span>
        <button type="button" class="rounded-lg border border-slate-200 px-2.5 py-1.5 hover:bg-slate-50 disabled:opacity-40" :disabled="!hasNext" aria-label="Próxima página" @click="emit('page-change', page + 1)">Próxima</button>
      </nav>
    </div>
  </div>
</template>
