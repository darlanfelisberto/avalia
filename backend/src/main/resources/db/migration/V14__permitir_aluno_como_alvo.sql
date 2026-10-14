-- Alunos vinculados à turma podem ser alvos de uma avaliação de turma.
update avaliacao.tipo_entidade
set pode_ser_avaliada = true
where codigo = 'ALUNO';
