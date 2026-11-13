package br.com.citizentech.zeladoria;
import org.junit.jupiter.api.Test;
import br.com.citizentech.zeladoria.model.*;
import static org.junit.jupiter.api.Assertions.*;
class PrioridadeTest {
 @Test void limites(){
  assertEquals(new Prioridade(0,"BAIXA"),Prioridade.calcular(false,false,false));
  assertEquals(new Prioridade(20,"MEDIA"),Prioridade.calcular(false,false,true));
  assertEquals(new Prioridade(50,"ALTA"),Prioridade.calcular(true,false,false));
  assertEquals(new Prioridade(70,"CRITICA"),Prioridade.calcular(true,false,true));
  assertEquals(new Prioridade(100,"CRITICA"),Prioridade.calcular(true,true,true));
 }
 @Test void fluxo(){
  assertFalse(StatusOcorrencia.RECEBIDO.permite(StatusOcorrencia.CONCLUIDO));
  assertFalse(StatusOcorrencia.CONCLUIDO.permite(StatusOcorrencia.EM_ANALISE));
  assertTrue(StatusOcorrencia.EM_MANUTENCAO.permite(StatusOcorrencia.CONCLUIDO));
 }
}
