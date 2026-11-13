package br.com.citizentech.zeladoria.exception;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.slf4j.LoggerFactory;

@RestControllerAdvice

public class TratadorExcecoes {
 private ProblemDetail erro(HttpStatus s, String mensagem) {
  return ProblemDetail.forStatusAndDetail(s, mensagem);
 }

 @ExceptionHandler(NaoEncontradoException.class)

 @ResponseStatus(HttpStatus.NOT_FOUND)

 public ProblemDetail ausente(NaoEncontradoException e) {
  return erro(HttpStatus.NOT_FOUND, e.getMessage());
 }

 @ExceptionHandler(RegraNegocioException.class)

 @ResponseStatus(HttpStatus.CONFLICT)

 public ProblemDetail regra(RegraNegocioException e) {
  return erro(HttpStatus.CONFLICT, e.getMessage());
 }

 @ExceptionHandler(MethodArgumentNotValidException.class)

 @ResponseStatus(HttpStatus.BAD_REQUEST)

 public ProblemDetail validacao(MethodArgumentNotValidException e) {
  var p = erro(HttpStatus.BAD_REQUEST, "Campos inválidos");

  p.setProperty("campos", e.getBindingResult().getFieldErrors().stream().map(f -> f.getField()+": "+f.getDefaultMessage()).toList());

  return p;
 }

 @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, ConstraintViolationException.class})

 @ResponseStatus(HttpStatus.BAD_REQUEST)

 public ProblemDetail formato(Exception e) {
  return erro(HttpStatus.BAD_REQUEST, "JSON, parâmetro ou valor inválido");
 }

 @ExceptionHandler(DataIntegrityViolationException.class)

 @ResponseStatus(HttpStatus.CONFLICT)

 public ProblemDetail integridade(DataIntegrityViolationException e) {
  return erro(HttpStatus.CONFLICT, "Operação viola um vínculo, unicidade ou restrição dos dados");
 }

 @ExceptionHandler(DataAccessException.class)

 @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)

 public ProblemDetail banco(DataAccessException e) {
  LoggerFactory.getLogger(getClass()).error("Falha de acesso ao banco: {}", e.getClass().getSimpleName());
  return erro(HttpStatus.SERVICE_UNAVAILABLE, "Banco indisponível ou operação de persistência não concluída");
 }
}
