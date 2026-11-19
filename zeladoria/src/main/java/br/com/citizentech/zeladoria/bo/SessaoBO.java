package br.com.citizentech.zeladoria.bo;
import br.com.citizentech.zeladoria.dao.SessaoDAO;
import br.com.citizentech.zeladoria.dto.SessaoResposta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class SessaoBO {
 private final SessaoDAO dao;
 public SessaoBO(SessaoDAO dao) {this.dao=dao;}
 public SessaoResposta identificar(String usuario,boolean gestor) {
  return new SessaoResposta(usuario,gestor?"GESTOR":"CIDADAO",gestor?null:dao.cidadaoPorEmail(usuario));
 }
}
