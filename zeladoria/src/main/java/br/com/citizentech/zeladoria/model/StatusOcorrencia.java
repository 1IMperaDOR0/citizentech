package br.com.citizentech.zeladoria.model;

public enum StatusOcorrencia {
 RECEBIDO, EM_ANALISE, EM_MANUTENCAO, CONCLUIDO;

 public boolean permite(StatusOcorrencia destino) {
  return switch(this) {
   case RECEBIDO -> destino == EM_ANALISE;
   case EM_ANALISE -> destino == EM_MANUTENCAO;
   case EM_MANUTENCAO -> destino == CONCLUIDO;
   case CONCLUIDO -> false;
  };
 }
}
