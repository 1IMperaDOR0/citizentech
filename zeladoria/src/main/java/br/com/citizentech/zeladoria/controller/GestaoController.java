package br.com.citizentech.zeladoria.controller;

import br.com.citizentech.zeladoria.bo.*;
import br.com.citizentech.zeladoria.dto.Requisicoes.CadastrarCidadao;
import br.com.citizentech.zeladoria.model.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.util.*;

@RestController @RequestMapping("/api")
public class GestaoController {
 private final CadastroBO cadastro;
 private final OcorrenciaBO ocorrencias;

 public GestaoController(CadastroBO c, OcorrenciaBO o){
  cadastro = c;
  ocorrencias = o;
 }

 @GetMapping("/bairros") public List<Bairro> bairros(){
  return cadastro.bairros();
 }

 @GetMapping("/categorias") public List<Categoria> categorias(){
  return cadastro.categorias();
 }

 @PostMapping("/cidadaos") public ResponseEntity<Map<String,Long>> cadastrar(@Valid @RequestBody CadastrarCidadao c){
  return ResponseEntity.status(201).body(Map.of("id", cadastro.cadastrar(c)));
 }

 @GetMapping("/gestao/ocorrencias/{id}/relatos") public List<Relato> relatos(@PathVariable long id){
  return ocorrencias.relatos(id);
 }

 @GetMapping("/gestao/ocorrencias/{id}/ordens-servico") public List<OrdemServico> ordens(@PathVariable long id){
  return ocorrencias.ordens(id);
 }
}
