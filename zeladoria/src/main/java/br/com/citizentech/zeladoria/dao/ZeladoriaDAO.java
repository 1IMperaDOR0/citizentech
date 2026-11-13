package br.com.citizentech.zeladoria.dao;

import br.com.citizentech.zeladoria.model.*;
import br.com.citizentech.zeladoria.dto.Requisicoes.*;
import br.com.citizentech.zeladoria.exception.NaoEncontradoException;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.*;
import java.util.*;
import java.sql.*;

@Repository

public class ZeladoriaDAO {
 private final JdbcTemplate jdbc;

 public ZeladoriaDAO(JdbcTemplate jdbc) {
  this.jdbc = jdbc;
 }

 private static final RowMapper<Ocorrencia> MAPA = (rs, n) -> new Ocorrencia(rs.getLong("ID_OCORRENCIA"), rs.getLong("ID_BAIRRO"), rs.getLong("ID_CATEGORIA"), rs.getString("DESCRICAO"), rs.getBigDecimal("LATITUDE"), rs.getBigDecimal("LONGITUDE"), rs.getInt("BLOQUEIO_TOTAL") == 1, rs.getInt("PROXIMO_HOSPITAL") == 1, rs.getInt("PROXIMO_ESCOLA") == 1, StatusOcorrencia.valueOf(rs.getString("STATUS")), rs.getTimestamp("CRIADA_EM").toLocalDateTime(), rs.getInt("ATIVA") == 1);

 private long inserir(String sql, String coluna, Object... args) {
  KeyHolder chave = new GeneratedKeyHolder();

  jdbc.update(con -> {
    var ps = con.prepareStatement(sql,new String[]{coluna});

    for(int i=0; i<args.length; i++){
     ps.setObject(i+1,args[i]);
    }

    return ps;
   }, chave
  );

  return Objects.requireNonNull(chave.getKey()).longValue();
 }

 public List<Ocorrencia> listar(int limite) {
  return jdbc.query("SELECT * FROM GS_OCORRENCIA WHERE ATIVA=1 ORDER BY (BLOQUEIO_TOTAL*50+PROXIMO_HOSPITAL*30+PROXIMO_ESCOLA*20) DESC, CRIADA_EM, ID_OCORRENCIA FETCH FIRST ? ROWS ONLY",MAPA,limite);
 }

 public Ocorrencia buscar(long id, boolean bloquear) {
  var lista=jdbc.query("SELECT * FROM GS_OCORRENCIA WHERE ID_OCORRENCIA=? AND ATIVA=1"+(bloquear?" FOR UPDATE":""), MAPA, id);

  if(lista.isEmpty())throw new NaoEncontradoException("Ocorrência não encontrada");

  return lista.getFirst();
 }

 public boolean referencias(long bairro, long categoria, long cidadao) {
  return jdbc.queryForObject("SELECT COUNT(*) FROM GS_BAIRRO b CROSS JOIN GS_CATEGORIA c CROSS JOIN GS_CIDADAO p WHERE b.ID_BAIRRO=? AND c.ID_CATEGORIA=? AND p.ID_CIDADAO=?", Integer.class, bairro, categoria, cidadao) == 1;
 }

 public boolean pertence(long id,String email) {
  return jdbc.queryForObject("SELECT COUNT(*) FROM GS_CIDADAO WHERE ID_CIDADAO=? AND EMAIL=?", Integer.class, id, email.toLowerCase(Locale.ROOT)) == 1;
 }

 public boolean cidadaoExiste(long id) {
  return jdbc.queryForObject("SELECT COUNT(*) FROM GS_CIDADAO WHERE ID_CIDADAO=?", Integer.class, id) == 1;
 }

 public long criar(DadosOcorrencia d) {
  return inserir("INSERT INTO GS_OCORRENCIA (ID_BAIRRO,ID_CATEGORIA,DESCRICAO,LATITUDE,LONGITUDE,BLOQUEIO_TOTAL,PROXIMO_HOSPITAL,PROXIMO_ESCOLA) VALUES (?,?,?,?,?,?,?,?)","ID_OCORRENCIA", d.bairroId(), d.categoriaId(), d.descricao(), d.latitude(), d.longitude(), d.bloqueioTotal()?1:0, d.proximoHospital()?1:0, d.proximoEscola()?1:0);
 }

 public void atualizar(long id, DadosOcorrencia d) {
  jdbc.update("UPDATE GS_OCORRENCIA SET ID_BAIRRO=?,ID_CATEGORIA=?,DESCRICAO=?,LATITUDE=?,LONGITUDE=?,BLOQUEIO_TOTAL=?,PROXIMO_HOSPITAL=?,PROXIMO_ESCOLA=? WHERE ID_OCORRENCIA=?", d.bairroId(), d.categoriaId(), d.descricao(), d.latitude(), d.longitude(), d.bloqueioTotal()?1:0, d.proximoHospital()?1:0, d.proximoEscola()?1:0, id);
 }

 public long relatar(long id,RelatoInput r) {
  return inserir("INSERT INTO GS_RELATO (ID_OCORRENCIA,ID_CIDADAO,DESCRICAO,FOTO_URL) VALUES (?,?,?,?)", "ID_RELATO", id, r.cidadaoId(), r.descricao(), r.fotoUrl());
 }

 public void historico(long id, StatusOcorrencia anterior, StatusOcorrencia novo, String usuario, String obs) {
  inserir("INSERT INTO GS_HISTORICO_STATUS (ID_OCORRENCIA,STATUS_ANTERIOR,STATUS_NOVO,RESPONSAVEL,OBSERVACAO) VALUES (?,?,?,?,?)", "ID_HISTORICO", id, anterior == null?null:anterior.name(), novo.name(), usuario,obs);
 }

 public void status(long id,StatusOcorrencia novo) {
  jdbc.update("UPDATE GS_OCORRENCIA SET STATUS=? WHERE ID_OCORRENCIA=?", novo.name(), id);
 }

 public boolean temOrdem(long id) {
  return jdbc.queryForObject("SELECT COUNT(*) FROM GS_ORDEM_SERVICO WHERE ID_OCORRENCIA=?", Integer.class, id) > 0;
 }

 public long abrirOrdem(long id,String equipe) {
  return inserir("INSERT INTO GS_ORDEM_SERVICO (ID_OCORRENCIA,EQUIPE) VALUES (?,?)", "ID_ORDEM", id, equipe);
 }

 public void concluirOrdem(long id) {
  jdbc.update("UPDATE GS_ORDEM_SERVICO SET CONCLUIDA_EM=CURRENT_TIMESTAMP WHERE ID_OCORRENCIA=?", id);
 }

 public void arquivar(long id) {
  jdbc.update("UPDATE GS_OCORRENCIA SET ATIVA=0 WHERE ID_OCORRENCIA=?", id);
 }

 public List<Bairro> bairros() {
  return jdbc.query("SELECT * FROM GS_BAIRRO ORDER BY NOME", (r, n) -> new Bairro(r.getLong("ID_BAIRRO"), r.getString("NOME"), r.getString("CIDADE"), r.getString("UF")));
 }

 public List<Categoria> categorias() {
  return jdbc.query("SELECT * FROM GS_CATEGORIA ORDER BY NOME", (r, n) -> new Categoria(r.getLong("ID_CATEGORIA"), r.getString("NOME")));
 }

 public long cadastrar(CadastrarCidadao c) {
  return inserir("INSERT INTO GS_CIDADAO (NOME,EMAIL) VALUES (?,?)", "ID_CIDADAO", c.nome().trim(), c.email().trim().toLowerCase(Locale.ROOT));
 }

 public List<Relato> relatos(long id) {
  return jdbc.query("SELECT * FROM GS_RELATO WHERE ID_OCORRENCIA=? ORDER BY CRIADO_EM,ID_RELATO", (r, n) -> new Relato(r.getLong("ID_RELATO"), id, r.getLong("ID_CIDADAO"), r.getString("DESCRICAO"), r.getString("FOTO_URL"), r.getTimestamp("CRIADO_EM").toLocalDateTime()), id);
 }

 public List<HistoricoStatus> historico(long id) {
  return jdbc.query("SELECT * FROM GS_HISTORICO_STATUS WHERE ID_OCORRENCIA=? ORDER BY ALTERADO_EM,ID_HISTORICO", (r, n) -> new HistoricoStatus(r.getLong("ID_HISTORICO"), id, r.getString("STATUS_ANTERIOR"), StatusOcorrencia.valueOf(r.getString("STATUS_NOVO")), r.getString("RESPONSAVEL"), r.getString("OBSERVACAO"), r.getTimestamp("ALTERADO_EM").toLocalDateTime()), id);
 }

 public List<OrdemServico> ordens(long id) {
  return jdbc.query("SELECT * FROM GS_ORDEM_SERVICO WHERE ID_OCORRENCIA=?", (r, n) -> new OrdemServico(r.getLong("ID_ORDEM"), id, r.getString("EQUIPE"), r.getTimestamp("ABERTA_EM").toLocalDateTime(), r.getTimestamp("CONCLUIDA_EM") == null?null:r.getTimestamp("CONCLUIDA_EM").toLocalDateTime()), id);
 }

 public List<Ocorrencia> candidatas(long categoria) {
  return jdbc.query("SELECT * FROM GS_OCORRENCIA WHERE ID_CATEGORIA=? AND ATIVA=1 AND STATUS<>'CONCLUIDO'", MAPA, categoria);
 }
}
