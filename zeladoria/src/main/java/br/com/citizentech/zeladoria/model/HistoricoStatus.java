package br.com.citizentech.zeladoria.model;

public record HistoricoStatus(long id, long ocorrenciaId, String statusAnterior, StatusOcorrencia statusNovo, String responsavel, String observacao, java.time.LocalDateTime alteradoEm) {}
