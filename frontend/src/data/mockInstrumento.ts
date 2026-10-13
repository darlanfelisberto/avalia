import type { InstrumentoAvaliativo } from '../types/avaliacao';

export const instrumentoADPDMock: InstrumentoAvaliativo = {
  id: 'inst-adpd-2026-1',
  titulo: 'Avaliação Docente pelo Discente (ADPD)',
  subtitulo: 'Instituto Federal Farroupilha - Resolução CONSUP nº 104/2013',
  modulo: 'ADPD',
  anoCiclo: '2026/1',
  periodoLetivo: '1º Semestre de 2026',
  dataTermino: '2026-11-30T23:59:59',
  anonimo: true,
  componentesMatriculados: [
    {
      id: 'comp-1',
      codigo: 'IFF-ADS-01',
      nome: 'Arquitetura e Engenharia de Software',
      docenteNome: 'Prof. Dr. Roberto Alencar',
      turma: 'ADS-2026'
    },
    {
      id: 'comp-2',
      codigo: 'IFF-ADS-02',
      nome: 'Banco de Dados Avançado',
      docenteNome: 'Profa. Dra. Mariana Silveira',
      turma: 'ADS-2026'
    },
    {
      id: 'comp-3',
      codigo: 'IFF-ADS-03',
      nome: 'Redes e Sistemas Distribuídos',
      docenteNome: 'Prof. Me. Carlos Eduardo Lima',
      turma: 'ADS-2026'
    }
  ],
  gruposAutoavaliacaoDiscente: [
    {
      id: 'grp-auto-1',
      titulo: 'Etapa 1: Autoavaliação do Estudante',
      descricao: 'Reflita sobre sua dedicação, pontualidade e engajamento nas atividades acadêmicas ao longo do semestre letivo.',
      questoes: [
        {
          id: 'q-auto-01',
          enunciado: 'Mantive frequência regular, pontualidade e dedicação compatível com a carga horária das disciplinas cursadas.',
          tipo: 'ESCALA',
          obrigatoria: true,
          escalaMin: 1,
          escalaMax: 5,
          rotuloMin: '1 - Nunca / Raramente',
          rotuloMax: '5 - Sempre / Plenamente',
          ajuda: 'Considere presença em aulas síncronas/presenciais e prazos de entregas.'
        },
        {
          id: 'q-auto-02',
          enunciado: 'Participei ativamente das aulas, esclarecendo dúvidas e colaborando com os colegas nos trabalhos em equipe.',
          tipo: 'ESCALA',
          obrigatoria: true,
          escalaMin: 1,
          escalaMax: 5,
          rotuloMin: '1 - Insatisfatório',
          rotuloMax: '5 - Muito Participativo'
        },
        {
          id: 'q-auto-03',
          enunciado: 'Utilizei os canais de atendimento extraclasse (monitoria, atendimento ao aluno) quando necessário.',
          tipo: 'ESCOLHA_UNICA',
          obrigatoria: true,
          opcoes: [
            { id: 'opt-sim', texto: 'Sim, utilizei com frequência' },
            { id: 'opt-as-vezes', texto: 'Ocasionalmente, quando tive dúvidas pontuais' },
            { id: 'opt-nao-precisei', texto: 'Não senti necessidade' },
            { id: 'opt-nao-consegui', texto: 'Tive dificuldades de horário para acessar' }
          ]
        }
      ]
    }
  ],
  gruposAvaliacaoDocente: [
    {
      id: 'grp-doc-didatica',
      titulo: 'Dimensão Pedagógica e Prática Docente',
      descricao: 'Avalie a condução da disciplina, planejamento, métodos de ensino e domínio do conteúdo pelo docente.',
      questoes: [
        {
          id: 'q-doc-01',
          enunciado: 'O docente apresentou o plano de ensino (objetivos, metodologia e critérios de avaliação) no início das aulas.',
          tipo: 'ESCALA',
          obrigatoria: true,
          escalaMin: 1,
          escalaMax: 5,
          rotuloMin: '1 - Não apresentou',
          rotuloMax: '5 - Apresentou com clareza'
        },
        {
          id: 'q-doc-02',
          enunciado: 'Demonstra domínio aprofundado dos conteúdos e relaciona a teoria com aplicações práticas relevantes.',
          tipo: 'ESCALA',
          obrigatoria: true,
          escalaMin: 1,
          escalaMax: 5,
          rotuloMin: '1 - Pouco domínio',
          rotuloMax: '5 - Excelente domínio'
        },
        {
          id: 'q-doc-03',
          enunciado: 'Utiliza recursos e estratégias didáticas que facilitam a compreensão e incentivam o pensamento crítico.',
          tipo: 'ESCALA',
          obrigatoria: true,
          escalaMin: 1,
          escalaMax: 5,
          rotuloMin: '1 - Rígido / Pouco claro',
          rotuloMax: '5 - Dinâmico e Eficaz'
        }
      ]
    },
    {
      id: 'grp-doc-relacionamento',
      titulo: 'Avaliação da Aprendizagem e Relação Dialógica',
      descricao: 'Avalie a pontualidade na devolutiva das avaliações e a disponibilidade para esclarecimento de dúvidas.',
      questoes: [
        {
          id: 'q-doc-04',
          enunciado: 'Fornece devolutivas (feedback) construtivas sobre as atividades e provas em tempo hábil para o aprendizado.',
          tipo: 'ESCALA',
          obrigatoria: true,
          escalaMin: 1,
          escalaMax: 5,
          rotuloMin: '1 - Sem devolutiva',
          rotuloMax: '5 - Feedback pontual e detalhado'
        },
        {
          id: 'q-doc-05',
          enunciado: 'Mantém postura ética, respeitosa e acessível para o diálogo acadêmico com a turma.',
          tipo: 'ESCALA',
          obrigatoria: true,
          escalaMin: 1,
          escalaMax: 5,
          rotuloMin: '1 - Inadequado',
          rotuloMax: '5 - Sempre exemplar'
        },
        {
          id: 'q-doc-06',
          enunciado: 'Comentários abertos e sugestões para o docente (opcional e preservado o anonimato):',
          tipo: 'TEXTO_LIVRE',
          obrigatoria: false,
          ajuda: 'Escreva observações construtivas que possam auxiliar no aprimoramento pedagógico da disciplina.'
        }
      ]
    }
  ]
};
