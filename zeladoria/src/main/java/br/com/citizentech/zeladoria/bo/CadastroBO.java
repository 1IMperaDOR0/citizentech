package br.com.citizentech.zeladoria.bo;

import br.com.citizentech.zeladoria.dao.ZeladoriaDAO;
import br.com.citizentech.zeladoria.model.*;
import br.com.citizentech.zeladoria.dto.Requisicoes.CadastrarCidadao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly=true)

public class CadastroBO {
 private final ZeladoriaDAO dao;

 public CadastroBO(ZeladoriaDAO dao) {
  this.dao = dao;
 }

 public List<Bairro> bairros(){
  return dao.bairros();
 }

 public List<Categoria> categorias(){
  return dao.categorias();
 }

 @Transactional public long cadastrar(CadastrarCidadao c){
  return dao.cadastrar(c);
 }
}
