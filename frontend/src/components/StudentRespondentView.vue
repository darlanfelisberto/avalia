<script setup lang="ts">
import { computed } from 'vue';
import type { InstrumentoAvaliativo, EstadoPreenchimento, RespostaItem, ComponenteCurricular } from '../types/avaliacao';

const props = defineProps<{
  instrumento: InstrumentoAvaliativo;
  estado: EstadoPreenchimento;
}>();

const emit = defineEmits<{
  (e: 'respostaAtualizada', resposta: RespostaItem): void;
  (e: 'mudarEtapa', etapa: 'autoavaliacao' | 'docentes' | 'finalizacao'): void;
  (e: 'mudarComponente', compId: string): void;
  (e: 'concluirEnvio'): void;
}>();

// Disciplina ativa na etapa de avaliação dos docentes
const componenteAtivo = computed(() => {
  return props.instrumento.componentesMatriculados.find(c => c.id === props.estado.componenteAtivoId)
    || props.instrumento.componentesMatriculados[0];
});

// Chave auxiliar para buscar resposta no mapa
function getChaveResposta(questaoId: string, compId?: string): string {
  return `${questaoId}_${compId || 'geral'}`;
}

// Obter valor atual de uma resposta
function getRespostaAtual(questaoId: string, compId?: string): RespostaItem | undefined {
  return props.estado.respostas[getChaveResposta(questaoId, compId)];
}

// Registrar seleção de escala ou opção
function setValorEscala(questaoId: string, valor: number, compId?: string) {
  const item: RespostaItem = {
    questaoId,
    componenteId: compId,
    valorEscala: valor
  };
  emit('respostaAtualizada', item);
}

function setOpcaoUnica(questaoId: string, opcaoId: string, compId?: string) {
  const item: RespostaItem = {
    questaoId,
    componenteId: compId,
    opcaoId
  };
  emit('respostaAtualizada', item);
}

function setTextoLivre(questaoId: string, texto: string, compId?: string) {
  const item: RespostaItem = {
    questaoId,
    componenteId: compId,
    textoLivre: texto
  };
  emit('respostaAtualizada', item);
}

// Verifica se um componente curricular específico já teve todas as questões obrigatórias preenchidas
function componenteCompleto(comp: ComponenteCurricular): boolean {
  for (const grupo of props.instrumento.gruposAvaliacaoDocente) {
    for (const q of grupo.questoes) {
      if (q.obrigatoria) {
        const resp = getRespostaAtual(q.id, comp.id);
        if (!resp || (resp.valorEscala === undefined && !resp.opcaoId)) {
          return false;
        }
      }
    }
  }
  return true;
}

// Verifica se a etapa de autoavaliação discente está completa
const autoavaliacaoCompleta = computed(() => {
  for (const grupo of props.instrumento.gruposAutoavaliacaoDiscente) {
    for (const q of grupo.questoes) {
      if (q.obrigatoria) {
        const resp = getRespostaAtual(q.id);
        if (!resp || (resp.valorEscala === undefined && !resp.opcaoId)) {
          return false;
        }
      }
    }
  }
  return true;
});

// Total de disciplinas avaliadas
const totalComponentesConcluidos = computed(() => {
  return props.instrumento.componentesMatriculados.filter(c => componenteCompleto(c)).length;
});

const todasDisciplinasConcluidas = computed(() => {
  return totalComponentesConcluidos.value === props.instrumento.componentesMatriculados.length;
});
</script>

<template>
  <div class="space-y-6">
    <!-- Banner de Anonimato Institucional (RNF03 & RNF-AD01) -->
    <div class="bg-emerald-50 border border-emerald-200 rounded-xl p-4 flex items-start gap-3.5 shadow-2xs">
      <div class="text-2xl text-emerald-700 select-none">🔒</div>
      <div class="space-y-1">
        <h2 class="text-sm font-semibold text-emerald-950">
          Garantia de Anonimato e Segurança do Respondente
        </h2>
        <p class="text-xs text-emerald-800 leading-relaxed">
          Em consonância com o art. 14 da Lei nº 12.772/2012, a Resolução CONSUP nº 104/2013 e a LGPD (Lei nº 13.709/2018), 
          <strong>suas respostas são totalmente anônimas</strong>. O sistema registra apenas a confirmação de participação no ciclo, 
          sendo tecnicamente impossível a correlação reversa das notas e comentários com a sua matrícula discente.
        </p>
      </div>
    </div>

    <!-- Navegador de Etapas do Instrumento -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
      <!-- Card Etapa 1 -->
      <button
        type="button"
        @click="emit('mudarEtapa', 'autoavaliacao')"
        :class="[
          'p-4 rounded-xl border text-left transition-all duration-200 flex items-center justify-between',
          estado.etapaAtual === 'autoavaliacao'
            ? 'border-emerald-600 bg-white ring-2 ring-emerald-500/20 shadow-sm'
            : 'border-slate-200 bg-slate-50/70 hover:bg-white text-slate-600'
        ]"
      >
        <div class="flex items-center gap-3">
          <div
            :class="[
              'w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs',
              autoavaliacaoCompleta ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-700'
            ]"
          >
            {{ autoavaliacaoCompleta ? '✓' : '1' }}
          </div>
          <div>
            <div class="text-xs font-semibold uppercase tracking-wider text-slate-400">Etapa 1</div>
            <div class="text-sm font-bold text-slate-900">Autoavaliação do Estudante</div>
          </div>
        </div>
        <span
          :class="[
            'text-xs px-2.5 py-1 rounded-full font-medium',
            autoavaliacaoCompleta ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
          ]"
        >
          {{ autoavaliacaoCompleta ? 'Concluída' : 'Pendente' }}
        </span>
      </button>

      <!-- Card Etapa 2 -->
      <button
        type="button"
        @click="emit('mudarEtapa', 'docentes')"
        :class="[
          'p-4 rounded-xl border text-left transition-all duration-200 flex items-center justify-between',
          estado.etapaAtual === 'docentes'
            ? 'border-emerald-600 bg-white ring-2 ring-emerald-500/20 shadow-sm'
            : 'border-slate-200 bg-slate-50/70 hover:bg-white text-slate-600'
        ]"
      >
        <div class="flex items-center gap-3">
          <div
            :class="[
              'w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs',
              todasDisciplinasConcluidas ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-700'
            ]"
          >
            {{ todasDisciplinasConcluidas ? '✓' : '2' }}
          </div>
          <div>
            <div class="text-xs font-semibold uppercase tracking-wider text-slate-400">Etapa 2</div>
            <div class="text-sm font-bold text-slate-900">Avaliação Docente por Disciplina</div>
          </div>
        </div>
        <span
          :class="[
            'text-xs px-2.5 py-1 rounded-full font-medium',
            todasDisciplinasConcluidas ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
          ]"
        >
          {{ totalComponentesConcluidos }}/{{ instrumento.componentesMatriculados.length }} avaliados
        </span>
      </button>
    </div>

    <!-- CONTEÚDO DA ETAPA 1: Autoavaliação do Estudante -->
    <section v-if="estado.etapaAtual === 'autoavaliacao'" class="space-y-6">
      <div
        v-for="grupo in instrumento.gruposAutoavaliacaoDiscente"
        :key="grupo.id"
        class="bg-white border border-slate-200 rounded-2xl p-5 md:p-7 shadow-xs space-y-6"
      >
        <div class="border-b border-slate-100 pb-4">
          <h3 class="text-base font-bold text-slate-900">{{ grupo.titulo }}</h3>
          <p v-if="grupo.descricao" class="text-xs text-slate-500 mt-1">{{ grupo.descricao }}</p>
        </div>

        <div class="space-y-8">
          <div
            v-for="(questao, qIndex) in grupo.questoes"
            :key="questao.id"
            class="space-y-3"
          >
            <div class="flex items-start justify-between gap-3">
              <label class="text-sm font-medium text-slate-800 leading-snug">
                <span class="font-bold text-slate-400 mr-1">{{ qIndex + 1 }}.</span>
                {{ questao.enunciado }}
                <span v-if="questao.obrigatoria" class="text-rose-500 ml-0.5">*</span>
              </label>
            </div>
            <p v-if="questao.ajuda" class="text-xs text-slate-500 italic">{{ questao.ajuda }}</p>

            <!-- Renderizador de Escala Likert 1 a 5 (Mobile Friendly) -->
            <div v-if="questao.tipo === 'ESCALA'" class="space-y-2">
              <div class="grid grid-cols-5 gap-2 sm:gap-3">
                <button
                  v-for="valor in 5"
                  :key="valor"
                  type="button"
                  @click="setValorEscala(questao.id, valor)"
                  :class="[
                    'py-3.5 px-2 rounded-xl text-center font-bold text-base transition-all duration-150 border',
                    getRespostaAtual(questao.id)?.valorEscala === valor
                      ? 'bg-emerald-600 text-white border-emerald-600 shadow-md ring-2 ring-emerald-500/30 scale-102'
                      : 'bg-slate-50 hover:bg-slate-100 text-slate-700 border-slate-200 hover:border-slate-300'
                  ]"
                >
                  {{ valor }}
                </button>
              </div>
              <div class="flex justify-between text-[11px] text-slate-500 px-1 font-medium">
                <span>{{ questao.rotuloMin || '1 - Mínimo' }}</span>
                <span>{{ questao.rotuloMax || '5 - Máximo' }}</span>
              </div>
            </div>

            <!-- Renderizador de Escolha Única -->
            <div v-else-if="questao.tipo === 'ESCOLHA_UNICA' && questao.opcoes" class="space-y-2">
              <div
                v-for="opt in questao.opcoes"
                :key="opt.id"
                @click="setOpcaoUnica(questao.id, opt.id)"
                :class="[
                  'p-3.5 rounded-xl border flex items-center gap-3 cursor-pointer transition-colors',
                  getRespostaAtual(questao.id)?.opcaoId === opt.id
                    ? 'border-emerald-600 bg-emerald-50/60 text-emerald-950 font-medium'
                    : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                ]"
              >
                <div
                  :class="[
                    'w-4 h-4 rounded-full border flex items-center justify-center shrink-0',
                    getRespostaAtual(questao.id)?.opcaoId === opt.id
                      ? 'border-emerald-600 bg-emerald-600'
                      : 'border-slate-300 bg-white'
                  ]"
                >
                  <div
                    v-if="getRespostaAtual(questao.id)?.opcaoId === opt.id"
                    class="w-1.5 h-1.5 rounded-full bg-white"
                  ></div>
                </div>
                <span class="text-xs md:text-sm">{{ opt.texto }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Ações do rodapé da Etapa 1 -->
      <div class="flex justify-end">
        <button
          type="button"
          @click="emit('mudarEtapa', 'docentes')"
          class="bg-emerald-700 hover:bg-emerald-800 text-white font-medium px-6 py-3 rounded-xl shadow-sm flex items-center gap-2 text-sm transition-all"
        >
          <span>Avançar para Avaliação das Disciplinas</span>
          <span>→</span>
        </button>
      </div>
    </section>

    <!-- CONTEÚDO DA ETAPA 2: Avaliação Docente pelo Discente (RF-AD02) -->
    <section v-else-if="estado.etapaAtual === 'docentes'" class="space-y-6">
      <!-- Seletor de Disciplina Cursada (Chips/Tabs horizontais) -->
      <div class="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs">
        <div class="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2.5">
          Selecione a disciplina para responder:
        </div>
        <div class="flex gap-2 overflow-x-auto pb-1 scrollbar-thin">
          <button
            v-for="comp in instrumento.componentesMatriculados"
            :key="comp.id"
            type="button"
            @click="emit('mudarComponente', comp.id)"
            :class="[
              'px-4 py-2.5 rounded-xl border text-left shrink-0 transition-all flex items-center gap-2.5',
              estado.componenteAtivoId === comp.id
                ? 'border-emerald-600 bg-emerald-50 text-emerald-950 ring-2 ring-emerald-500/20 font-semibold'
                : 'border-slate-200 hover:border-slate-300 bg-white text-slate-700'
            ]"
          >
            <span
              :class="[
                'w-5 h-5 rounded-full text-[11px] flex items-center justify-center font-bold',
                componenteCompleto(comp) ? 'bg-emerald-200 text-emerald-900' : 'bg-slate-200 text-slate-700'
              ]"
            >
              {{ componenteCompleto(comp) ? '✓' : '•' }}
            </span>
            <div class="text-xs">
              <div class="font-medium">{{ comp.nome }}</div>
              <div class="text-[11px] text-slate-500 font-normal">{{ comp.docenteNome }}</div>
            </div>
          </button>
        </div>
      </div>

      <!-- Informações da Disciplina e Docente Selecionado -->
      <div class="bg-slate-900 text-white rounded-2xl p-5 shadow-xs flex flex-col sm:flex-row justify-between sm:items-center gap-3">
        <div>
          <div class="text-xs uppercase tracking-wider text-emerald-400 font-semibold font-mono">
            {{ componenteAtivo.codigo }} • {{ componenteAtivo.turma }}
          </div>
          <h3 class="text-lg font-bold mt-0.5">{{ componenteAtivo.nome }}</h3>
          <p class="text-xs text-slate-300 mt-1 flex items-center gap-1.5">
            <span>Docente avaliado(a):</span>
            <strong class="text-white">{{ componenteAtivo.docenteNome }}</strong>
          </p>
        </div>
        <div class="text-xs bg-slate-800 border border-slate-700 px-3 py-1.5 rounded-lg text-slate-300 self-start sm:self-auto">
          Componente {{ instrumento.componentesMatriculados.indexOf(componenteAtivo) + 1 }} de {{ instrumento.componentesMatriculados.length }}
        </div>
      </div>

      <!-- Grupos de Questões da Avaliação Docente -->
      <div
        v-for="grupo in instrumento.gruposAvaliacaoDocente"
        :key="grupo.id"
        class="bg-white border border-slate-200 rounded-2xl p-5 md:p-7 shadow-xs space-y-6"
      >
        <div class="border-b border-slate-100 pb-3">
          <h4 class="text-sm font-bold text-slate-900 uppercase tracking-wide">{{ grupo.titulo }}</h4>
          <p v-if="grupo.descricao" class="text-xs text-slate-500 mt-0.5">{{ grupo.descricao }}</p>
        </div>

        <div class="space-y-7">
          <div
            v-for="(questao, qIndex) in grupo.questoes"
            :key="questao.id"
            class="space-y-3"
          >
            <label class="block text-sm font-medium text-slate-800 leading-snug">
              <span class="font-bold text-slate-400 mr-1">{{ qIndex + 1 }}.</span>
              {{ questao.enunciado }}
              <span v-if="questao.obrigatoria" class="text-rose-500 ml-0.5">*</span>
            </label>

            <!-- Escala Likert -->
            <div v-if="questao.tipo === 'ESCALA'" class="space-y-2">
              <div class="grid grid-cols-5 gap-2 sm:gap-3">
                <button
                  v-for="valor in 5"
                  :key="valor"
                  type="button"
                  @click="setValorEscala(questao.id, valor, componenteAtivo.id)"
                  :class="[
                    'py-3.5 px-2 rounded-xl text-center font-bold text-base transition-all duration-150 border',
                    getRespostaAtual(questao.id, componenteAtivo.id)?.valorEscala === valor
                      ? 'bg-emerald-600 text-white border-emerald-600 shadow-md ring-2 ring-emerald-500/30 scale-102'
                      : 'bg-slate-50 hover:bg-slate-100 text-slate-700 border-slate-200 hover:border-slate-300'
                  ]"
                >
                  {{ valor }}
                </button>
              </div>
              <div class="flex justify-between text-[11px] text-slate-500 px-1 font-medium">
                <span>{{ questao.rotuloMin || '1 - Não atende' }}</span>
                <span>{{ questao.rotuloMax || '5 - Atende plenamente' }}</span>
              </div>
            </div>

            <!-- Campo Aberto (Texto Livre / Comentários construtivos) -->
            <div v-else-if="questao.tipo === 'TEXTO_LIVRE'" class="space-y-1.5">
              <textarea
                :value="getRespostaAtual(questao.id, componenteAtivo.id)?.textoLivre || ''"
                @input="setTextoLivre(questao.id, ($event.target as HTMLTextAreaElement).value, componenteAtivo.id)"
                rows="3"
                placeholder="Escreva aqui suas considerações para o docente (opcional e mantido sigilo total)..."
                class="w-full text-xs md:text-sm p-3.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-600 outline-hidden transition-all bg-slate-50/50 hover:bg-white"
              ></textarea>
              <div class="flex justify-between text-[11px] text-slate-400">
                <span>{{ questao.ajuda }}</span>
                <span>Salvamento contínuo ativo</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Barra de Ações / Próxima Disciplina ou Conclusão -->
      <div class="flex flex-col sm:flex-row justify-between items-center gap-3 pt-2">
        <button
          type="button"
          @click="emit('mudarEtapa', 'autoavaliacao')"
          class="text-xs text-slate-600 hover:text-slate-900 font-medium px-4 py-2"
        >
          ← Voltar para Autoavaliação
        </button>

        <div class="flex gap-3 w-full sm:w-auto">
          <button
            v-if="!todasDisciplinasConcluidas"
            type="button"
            @click="() => {
              const proxima = instrumento.componentesMatriculados.find(c => !componenteCompleto(c));
              if (proxima) emit('mudarComponente', proxima.id);
            }"
            class="w-full sm:w-auto bg-slate-800 hover:bg-slate-900 text-white font-medium px-5 py-3 rounded-xl text-sm transition-all"
          >
            Próxima Disciplina Pendente →
          </button>

          <button
            v-else
            type="button"
            @click="emit('concluirEnvio')"
            class="w-full sm:w-auto bg-emerald-600 hover:bg-emerald-700 text-white font-bold px-6 py-3 rounded-xl text-sm shadow-md hover:shadow-lg transition-all flex items-center justify-center gap-2"
          >
            <span>✓</span>
            <span>Finalizar e Enviar Ciclo Avaliativo</span>
          </button>
        </div>
      </div>
    </section>

    <!-- TELA DE CONCLUSÃO / SUCESSO DO ENVIO (RF07: Edição permitida durante o período) -->
    <section v-else-if="estado.etapaAtual === 'finalizacao'" class="bg-white border border-emerald-200 rounded-2xl p-6 md:p-10 text-center space-y-5 shadow-xs">
      <div class="w-16 h-16 bg-emerald-100 text-emerald-700 text-3xl font-bold rounded-full flex items-center justify-center mx-auto shadow-inner">
        ✓
      </div>
      <div class="max-w-md mx-auto space-y-2">
        <h3 class="text-xl font-bold text-slate-900">Avaliação Enviada com Sucesso!</h3>
        <p class="text-xs md:text-sm text-slate-600 leading-relaxed">
          Sua contribuição para a melhoria do ensino no IFFar foi registrada com segurança. 
          O anonimato dos seus apontamentos está plenamente resguardado.
        </p>
      </div>

      <div class="bg-slate-50 border border-slate-200 rounded-xl p-4 max-w-md mx-auto text-left text-xs text-slate-700 space-y-1.5">
        <div class="font-semibold text-slate-900 flex justify-between">
          <span>Comprovante de Envio:</span>
          <span class="text-emerald-700 font-mono">HASH-{{ Date.now().toString(36).toUpperCase() }}</span>
        </div>
        <div><strong>Ciclo:</strong> {{ instrumento.anoCiclo }} - {{ instrumento.titulo }}</div>
        <div><strong>Disciplinas avaliadas:</strong> {{ instrumento.componentesMatriculados.length }}</div>
        <div><strong>Data e Hora:</strong> {{ new Date(estado.ultimaModificacao).toLocaleString('pt-BR') }}</div>
      </div>

      <!-- Requisito RF07: permissão de reabertura/edição enquanto o período estiver aberto -->
      <div class="pt-4 border-t border-slate-100 flex flex-col sm:flex-row justify-center gap-3">
        <button
          type="button"
          @click="emit('mudarEtapa', 'autoavaliacao')"
          class="border border-slate-300 hover:border-slate-400 bg-white text-slate-700 px-5 py-2.5 rounded-xl text-xs font-semibold transition-all"
        >
          Revisar ou Editar Respostas (RF07)
        </button>
      </div>
    </section>
  </div>
</template>
