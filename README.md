# 🌎 CitizenTech: Zeladoria e Manutenção Urbana

API REST para registrar danos em espaços públicos, reunir relatos de moradores e acompanhar a execução de serviços de manutenção urbana.

Projeto desenvolvido para a **Global Solution CitizenTech — FIAP 2026, 2º semestre**, nas disciplinas **Database Design** e **Domain Driven Design — Java**, do curso de Engenharia de Software.

## Sobre o projeto

Buracos, calçadas quebradas, obstáculos e falhas de acessibilidade afetam a segurança e a mobilidade nas cidades. Quando os relatos ficam dispersos, a gestão tem dificuldade para identificar a demanda e priorizar reparos.

A solução separa o **problema físico**, representado por uma ocorrência, dos **relatos enviados por cidadãos**. Assim, diferentes pessoas podem contribuir com evidências sobre o mesmo dano, mantendo uma única ocorrência e, neste MVP, no máximo uma ordem de serviço.

O público beneficiado inclui moradores, pessoas com deficiência, idosos e pessoas com carrinhos de bebê. A gestão pública utiliza os dados para organizar e acompanhar a manutenção.

## Estado atual

- API e persistência implementadas.
- Scripts executados e integração local com o Oracle FIAP validada pelo grupo.
- Fluxo de requisições testado localmente pelo Postman.
- Protótipos de interface documentados;
- Publicação no Render em configuração.

| Informação | Link ou identificação |
|---|---|
| Nome da equipe | GIT PUSH --FORCE |
| Repositório GitHub | https://github.com/1IMperaDOR0/citizentech |
| API publicada no Render | https://citizentech.onrender.com |

## Funcionalidades

- Registrar uma ocorrência com descrição, categoria, bairro, coordenadas e primeiro relato.
- Armazenar uma URL HTTPS de foto junto ao relato.
- Associar novos relatos a uma ocorrência existente.
- Sugerir ocorrências abertas próximas e da mesma categoria.
- Calcular prioridade com regras de impacto na mobilidade.
- Consultar ocorrências em ordem de pontuação, com desempate por data e ID.
- Editar dados antes do início da manutenção.
- Abrir uma ordem de serviço e acompanhar as mudanças de estado.
- Consultar o histórico público de status.
- Arquivar logicamente ocorrências recebidas, preservando os registros relacionados.

## Tecnologias

| Tecnologia | Utilização |
|---|---|
| Java 21 | Linguagem e plataforma de execução |
| Spring Boot 3.5.7 | Configuração e execução da API |
| Spring Web | Endpoints REST |
| Spring JDBC e JdbcTemplate | Persistência com SQL parametrizado |
| Oracle Database e ojdbc11 | Banco de dados da aplicação |
| Bean Validation | Validação dos dados de entrada |
| Spring Security | Autenticação HTTP Basic e autorização por perfil |
| Spring Boot Actuator | Verificação de saúde da aplicação |
| Maven | Dependências, compilação, testes e empacotamento |
| JUnit, Spring Boot Test e MockMvc | Testes de domínio e integração HTTP |
| H2 em modo Oracle | Banco isolado para testes automatizados |
| Docker e Render | Empacotamento e publicação da API |
| Oracle SQL Developer e Data Modeler | Execução de scripts e modelagem de dados |

## Arquitetura

A aplicação está organizada em camadas, com DAO e BO explícitas.

| Pacote ou diretório | Responsabilidade |
|---|---|
| `model` | Modelos do domínio, estados e política de prioridade |
| `dto` | Contratos de entrada e saída e validações |
| `controller` | Rotas HTTP e respostas da API |
| `bo` | Regras de negócio e transações |
| `dao` | SQL, mapeamento JDBC e acesso ao banco |
| `exception` | Tratamento centralizado de exceções |
| `config` | Configuração de segurança |
| `database/` | Scripts DDL, DML, DQL e verificação |
| `docs/` | Documentação e diagramas |
| `examples/` | Exemplos de requisições HTTP |

O pacote Java principal é `br.com.citizentech.zeladoria`. A classe de inicialização é `ZeladoriaApplication`.

A linguagem do domínio distingue ocorrência, relato e ordem de serviço. Estado e prioridade encapsulam regras específicas; as BOs delimitam os fluxos transacionais.

## Regras de negócio

### Fluxo de manutenção

As transições seguem esta ordem:

1. `RECEBIDO`
2. `EM_ANALISE`
3. `EM_MANUTENCAO`
4. `CONCLUIDO`

Não é permitido pular etapas. Para iniciar a manutenção, deve existir uma ordem de serviço. Ao concluir o reparo, a aplicação atualiza o estado da ocorrência, grava o histórico e registra a conclusão da OS na mesma transação.

Uma ocorrência admite no máximo uma OS. Novos problemas após a conclusão devem gerar uma nova ocorrência. Registros concluídos não aceitam novos relatos neste MVP.

### Priorização

| Fator | Pontos |
|---|---:|
| Bloqueio total da passagem | 50 |
| Proximidade de hospital | 30 |
| Proximidade de escola | 20 |

| Pontuação total | Nível retornado |
|---|---|
| 0 a 19 | `BAIXA` |
| 20 a 49 | `MEDIA` |
| 50 a 69 | `ALTA` |
| 70 a 100 | `CRITICA` |

Os fatores são informados no registro e podem ser corrigidos pelo gestor durante a triagem. Os pesos são hipóteses do projeto; não representam critérios oficiais de prefeitura nem um modelo de inteligência artificial treinado.

### Sugestão de duplicidade

A API utiliza a fórmula de Haversine para sugerir ocorrências ativas, não concluídas, da mesma categoria, a até **30 metros** das coordenadas informadas. O vínculo do relato é confirmado pelo usuário. Não há fusão automática de ocorrências.

### Arquivamento lógico

O `DELETE` arquiva apenas ocorrências em `RECEBIDO`, alterando `ATIVA` para `0`. Os relatos e o histórico permanecem no banco.

- `204 No Content`: arquivamento concluído.
- `404 Not Found` em uma consulta posterior: o registro arquivado não está disponível entre as ocorrências ativas.
- `409 Conflict`: a ocorrência está em um estado que impede o arquivamento.

## Banco de dados

| Tabela | Finalidade | Linhas na carga inicial |
|---|---|---:|
| `GS_BAIRRO` | Cadastro territorial | 5 |
| `GS_CATEGORIA` | Categorias de danos | 5 |
| `GS_CIDADAO` | Cadastro de autores dos relatos | 5 |
| `GS_OCORRENCIA` | Problemas urbanos | 6 |
| `GS_RELATO` | Contribuições dos cidadãos | 7 |
| `GS_ORDEM_SERVICO` | Encaminhamento de manutenção | 5 |
| `GS_HISTORICO_STATUS` | Registro das mudanças de estado | 11 |

O modelo inclui PKs, FKs, unicidade, obrigatoriedade e restrições `CHECK`. A FK de ocorrência na OS é única. A prioridade é calculada a partir dos fatores, sem armazenamento redundante da pontuação.

### Preparar o schema

No SQL Developer, conecte-se ao Oracle FIAP e execute os arquivos com **F5**, na seguinte ordem:

| Ordem | Arquivo | Conteúdo |
|---|---|---|
| 1 | [`database/01_ddl.sql`](database/01_ddl.sql) | Tabelas, identidades, constraints e índices |
| 2 | [`database/02_dml.sql`](database/02_dml.sql) | Carga de dados fictícios e `COMMIT` |
| 3 | [`database/03_dql.sql`](database/03_dql.sql) | Três consultas solicitadas na atividade |
| 4 | [`database/04_verificacao.sql`](database/04_verificacao.sql) | Conferência da quantidade de linhas |

Executar criação e carga em um **schema vazio**. Os scripts não são idempotentes. Se o banco já estiver preparado, não repita DDL e DML para iniciar a API.

Os IDs gerados começam em `1000`; a carga didática reserva IDs de `1` a `999`. As contagens do quadro se referem à carga inicial e aumentam com os testes de cadastro. Fotos e coordenadas da carga são fictícias.

## Executar localmente

### Pré-requisitos

- JDK 21.
- Maven 3.9.11 ou versão compatível com o projeto.
- Acesso ao Oracle FIAP com o schema preparado.
- IntelliJ IDEA ou outra IDE Java, opcional.

Baixe ou clone este repositório e abra um terminal na pasta que contém o `pom.xml`.

### Variáveis de ambiente

| Variável | Obrigatória | Configuração |
|---|---|---|
| `DB_URL` | Sim | URL JDBC do Oracle FIAP |
| `DB_USER` | Sim | Usuário do banco |
| `DB_PASSWORD` | Sim | Senha do banco |
| `ADMIN_USER` | Sim | Login do gestor da API |
| `ADMIN_PASSWORD` | Sim | Senha do gestor, com pelo menos 12 caracteres |
| `CITIZEN_USER` | Não | Email de um cidadão previamente cadastrado no Oracle |
| `CITIZEN_PASSWORD` | Se houver conta cidadã | Senha com pelo menos 12 caracteres |
| `PORT` | Não | Porta HTTP; padrão `8080`. Os exemplos abaixo usam `8081` |

Formato JDBC com **service name**:

```text
jdbc:oracle:thin:@//HOST:PORTA/SERVICE_NAME
```

Formato JDBC com **SID**, caso essa seja a conexão fornecida pela FIAP:

```text
jdbc:oracle:thin:@HOST:PORTA:SID
```

As credenciais do gestor são independentes das credenciais do Oracle. Não publique senhas no código, nos arquivos de configuração versionados ou nas coleções exportadas.

Exemplo no **PowerShell**, substituindo os valores entre `< >`:

```powershell
$env:DB_URL = "jdbc:oracle:thin:@//<HOST>:<PORTA>/<SERVICE_NAME>"
$env:DB_USER = "<USUARIO_ORACLE>"
$env:DB_PASSWORD = "<SENHA_ORACLE>"
$env:ADMIN_USER = "<LOGIN_GESTOR>"
$env:ADMIN_PASSWORD = "<SENHA_GESTOR_COM_12_OU_MAIS_CARACTERES>"
$env:PORT = "8081"

mvn spring-boot:run
```

No IntelliJ, configure os mesmos nomes em **Run → Edit Configurations → ZeladoriaApplication → Environment variables**. Variáveis definidas no terminal não configuram automaticamente uma execução já aberta na IDE.

### Conferir a inicialização

Depois da mensagem `Started ZeladoriaApplication`, consulte:

| Endereço local | Resultado esperado |
|---|---|
| `http://localhost:8081/actuator/health` | `200 OK`, com `{"status":"UP"}` |
| `http://localhost:8081/api/ocorrencias` | Lista de ocorrências do Oracle |
| `http://localhost:8081/api/categorias` | Lista de categorias |

Se não definir `PORT`, utilize `8080` nos endereços. A rota `/` não possui página inicial; um `404` nessa rota não indica falha da API.

### Autenticação

Consultas públicas de ocorrências, histórico, bairros e categorias não exigem autenticação. Operações administrativas exigem perfil `GESTOR`. A conta cidadã opcional pode registrar ocorrências e relatos com o próprio cadastro.

No Postman, use **Basic Auth** com os valores de `ADMIN_USER` e `ADMIN_PASSWORD` para os testes administrativos.

As contas de autenticação são configuradas em memória. Cadastrar um cidadão no Oracle não cria um login automaticamente. A conta cidadã opcional deve usar como login o email desse cidadão. Fora do ambiente local, utilizar HTTPS.

## Contrato da API

Todas as rotas `/api` utilizam JSON quando há corpo de requisição ou resposta. Consulte também [`docs/ENDPOINTS.md`](docs/ENDPOINTS.md) e [`examples/roteiro.http`](examples/roteiro.http).

| Método | Rota | Perfil |
|---|---|---|
| GET | `/actuator/health` | Público |
| GET | `/api/bairros` | Público |
| GET | `/api/categorias` | Público |
| GET | `/api/ocorrencias?limite=100` | Público |
| GET | `/api/ocorrencias/{id}` | Público |
| GET | `/api/ocorrencias/semelhantes` | Público |
| GET | `/api/ocorrencias/{id}/historico` | Público |
| POST | `/api/ocorrencias` | Cidadão ou gestor |
| POST | `/api/ocorrencias/{id}/relatos` | Cidadão ou gestor |
| PUT | `/api/ocorrencias/{id}` | Gestor |
| PUT | `/api/ocorrencias/{id}/status` | Gestor |
| DELETE | `/api/ocorrencias/{id}` | Gestor |
| POST | `/api/ocorrencias/{id}/ordens-servico` | Gestor |
| POST | `/api/cidadaos` | Gestor |
| GET | `/api/gestao/ocorrencias/{id}/relatos` | Gestor |
| GET | `/api/gestao/ocorrencias/{id}/ordens-servico` | Gestor |

A consulta de semelhantes recebe `categoriaId`, `latitude` e `longitude` como query parameters. A listagem de ocorrências retorna até `500` registros; o padrão é `100`.

### Exemplo de criação de ocorrência

```http
POST /api/ocorrencias
Content-Type: application/json
Authorization: Basic <CREDENCIAL_CONFIGURADA_NO_CLIENTE_HTTP>
```

```json
{
  "dados": {
    "bairroId": 1,
    "categoriaId": 1,
    "descricao": "Calçada com passagem totalmente bloqueada",
    "latitude": -23.550100,
    "longitude": -46.630100,
    "bloqueioTotal": true,
    "proximoHospital": true,
    "proximoEscola": false
  },
  "relato": {
    "cidadaoId": 1,
    "descricao": "Trecho impede a passagem de cadeira de rodas",
    "fotoUrl": "https://example.org/foto-local.jpg"
  }
}
```

O retorno esperado é `201 Created`, com cabeçalho `Location`. O identificador está em `ocorrencia.id` na resposta. A URL de foto acima é ilustrativa.

### Principais respostas HTTP

| Código | Significado |
|---|---|
| `200` | Consulta ou atualização concluída |
| `201` | Recurso criado |
| `204` | Arquivamento concluído, sem corpo de resposta |
| `400` | Campos, JSON ou parâmetros inválidos |
| `401` | Credenciais ausentes ou inválidas |
| `403` | Perfil sem permissão |
| `404` | Recurso ativo ou referência não encontrada |
| `409` | Regra de negócio ou restrição de integridade violada |
| `503` | Falha de persistência ou banco indisponível |

Erros da camada da aplicação são tratados centralmente com `ProblemDetail`. Falhas de autenticação e autorização são tratadas pelo Spring Security.

## Testes

Execute na raiz do projeto:

```bash
mvn clean verify
```

A base do projeto possui **10 testes automatizados**, cobrindo prioridade, transições de status, exigência de OS, associação de relatos, permissões, arquivamento, rollback transacional, carga de exemplo e as três consultas SQL.

Os testes utilizam H2 em modo Oracle e não dependem das credenciais da FIAP. A validação no H2 complementa, mas não substitui, a execução no Oracle. Na validação local do grupo, também foram exercitados os endpoints contra o Oracle FIAP.

### Testes manuais no Postman

Utilize o roteiro em `examples/roteiro.http`. Se a coleção `CitizenTech_Zeladoria.postman_collection.json` estiver disponível, importe-a no Postman e configure:

| Variável | Valor |
|---|---|
| `baseUrl` | `http://localhost:8081`, ou a URL HTTPS publicada |
| `usuarioGestor` | Valor configurado em `ADMIN_USER` |
| `senhaGestor` | Valor configurado em `ADMIN_PASSWORD` |

Execute as requisições na ordem numérica. A coleção captura o ID após a criação. As tentativas identificadas como inválidas devem retornar `409`. No fluxo de arquivamento, `204` no DELETE e `404` no GET seguinte são resultados esperados.

## Deploy no Render

1. Confirme que o código atualizado está no GitHub.
2. Confira se `Dockerfile`, `pom.xml`, `src/` e `database/` pertencem à pasta do projeto.
3. No estágio de build do Dockerfile, inclua a cópia da pasta `database`, utilizada pelos testes:

   ```dockerfile
   COPY pom.xml .
   COPY src ./src
   COPY database ./database
   RUN mvn -B verify
   ```

4. No Render, crie um **Web Service**, conecte o repositório e selecione o runtime **Docker**.
5. Configure a pasta raiz conforme a organização do repositório e o caminho do Dockerfile correspondente.
6. Cadastre `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ADMIN_USER` e `ADMIN_PASSWORD` no painel. As credenciais opcionais do cidadão só são necessárias se esse perfil for utilizado.
7. Configure **Health Check Path** como `/actuator/health`.
8. Mantenha `server.port=${PORT:8080}` e `server.address=0.0.0.0`. O Render fornece a porta; não fixe a porta local `8081` no código.
9. Após a publicação, verifique `/actuator/health` e repita os testes HTTP usando a URL HTTPS do serviço.

O projeto inclui `render.yaml` como alternativa de configuração por Blueprint. A conexão do Render ao Oracle FIAP depende da acessibilidade externa do servidor. Um build concluído, sozinho, não comprova que a API consegue consultar o banco.

Não execute novamente a carga SQL em cada deploy: a inicialização automática de scripts está desabilitada em `application.properties`.

## Escopo e próximos passos

| Implementado | Evolução prevista |
|---|---|
| API REST e persistência Oracle | Interface web para cidadão e gestão |
| URL de foto | Upload e armazenamento de imagens |
| Prioridade por regras | Validação dos critérios com usuários e dados |
| Sugestão de proximidade | Apoio por mapas e tratamento de duplicidades mais completo |
| Gestor e conta cidadã de demonstração | Cadastro e gestão de identidades |
| Histórico de mudanças de estado | Auditoria de edições e arquivamentos |

O MVP não possui frontend funcional, geocodificação, reconhecimento de imagem, fusão automática de ocorrências, paginação por cursor ou filtro de bairro na API. Os protótipos das telas estão na documentação. A descrição pública de ocorrências exige moderação antes de um uso real.

## Documentação

A documentação contempla objetivo, escopo, modelo de dados, diagramas de classes, protótipos de tela, endpoints e instruções de execução. Os arquivos de apoio estão em `docs/`.

## Equipe

| Integrante | RM |
|---|---|
| Bruno Lobosque | 561254 |
| Leonardo Vinicius de Souza | 562299 |
| Lucas Henrique Viana Estevam Sena | 566246 |


## Referências

- Briefing da Global Solution CitizenTech FIAP 2026 — páginas 31 a 33.
- [Spring Boot](https://docs.spring.io/spring-boot/3.5/index.html)
- [Spring JDBC](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html)
- [Oracle Database](https://docs.oracle.com/en/database/oracle/oracle-database/19/index.html)
- [Docker no Render](https://render.com/docs/docker)
