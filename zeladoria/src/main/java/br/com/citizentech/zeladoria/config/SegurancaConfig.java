package br.com.citizentech.zeladoria.config;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration

public class SegurancaConfig {
 @Bean org.springframework.security.crypto.password.PasswordEncoder encoder() {
  return new BCryptPasswordEncoder();
 }

 @Bean UserDetailsService usuarios(@Value("${app.admin.user}") String usuario, @Value("${app.admin.password}") String senha, @Value("${app.citizen.user:}") String cidadao, @Value("${app.citizen.password:}") String senhaCidadao) {
  if(usuario.isBlank() || senha.length()<12) {
   throw new IllegalArgumentException("Defina usuário e senha administrativa com pelo menos 12 caracteres");
  }

  var manager = new InMemoryUserDetailsManager(User.withUsername(usuario).password(new BCryptPasswordEncoder().encode(senha)).roles("GESTOR").build());

  if(!cidadao.isBlank()) {
   if(senhaCidadao.length() < 12 || cidadao.equals(usuario)) {
    throw new IllegalArgumentException("Credenciais de cidadão inválidas");
   }
   manager.createUser(User.withUsername(cidadao).password(new BCryptPasswordEncoder().encode(senhaCidadao)).roles("CIDADAO").build());
  }

  return manager;
 }

 @Bean SecurityFilterChain filtro(HttpSecurity http) throws Exception {
  return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a -> a
    .requestMatchers("/actuator/health","/error").permitAll()
    .requestMatchers(HttpMethod.GET,"/api/ocorrencias/**","/api/bairros","/api/categorias").permitAll()
    .requestMatchers(HttpMethod.POST,"/api/ocorrencias","/api/ocorrencias/*/relatos").authenticated()
    .anyRequest().hasRole("GESTOR"))
   .httpBasic(Customizer.withDefaults()).build();
 }
}
