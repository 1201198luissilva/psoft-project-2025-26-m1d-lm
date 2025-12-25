# CI/CD System Overview – ODSoft

# Visão Geral do Sistema de Gestão de Biblioteca (LMS)

## 1. Contexto

A Biblioteca **Central City** precisa de um sistema capaz de organizar e gerir os todos seus recursos, controlar os leitores e acompanhar todas as operações de requisições de livros. Atualmente, a biblioteca possui milhares de livros, classificados por género que os utilizadores podem requisitar, levar para casa e devolver dentro de um período máximo de 15 dias.

O projeto base, consiste num serviço backend baseado em REST, que até ao momento disponibiliza endpoints que permitem gerir:

- Os livros disponíveis  
- Os diferentes géneros literários  
- Os autores  
- Os leitores registados  
- Os empréstimos efetuados

## 2. Problema

O sistema de gestão atual apresenta algumas limitações importantes, nomeadamente:

- Falta de **extensibility**, o que dificulta a adição de novos recursos ou a integração com outros sistemas;  
- Baixa **configurability**, que impede diferentes clientes ou ambientes possam ter setups personalizados;
- Fragilidade em termos de **reliability**, com risco de inconsistências e erros em operações críticas.  

## 3. Introdução

O objetivo deste projeto é redesenhar o sistema existente, focando em três atributos de qualidade essenciais: **extensibility**, **configurability** e **reliability**.

Para isso, o trabalho seguirá três etapas principais:

1. Analisar o **sistema atual** (_System-as-is_) através de engenharia reversa, compreender a sua arquitetura, limitações e pontos críticos;  
2. Identificar os **requisitos-chave** que permitem superar essas limitações;  
3. Aplicar a metodologia **Attribute-Driven Design (ADD)** para rearquitetar o sistema (_System-to-be_), garantindo uma solução mais robusta, flexível e sustentável a longo prazo.

## 4. System-as-is

A versão atual do Library Management System (LMS) adota uma **arquitetura em camadas**, implementada como um serviço monolítico modular com comunicação REST. O backend foi desenvolvido em **Spring Boot**, utilizando **Spring Data JPA** para gerir a persistência de dados numa base relacional **H2**.  

### 4.1 Modelo de Domínio

O sistema centra-se em cinco entidades principais: **Livro**, **Género**, **Autor**, **Leitor** e **Empréstimo**. Cada entidade representa um elemento fundamental da operação da biblioteca: livros são associados a autores e géneros, leitores podem requisitar livros, e os empréstimos registam essas transações. Esta estrutura define claramente as regras de negócio e os relacionamentos entre os elementos do sistema.

### 4.2 Arquitetura e Visão Lógica

O backend organiza-se em quatro camadas, seguindo os princípios da **Clean Architecture**:

- **Frameworks e Drivers**: responsável pela interação com o mundo exterior, incluindo routers do Spring Boot e integração com serviços externos.  
- **Interface Adapters**: inclui controladores e componentes que adaptam dados entre o domínio e os serviços externos.  
- **Application Business**: encapsula os casos de uso através de serviços que implementam a lógica de negócio.  
- **Enterprise Business (Domínio)**: contém as entidades centrais e as regras de negócio fundamentais.  

A comunicação com o sistema é realizada através de **endpoints REST**, suportando operações genéricas como GET, POST, PUT, PATCH e DELETE.  

### 4.3 Perspectiva de Deploy

Historicamente, o LMS só era executável localmente, e o processo de deployment era **inteiramente manual**:

1. Construção do ficheiro gerado com `mvn clean package`;  
2. Localização do ficheiro `.jar` na pasta `target/`;  
3. Transferência manual do ficheiro para o servidor;  
4. Inicialização com `java -jar`.  

Este método não contemplava controlo de versões, testes automatizados ou integração contínua (CI/CD), tornando a gestão de builds e deployments **propensa a erros e dependente do ambiente local**.  

### 4.4 Limitações do System-as-is

As principais limitações da versão atual incluem:

- **Extensibilidade reduzida**: o sistema é fortemente acoplado ao H2 e à arquitetura monolítica;  
- **Configurabilidade limitada**: mudanças de DBMS, serviços externos ou formatos de ID exigem modificações no código;  
- **Confiabilidade fraca**: ausência de testes automatizados impede a validação consistente do comportamento após alterações.  

Estas limitações motivam a necessidade de uma **reengenharia arquitetural**, de forma a tornar o LMS mais flexível, configurável e confiável.



## 5. System-to-be (CI/CD Implementation)

After setting up Jenkins locally and on the Azure VM, a new **Jenkinsfile** was created to define all stages of the build and deployment process.

### 5.1 Jenkins Environments
- **Local environment:** used for initial testing and validation of the Jenkinsfile.
- **Remote environment (Azure VM):** used for the final and stable pipeline execution.  
  Jenkins was configured as a service and used SSH keys for secure communication and automatic deployment.

### 5.2 Pipeline Structure
The pipeline includes the following main stages:

| Stage | Description |
| ------ | ------------ |
| **Checkout** | Clones the GitHub repository. |
| **Build** | Executes Maven commands to compile and package the project: `mvn clean package -DskipTests`. |
| **Test** | Runs project tests with `mvn test`. |
| **Archive Artifact** | Stores the generated `.jar` file from the `target` folder. |
| **Deploy Remote** | Sends the file to the Azure VM and starts it automatically. |

### 5.3 Key Commands

```bash
# Build and test
mvn clean package -DskipTests
mvn test
```

# Transfer artifact to the server
scp -i ~/.ssh/Odsoft_key.pem target/*.jar azureuser@20.250.145.159:/home/azureuser/app/

# Run remotely
nohup java -jar /home/azureuser/app/*.jar --spring.profiles.active=prod &
These commands are executed automatically by Jenkins during the pipeline run.

## 5. System-to-be (Implementação CI/CD)

Após a conclusão do processo ADD, o LMS evoluiu para um sistema mais **extensível, configurável e fiável**.  
Como parte desta evolução, foi implementado um pipeline de integração e entrega contínua (CI/CD) utilizando **Jenkins**, tanto em ambiente local como em uma VM na Azure.

### 5.1 Ambientes Jenkins
- **Local:** utilizado para testes iniciais e validação do Jenkinsfile.  
- **Remoto (Azure VM):** ambiente de execução final do pipeline, configurado como serviço. A comunicação segura e o deployment automático são realizados via **chaves SSH**.

### 5.2 Estrutura do Pipeline
O pipeline automatiza todas as etapas do ciclo de vida do software, desde a construção até o deployment, incluindo:

| Etapa | Descrição |
| ------ | ---------- |
| **Checkout** | Clona o repositório GitHub. |
| **Build** | Executa comandos Maven para compilar e empacotar o projeto (`mvn clean package -DskipTests`). |
| **Test** | Executa os testes do projeto (`mvn test`). |
| **Archive Artifact** | Armazena o ficheiro `.jar` gerado na pasta `target/`. |
| **Deploy Remoto** | Transfere o ficheiro gerado para a Azure VM e inicia a aplicação automaticamente. |

### 5.3 Comandos Principais

```bash
# Compilar e testar
mvn clean package -DskipTests
mvn test

# Transferir ficheiro gerado para o servidor
scp -i ~/.ssh/Odsoft_key.pem target/*.jar azureuser@20.250.145.159:/home/azureuser/app/

# Executar remotamente
nohup java -jar /home/azureuser/app/*.jar --spring.profiles.active=prod &
```
## 7. Análise Crítica

Esta implementação estabelece com sucesso um fluxo automatizado de CI/CD utilizando o Jenkins, garantindo que os processos de construção e deployment são repetíveis e independentes da execução manual.

No entanto, existem ainda vários pontos a melhorar no futuro:

- Integração com o SonarQube para análise da qualidade do código;  
- Cobertura e reporte dos testes automatizados;  
- Integração com Docker para deployment em containers;  
- Notificação e registo da duração das builds e métricas de saúde.

Apesar destas limitações, a solução cumpre o objetivo essencial desta fase: implementar uma pipeline CI/CD operacional capaz de construir e fazer deploy do projeto automaticamente para um servidor remoto.

8. References
Jenkins Documentation — https://www.jenkins.io/doc/

Apache Maven — https://maven.apache.org/

Microsoft Azure — https://learn.microsoft.com/azure