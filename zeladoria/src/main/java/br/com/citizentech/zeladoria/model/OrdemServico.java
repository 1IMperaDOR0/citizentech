package br.com.citizentech.zeladoria.model;

public record OrdemServico(long id, long ocorrenciaId, String equipe, java.time.LocalDateTime abertaEm, java.time.LocalDateTime concluidaEm) {}
