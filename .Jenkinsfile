pipeline {
    agent any

    parameters {
        choice(
            name: 'SERVICE',
            choices: ['lms-authors', 'lms-genres', 'lms-books'],
            description: 'Serviço a buildar e redeploy'
        )
    }

    environment {
        MAVEN_HOME = "/usr/share/maven"
        PATH = "$PATH:$MAVEN_HOME/bin"
        COMPOSE_FILE = "docker-compose-services.yml"
        BUILD_TIMEOUT = "30"
        DEPLOY_RETRIES = "3"
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'A clonar código do repositório...'
                checkout scm
            }
        }

        stage('Validate') {
            steps {
                echo 'A validar configurações do projeto...'
                sh '''
                    # Validar que o serviço existe
                    if [ ! -d "${SERVICE}" ]; then
                        echo "Erro: Serviço ${SERVICE} não encontrado"
                        exit 1
                    fi
                    # Validar Maven
                    mvn --version
                '''
            }
        }

        stage('Build JAR Maven') {
            steps {
                echo 'A compilar e gerar o pacote do projeto...'
                sh '''
                    cd ${SERVICE}
                    mvn -DskipTests clean package
                '''
            }
        }

        stage('Static Code Analysis') {
            steps {
                echo 'A executar análise estática de código...'
                sh '''
                    cd ${SERVICE}
                    # mvn sonar:sonar (quando SonarQube estiver integrado)
                    mvn validate
                '''
            }
        }

        stage('Unit Tests') {
            steps {
                echo 'A correr testes unitários...'
                sh '''
                    cd ${SERVICE}
                    mvn test
                '''
            }
            post {
                always {
                    junit '${SERVICE}/target/surefire-reports/*.xml'
                    publishHTML([
                        reportDir: '${SERVICE}/target/surefire-reports',
                        reportFiles: 'index.html',
                        reportName: 'Test Report - ${SERVICE}'
                    ])
                }
            }
        }

        stage('Package') {
            steps {
                echo 'A empacotar aplicação...'
                sh '''
                    cd ${SERVICE}
                    if [ ! -f target/*.jar ]; then
                        echo "Erro: JAR não foi gerado"
                        exit 1
                    fi
                '''
                archiveArtifacts artifacts: '${SERVICE}/target/*.jar', fingerprint: true
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'A construir imagem Docker...'
                sh '''
                    cd ${SERVICE}
                    
                    # Determinar nome da imagem baseado no serviço
                    case "${SERVICE}" in
                        "lms-authors")
                            IMG="lmsauthors"
                            ;;
                        "lms-genres")
                            IMG="lmsgenres"
                            ;;
                        "lms-books")
                            IMG="lmsbooks"
                            ;;
                        *)
                            echo "Erro: Serviço desconhecido ${SERVICE}"
                            exit 1
                            ;;
                    esac
                    
                    # Construir imagem
                    docker build -t ${IMG}:latest .
                    docker images | grep ${IMG}
                '''
            }
        }

        stage('Deploy - DEV (local)') {
            when {
                expression { env.BRANCH_NAME == 'dev' || env.GIT_BRANCH == 'origin/dev' || true }
            }
            steps {
                echo 'Deploy ambiente DEV (H2 em memória)...'
                sh '''
                    cd ${SERVICE}
                    
                    # Parar aplicação anterior
                    pkill -f "${SERVICE}.*dev" || true
                    sleep 2
                    
                    # Verificar que o JAR existe
                    if [ ! -f target/*.jar ]; then
                        echo "Erro: JAR não encontrado para deploy DEV"
                        exit 1
                    fi
                    
                    # Deploy
                    nohup java -jar target/*.jar --spring.profiles.active=dev > ../dev-${SERVICE}.log 2>&1 &
                    
                    # Aguardar startup
                    sleep 10
                    
                    # Log início
                    echo "Deploy DEV iniciado para ${SERVICE}"
                    tail -20 ../dev-${SERVICE}.log
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'dev-${SERVICE}.log', allowEmptyArchive: true
                }
            }
        }

        stage('Deploy - STAGING (Docker)') {
            when {
                expression { env.BRANCH_NAME == 'staging' || env.GIT_BRANCH == 'origin/staging' || true }
            }
            steps {
                echo 'Deploy ambiente STAGING via Docker...'
                sh '''
                    # Parar aplicações anteriores
                    pkill -f "${SERVICE}.*dev" || true
                    sleep 2
                    
                    # Decompose e recompose
                    docker-compose -f docker-compose-staging.yml down || true
                    sleep 3
                    docker-compose -f docker-compose-staging.yml up -d --build
                    
                    # Verificar containers
                    sleep 5
                    docker-compose -f docker-compose-staging.yml ps
                    
                    # Logs
                    echo "Deploy STAGING concluído"
                    docker-compose -f docker-compose-staging.yml logs --tail=50
                '''
            }
            post {
                always {
                    sh 'docker-compose -f docker-compose-staging.yml logs > staging-${SERVICE}.log 2>&1 || true'
                    archiveArtifacts artifacts: 'staging-${SERVICE}.log', allowEmptyArchive: true
                }
            }
        }

        stage('Deploy - PROD (remoto)') {
            when {
                expression { env.BRANCH_NAME == 'main' || env.GIT_BRANCH == 'origin/main' || true }
            }
            steps {
                echo 'Deploy ambiente PROD (servidor remoto)...'
                sh '''
                    SSH_KEY="${HOME}/.ssh/Odsoft_key.pem"
                    SSH_USER="azureuser"
                    SSH_HOST="4.178.176.171"
                    APP_DIR="/home/azureuser/app"
                    
                    # Validar chave SSH
                    if [ ! -f "${SSH_KEY}" ]; then
                        echo "Erro: Chave SSH não encontrada"
                        exit 1
                    fi
                    
                    # Validar JAR local
                    if [ ! -f "${SERVICE}/target/*.jar" ]; then
                        echo "Erro: JAR não encontrado para deploy PROD"
                        exit 1
                    fi
                    
                    cd ${SERVICE}
                    
                    # Preparar diretório remoto
                    ssh -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "mkdir -p ${APP_DIR}"
                    
                    # Copiar JAR
                    scp -i ${SSH_KEY} target/*.jar ${SSH_USER}@${SSH_HOST}:${APP_DIR}/
                    
                    # Deploy com retry logic
                    ssh -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} '''
                        pkill -f '${SERVICE}.*prod' || true
                        sleep 2
                        cd ${APP_DIR}
                        nohup java -jar *.jar --spring.profiles.active=prod > prod-${SERVICE}.log 2>&1 &
                        sleep 5
                        if pgrep -f '${SERVICE}.*prod' > /dev/null; then
                            echo "Deploy PROD bem-sucedido"
                        else
                            echo "Erro: Aplicação não iniciou"
                            tail -50 prod-${SERVICE}.log
                            exit 1
                        fi
                    '''
                '''
            }
            post {
                always {
                    echo 'Deploy PROD concluído'
                }
            }
        }
    }

    post {
        success {
            echo '✓ Pipeline concluída com sucesso!'
            echo "Serviço: ${SERVICE}"
        }
        failure {
            echo '✗ Erro durante a execução da pipeline'
            echo "Serviço: ${SERVICE}"
        }
        always {
            cleanWs()
        }
    }
}
