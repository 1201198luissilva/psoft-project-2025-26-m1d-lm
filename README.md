# PSOFT — Library Management System (Luis Silva, 2025/2026)

## Enquadramento
Este repositório contém os artefactos desenvolvidos no âmbito do segundo projeto da unidade curricular de Arquitetura de Software (edição 2025/2026). Dando sequência ao trabalho anterior, que focou na configurabilidade do sistema, este ciclo de desenvolvimento teve como meta primordial mitigar as limitações inerentes à arquitetura monolítica original do LMS, visando melhorias substanciais em performance, disponibilidade e escalabilidade.

## Desafios Identificados
Embora a versão monolítica cumprisse os requisitos funcionais básicos, a sua estrutura centralizada impunha barreiras significativas:

*   **Performance:** Degradação acentuada do tempo de resposta sob carga intensa.
*   **Resiliência:** A existência de pontos únicos de falha comprometia a disponibilidade global do sistema.
*   **Gestão de Recursos:** Dificuldade em ajustar o consumo de recursos de forma elástica e eficiente.
*   **Manutenibilidade:** Complexidade crescente na adaptação a novos requisitos de negócio.

## Objetivos da Reengenharia
A transformação do LMS para uma arquitetura distribuída baseada em microserviços orientou-se pelos seguintes vetores:

*   Maximização da disponibilidade do serviço.
*   Otimização do desempenho (meta de melhoria >25% em picos de carga).
*   Implementação de elasticidade para uso racional da infraestrutura.
*   Garantia de retrocompatibilidade da API pública.
*   Alinhamento com práticas de SOA e conectividade orientada a APIs.

## Requisitos do Sistema

### Requisitos Não-Funcionais (NFRs)
*   Alta disponibilidade e tolerância a falhas.
*   Escalabilidade horizontal dinâmica (auto-scaling).
*   Eficiência operacional e de custos.
*   Facilidade de deployment e manutenção (releasability).
*   Estabilidade da interface para consumidores externos.

### Requisitos Funcionais
*   Registo atómico de Livro, Autor e Género (perfil Bibliotecário).
*   Mecanismo de sugestão de aquisições (perfil Leitor).
*   Sistema de recomendação contextual na devolução (perfil Leitor).

## Estratégia Arquitetural
*   **Decomposição em Microserviços:** Segregação dos domínios funcionais (Autores, Livros, Géneros) em unidades de deploy independentes, promovendo isolamento e escalabilidade dedicada.
*   **API Gateway:** Ponto único de entrada que gere o roteamento, segurança e abstrai a complexidade da rede interna.
*   **Comunicação Assíncrona:** Utilização de mensageria para desacoplar serviços e aumentar a robustez do sistema.
*   **Elasticidade:** Configuração de mecanismos de auto-scaling para resposta dinâmica à variação de carga.
*   **Validação de Performance:** Baterias de testes (JMeter/Postman) para assegurar o cumprimento dos SLAs definidos.
*   **Documentação Viva:** Coleções Postman e especificações OpenAPI para facilitar a integração e teste.

## Padrões e Táticas Adotadas
*   **Service Discovery:** Para localização dinâmica de instâncias e balanceamento de carga.
*   **Circuit Breaker & Retry:** Para prevenir falhas em cascata e lidar com instabilidade transitória.
*   **API Versioning:** Para permitir a evolução dos serviços sem quebrar clientes existentes.
*   **Event Sourcing / CQRS:** Aplicado seletivamente para otimizar escrita e leitura.
*   **Containerização:** Uso de Docker para garantir paridade entre ambientes de desenvolvimento e produção.

## Stack Tecnológico
*   **Java 17:** Linguagem base para o backend.
*   **Spring Boot:** Framework para criação e gestão dos microserviços e componentes de infraestrutura.
*   **RabbitMQ:** Broker de mensagens para comunicação entre serviços.
*   **MySQL:** Base de dados relacional (instâncias dedicadas por serviço).
*   **Docker & Docker Compose:** Ferramentas de orquestração de contentores.
*   **JMeter:** Ferramenta para testes de carga e stress.
*   **Postman:** Suite para testes de API e documentação.
*   **Swagger/OpenAPI:** Standard para documentação de interfaces REST.

## Estrutura do Projeto
*   `lms-authors/` — Microserviço responsável pela gestão de autores.
*   `lms-books/` — Microserviço responsável pelo catálogo de livros.
*   `lms-genres/` — Microserviço responsável pela taxonomia de géneros.
*   `Docs/` — Documentação técnica, diagramas e documentação arquitetural

## Racional da Solução
A transição para uma arquitetura distribuída foi motivada pela necessidade de garantir que cada componente do sistema possa evoluir e escalar de forma independente. Esta abordagem não só reduz o risco sistémico através do isolamento de falhas, como também permite uma gestão mais eficiente dos recursos computacionais, alinhando a infraestrutura tecnológica com as necessidades dinâmicas do negócio.
