package br.com.citizentech.zeladoria.dto;
/** Identificação do perfil autenticado; não retorna senha ou cabeçalho de autenticação. */
public record SessaoResposta(String usuario, String perfil, Long cidadaoId) {}
