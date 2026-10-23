import { createRouter, createWebHistory } from 'vue-router';
import InstrumentoAvaliativoList from './components/InstrumentoAvaliativoList.vue';
import InstrumentoFormularioView from './components/InstrumentoFormularioView.vue';
import InstrumentoEstruturaView from './components/InstrumentoEstruturaView.vue';
import PermissoesTiposEntidadeView from './components/PermissoesTiposEntidadeView.vue';
import ResponderAvaliacoesView from './components/ResponderAvaliacoesView.vue';
import FormularioResponderAvaliacao from './components/FormularioResponderAvaliacao.vue';
import SincronizacaoView from './components/SincronizacaoView.vue';
import AcessoNegadoView from './components/AcessoNegadoView.vue';
import { carregarUsuarioAtivo, temPermissao } from './services/storage';
import type { UsuarioContexto } from './types/avaliacao';

type RotaAutenticada = {
  exigeGerenciaInstrumentos?: boolean;
  permissao?: 'INSTRUMENTO_CRIAR' | 'AVALIACAO_RESPONDER' | 'SIGAA_SINCRONIZAR';
};

function podeGerenciarInstrumentos(usuario: UsuarioContexto): boolean {
  return usuario.papeis.some(papel => papel === 'CPA' || papel === 'ADMIN')
    && temPermissao(usuario, 'INSTRUMENTO_VISUALIZAR');
}

function rotaInicial(usuario: UsuarioContexto | null): string {
  if (!usuario) return '/responder';
  if (podeGerenciarInstrumentos(usuario)) return '/instrumentos';
  if (temPermissao(usuario, 'SIGAA_SINCRONIZAR')) return '/sincronizacao';
  return '/responder';
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: () => rotaInicial(carregarUsuarioAtivo()) },
    { path: '/responder', name: 'responder', component: ResponderAvaliacoesView },
    { path: '/responder/:instrumentoId', name: 'responder-formulario', component: FormularioResponderAvaliacao },
    { path: '/instrumentos', name: 'instrumentos', component: InstrumentoAvaliativoList, meta: { exigeGerenciaInstrumentos: true } },
    { path: '/instrumentos/novo', name: 'instrumento-novo', component: InstrumentoFormularioView, meta: { exigeGerenciaInstrumentos: true, permissao: 'INSTRUMENTO_CRIAR' } },
    { path: '/instrumentos/:id', name: 'instrumento-editar', component: InstrumentoFormularioView, meta: { exigeGerenciaInstrumentos: true } },
    { path: '/instrumentos/:id/estrutura', name: 'instrumento-estrutura', component: InstrumentoEstruturaView, meta: { exigeGerenciaInstrumentos: true } },
    { path: '/permissoes', name: 'permissoes', component: PermissoesTiposEntidadeView, meta: { exigeGerenciaInstrumentos: true } },
    { path: '/sincronizacao', name: 'sincronizacao', component: SincronizacaoView, meta: { permissao: 'SIGAA_SINCRONIZAR' } },
    { path: '/acesso-negado', name: 'acesso-negado', component: AcessoNegadoView },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
});

router.beforeEach(to => {
  const usuario = carregarUsuarioAtivo();
  // A tela de login do App continua responsável por autenticar usuários sem sessão.
  if (!usuario || to.name === 'acesso-negado') return true;

  const meta = to.meta as RotaAutenticada;
  const autorizado = (!meta.exigeGerenciaInstrumentos || podeGerenciarInstrumentos(usuario))
    && (!meta.permissao || temPermissao(usuario, meta.permissao));

  return autorizado ? true : rotaInicial(usuario);
});

export { podeGerenciarInstrumentos, rotaInicial };
export default router;
