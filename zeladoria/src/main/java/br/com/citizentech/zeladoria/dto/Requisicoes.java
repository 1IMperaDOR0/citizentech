package br.com.citizentech.zeladoria.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import br.com.citizentech.zeladoria.model.StatusOcorrencia;

public final class Requisicoes {
 private Requisicoes() {}

 public record RelatoInput(@NotNull @Positive Long cidadaoId,
  @NotBlank @Size(max=1000) String descricao,
  @NotBlank @Size(max=500) @Pattern(regexp="https://[^\\s]+", message="deve ser URL HTTPS") String fotoUrl) {}

 public record DadosOcorrencia(@NotNull @Positive Long bairroId, @NotNull @Positive Long categoriaId,
  @NotBlank @Size(max=1000) String descricao,
  @NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer=3,fraction=6) BigDecimal latitude,
  @NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer=3,fraction=6) BigDecimal longitude,
  @NotNull Boolean bloqueioTotal, @NotNull Boolean proximoHospital, @NotNull Boolean proximoEscola) {}

 public record CriarOcorrencia(@NotNull @Valid DadosOcorrencia dados, @NotNull @Valid RelatoInput relato) {}

 public record AlterarStatus(@NotNull StatusOcorrencia status, @NotBlank @Size(max=500) String observacao) {}

 public record AbrirOrdem(@NotBlank @Size(max=100) String equipe) {}

 public record CadastrarCidadao(@NotBlank @Size(max=120) String nome, @NotBlank @Email @Size(max=180) String email) {}
}
