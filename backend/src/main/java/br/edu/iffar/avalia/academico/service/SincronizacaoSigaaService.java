package br.edu.iffar.avalia.academico.service;

import br.edu.iffar.avalia.avaliacao.service.AvaliacaoService;
import br.edu.iffar.avalia.avaliacao.dao.ResponderAvaliacaoDao;
import br.edu.iffar.avalia.academico.dao.SincronizacaoSigaaDao;
import br.edu.iffar.avalia.academico.dto.SincronizacaoRequestDTO;
import br.edu.iffar.avalia.academico.dto.SincronizacaoStatusDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serviço responsável por sincronizar os dados do banco SIGAA com o schema 'academico' do AVALIA.
 * Executa as cargas em ordem topológica utilizando operações em lote (Batch Upsert).
 */
@ApplicationScoped
public class SincronizacaoSigaaService {

    private static final Logger LOGGER = Logger.getLogger(SincronizacaoSigaaService.class.getName());

    @Inject
    private SigaaConnectionProvider connectionProvider;

    @Inject
    private SincronizacaoSigaaDao dao;

    @Inject
    private AvaliacaoService avaliacaoService;

    @Inject
    private ResponderAvaliacaoDao respostaDao;

    private final AtomicBoolean emExecucao = new AtomicBoolean(false);

    public boolean isEmExecucao() {
        return emExecucao.get();
    }

    /**
     * Retorna o status atual de conectividade e contadores do schema academico.
     */
    public SincronizacaoStatusDTO getStatus() {
        SincronizacaoStatusDTO status = new SincronizacaoStatusDTO();
        status.urlSigaa = connectionProvider.getRecursoIdentificador();
        status.usuarioSigaa = "DataSource JNDI (Open Liberty)";
        status.conectadoSigaa = connectionProvider.testConnection();
        status.emExecucao = emExecucao.get();
        status.contadoresLocais = dao.obterContadoresLocais();
        return status;
    }

    /**
     * Executa a rotina completa ou parcial de sincronização.
     */
    public Map<String, Object> executarSincronizacao(SincronizacaoRequestDTO req) {
        if (!emExecucao.compareAndSet(false, true)) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("sucesso", false);
            resp.put("mensagem", "Uma sincronização já está em andamento.");
            return resp;
        }

        int ano = (req != null && req.ano != null) ? req.ano : 2025;
        Integer periodo = (req != null) ? req.periodo : null;
        boolean syncEstrutura = (req == null || req.sincronizarEstrutura == null || req.sincronizarEstrutura);
        boolean syncTurmas = (req == null || req.sincronizarTurmasMatriculas == null || req.sincronizarTurmasMatriculas);

        Map<String, Object> resultado = new HashMap<>();
        new Thread(() -> {
            try {
                LOGGER.info(">>> Iniciando sincronização SIGAA -> AVALIA (Ano: " + ano + ", Período: " + periodo + ") <<<");
                dao.sincronizar(ano, periodo, syncEstrutura, syncTurmas);
                respostaDao.atualizarTiposUsuario();
                avaliacaoService.sincronizarEntidadesAcademicas();

                LOGGER.info(">>> Sincronização SIGAA -> AVALIA concluída com sucesso! <<<");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erro durante a sincronização com o SIGAA", e);
            } finally {
                emExecucao.set(false);
            }
        }).start();

        resultado.put("sucesso", true);
        resultado.put("mensagem", "Sincronização iniciada em segundo plano para o ano " + ano + ".");
        return resultado;
    }

}
