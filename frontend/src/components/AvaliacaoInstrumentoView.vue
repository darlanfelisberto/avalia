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
  RASCUNHO: 'bg-slate-100 text-slate-700',
  PUBLICADO: 'bg-emerald-100 text-emerald-800',
  ENCERRADO: 'bg-amber-100 text-amber-800',
  CONSOLIDADO: 'bg-purple-100 text-purple-800'
};
const colunasInstrumentos = [
  { key: 'titulo', label: 'Título', class: 'font-semibold text-slate-900' },
  { key: 'cicloReferencia', label: 'Ciclo', class: 'text-slate-600' },
  { key: 'tipoEntidadeAvaliadaNome', label: 'Avalia', class: 'text-slate-600' },
  { key: 'periodo', label: 'Período', class: 'text-slate-600' },
  { key: 'status', label: 'Status' },
  { key: 'acoes', label: '', class: 'text-right whitespace-nowrap' }
];
</script>

<template>
  <div class="space-y-6">
    <p v-if="erro" class="p-4 bg-rose-50 border border-rose-300 rounded-xl text-xs text-rose-900 font-semibold flex justify-between gap-3">
      <span>⚠️ {{ erro }}</span><button type="button" @click="erro = null">✕</button>
    </p>

    <div class="bg-white border border-slate-200 rounded-2xl shadow-xs overflow-hidden">
      <div class="p-5 md:p-6 flex flex-wrap items-center justify-between gap-3 border-b border-slate-100">
        <div>
          <h2 class="text-lg font-bold text-slate-900">Instrumentos de Avaliação</h2>
          <p class="text-xs text-slate-500">Questionários cadastrados no schema <code>avaliacao</code>.</p>
        </div>
        <button v-if="podeCriar" type="button" @click="abrirNovo" class="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold px-4 py-2.5 rounded-xl">
          + Novo instrumento
        </button>
      </div>

      <p v-if="carregando" class="p-8 text-center text-xs text-slate-400 animate-pulse">Carregando…</p>
      <p v-else-if="instrumentos.length === 0" class="p-8 text-center text-xs text-slate-400">Nenhum instrumento cadastrado.</p>
      <PaginatedTable v-else :rows="instrumentos" :columns="colunasInstrumentos" :page-size="tamanhoPagina"
                      :page="paginaAtual" :has-next="temProximaPagina" @page-change="carregarLista">
        <template #cell="{ row, column }">
          <template v-if="column === 'periodo'">
            {{ fmtData(row.inicioAplicacao) }} – {{ fmtData(row.fimAplicacao) }}
          </template>
          <span v-else-if="column === 'status'" :class="['px-2 py-0.5 rounded-full font-bold text-[10px]', corStatus[row.status || 'RASCUNHO']]">{{ row.status }}</span>
          <div v-else-if="column === 'acoes'">
            <button type="button" class="font-bold text-emerald-700 hover:underline mr-3" @click="abrirInstrumento(row.id!)">{{ row.status === 'RASCUNHO' ? 'Editar' : 'Abrir' }}</button>
            <button v-if="podeExcluir && row.status === 'RASCUNHO'" type="button" class="font-bold text-rose-600 hover:underline" @click="excluir(row.id!, row.titulo)">Excluir</button>
          </div>
        </template>
      </PaginatedTable>
    </div>
  </div>
</template>
