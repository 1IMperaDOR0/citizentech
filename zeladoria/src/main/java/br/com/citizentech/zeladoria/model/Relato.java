package br.com.citizentech.zeladoria.model;

public record Relato(long id, long ocorrenciaId, long cidadaoId, String descricao, String fotoUrl, java.time.LocalDateTime criadoEm) {}
