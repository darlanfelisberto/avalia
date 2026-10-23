<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { InstrumentoAvaliativoDTO } from '../types/instrumentoAvaliacao';
import { avaliacaoApi } from '../services/avaliacaoApi';
import { carregarUsuarioAtivo, temPermissao } from '../services/storage';
import PaginatedTable from './PaginatedTable.vue';

const instrumentos = ref<InstrumentoAvaliativoDTO[]>([]);
const paginaAtual = ref(1);
const temProximaPagina = ref(false);
const tamanhoPagina = 10;
const carregando = ref(false);
const erro = ref<string | null>(null);
const usuario = carregarUsuarioAtivo();
const podeCriar = temPermissao(usuario, 'INSTRUMENTO_CRIAR');
const podeExcluir = temPermissao(usuario, 'INSTRUMENTO_EXCLUIR');

async function carregarLista(pagina = paginaAtual.value) {
  carregando.value = true;
  const res = await avaliacaoApi.listar(pagina, tamanhoPagina);
  carregando.value = false;
  if (res.ok && res.dados) {
    if (res.dados.length === 0 && pagina > 1) {
      temProximaPagina.value = false;
      await carregarLista(pagina - 1);
      return;
    }
    instrumentos.value = res.dados;
    paginaAtual.value = pagina;
    temProximaPagina.value = res.dados.length === tamanhoPagina;
  } else {
    erro.value = res.erro || 'Erro ao carregar instrumentos.';
  }
}

async function excluir(id: string, titulo: string) {
  if (!confirm(`Excluir o rascunho "${titulo}"?`)) return;
  const res = await avaliacaoApi.excluir(id);
  if (res.ok) await carregarLista();
  else erro.value = res.erro || 'Erro ao excluir.';
}

onMounted(() => carregarLista());

const fmtData = (iso?: string) => (iso ? new Date(iso).toLocaleDateString('pt-BR') : '—');
const corStatus: Record<string, string> = {
  RASCUNHO: 'status-badge--draft',
  PUBLICADO: 'status-badge--published',
  ENCERRADO: 'status-badge--closed',
  CONSOLIDADO: 'status-badge--consolidated'
};
</script>

<template>
  <div class="page-stack">
    <p v-if="erro" class="alert alert--error instrument-list__error">
      <span>⚠️ {{ erro }}</span><button type="button" @click="erro = null">✕</button>
    </p>

    <div class="panel overflow-hidden">
      <div class="instrument-list__header">
        <div>
          <h2 class="instrument-list__title">Instrumentos de Avaliação</h2>
          <p class="instrument-list__description">Questionários cadastrados no schema <code>avaliacao</code>.</p>
        </div>
        <RouterLink v-if="podeCriar" :to="{ name: 'instrumento-novo' }" class="button button--primary instrument-list__create">
          + Novo instrumento
        </RouterLink>
      </div>

      <p v-if="carregando" class="instrument-list__empty animate-pulse">Carregando…</p>
      <p v-else-if="instrumentos.length === 0" class="instrument-list__empty">Nenhum instrumento cadastrado.</p>
      <PaginatedTable v-else :rows="instrumentos" :page-size="tamanhoPagina" :page="paginaAtual" :has-next="temProximaPagina" @page-change="carregarLista">
        <template #header>
          <tr>
            <th class="data-table__heading">Título</th>
            <th class="data-table__heading">Ciclo</th>
            <th class="data-table__heading">Avalia</th>
            <th class="data-table__heading">Período</th>
            <th class="data-table__heading">Status</th>
            <th class="data-table__heading text-right">Ações</th>
          </tr>
        </template>
        <template #body="{ row }">
          <tr class="data-table__row">
            <td class="data-table__cell instrument-table__title">{{ row.titulo }}</td>
            <td class="data-table__cell instrument-table__secondary">{{ row.cicloReferencia }}</td>
            <td class="data-table__cell instrument-table__secondary">{{ row.tipoEntidadeAvaliada?.nome || 'Institucional' }}</td>
            <td class="data-table__cell instrument-table__secondary">{{ fmtData(row.inicioAplicacao) }} – {{ fmtData(row.fimAplicacao) }}</td>
            <td class="data-table__cell"><span :class="['status-badge', corStatus[row.status || 'RASCUNHO']]">{{ row.status }}</span></td>
            <td class="data-table__cell text-right whitespace-nowrap">
              <RouterLink :to="{ name: 'instrumento-editar', params: { id: row.id } }" class="text-action text-action--primary instrument-table__open">{{ row.status === 'RASCUNHO' ? 'Editar cadastro' : 'Abrir cadastro' }}</RouterLink>
              <RouterLink :to="{ name: 'instrumento-estrutura', params: { id: row.id } }" class="text-action text-action--primary">{{ row.status === 'RASCUNHO' ? 'Editar estrutura' : 'Ver estrutura' }}</RouterLink>
              <button v-if="podeExcluir && row.status === 'RASCUNHO'" type="button" class="text-action text-action--danger" @click="excluir(row.id!, row.titulo)">Excluir</button>
            </td>
          </tr>
        </template>
      </PaginatedTable>
    </div>
  </div>
</template>
