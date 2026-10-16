<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { InstrumentoAvaliativoDTO } from '../types/instrumentoAvaliacao';
import { avaliacaoApi } from '../services/avaliacaoApi';
import { carregarUsuarioAtivo, temPermissao } from '../services/storage';
import PaginatedTable from './PaginatedTable.vue';

const router = useRouter();
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

function abrirNovo() {
  void router.push('/instrumentos/novo');
}

function abrirInstrumento(id: string) {
  void router.push(`/instrumentos/${encodeURIComponent(id)}`);
}

onMounted(() => carregarLista());

const fmtData = (iso?: string) => (iso ? new Date(iso).toLocaleDateString('pt-BR') : '—');
const corStatus: Record<string, string> = {
  RASCUNHO: 'status-badge--draft',
  PUBLICADO: 'status-badge--published',
  ENCERRADO: 'status-badge--closed',
  CONSOLIDADO: 'status-badge--consolidated'
};
const colunasInstrumentos = [
  { key: 'titulo', label: 'Título', class: 'instrument-table__title' },
  { key: 'cicloReferencia', label: 'Ciclo', class: 'instrument-table__secondary' },
  { key: 'tipoEntidadeAvaliadaNome', label: 'Avalia', class: 'instrument-table__secondary' },
  { key: 'periodo', label: 'Período', class: 'instrument-table__secondary' },
  { key: 'status', label: 'Status' },
  { key: 'acoes', label: '', class: 'text-right whitespace-nowrap' }
];
</script>

<template>
  <div class="instrument-list">
    <p v-if="erro" class="alert alert--error instrument-list__error">
      <span>⚠️ {{ erro }}</span><button type="button" @click="erro = null">✕</button>
    </p>

    <div class="instrument-list__card">
      <div class="instrument-list__header">
        <div>
          <h2 class="instrument-list__title">Instrumentos de Avaliação</h2>
          <p class="instrument-list__description">Questionários cadastrados no schema <code>avaliacao</code>.</p>
        </div>
        <button v-if="podeCriar" type="button" @click="abrirNovo" class="button button--primary instrument-list__create">
          + Novo instrumento
        </button>
      </div>

      <p v-if="carregando" class="instrument-list__empty instrument-list__empty--loading">Carregando…</p>
      <p v-else-if="instrumentos.length === 0" class="instrument-list__empty">Nenhum instrumento cadastrado.</p>
      <PaginatedTable v-else :rows="instrumentos" :columns="colunasInstrumentos" :page-size="tamanhoPagina"
                      :page="paginaAtual" :has-next="temProximaPagina" @page-change="carregarLista">
        <template #cell="{ row, column }">
          <template v-if="column === 'periodo'">
            {{ fmtData(row.inicioAplicacao) }} – {{ fmtData(row.fimAplicacao) }}
          </template>
          <span v-else-if="column === 'status'" :class="['status-badge', corStatus[row.status || 'RASCUNHO']]">{{ row.status }}</span>
          <div v-else-if="column === 'acoes'">
            <button type="button" class="text-action text-action--primary instrument-table__open" @click="abrirInstrumento(row.id!)">{{ row.status === 'RASCUNHO' ? 'Editar' : 'Abrir' }}</button>
            <button v-if="podeExcluir && row.status === 'RASCUNHO'" type="button" class="text-action text-action--danger" @click="excluir(row.id!, row.titulo)">Excluir</button>
          </div>
        </template>
      </PaginatedTable>
    </div>
  </div>
</template>
