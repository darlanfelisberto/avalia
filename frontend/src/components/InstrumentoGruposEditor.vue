<script setup lang="ts">
import type { GrupoQuestoes, Questao, TipoQuestao } from '../types/avaliacao';

const props = defineProps<{
  grupos: GrupoQuestoes[];
}>();

const emit = defineEmits<{
  (e: 'update:grupos', grupos: GrupoQuestoes[]): void;
}>();

function adicionarGrupo() {
  const novoGrupo: GrupoQuestoes = {
    id: crypto.randomUUID(),
    titulo: 'Novo Grupo de Questões',
    descricao: 'Descreva os objetivos deste grupo...',
    questoes: []
  };
  props.grupos.push(novoGrupo);
}

function removerGrupo(grupoIndex: number) {
  if (confirm('Tem certeza que deseja remover este grupo e todas as suas questões?')) {
    props.grupos.splice(grupoIndex, 1);
  }
}

// Gerenciamento de Questões
function adicionarQuestao(grupo: GrupoQuestoes, tipo: TipoQuestao = 'ESCALA') {
  const novaQuestao: Questao = {
    id: crypto.randomUUID(),
    enunciado: 'Novo enunciado de questão...',
    tipo,
    obrigatoria: true,
    escalaMin: 1,
    escalaMax: 5,
    rotuloMin: '1 - Não atende / Insatisfatório',
    rotuloMax: '5 - Atende plenamente / Excelente',
    opcoes: tipo === 'ESCOLHA_UNICA' || tipo === 'MULTIPLA_ESCOLHA' ? [
      { id: crypto.randomUUID(), texto: 'Opção 1' },
      { id: crypto.randomUUID(), texto: 'Opção 2' }
    ] : undefined,
    ajuda: ''
  };
  grupo.questoes.push(novaQuestao);
}

function duplicarQuestao(grupo: GrupoQuestoes, questaoIndex: number) {
  const original = grupo.questoes[questaoIndex];
  const copia: Questao = JSON.parse(JSON.stringify(original));
  copia.id = crypto.randomUUID();
  copia.enunciado = `${copia.enunciado} (Cópia)`;
  grupo.questoes.splice(questaoIndex + 1, 0, copia);
}

function removerQuestao(grupo: GrupoQuestoes, questaoIndex: number) {
  grupo.questoes.splice(questaoIndex, 1);
}

function moverQuestao(grupo: GrupoQuestoes, questaoIndex: number, direcao: 'cima' | 'baixo') {
  if (direcao === 'cima' && questaoIndex > 0) {
    const item = grupo.questoes.splice(questaoIndex, 1)[0];
    grupo.questoes.splice(questaoIndex - 1, 0, item);
  } else if (direcao === 'baixo' && questaoIndex < grupo.questoes.length - 1) {
    const item = grupo.questoes.splice(questaoIndex, 1)[0];
    grupo.questoes.splice(questaoIndex + 1, 0, item);
  }
}

// Gerenciamento de Opções em Questões de Escolha
function adicionarOpcao(questao: Questao) {
  if (!questao.opcoes) questao.opcoes = [];
  questao.opcoes.push({
    id: crypto.randomUUID(),
    texto: `Nova Opção ${questao.opcoes.length + 1}`
  });
}

function removerOpcao(questao: Questao, optIndex: number) {
  if (questao.opcoes && questao.opcoes.length > 1) {
    questao.opcoes.splice(optIndex, 1);
  }
}

// Ao mudar o tipo de questão
function mudarTipoQuestao(questao: Questao, novoTipo: TipoQuestao) {
  questao.tipo = novoTipo;
  if ((novoTipo === 'ESCOLHA_UNICA' || novoTipo === 'MULTIPLA_ESCOLHA') && (!questao.opcoes || questao.opcoes.length === 0)) {
    questao.opcoes = [
      { id: crypto.randomUUID(), texto: 'Opção 1' },
      { id: crypto.randomUUID(), texto: 'Opção 2' }
    ];
  }
}
</script>

<template>
  <div class="bg-white border border-slate-200 rounded-2xl p-5 md:p-6 shadow-xs space-y-6">
    <div class="flex flex-col sm:flex-row justify-between sm:items-center gap-3 border-b border-slate-100 pb-4">
      <div>
        <h3 class="text-sm font-bold text-slate-900 uppercase tracking-wide">
          2. Questões do Instrumento
        </h3>
        <p class="text-xs text-slate-500 mt-0.5">
          Organize o questionário em dimensões temáticas ou grupos de perguntas.
        </p>
      </div>

      <button
        type="button"
        @click="adicionarGrupo"
        class="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold px-4 py-2 rounded-xl transition-all flex items-center gap-1.5 self-start sm:self-center shadow-xs cursor-pointer"
      >
        <span>＋</span>
        <span>Novo Grupo de Questões</span>
      </button>
    </div>

    <!-- VISÃO: CONSTRUTOR DE GRUPOS E QUESTÕES DIRETAS -->
    <div class="space-y-6">
      <div v-if="grupos.length === 0" class="p-8 text-center border-2 border-dashed border-slate-200 rounded-2xl space-y-3">
        <p class="text-xs text-slate-500">Este questionário ainda não possui um grupo e questões cadastradas.</p>
        <button
          type="button"
          @click="adicionarGrupo"
          class="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-lg cursor-pointer"
        >
          Adicionar o primeiro Grupo
        </button>
      </div>

      <!-- Lista de Grupos -->
      <div
        v-for="(grupo, gIdx) in grupos"
        :key="grupo.id"
        class="border-2 border-slate-200 rounded-2xl p-5 md:p-6 bg-white space-y-5 shadow-xs"
      >
        <!-- Cabeçalho do Grupo -->
        <div class="flex flex-col sm:flex-row justify-between sm:items-center gap-3 border-b border-slate-100 pb-3">
          <div class="flex-1 space-y-1">
            <input
              v-model="grupo.titulo"
              type="text"
              placeholder="Título do Grupo / Dimensão (ex: Prática Pedagógica)"
              class="w-full text-sm font-bold text-slate-900 p-1.5 border-b border-dashed border-slate-300 focus:border-emerald-600 outline-hidden"
            />
            <input
              v-model="grupo.descricao"
              type="text"
              placeholder="Descrição ou orientações gerais para este grupo de questões..."
              class="w-full text-xs text-slate-500 p-1 border-b border-transparent focus:border-slate-300 outline-hidden"
            />
          </div>

          <div class="flex items-center gap-2">
            <button
              type="button"
              @click="adicionarQuestao(grupo, 'ESCALA')"
              class="bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 text-xs font-semibold px-3 py-1.5 rounded-lg transition-all flex items-center gap-1 cursor-pointer"
            >
              <span>+</span>
              <span>Nova Questão</span>
            </button>

            <button
              type="button"
              @click="removerGrupo(gIdx)"
              title="Excluir Grupo"
              class="text-rose-500 hover:text-rose-700 text-xs px-2 py-1 font-bold cursor-pointer"
            >
              Excluir Grupo
            </button>
          </div>
        </div>

        <!-- Lista de Questões dentro do Grupo -->
        <div class="space-y-4">
          <div
            v-if="grupo.questoes.length === 0"
            class="p-6 text-center text-xs text-slate-400 bg-slate-50 rounded-xl border border-dashed border-slate-200"
          >
            Nenhuma questão neste grupo. Clique em "+ Nova Questão" para adicionar.
          </div>

          <div
            v-for="(questao, qIdx) in grupo.questoes"
            :key="questao.id"
            class="border border-slate-200 rounded-xl p-4 bg-slate-50/40 hover:bg-white hover:border-slate-300 transition-all space-y-3"
          >
            <!-- Linha Superior da Questão: Enunciado, Tipo e Ações -->
            <div class="flex flex-col sm:flex-row gap-3 items-start justify-between">
              <div class="flex items-center gap-2 text-xs font-bold text-slate-400 shrink-0">
                <span class="w-6 h-6 rounded-full bg-slate-200 text-slate-700 flex items-center justify-center text-[11px]">
                  {{ qIdx + 1 }}
                </span>
              </div>

              <div class="flex-1 w-full space-y-1.5">
                <textarea
                  v-model="questao.enunciado"
                  rows="2"
                  placeholder="Digite o enunciado da questão..."
                  class="w-full text-xs md:text-sm p-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden font-medium bg-white"
                ></textarea>

                <input
                  v-model="questao.ajuda"
                  type="text"
                  placeholder="Texto de ajuda ou instrução complementar (opcional)..."
                  class="w-full text-[11px] p-1.5 border border-slate-100 rounded-md text-slate-500 bg-white"
                />
              </div>

              <div class="flex sm:flex-col items-center gap-1.5 shrink-0 self-end sm:self-auto">
                <div class="flex gap-1">
                  <button
                    type="button"
                    @click="moverQuestao(grupo, qIdx, 'cima')"
                    :disabled="qIdx === 0"
                    title="Mover para cima"
                    class="px-2 py-1 text-xs border rounded bg-white text-slate-600 disabled:opacity-30 cursor-pointer"
                  >
                    ▲
                  </button>
                  <button
                    type="button"
                    @click="moverQuestao(grupo, qIdx, 'baixo')"
                    :disabled="qIdx === grupo.questoes.length - 1"
                    title="Mover para baixo"
                    class="px-2 py-1 text-xs border rounded bg-white text-slate-600 disabled:opacity-30 cursor-pointer"
                  >
                    ▼
                  </button>
                </div>
                <div class="flex gap-1">
                  <button
                    type="button"
                    @click="duplicarQuestao(grupo, qIdx)"
                    title="Duplicar questão"
                    class="px-2 py-1 text-xs border rounded bg-white text-slate-600 hover:text-slate-900 cursor-pointer"
                  >
                    📋
                  </button>
                  <button
                    type="button"
                    @click="removerQuestao(grupo, qIdx)"
                    title="Excluir questão"
                    class="px-2 py-1 text-xs border rounded bg-white text-rose-600 hover:bg-rose-50 cursor-pointer"
                  >
                    ✕
                  </button>
                </div>
              </div>
            </div>

            <!-- Configurações Específicas do Tipo da Questão (RF02 e RF03) -->
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-slate-100 text-xs">
              <div>
                <label class="font-bold text-slate-600 block mb-1">Tipo de Resposta (RF02):</label>
                <select
                  :value="questao.tipo"
                  @change="mudarTipoQuestao(questao, ($event.target as HTMLSelectElement).value as TipoQuestao)"
                  class="w-full p-2 border border-slate-200 rounded-lg bg-white font-medium"
                >
                  <option value="ESCALA">Escala Likert (Pontuação 1 a 5)</option>
                  <option value="ESCOLHA_UNICA">Escolha Única (Radio)</option>
                  <option value="MULTIPLA_ESCOLHA">Múltipla Escolha (Caixa de Seleção)</option>
                  <option value="TEXTO_LIVRE">Texto Livre (Dissertativa / Aberta)</option>
                </select>
              </div>

              <div class="flex items-center">
                <label class="flex items-center gap-2 cursor-pointer font-medium text-slate-700 mt-4 sm:mt-0">
                  <input
                    v-model="questao.obrigatoria"
                    type="checkbox"
                    class="w-4 h-4 text-emerald-600 rounded-sm border-slate-300"
                  />
                  <span>Resposta Obrigatória (RF03)</span>
                </label>
              </div>
            </div>

            <!-- Subpainel para Escala Likert -->
            <div v-if="questao.tipo === 'ESCALA'" class="bg-white p-3 rounded-lg border border-slate-200 grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              <div>
                <label class="text-[11px] font-bold text-slate-500">Rótulo Mínimo (Valor 1):</label>
                <input
                  v-model="questao.rotuloMin"
                  type="text"
                  placeholder="Ex: 1 - Não atende"
                  class="w-full p-1.5 border border-slate-200 rounded text-xs mt-0.5"
                />
              </div>
              <div>
                <label class="text-[11px] font-bold text-slate-500">Rótulo Máximo (Valor 5):</label>
                <input
                  v-model="questao.rotuloMax"
                  type="text"
                  placeholder="Ex: 5 - Atende plenamente"
                  class="w-full p-1.5 border border-slate-200 rounded text-xs mt-0.5"
                />
              </div>
            </div>

            <!-- Subpainel para Escolha Única / Múltipla Escolha -->
            <div v-else-if="questao.tipo === 'ESCOLHA_UNICA' || questao.tipo === 'MULTIPLA_ESCOLHA'" class="bg-white p-3 rounded-lg border border-slate-200 space-y-2 text-xs">
              <div class="flex justify-between items-center font-bold text-slate-700">
                <span>Opções de Resposta:</span>
                <button
                  type="button"
                  @click="adicionarOpcao(questao)"
                  class="text-emerald-700 hover:text-emerald-800 text-[11px] font-bold cursor-pointer"
                >
                  + Adicionar Opção
                </button>
              </div>
              <div
                v-for="(opt, optIdx) in questao.opcoes"
                :key="opt.id"
                class="flex items-center gap-2"
              >
                <span class="text-slate-400 text-[11px] font-mono">{{ optIdx + 1 }}.</span>
                <input
                  v-model="opt.texto"
                  type="text"
                  placeholder="Texto da opção..."
                  class="flex-1 p-1.5 border border-slate-200 rounded text-xs"
                />
                <button
                  type="button"
                  @click="removerOpcao(questao, optIdx)"
                  class="text-slate-400 hover:text-rose-600 px-1 font-bold cursor-pointer"
                >
                  ✕
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
