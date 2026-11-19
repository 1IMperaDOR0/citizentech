package br.com.citizentech.zeladoria.dao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Locale;
@Repository
public class SessaoDAO {
 private final JdbcTemplate jdbc;
 public SessaoDAO(JdbcTemplate jdbc) {this.jdbc=jdbc;}
 public Long cidadaoPorEmail(String email) {
  var ids=jdbc.query("SELECT ID_CIDADAO FROM GS_CIDADAO WHERE EMAIL=?",(r,n)->r.getLong(1),email.toLowerCase(Locale.ROOT));
  return ids.isEmpty()?null:ids.getFirst();
 }
}
