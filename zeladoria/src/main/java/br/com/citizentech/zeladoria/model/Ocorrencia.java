package br.com.citizentech.zeladoria.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Ocorrencia(long id, long bairroId, long categoriaId, String descricao,
 BigDecimal latitude, BigDecimal longitude, boolean bloqueioTotal, boolean proximoHospital,
 boolean proximoEscola, StatusOcorrencia status, LocalDateTime criadaEm, boolean ativa) {
 public Prioridade prioridade() {
  return Prioridade.calcular(bloqueioTotal, proximoHospital, proximoEscola);
 }
}
