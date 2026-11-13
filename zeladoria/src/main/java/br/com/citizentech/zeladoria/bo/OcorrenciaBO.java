package br.com.citizentech.zeladoria.bo;

import br.com.citizentech.zeladoria.dao.ZeladoriaDAO;
import br.com.citizentech.zeladoria.model.*;
import br.com.citizentech.zeladoria.dto.Requisicoes.*;
import br.com.citizentech.zeladoria.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly=true)

public class OcorrenciaBO {
 private final ZeladoriaDAO dao;

 public OcorrenciaBO(ZeladoriaDAO dao) {
  this.dao=dao;
 }

 public List<Ocorrencia> listar(int limite) {
  return dao.listar(limite);
 }

 public Ocorrencia buscar(long id) {
  return dao.buscar(id, false);
 }

 @Transactional public Ocorrencia criar(CriarOcorrencia input, String usuario, boolean gestor) {
  var d = input.dados();
  validarAutor(input.relato().cidadaoId(), usuario,gestor);

  if(!dao.referencias(d.bairroId(), d.categoriaId(), input.relato().cidadaoId())) {
   throw new NaoEncontradoException("Bairro, categoria ou cidadão inexistente");
  }

  long id = dao.criar(d);
  dao.relatar(id, input.relato());
  dao.historico(id, null, StatusOcorrencia.RECEBIDO, usuario, "Registro inicial");

  return dao.buscar(id,false);
 }

 @Transactional public Ocorrencia atualizar(long id, DadosOcorrencia d) {
  var o = dao.buscar(id,true);

  if(o.status() != StatusOcorrencia.RECEBIDO && o.status()!=StatusOcorrencia.EM_ANALISE) {
   throw new RegraNegocioException("Edição permitida apenas antes da manutenção");
  }

  // As chaves estrangeiras validam bairro e categoria na mesma transação.
  dao.atualizar(id, d);

  return dao.buscar(id, false);
 }

 @Transactional public long relatar(long id,RelatoInput r, String usuario,boolean gestor) {
  validarAutor(r.cidadaoId(), usuario,gestor);

  var o = dao.buscar(id, true);

  if(o.status() == StatusOcorrencia.CONCLUIDO){
   throw new RegraNegocioException("Ocorrência concluída: registre um novo problema se houver reincidência");
  }

  if(!dao.cidadaoExiste(r.cidadaoId())){
   throw new NaoEncontradoException("Cidadão inexistente");
  }

  return dao.relatar(id, r);
 }

 @Transactional public Ocorrencia alterarStatus(long id, AlterarStatus d, String usuario) {
  var o = dao.buscar(id, true);

  if(!o.status().permite(d.status())) {
   throw new RegraNegocioException("Transição de status não permitida");
  }

  if(d.status() == StatusOcorrencia.EM_MANUTENCAO && !dao.temOrdem(id)) {
   throw new RegraNegocioException("Abra uma ordem de serviço antes de iniciar a manutenção");
  }

  dao.status(id,d.status());
  dao.historico(id,o.status(), d.status(), usuario, d.observacao());

  if(d.status() == StatusOcorrencia.CONCLUIDO) {
   dao.concluirOrdem(id);
  }

  return dao.buscar(id, false);
 }

 @Transactional public long abrirOrdem(long id, AbrirOrdem d) {
  var o = dao.buscar(id, true);

  if(o.status() != StatusOcorrencia.EM_ANALISE){
   throw new RegraNegocioException("Ordem exige ocorrência em análise");
  }

  if(dao.temOrdem(id)){
   throw new RegraNegocioException("Já existe ordem para esta ocorrência");
  }

  return dao.abrirOrdem(id, d.equipe());
 }

 @Transactional public void arquivar(long id) {
  var o = dao.buscar(id, true);

  if(o.status()!=StatusOcorrencia.RECEBIDO){
   throw new RegraNegocioException("Apenas registro recebido pode ser arquivado");
  }

  dao.arquivar(id);
 }

 public List<Ocorrencia> semelhantes(long categoria, double latitude, double longitude) {
  return dao.candidatas(categoria).stream().filter(o -> distancia(latitude, longitude, o.latitude().doubleValue(), o.longitude().doubleValue()) <= 30).toList();
 }

 private void validarAutor(long id, String usuario, boolean gestor) {
  if(!gestor && !dao.pertence(id, usuario)){
   throw new RegraNegocioException("Cidadão não corresponde ao usuário autenticado");
  }
 }

 static double distancia(double lat1, double lon1, double lat2, double lon2) {
  double a = Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(Math.toRadians(lon2-lon1)/2), 2);
  return 6371000*2*Math.asin(Math.sqrt(Math.min(1, a)));
 }

 public List<HistoricoStatus> historico(long id) {
  buscar(id);
  return dao.historico(id);
 }

 public List<Relato> relatos(long id) {
  buscar(id);
  return dao.relatos(id);
 }

 public List<OrdemServico> ordens(long id) {
  buscar(id);
  return dao.ordens(id);
 }
}
