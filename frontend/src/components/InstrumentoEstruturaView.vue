<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { avaliacaoApi } from '../services/avaliacaoApi';
import { carregarUsuarioAtivo, temPermissao } from '../services/storage';
import type { ConjuntoOpcaoDTO, InstrumentoAvaliativoDTO } from '../types/instrumentoAvaliacao';
import AvaliacaoEstruturaEditor from './AvaliacaoEstruturaEditor.vue';
import AvaliacaoConjuntoModal from './AvaliacaoConjuntoModal.vue';
import InstrumentoPreview from './InstrumentoPreview.vue';

const route = useRoute();
const id = computed(() => String(route.params.id || ''));
const usuario = carregarUsuarioAtivo();
const podeEditar = temPermissao(usuario, 'INSTRUMENTO_EDITAR');
const podeCriarConjunto = temPermissao(usuario, 'INSTRUMENTO_CRIAR');
const instrumento = reactive<InstrumentoAvaliativoDTO>({
  tipoEntidadeAvaliada: { tipoEntidadeAvaliadaId: '', codigo: '', nome: '', podeSerAvaliada: false, podeResponder: false },
  titulo: '', cicloReferencia: '', inicioAplicacao: '', fimAplicacao: '', anonimo: true,
  permiteEdicao: true, respondentes: [], paginas: [], sessoes: []
});
const conjuntos = ref<ConjuntoOpcaoDTO[]>([]);
const carregando = ref(true);
const salvando = ref(false);
const publicando = ref(false);
const previsualizando = ref(false);
const modalConjunto = ref(false);
const erro = ref<string | null>(null);
const sucesso = ref<string | null>(null);
const somenteLeitura = computed(() => !podeEditar || instrumento.status !== 'RASCUNHO');
const editorBloqueado = computed(() => somenteLeitura.value || salvando.value || publicando.value);

onMounted(async () => {
  const [resposta, catalogo] = await Promise.all([avaliacaoApi.buscar(id.value), avaliacaoApi.conjuntos()]);
  if (catalogo.ok) conjuntos.value = catalogo.dados || [];
  else erro.value = catalogo.erro || 'Erro ao carregar os conjuntos de opções.';
  if (resposta.ok && resposta.dados) Object.assign(instrumento, resposta.dados);
  else erro.value = resposta.erro || 'Instrumento não encontrado.';
  carregando.value = false;
});

async function salvar(): Promise<boolean> {
  if (somenteLeitura.value || salvando.value) return false;
  erro.value = sucesso.value = null;
  salvando.value = true;
  const resposta = await avaliacaoApi.salvarEstrutura(id.value, {
    paginas: instrumento.paginas,
    sessoes: instrumento.sessoes.map(sessao => ({
      ...sessao,
      questoes: sessao.questoes.map(questao => ({
        ...questao,
        opcoes: questao.conjuntoOpcaoId ? [] : questao.opcoes || []
      }))
    }))
  });
  salvando.value = false;
  if (!resposta.ok || !resposta.dados) {
    erro.value = resposta.erro || 'Erro ao salvar a estrutura.';
    return false;
  }
  Object.assign(instrumento, resposta.dados);
  sucesso.value = 'Estrutura salva com sucesso.';
  return true;
}

async function publicar() {
  if (!(await salvar())) return;
  publicando.value = true;
  const resposta = await avaliacaoApi.publicar(id.value);
  publicando.value = false;
  if (resposta.ok && resposta.dados) {
    Object.assign(instrumento, resposta.dados);
    sucesso.value = 'Instrumento publicado.';
  } else erro.value = resposta.erro || 'Erro ao publicar.';
}

function aoSalvarConjunto(conjunto: ConjuntoOpcaoDTO) {
  conjuntos.value.push(conjunto);
  conjuntos.value.sort((a, b) => a.nome.localeCompare(b.nome));
  modalConjunto.value = false;
}
</script>

<template>
  <div class="page-stack">
    <p v-if="erro" class="alert alert--error instrument-form__alert">{{ erro }}</p>
    <p v-if="sucesso" class="alert alert--success instrument-form__alert">{{ sucesso }}</p>
    <p v-if="carregando">Carregando instrumento…</p>
    <template v-else-if="instrumento.id">
      <InstrumentoPreview v-if="previsualizando" :instrumento="instrumento" :conjuntos="conjuntos" @voltar="previsualizando = false" />
      <template v-else>
        <div class="panel panel--padded instrument-form__heading">
          <div>
            <RouterLink :to="{ name: 'instrumentos' }" class="text-action text-action--primary">← Voltar à lista</RouterLink>
            <h2 class="instrument-form__title">Estrutura: {{ instrumento.titulo }}</h2>
          </div>
          <div class="instrument-form__actions">
            <button type="button" class="button button--secondary" @click="previsualizando = true">👁 Visualizar instrumento</button>
            <button v-if="!somenteLeitura" type="button" class="button button--primary" :disabled="salvando || publicando" @click="salvar">{{ salvando ? 'Salvando…' : 'Salvar estrutura' }}</button>
            <button v-if="!somenteLeitura" type="button" class="button button--publish" :disabled="salvando || publicando" @click="publicar">🚀 Salvar e publicar</button>
          </div>
        </div>
        <AvaliacaoEstruturaEditor
          v-model:paginas="instrumento.paginas"
          v-model:sessoes="instrumento.sessoes"
          :conjuntos="conjuntos"
          :somente-leitura="editorBloqueado"
          :pode-criar-conjunto="podeCriarConjunto"
          @novo-conjunto="modalConjunto = true"
        />
      </template>
    </template>
    <AvaliacaoConjuntoModal v-if="modalConjunto" @salvo="aoSalvarConjunto" @fechar="modalConjunto = false" />
  </div>
</template>
