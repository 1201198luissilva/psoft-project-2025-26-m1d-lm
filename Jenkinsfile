pipeline {
    agent any

    environment {
        MAVEN_HOME = "/usr/share/maven"
        PATH = "$PATH:$MAVEN_HOME/bin"
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'A Clonar código do repositório...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'A compilar e a gerar o pacote do projeto...'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Static Code Analysis') {
            steps {
                echo 'A executar análise estática de código...'
                // Quando integrar SonarQube, substitir por "mvn sonar:sonar"
                sh 'mvn validate'
            }
        }

        stage('Unit Tests') {
            steps {
                echo 'A correr testes unitários...'
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo 'A empacotar aplicação...'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Deploy - DEV (local)') {
            steps {
                echo 'Deploy ambiente DEV (H2 em memória)...'
                sh '''
                pkill -f "psoft-g1.*dev" || true
                nohup java -jar target/*.jar --spring.profiles.active=dev > dev.log 2>&1 &
                '''
            }
        }

        stage('Deploy - STAGING (Docker)') {
            steps {
                echo 'Deploy ambiente STAGING via Docker...'
                sh '''
                # Para evitar conflito de portas
                pkill -f "psoft-g1.*dev" || true
                docker-compose -f docker-compose-staging.yml down || true
                docker-compose -f docker-compose-staging.yml up -d --build
                '''
            }
        }
        
        stage('Deploy - PROD (remoto)') {
            steps {
                echo 'Deploy ambiente PROD (servidor remoto)...'
                sh '''
                ssh -i ~/.ssh/Odsoft_key.pem azureuser@20.250.145.159 "mkdir -p /home/azureuser/app"
                scp -i ~/.ssh/Odsoft_key.pem target/*.jar azureuser@20.250.145.159:/home/azureuser/app/
                ssh -i ~/.ssh/Odsoft_key.pem azureuser@20.250.145.159 "pkill -f 'psoft-g1.*prod' || true && nohup java -jar /home/azureuser/app/*.jar --spring.profiles.active=prod > prod.log 2>&1 &"
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline concluída com sucesso!'
        }
        failure {
            echo 'Erro durante a execução da pipeline.'
        }
    }
}
