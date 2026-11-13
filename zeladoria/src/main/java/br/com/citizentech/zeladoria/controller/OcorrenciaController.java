package br.com.citizentech.zeladoria.controller;

import br.com.citizentech.zeladoria.bo.OcorrenciaBO;
import br.com.citizentech.zeladoria.dto.*;
import br.com.citizentech.zeladoria.dto.Requisicoes.*;
import br.com.citizentech.zeladoria.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import org.springframework.security.core.Authentication;
import java.util.*;

@RestController
@RequestMapping("/api/ocorrencias")
@Validated

public class OcorrenciaController {
 private boolean gestor(Authentication p) {
  return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR"));
 }

 private final OcorrenciaBO bo;

 public OcorrenciaController(OcorrenciaBO bo) {
  this.bo = bo;
 }

 @GetMapping public List<OcorrenciaResposta> listar(@RequestParam(defaultValue = "100") @Min(1) @Max(500) int limite) {
  return bo.listar(limite).stream().map(OcorrenciaResposta::de).toList();
 }

 @GetMapping("/semelhantes") public List<OcorrenciaResposta> semelhantes(@RequestParam @Positive long categoriaId, @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitude, @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitude) {
  return bo.semelhantes(categoriaId, latitude, longitude).stream().map(OcorrenciaResposta::de).toList();
 }

 @GetMapping("/{id}") public OcorrenciaResposta buscar(@PathVariable @Positive long id) {
  return OcorrenciaResposta.de(bo.buscar(id));
 }

 @PostMapping public ResponseEntity<OcorrenciaResposta> criar(@RequestBody @Valid CriarOcorrencia d, Authentication p) {
  var o = bo.criar(d,p.getName(), gestor(p));
  return ResponseEntity.created(URI.create("/api/ocorrencias/"+o.id())).body(OcorrenciaResposta.de(o));
 }

 @PutMapping("/{id}") public OcorrenciaResposta editar(@PathVariable @Positive long id, @RequestBody @Valid DadosOcorrencia d) {
  return OcorrenciaResposta.de(bo.atualizar(id, d));
 }

 @PutMapping("/{id}/status") public OcorrenciaResposta status(@PathVariable @Positive long id, @RequestBody @Valid AlterarStatus d, Authentication p) {
  return OcorrenciaResposta.de(bo.alterarStatus(id, d, p.getName()));
 }

 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable @Positive long id) {
  bo.arquivar(id);
  return ResponseEntity.noContent().build();
 }

 @PostMapping("/{id}/relatos") public ResponseEntity<Map<String,Long>> relatar(@PathVariable @Positive long id, @RequestBody @Valid RelatoInput d, Authentication p) {
  return ResponseEntity.status(201).body(Map.of("id", bo.relatar(id, d, p.getName(), gestor(p))));
 }

 @PostMapping("/{id}/ordens-servico") public ResponseEntity<Map<String,Long>> abrir(@PathVariable @Positive long id, @RequestBody @Valid AbrirOrdem d) {
  return ResponseEntity.status(201).body(Map.of("id", bo.abrirOrdem(id, d)));
 }

 // Projeção pública sem identificadores do cidadão, nome do gestor, foto ou texto livre de histórico.
 public record EventoPublico(StatusOcorrencia status,java.time.LocalDateTime alteradoEm) {}

 @GetMapping("/{id}/historico") public List<EventoPublico> historico(@PathVariable @Positive long id) {
  return bo.historico(id).stream().map(h -> new EventoPublico(h.statusNovo(), h.alteradoEm())).toList();
 }
}
