package br.com.citizentech.zeladoria;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class FluxoIntegrationTest {
 @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired ObjectMapper json;
 String auth="Basic "+Base64.getEncoder().encodeToString("gestor:SenhaDeTeste123!".getBytes());
 String dados="{\"bairroId\":1,\"categoriaId\":1,\"descricao\":\"Calçada bloqueada\",\"latitude\":-23.55,\"longitude\":-46.63,\"bloqueioTotal\":true,\"proximoHospital\":true,\"proximoEscola\":false}";
 String relato="{\"cidadaoId\":1,\"descricao\":\"Foto do local\",\"fotoUrl\":\"https://example.org/foto.jpg\"}";
 @BeforeEach void preparar(){
  for(String t:new String[]{"HISTORICO_STATUS","ORDEM_SERVICO","RELATO","OCORRENCIA","CIDADAO","CATEGORIA","BAIRRO"})jdbc.update("DELETE FROM GS_"+t);
  jdbc.update("INSERT INTO GS_BAIRRO(ID_BAIRRO,NOME,CIDADE,UF) VALUES(1,'Centro','São Paulo','SP')");
  jdbc.update("INSERT INTO GS_CATEGORIA(ID_CATEGORIA,NOME) VALUES(1,'Calçada')");
  jdbc.update("INSERT INTO GS_CIDADAO(ID_CIDADAO,NOME,EMAIL) VALUES(1,'Exemplo','a@example.org')");
 }
 long criar() throws Exception {
  String body=mvc.perform(post("/api/ocorrencias").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"dados\":"+dados+",\"relato\":"+relato+"}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.prioridade.pontos").value(80)).andReturn().getResponse().getContentAsString();
  return json.readTree(body).at("/ocorrencia/id").asLong();
 }
 void transicao(long id,String status,int esperado) throws Exception {
  mvc.perform(put("/api/ocorrencias/"+id+"/status").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\""+status+"\",\"observacao\":\"Teste\"}")).andExpect(status().is(esperado));
 }
 @Test void cicloCompletoExigeOrdemEGravaHistorico() throws Exception {
  long id=criar();transicao(id,"CONCLUIDO",409);transicao(id,"EM_ANALISE",200);transicao(id,"EM_MANUTENCAO",409);
  mvc.perform(post("/api/ocorrencias/"+id+"/ordens-servico").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"equipe\":\"Centro\"}")).andExpect(status().isCreated());
  mvc.perform(post("/api/ocorrencias/"+id+"/ordens-servico").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"equipe\":\"Outra\"}")).andExpect(status().isConflict());
  transicao(id,"EM_MANUTENCAO",200);transicao(id,"CONCLUIDO",200);
  assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM GS_HISTORICO_STATUS WHERE ID_OCORRENCIA=?",Integer.class,id));
  assertNotNull(jdbc.queryForObject("SELECT CONCLUIDA_EM FROM GS_ORDEM_SERVICO WHERE ID_OCORRENCIA=?",java.sql.Timestamp.class,id));
 }
 @Test void agrupaRelatosESugereProximos() throws Exception {
  long id=criar();
  mvc.perform(post("/api/ocorrencias/"+id+"/relatos").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(relato)).andExpect(status().isCreated());
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM GS_RELATO WHERE ID_OCORRENCIA=?",Integer.class,id));
  mvc.perform(get("/api/ocorrencias/semelhantes?categoriaId=1&latitude=-23.55&longitude=-46.63")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/ocorrencias/semelhantes?categoriaId=1&latitude=-23&longitude=-46")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
 }
 @Test void edicaoEArquivamentoPreservamFilhos() throws Exception {
  long id=criar();
  mvc.perform(put("/api/ocorrencias/"+id).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(dados.replace("Calçada bloqueada","Calçada corrigida"))).andExpect(status().isOk()).andExpect(jsonPath("$.ocorrencia.descricao").value("Calçada corrigida"));
  mvc.perform(delete("/api/ocorrencias/"+id).header("Authorization",auth)).andExpect(status().isNoContent());
  mvc.perform(get("/api/ocorrencias/"+id)).andExpect(status().isNotFound());
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM GS_RELATO WHERE ID_OCORRENCIA=?",Integer.class,id));
 }
 @Test void validaEntradaEImpedeEscritaAnonima() throws Exception {
  mvc.perform(post("/api/ocorrencias").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/ocorrencias").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/ocorrencias?limite=501")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/gestao/ocorrencias/1/relatos")).andExpect(status().isUnauthorized());
 }
 @Test void referenciaInvalidaNaoDeixaGravacaoParcial() throws Exception {
  mvc.perform(post("/api/ocorrencias").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"dados\":"+dados+",\"relato\":"+relato.replace("\"cidadaoId\":1","\"cidadaoId\":999")+"}")).andExpect(status().isNotFound());
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM GS_OCORRENCIA",Integer.class));
 }
 @Test void cidadaoRegistraMasNaoFazTriagemNemAssumeOutraIdentidade() throws Exception {
  String cid="Basic "+Base64.getEncoder().encodeToString("a@example.org:SenhaCidadao123!".getBytes());
  mvc.perform(post("/api/ocorrencias").header("Authorization",cid).contentType(MediaType.APPLICATION_JSON).content("{\"dados\":"+dados+",\"relato\":"+relato+"}")).andExpect(status().isCreated());
  mvc.perform(post("/api/ocorrencias").header("Authorization",cid).contentType(MediaType.APPLICATION_JSON).content("{\"dados\":"+dados+",\"relato\":"+relato.replace("\"cidadaoId\":1","\"cidadaoId\":2")+"}")).andExpect(status().isConflict());
  mvc.perform(put("/api/ocorrencias/1/status").header("Authorization",cid).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EM_ANALISE\",\"observacao\":\"teste\"}")).andExpect(status().isForbidden());
 }
 @Test void falhaDepoisDeInserirOcorrenciaFazRollback() throws Exception {
  jdbc.execute("ALTER TABLE GS_RELATO ADD CONSTRAINT CK_TEST_FOTO CHECK (FOTO_URL NOT LIKE '%falha%')");
  try {
   mvc.perform(post("/api/ocorrencias").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"dados\":"+dados+",\"relato\":"+relato.replace("foto.jpg","falha.jpg")+"}")).andExpect(status().isConflict());
   assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM GS_OCORRENCIA",Integer.class));
   assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM GS_RELATO",Integer.class));
  } finally {jdbc.execute("ALTER TABLE GS_RELATO DROP CONSTRAINT CK_TEST_FOTO");}
 }
 @Test void cargaEConsultasDoEnunciado() throws Exception {
  for(String t:new String[]{"HISTORICO_STATUS","ORDEM_SERVICO","RELATO","OCORRENCIA","CIDADAO","CATEGORIA","BAIRRO"})jdbc.update("DELETE FROM GS_"+t);
  var carga=new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.ClassPathResource("database/02_dml.sql"));
  carga.setSqlScriptEncoding("UTF-8");carga.execute(jdbc.getDataSource());
  for(String t:new String[]{"BAIRRO","CATEGORIA","CIDADAO","OCORRENCIA","RELATO","ORDEM_SERVICO","HISTORICO_STATUS"})assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM GS_"+t,Integer.class)>=5);
  String raw=new org.springframework.core.io.ClassPathResource("database/03_dql.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  String texto=raw.lines().filter(l -> !l.strip().startsWith("--")).collect(java.util.stream.Collectors.joining("\n"));
  var consultas=texto.split(";");assertEquals(3,consultas.length);
  assertEquals(6,jdbc.queryForList(consultas[0]).size());
  assertEquals(5,jdbc.queryForList(consultas[1]).size());
  var esquerda=jdbc.queryForList(consultas[2]);assertEquals(1,esquerda.size());assertNull(esquerda.getFirst().get("ID_ORDEM"));
 }
}
