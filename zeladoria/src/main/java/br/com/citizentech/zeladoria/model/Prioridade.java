package br.com.citizentech.zeladoria.model;

public record Prioridade(int pontos, String nivel) {
 public static Prioridade calcular(boolean bloqueioTotal, boolean proximoHospital, boolean proximoEscola) {
  int pontos = (bloqueioTotal ? 50 : 0) + (proximoHospital ? 30 : 0) + (proximoEscola ? 20 : 0);
  return new Prioridade(pontos, pontos >= 70 ? "CRITICA" : pontos >= 50 ? "ALTA" : pontos >= 20 ? "MEDIA" : "BAIXA");
 }
}
