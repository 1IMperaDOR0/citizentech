package br.com.citizentech.zeladoria;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Base64;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class FrontendIntegrationTest {
 @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;
 private String auth(String user,String pass){return "Basic "+Base64.getEncoder().encodeToString((user+":"+pass).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 @BeforeEach void preparar(){
  for(String t:new String[]{"HISTORICO_STATUS","ORDEM_SERVICO","RELATO","OCORRENCIA","CIDADAO","CATEGORIA","BAIRRO"})jdbc.update("DELETE FROM GS_"+t);
  jdbc.update("INSERT INTO GS_CIDADAO(ID_CIDADAO,NOME,EMAIL) VALUES(1,'Cidadão exemplo','a@example.org')");
 }
 @Test void paginaERecursosPublicos() throws Exception {
  mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
  mvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(containsString("CitizenTech")));
  mvc.perform(get("/assets/app.js")).andExpect(status().isOk());
  mvc.perform(get("/assets/app.css")).andExpect(status().isOk());
  mvc.perform(get("/favicon.svg")).andExpect(status().isOk());
 }
 @Test void sessaoExigeCredenciaisENaoDisparaPopupBasic() throws Exception {
  mvc.perform(get("/api/sessao")).andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("WWW-Authenticate"));
  mvc.perform(get("/api/sessao").header("Authorization",auth("gestor","senhaerrada"))).andExpect(status().isUnauthorized());
 }
 @Test void gestorTemPerfilSemExporSenha() throws Exception {
  mvc.perform(get("/api/sessao").header("Authorization",auth("gestor","SenhaDeTeste123!")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("GESTOR"))
   .andExpect(jsonPath("$.usuario").value("gestor")).andExpect(jsonPath("$.senha").doesNotExist());
 }
 @Test void cidadaoRecebeSomenteSeuIdentificador() throws Exception {
  mvc.perform(get("/api/sessao").header("Authorization",auth("a@example.org","SenhaCidadao123!")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("CIDADAO"))
   .andExpect(jsonPath("$.cidadaoId").value(1));
 }
 @Test void cidadaoNaoAcessaGestao() throws Exception {
  mvc.perform(put("/api/gestao/ocorrencias/1/status").header("Authorization",auth("a@example.org","SenhaCidadao123!"))
   .contentType("application/json").content("{\"status\":\"EM_ANALISE\"}"))
   .andExpect(status().isForbidden());
 }
 @Test void contaSemCadastroNaoRecebeIdInventado() throws Exception {
  jdbc.update("DELETE FROM GS_CIDADAO WHERE ID_CIDADAO=1");
  mvc.perform(get("/api/sessao").header("Authorization",auth("a@example.org","SenhaCidadao123!")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.cidadaoId").isEmpty());
 }
}
