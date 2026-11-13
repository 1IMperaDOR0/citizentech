package br.com.citizentech.zeladoria.dto;

import br.com.citizentech.zeladoria.model.*;

public record OcorrenciaResposta(Ocorrencia ocorrencia, Prioridade prioridade) {
 public static OcorrenciaResposta de(Ocorrencia o) {
  return new OcorrenciaResposta(o, o.prioridade());
 }
}
