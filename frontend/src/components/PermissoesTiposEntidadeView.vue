<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { avaliacaoApi } from '../services/avaliacaoApi';
import type { PermissaoTipoEntidadeDTO, TipoEntidadeDTO } from '../types/instrumentoAvaliacao';
import { carregarUsuarioAtivo, temPermissao } from '../services/storage';

const podeEditar = temPermissao(carregarUsuarioAtivo(), 'INSTRUMENTO_EDITAR');

const tipos = ref<TipoEntidadeDTO[]>([]);
const permissoes = ref(new Set<string>());
const carregando = ref(true);
const salvando = ref(false);
const erro = ref('');
const sucesso = ref('');

const respondentes = computed(() => tipos.value.filter(tipo => tipo.podeResponder));
const alvos = computed(() => tipos.value.filter(tipo => tipo.podeSerAvaliada));
const chave = (respondenteId: string, avaliadoId: string) => `${respondenteId}:${avaliadoId}`;

function estaPermitido(respondenteId: string, avaliadoId: string) {
  return permissoes.value.has(chave(respondenteId, avaliadoId));
}

function alternar(respondenteId: string, avaliadoId: string) {
  const atualizadas = new Set(permissoes.value);
  const par = chave(respondenteId, avaliadoId);
  if (atualizadas.has(par)) atualizadas.delete(par);
  else atualizadas.add(par);
  permissoes.value = atualizadas;
  erro.value = sucesso.value = '';
}

async function carregar() {
  carregando.value = true;
  erro.value = '';
  const [tiposResposta, permissoesResposta] = await Promise.all([
    avaliacaoApi.tiposEntidade(),
    avaliacaoApi.permissoesTiposEntidade()
  ]);
  carregando.value = false;

  if (!tiposResposta.ok || !tiposResposta.dados) {
    erro.value = tiposResposta.erro || 'Não foi possível carregar os tipos de entidade.';
    return;
  }
  if (!permissoesResposta.ok || !permissoesResposta.dados) {
    erro.value = permissoesResposta.erro || 'Não foi possível carregar as permissões.';
    return;
  }

  tipos.value = tiposResposta.dados;
  permissoes.value = new Set(permissoesResposta.dados.map(p => chave(p.tipoRespondenteId, p.tipoAvaliadoId)));
}

async function salvar() {
  salvando.value = true;
  erro.value = sucesso.value = '';
  const payload: PermissaoTipoEntidadeDTO[] = [...permissoes.value].map(par => {
    const [tipoRespondenteId, tipoAvaliadoId] = par.split(':');
    return { tipoRespondenteId, tipoAvaliadoId };
  });
  const resposta = await avaliacaoApi.salvarPermissoesTiposEntidade(payload);
  salvando.value = false;
  if (!resposta.ok) {
    erro.value = resposta.erro || 'Não foi possível salvar as permissões.';
    return;
  }
  sucesso.value = 'Permissões salvas. Elas serão aplicadas aos próximos instrumentos.';
}

onMounted(carregar);
</script>

<template>
  <section class="space-y-5">
    <header class="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
      <p class="text-xs font-bold uppercase tracking-wide text-emerald-700">Regras de participação</p>
      <h1 class="mt-1 text-xl font-bold text-slate-900">Quem pode avaliar cada tipo de entidade</h1>
      <p class="mt-2 max-w-3xl text-sm text-slate-600">
        Marque cada combinação permitida. As linhas indicam quem responde e as colunas indicam quem pode ser avaliado.
        Essas regras serão verificadas ao configurar novos instrumentos.
      </p>
    </header>

    <div v-if="erro" role="alert" class="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{{ erro }}</div>
    <div v-if="sucesso" role="status" class="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">{{ sucesso }}</div>

    <div v-if="carregando" class="rounded-2xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">
      Carregando tipos de entidade…
    </div>

    <div v-else class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div class="overflow-x-auto">
        <table class="min-w-full border-collapse text-sm">
          <thead>
            <tr class="bg-slate-50">
              <th scope="col" class="sticky left-0 z-10 min-w-48 border-b border-r border-slate-200 bg-slate-50 p-4 text-left font-semibold text-slate-700">
                Quem responde ↓<br><span class="font-normal text-slate-500">Quem é avaliado →</span>
              </th>
              <th v-for="alvo in alvos" :key="alvo.id" scope="col" class="min-w-36 border-b border-slate-200 p-4 text-center font-semibold text-slate-700">
                {{ alvo.nome }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="respondente in respondentes" :key="respondente.id" class="border-b border-slate-100 last:border-0 hover:bg-slate-50/70">
              <th scope="row" class="sticky left-0 z-10 border-r border-slate-200 bg-white p-4 text-left font-semibold text-slate-800">
                {{ respondente.nome }}
              </th>
              <td v-for="alvo in alvos" :key="alvo.id" class="p-4 text-center">
                <label class="inline-flex cursor-pointer items-center justify-center rounded-lg p-2 hover:bg-emerald-50" :title="`${respondente.nome} pode avaliar ${alvo.nome}`">
                  <input
                    type="checkbox"
                    class="h-4 w-4 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
                    :aria-label="`${respondente.nome} pode avaliar ${alvo.nome}`"
                    :checked="estaPermitido(respondente.id, alvo.id)"
                    :disabled="salvando || !podeEditar"
                    @change="alternar(respondente.id, alvo.id)"
                  />
                </label>
              </td>
            </tr>
            <tr v-if="respondentes.length === 0 || alvos.length === 0">
              <td :colspan="Math.max(1, alvos.length + 1)" class="p-8 text-center text-sm text-slate-500">
                Não há tipos habilitados como respondente ou alvo.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="flex flex-col gap-3 border-t border-slate-200 bg-slate-50 p-4 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-xs text-slate-500">{{ permissoes.size }} combinação(ões) permitida(s)</p>
        <button v-if="podeEditar" type="button" class="rounded-xl bg-emerald-700 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60" :disabled="salvando" @click="salvar">
          {{ salvando ? 'Salvando…' : 'Salvar permissões' }}
        </button>
      </footer>
    </div>
  </section>
</template>
