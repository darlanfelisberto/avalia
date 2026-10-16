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
  <section class="permissions-view">
    <header class="permissions-view__intro">
      <p class="permissions-view__eyebrow">Regras de participação</p>
      <h1 class="permissions-view__title">Quem pode avaliar cada tipo de entidade</h1>
      <p class="permissions-view__description">
        Marque cada combinação permitida. As linhas indicam quem responde e as colunas indicam quem pode ser avaliado.
        Essas regras serão verificadas ao configurar novos instrumentos.
      </p>
    </header>

    <div v-if="erro" role="alert" class="alert alert--error">{{ erro }}</div>
    <div v-if="sucesso" role="status" class="alert alert--success">{{ sucesso }}</div>

    <div v-if="carregando" class="permissions-view__loading">
      Carregando tipos de entidade…
    </div>

    <div v-else class="permissions-matrix">
      <div class="permissions-matrix__scroll">
        <table class="permissions-matrix__table">
          <thead>
            <tr class="permissions-matrix__heading-row">
              <th scope="col" class="permissions-matrix__corner">
                Quem responde ↓<br><span class="permissions-matrix__subheading">Quem é avaliado →</span>
              </th>
              <th v-for="alvo in alvos" :key="alvo.id" scope="col" class="permissions-matrix__column-heading">
                {{ alvo.nome }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="respondente in respondentes" :key="respondente.id" class="permissions-matrix__row">
              <th scope="row" class="permissions-matrix__row-heading">
                {{ respondente.nome }}
              </th>
              <td v-for="alvo in alvos" :key="alvo.id" class="permissions-matrix__cell">
                <label class="permissions-matrix__toggle" :title="`${respondente.nome} pode avaliar ${alvo.nome}`">
                  <input
                    type="checkbox"
                    class="permissions-matrix__checkbox"
                    :aria-label="`${respondente.nome} pode avaliar ${alvo.nome}`"
                    :checked="estaPermitido(respondente.id, alvo.id)"
                    :disabled="salvando || !podeEditar"
                    @change="alternar(respondente.id, alvo.id)"
                  />
                </label>
              </td>
            </tr>
            <tr v-if="respondentes.length === 0 || alvos.length === 0">
              <td :colspan="Math.max(1, alvos.length + 1)" class="permissions-matrix__empty">
                Não há tipos habilitados como respondente ou alvo.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="permissions-matrix__footer">
        <p class="permissions-matrix__count">{{ permissoes.size }} combinação(ões) permitida(s)</p>
        <button v-if="podeEditar" type="button" class="button button--primary permissions-matrix__save" :disabled="salvando" @click="salvar">
          {{ salvando ? 'Salvando…' : 'Salvar permissões' }}
        </button>
      </footer>
    </div>
  </section>
</template>
