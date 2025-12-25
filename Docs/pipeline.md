# Jenkins Pipeline - OdSoft

Este ficheiro descreve a configuração e o funcionamento da pipeline Jenkins utilizada no projeto **OdSoft**.  
O principal objetivo foi automatizar o processo de build e deployment da aplicação, garantindo uma execução simples e reprodutível tanto em ambiente local como remoto (VM Azure).

---

## Estrutura da Pipeline

A pipeline foi desenvolvida em **Jenkins Declarative Pipeline Syntax**, organizada em várias etapas principais, que cobrem o ciclo básico de integração contínua:

| Etapa | Descrição |
|-------|------------|
| **Checkout** | Obtém o código do repositório GitHub associado ao projeto. |
| **Build** | Compila o projeto e gera o ficheiro `.jar` com o Maven. |
| **Static Code Analysis** | Executa validações básicas no código através do `mvn validate`. |
| **Unit Tests** | Corre testes unitários definidos no projeto, gerando relatórios XML. |
| **Package** | Arquiva o artefacto gerado (`.jar`) para uso posterior. |
| **Deploy - DEV (local)** | Faz o deploy em ambiente de desenvolvimento com base de dados H2 em memória. |
| **Deploy - STAGING (Docker)** | Inclui estrutura para futuro uso com Docker, preparada no Jenkinsfile. |
| **Deploy - PROD (remoto)** | Executa o envio do `.jar` para a máquina virtual na Azure e inicia o processo da aplicação. |

---

## Configuração do Jenkins

Foram configurados dois ambientes:

1. **Local** — Jenkins executado manualmente através de um ficheiro `.bat` que corre o comando:
```bash
   java -jar jenkins.war
``` 
Este ambiente foi usado para testar e validar o Jenkinsfile antes de enviar para a VM.

2. **Remoto (VM Azure)** — Jenkins instalado e configurado como serviço na máquina virtual, utilizado para execução da pipeline de forma mais estável.

Durante o processo foi também criada a estrutura de chaves SSH e respetivos diretórios para permitir deploy remoto automático, sem necessidade de inserção manual de credenciais.

## Jenkinsfile

O ficheiro Jenkinsfile define todas as etapas de execução da pipeline.
Os pontos mais relevantes são:

Uso de variáveis de ambiente para definir o Maven e o PATH;

Execução de comandos Maven:
```bash
mvn clean package -DskipTests
mvn test
```

Geração e arquivamento do artefacto `.jar` dentro da pasta `target/`;

Envio automático do ficheiro para o servidor remoto com:
```bash
scp -i ~/.ssh/Odsoft_key.pem target/*.jar azureuser@20.250.145.159:/home/azureuser/app/
```

Execução remota da aplicação:
```bash
nohup java -jar /home/azureuser/app/*.jar --spring.profiles.active=prod &
```
Deploy Remoto

O deploy remoto foi validado através da ligação SSH à máquina virtual Azure.
O Jenkins, ao finalizar o build, copia o `.jar` gerado para o diretório `/home/azureuser/app/` no servidor e executa o comando de arranque da aplicação.
Este processo confirma que a pipeline é capaz de realizar o build local e o deploy remoto de forma automática.

Testes e Validação

Durante os testes, o Jenkins foi capaz de:

Clonar corretamente o código do repositório GitHub;

Compilar o projeto com Maven sem erros;

Gerar o .jar no diretório target;

Transferir o artefacto para a VM e executar a aplicação com sucesso.

## Conclusão

A pipeline OdSoft cumpre os requisitos principais de integração contínua.
Embora não inclua ainda ferramentas adicionais como SonarQube ou integração total com Docker, foi possível configurar uma estrutura funcional com build automatizado e deploy remoto, demonstrando a ligação entre o ambiente local e o ambiente de produção na Azure.