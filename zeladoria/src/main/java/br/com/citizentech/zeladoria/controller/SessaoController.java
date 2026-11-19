package br.com.citizentech.zeladoria.controller;
import br.com.citizentech.zeladoria.bo.SessaoBO;
import br.com.citizentech.zeladoria.dto.SessaoResposta;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/sessao")
public class SessaoController {
 private final SessaoBO bo;
 public SessaoController(SessaoBO bo) {this.bo=bo;}
 @GetMapping public SessaoResposta identificar(Authentication usuario) {
  boolean gestor=usuario.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR"));
  return bo.identificar(usuario.getName(),gestor);
 }
}
