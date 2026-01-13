# 🚀 Deployment Guide - LMS Microservices Project

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Testing (Without Jenkins)](#local-testing-without-jenkins)
3. [Jenkins Setup](#jenkins-setup)
4. [Jenkins Pipeline Deployment - Local](#jenkins-pipeline-deployment---local)
5. [Jenkins Pipeline Deployment - VM](#jenkins-pipeline-deployment---vm)
6. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- ✅ Docker & Docker Compose installed
- ✅ Maven 3.6+
- ✅ Java 17+
- ✅ Git
- ✅ Jenkins (for pipeline deployment)

### Verify Installations
```bash
docker --version
docker-compose --version
mvn --version
java -version
git --version
```

---

## Local Testing (Without Jenkins)

### Step 1: Create Docker Network
```bash
docker network create lms_network
```

### Step 2: Start RabbitMQ
```bash
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  --network lms_network \
  rabbitmq:management
```

Verify RabbitMQ is running:
```bash
docker ps | grep rabbitmq
# Access management UI: http://localhost:15672 (guest/guest)
```

### Step 3: Test Each Microservice Locally

#### 3.1 Test lms-authors
```bash
cd lms-authors

# Build JAR
mvn clean package -DskipTests

# Run tests
mvn test

# Build Docker image
docker build -t lmsauthors:latest .

# Deploy with Docker Compose
docker-compose up -d --build

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Test endpoint
curl http://localhost:9001/api/authors

# Stop services
docker-compose down
```

#### 3.2 Test lms-genres
```bash
cd ../lms-genres

# Build and deploy
mvn clean package -DskipTests
mvn test
docker build -t lmsgenres:latest .
docker-compose up -d --build

# Check status
docker-compose ps

# Test endpoint
curl http://localhost:9005/api/genres

# Stop
docker-compose down
```

#### 3.3 Test lms-books
```bash
cd ../lms-books

# Build and deploy
mvn clean package -DskipTests
mvn test
docker build -t lmsbooks:latest .
docker-compose up -d --build

# Check status
docker-compose ps

# Test endpoint
curl http://localhost:9003/api/books

# Stop
docker-compose down
```

### Step 4: Deploy All Services Together (Optional)
- Before running the combined stack make sure you stopped any microservice compose runs from Step 3 (e.g. `cd lms-authors && docker-compose down`). Ports 9001, 9005 and 9003 are reserved by the aggregated compose file and will fail to bind if the services are already active.
```bash
cd ..

# If you have docker-compose-services.yml at root
docker-compose -f docker-compose-services.yml up -d --build

# Check all services
docker-compose -f docker-compose-services.yml ps

# Stop all
docker-compose -f docker-compose-services.yml down
```

---

## Jenkins Setup

### Step 1: Install Jenkins

**Option A: Using Docker (Recommended)**
```bash
# Create Jenkins volume
docker volume create jenkins_home

# Run Jenkins container
docker run -d \
  -p 8080:8080 -p 50000:50000 \
  --name jenkins \
  --network lms_network \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts

# Get initial admin password
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

**Option B: Native Installation**
- Download from: https://www.jenkins.io/download/
- Follow OS-specific installation instructions

### Step 2: Access Jenkins
1. Open browser: `http://localhost:8080`
2. Enter initial admin password
3. Install suggested plugins
4. Create admin user

### Step 3: Install Required Jenkins Plugins

Navigate to: **Manage Jenkins → Plugins → Available Plugins**

Install these plugins:
- ✅ **Docker Pipeline**
- ✅ **Docker Plugin**
- ✅ **Git Plugin**
- ✅ **Email Extension Plugin**
- ✅ **JUnit Plugin**
- ✅ **HTML Publisher Plugin**
- ✅ **Pipeline Stage View Plugin**

Click "Download now and install after restart"

### Step 4: Configure Jenkins Credentials

#### 4.1 GitHub Credentials
1. Go to: **Manage Jenkins → Credentials → System → Global credentials**
2. Click **Add Credentials**
3. Configure:
   - Kind: **Username with password** (or **Secret text** for token)
   - Scope: **Global**
   - Username: `your-github-username` (if using username/password)
   - Password/Secret: Your GitHub Personal Access Token
   - ID: **`github-token`** (IMPORTANT: must match Jenkinsfile)
   - Description: `GitHub Access Token`
4. Click **Create**

**To create GitHub token:**
- Go to GitHub: Settings → Developer settings → Personal access tokens → Tokens (classic)
- Generate new token with `repo` scope

#### 4.2 SSH Key for VM Deployment (For PROD)
1. **Add Credentials** again
2. Configure:
   - Kind: **SSH Username with private key**
   - Scope: **Global**
   - ID: **`vm-ssh-key`**
   - Username: `azureuser`
   - Private Key: **Enter directly** → Paste your Odsoft_key.pem content
   - Passphrase: (if key has one)
   - Description: `Azure VM SSH Key`
3. Click **Create**

**Alternative: Using secret file**
1. Upload `Odsoft_key.pem` to Jenkins server at: `~/.ssh/Odsoft_key.pem`
2. Set permissions: `chmod 600 ~/.ssh/Odsoft_key.pem`

### Step 5: Configure Email Notifications (Optional)

1. Go to: **Manage Jenkins → System**
2. Scroll to **Extended E-mail Notification**
3. Configure SMTP settings:
   - SMTP server: `smtp.gmail.com` (for Gmail)
   - SMTP port: `587`
   - Use TLS
   - Add credentials for your email account
4. Click **Save**

---

## Jenkins Pipeline Deployment - Local

### Pipeline 1: lms-authors

#### Step 1: Create Pipeline Job
1. Jenkins Dashboard → **New Item**
2. Name: `lms-authors-pipeline`
3. Type: **Pipeline**
4. Click **OK**

#### Step 2: Configure Pipeline
1. **General**:
   - ✅ Check **GitHub project**
   - Project URL: `https://github.com/1201198luissilva/psoft-project-2025-26-m1d-lm`

2. **Build Triggers** (Optional):
   - ✅ **GitHub hook trigger for GITScm polling** (if you set up webhooks)
   - ✅ **Poll SCM**: `H/5 * * * *` (check every 5 minutes)

3. **Pipeline**:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: `https://github.com/1201198luissilva/psoft-project-2025-26-m1d-lm.git`
   - Credentials: Select **github-token**
   - Branch: `*/main`
   - Script Path: `lms-authors/Jenkinsfile`

4. Click **Save**

#### Step 3: Run Pipeline
1. Click **Build Now**
2. Watch the pipeline stages execute
3. Check console output for any errors

#### Step 4: Interactive Stages
The pipeline will pause at:
- **Wait for Approval**: Click **Yes** to proceed
- **Scale Services**: Choose **1** or **2** instances
- **Shutdown Services**: Stops the services

### Pipeline 2: lms-genres

Repeat the same steps as Pipeline 1:
- Name: `lms-genres-pipeline`
- Script Path: `lms-genres/Jenkinsfile`

### Pipeline 3: lms-books

Repeat the same steps:
- Name: `lms-books-pipeline`
- Script Path: `lms-books/Jenkinsfile`

### Pipeline 4: Multi-Service Pipeline (Root Jenkinsfile)

This pipeline allows you to select which service to deploy from a single unified pipeline.

#### Step 1: Create Pipeline Job
1. Jenkins Dashboard → **New Item**
2. Name: `lms-multi-service-pipeline`
3. Type: **Pipeline**
4. Click **OK**

#### Step 2: Configure Pipeline

1. **General**:
   - ✅ Check **GitHub project**
   - Project URL: `https://github.com/1201198luissilva/psoft-project-2025-26-m1d-lm`

2. **Build Triggers** (Optional):
   - ✅ **Poll SCM**: `H/5 * * * *` (check every 5 minutes)
   - Or ✅ **GitHub hook trigger for GITScm polling** (if you configured webhooks)

3. **Pipeline**:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: `https://github.com/1201198luissilva/psoft-project-2025-26-m1d-lm.git`
   - Credentials: Select **github-token** (created in Step 4.1)
   - Branch Specifier: `*/main`
   - Script Path: `Jenkinsfile` ⚠️ **Note:** Use `Jenkinsfile` NOT `.Jenkinsfile`

4. Click **Save**

#### Step 3: Understanding the SERVICE Parameter

When you run this pipeline:
1. Click **Build with Parameters**
2. You'll see a **SERVICE** dropdown with options:
   - `lms-authors`
   - `lms-genres`
   - `lms-books`
3. Select the service you want to deploy
4. Click **Build**

The pipeline will automatically:
- Checkout the correct service directory
- Build the selected service
- Run tests
- Deploy using Docker Compose
- Execute health checks with rollback capability

#### Step 4: Run the Pipeline
1. Click **Build with Parameters**
2. Select desired service from **SERVICE** dropdown
3. Click **Build**
4. Monitor the pipeline execution

#### Step 5: Interactive Stages (Same as Individual Pipelines)
- **Wait for Approval**: Click **Yes** to proceed
- **Scale Services**: Choose **1** or **2** instances
- **Shutdown Services**: Stops the deployed service

---

## 🔄 Automatic Rollback Mechanism

### Overview
All microservice pipelines include **automatic rollback** functionality that ensures system reliability. If a deployment fails health checks, the system automatically reverts to the previous working version.

### How It Works

#### 1. Backup Phase
Before deploying a new version, the pipeline:
- Tags the current Docker image as `<service>:rollback`
- Example: `lmsauthors:latest` → `lmsauthors:rollback`
- This backup is stored locally and ready for instant rollback

#### 2. Deployment Phase
- Stops current containers with `docker-compose down`
- Builds and starts new version with `docker-compose up -d --build`
- Waits 15 seconds for services to initialize

#### 3. Health Check Phase
The pipeline tests each service instance:

**lms-authors:**
- Instance 1: `http://localhost:9001/actuator/health`
- Instance 2: `http://localhost:9002/actuator/health`

**lms-genres:**
- Instance 1: `http://localhost:9005/actuator/health`
- Instance 2: `http://localhost:9006/actuator/health`

**lms-books:**
- Instance 1: `http://localhost:9003/actuator/health`
- Instance 2: `http://localhost:9004/actuator/health`

Health check expects HTTP 200 response from Spring Boot Actuator endpoint.

#### 4. Rollback Decision
If **any** instance fails the health check:
1. Pipeline marks deployment as failed
2. Stops the faulty deployment
3. Restores the `rollback` image tag to `latest`
4. Restarts containers with previous version
5. Verifies rollback succeeded with another health check
6. Pipeline fails with exit code 1 and sends notification

If **all** instances pass:
- Deployment is marked successful
- Backup image is kept for next deployment
- Pipeline continues to scaling stage

### Testing the Rollback

#### Test Scenario 1: Simulate Database Connection Failure

1. **Break the service intentionally:**
   ```bash
   cd lms-authors/src/main/resources
   # Edit application-instance1.properties
   # Change: spring.datasource.url=jdbc:postgresql://postgres_in_lms_network:5432/authors_1
   # To: spring.datasource.url=jdbc:postgresql://invalid_host:5432/authors_1
   ```

2. **Commit and run pipeline:**
   ```bash
   git add .
   git commit -m "Test: Break database connection"
   git push
   ```

3. **Observe in Jenkins console:**
   ```
   === EXECUTANDO HEALTH CHECK ===
   ✗ Instância 1 (porta 9001) - FALHA NO HEALTH CHECK
   === INICIANDO ROLLBACK AUTOMÁTICO ===
   Parando versão com falha...
   Restaurando versão anterior...
   Reiniciando com versão anterior...
   === ROLLBACK CONCLUÍDO ===
   ✓ Versão anterior está funcionando corretamente
   ```

4. **Verify rollback:**
   ```bash
   curl http://localhost:9001/actuator/health
   # Should return healthy status from previous version
   ```

5. **Fix and redeploy:**
   ```bash
   # Revert the breaking change
   git revert HEAD
   git push
   # Run pipeline again - should succeed
   ```

#### Test Scenario 2: Simulate Spring Boot Startup Failure

1. **Add invalid configuration:**
   ```bash
   cd lms-genres/src/main/resources
   # Edit application.properties
   # Add: server.port=INVALID_PORT
   ```

2. **Run pipeline and watch rollback**

3. **Check Docker images:**
   ```bash
   docker images | grep lmsgenres
   # You'll see both 'latest' and 'rollback' tags
   ```

#### Test Scenario 3: Manual Rollback Test

```bash
# Manually trigger a deployment then check health
docker-compose -f lms-books/docker-compose.yml up -d --build

# Wait 15 seconds
sleep 15

# Check health manually
curl -f http://localhost:9003/actuator/health

# If it fails, manual rollback:
cd lms-books
docker-compose down
docker tag lmsbooks:rollback lmsbooks:latest
docker-compose up -d
```

### Rollback Limitations

⚠️ **Important Notes:**

1. **First Deployment:** No rollback available on first deployment (no previous version exists)
2. **Manual Changes:** If you manually modify containers outside Jenkins, rollback may not work correctly
3. **Image Cleanup:** Don't delete `rollback` tagged images - they're needed for recovery
4. **Database Migrations:** Rollback doesn't revert database schema changes - ensure migrations are backwards compatible

### Monitoring Rollback Events

#### Check Jenkins Console
```
Jenkins Dashboard → <pipeline-name> → Latest Build → Console Output
```
Search for:
- `=== INICIANDO ROLLBACK AUTOMÁTICO ===`
- `=== ROLLBACK CONCLUÍDO ===`

#### Check Docker Images
```bash
# See backup images
docker images | grep rollback

# Expected output:
# lmsauthors    rollback    abc123    2 hours ago    500MB
# lmsgenres     rollback    def456    1 hour ago     485MB
# lmsbooks      rollback    ghi789    30 mins ago    490MB
```

#### Email Notifications
Rollback triggers failure email:
```
Subject: ✗ lms-authors Pipeline Failed
Body: Pipeline failed during health check. Automatic rollback executed.
      Previous version restored successfully.
```

### Best Practices

✅ **Always test locally before Jenkins deployment**
```bash
cd lms-authors
mvn clean package -DskipTests
docker build -t lmsauthors:test .
docker run -d -p 9999:8080 lmsauthors:test
curl http://localhost:9999/actuator/health
```

✅ **Monitor logs during deployment**
```bash
# In separate terminal
docker-compose -f lms-authors/docker-compose.yml logs -f
```

✅ **Keep rollback images**
```bash
# Don't run this command:
# docker rmi lmsauthors:rollback  # ❌ Will break rollback!
```

✅ **Use feature flags for risky changes**
- Deploy code with feature disabled
- Enable feature after verifying health
- Rollback only affects code, not feature state

---

## Jenkins Pipeline Deployment - VM

### Prerequisites for VM Deployment

#### Step 1: Prepare the VM
SSH into your VM:
```bash
ssh -i ~/.ssh/Odsoft_key.pem azureuser@4.178.176.171
```

#### Step 2: Install Docker on VM
```bash
# Update packages
sudo apt-get update

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker azureuser

# Logout and login again for group changes
exit
ssh -i ~/.ssh/Odsoft_key.pem azureuser@4.178.176.171

# Verify Docker
docker --version
docker ps
```

#### Step 3: Install Java on VM (for JAR deployment)
```bash
sudo apt-get install -y openjdk-17-jre

# Verify
java -version
```

#### Step 4: Create Application Directory
```bash
mkdir -p /home/azureuser/app
```

#### Step 5: Configure VM Firewall (Azure Portal)
1. Go to Azure Portal → Your VM → Networking
2. Add inbound security rules for:
   - Port **8090** (lms-authors)
   - Port **8091** (lms-genres)
   - Port **8092** (lms-books)
   - Port **8080** (if deploying Docker containers)

### Running VM Deployment from Jenkins

#### Option 1: Using Multi-Service Pipeline with PROD Stage

1. Open `lms-multi-service-pipeline`
2. Click **Build with Parameters**
3. Select service (e.g., `lms-authors`)
4. Pipeline will execute all stages including **Deploy - PROD (remoto)**

The PROD stage will:
- ✅ Copy JAR to VM via SCP
- ✅ SSH into VM
- ✅ Stop old processes
- ✅ Start new application
- ✅ Verify deployment

#### Option 2: Manual Deployment with Rollback Support

1. Create a new Pipeline job: `vm-deployment-with-rollback`
2. Use this pipeline script with automatic rollback:

```groovy
pipeline {
    agent any
    
    parameters {
        choice(name: 'SERVICE', choices: ['lms-authors', 'lms-genres', 'lms-books'])
    }
    
    stages {
        stage('Build') {
            steps {
                script {
                    sh """
                        cd \${params.SERVICE}
                        mvn clean package -DskipTests
                    """
                }
            }
        }
        
        stage('Deploy to VM with Rollback') {
            steps {
                script {
                    def SERVICE = params.SERVICE
                    def PORT = SERVICE == 'lms-authors' ? '8090' : (SERVICE == 'lms-genres' ? '8091' : '8092')
                    
                    sh """
                        # Copy new JAR to VM
                        scp -i ~/.ssh/Odsoft_key.pem ${SERVICE}/target/*.jar azureuser@4.178.176.171:/home/azureuser/app/${SERVICE}-new.jar
                        
                        # Deploy with rollback capability
                        ssh -i ~/.ssh/Odsoft_key.pem azureuser@4.178.176.171 << 'ENDSSH'
                            cd /home/azureuser/app
                            
                            echo "=== BACKUP DA VERSÃO ANTERIOR ==="
                            # Backup current JAR and PID
                            if [ -f ${SERVICE}.jar ]; then
                                cp ${SERVICE}.jar ${SERVICE}-rollback.jar
                                echo "Backup criado: ${SERVICE}-rollback.jar"
                            else
                                echo "Primeira implantação - sem backup disponível"
                            fi
                            
                            # Save current PID for rollback
                            CURRENT_PID=\$(pgrep -f "${SERVICE}.jar" || echo "")
                            echo \$CURRENT_PID > ${SERVICE}-rollback.pid
                            
                            echo "=== DEPLOYMENT DA NOVA VERSÃO ==="
                            # Stop current version
                            pkill -f '${SERVICE}.jar' || true
                            sleep 3
                            
                            # Move new JAR into place
                            mv ${SERVICE}-new.jar ${SERVICE}.jar
                            
                            # Start new version
                            nohup java -jar ${SERVICE}.jar --spring.profiles.active=prod --server.port=${PORT} > ${SERVICE}.log 2>&1 &
                            NEW_PID=\$!
                            echo "Nova versão iniciada com PID: \$NEW_PID"
                            
                            # Wait for startup
                            echo "Aguardando inicialização..."
                            sleep 15
                            
                            echo "=== EXECUTANDO HEALTH CHECK ==="
                            # Health check
                            HEALTH_CHECK_RESULT=\$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${PORT}/actuator/health)
                            
                            if [ "\$HEALTH_CHECK_RESULT" = "200" ]; then
                                echo "✓ Health check PASSOU - Serviço está saudável"
                                echo "=== DEPLOYMENT CONCLUÍDO COM SUCESSO ==="
                                
                                # Clean up old backup
                                rm -f ${SERVICE}-rollback.pid
                                
                                # Show status
                                ps aux | grep ${SERVICE}.jar | grep -v grep
                                exit 0
                            else
                                echo "✗ Health check FALHOU - Código HTTP: \$HEALTH_CHECK_RESULT"
                                echo "=== INICIANDO ROLLBACK AUTOMÁTICO ==="
                                
                                # Stop failed version
                                pkill -f '${SERVICE}.jar' || true
                                sleep 2
                                
                                # Check if rollback is available
                                if [ -f ${SERVICE}-rollback.jar ]; then
                                    echo "Restaurando versão anterior..."
                                    
                                    # Restore previous version
                                    cp ${SERVICE}-rollback.jar ${SERVICE}.jar
                                    
                                    # Restart with previous version
                                    nohup java -jar ${SERVICE}.jar --spring.profiles.active=prod --server.port=${PORT} > ${SERVICE}.log 2>&1 &
                                    echo "Versão anterior reiniciada com PID: \$!"
                                    
                                    sleep 10
                                    
                                    # Verify rollback
                                    ROLLBACK_CHECK=\$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${PORT}/actuator/health)
                                    
                                    if [ "\$ROLLBACK_CHECK" = "200" ]; then
                                        echo "✓ Rollback bem-sucedido - Versão anterior funcionando"
                                        echo "=== ROLLBACK CONCLUÍDO ==="
                                    else
                                        echo "✗ ALERTA: Versão anterior também falhou no health check"
                                    fi
                                    
                                    exit 1
                                else
                                    echo "✗ Nenhuma versão anterior disponível para rollback"
                                    exit 1
                                fi
                            fi
ENDSSH
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo "✓ VM Deployment successful"
        }
        failure {
            echo "✗ VM Deployment failed - Check if rollback was executed"
        }
    }
}
```

### Verify VM Deployment

```bash
# SSH into VM
ssh -i ~/.ssh/Odsoft_key.pem azureuser@4.178.176.171

# Check running processes
ps aux | grep java

# Check logs
cd /home/azureuser/app
tail -f app.log

# Test endpoint
curl http://localhost:8090/api/authors  # or appropriate port
```

**Test from outside:**
```bash
curl http://4.178.176.171:8090/api/authors
```

### 🔄 VM Rollback Testing

#### Test Rollback on VM

1. **SSH into VM and check current setup:**
   ```bash
   ssh -i ~/.ssh/Odsoft_key.pem azureuser@4.178.176.171
   cd /home/azureuser/app
   ls -lh
   # You should see: lms-authors.jar, lms-authors-rollback.jar, lms-authors.log
   ```

2. **Check rollback files:**
   ```bash
   # Check if rollback JAR exists
   if [ -f lms-authors-rollback.jar ]; then
       echo "✓ Rollback disponível"
       ls -lh lms-authors-rollback.jar
   else
       echo "✗ Primeira implantação - rollback não disponível"
   fi
   ```

3. **Simulate failure scenario:**
   ```bash
   # Manually break the service (for testing only)
   pkill -f lms-authors.jar
   
   # Create a broken JAR (empty file)
   echo "broken" > lms-authors.jar
   
   # Try to start it
   java -jar lms-authors.jar --spring.profiles.active=prod --server.port=8090 &
   sleep 10
   
   # Health check will fail
   curl http://localhost:8090/actuator/health
   # Expected: connection refused or error
   ```

4. **Manual rollback:**
   ```bash
   echo "=== EXECUTANDO ROLLBACK MANUAL ==="
   
   # Stop broken service
   pkill -f lms-authors.jar
   
   # Restore backup
   cp lms-authors-rollback.jar lms-authors.jar
   
   # Restart
   nohup java -jar lms-authors.jar --spring.profiles.active=prod --server.port=8090 > lms-authors.log 2>&1 &
   
   sleep 10
   
   # Verify
   curl http://localhost:8090/actuator/health
   # Expected: {"status":"UP"}
   ```

5. **Check rollback logs:**
   ```bash
   cd /home/azureuser/app
   tail -50 lms-authors.log
   # Look for startup messages and verify no errors
   ```

#### Monitor VM Deployments

**Real-time monitoring during Jenkins deployment:**

1. **Open 2 terminals:**

   Terminal 1 - Jenkins Console Output:
   ```bash
   # Watch Jenkins build in browser
   http://localhost:8080/job/vm-deployment-with-rollback/lastBuild/console
   ```

   Terminal 2 - VM Logs:
   ```bash
   ssh -i ~/.ssh/Odsoft_key.pem azureuser@4.178.176.171
   tail -f /home/azureuser/app/lms-authors.log
   ```

2. **Watch for rollback indicators:**
   ```
   # In Jenkins console, look for:
   === BACKUP DA VERSÃO ANTERIOR ===
   Backup criado: lms-authors-rollback.jar
   === DEPLOYMENT DA NOVA VERSÃO ===
   Nova versão iniciada com PID: 12345
   === EXECUTANDO HEALTH CHECK ===
   ✗ Health check FALHOU - Código HTTP: 000
   === INICIANDO ROLLBACK AUTOMÁTICO ===
   Restaurando versão anterior...
   ✓ Rollback bem-sucedido - Versão anterior funcionando
   === ROLLBACK CONCLUÍDO ===
   ```

#### Verify Rollback Files on VM

```bash
ssh -i ~/.ssh/Odsoft_key.pem azureuser@4.178.176.171

# Navigate to app directory
cd /home/azureuser/app

# List all files
ls -lh

# Expected files:
# lms-authors.jar          - Current running version
# lms-authors-rollback.jar - Previous version (backup)
# lms-authors.log          - Application logs
# lms-genres.jar
# lms-genres-rollback.jar
# lms-genres.log
# lms-books.jar
# lms-books-rollback.jar
# lms-books.log

# Check file sizes (should be similar, around 50-100MB)
du -h *.jar

# Verify processes
ps aux | grep java
# Should show running JARs with their ports
```

#### Troubleshoot VM Rollback Issues

**Issue: Rollback JAR not found**
```bash
# Check if first deployment
ssh -i ~/.ssh/Odsoft_key.pem azureuser@4.178.176.171
cd /home/azureuser/app
ls -lh *rollback.jar

# If missing, deploy a working version first:
# 1. Deploy known good version via Jenkins
# 2. Verify it works
# 3. Then deploy breaking version to test rollback
```

**Issue: Both versions fail health check**
```bash
# Check if health endpoint exists
curl -v http://localhost:8090/actuator/health

# If 404, ensure Spring Boot Actuator is enabled
# Check application.properties:
# management.endpoints.web.exposure.include=health
```

**Issue: Port already in use**
```bash
# Find what's using the port
sudo lsof -i :8090

# Kill old process
pkill -f lms-authors.jar

# Or kill by PID
kill -9 <PID>
```

**Issue: Insufficient permissions**
```bash
# Ensure app directory has correct permissions
sudo chown -R azureuser:azureuser /home/azureuser/app
chmod 755 /home/azureuser/app
chmod 644 /home/azureuser/app/*.jar
```

#### Best Practices for VM Deployment

✅ **Always keep backups:**
```bash
# Before manual changes, backup current JARs
cd /home/azureuser/app
cp lms-authors.jar lms-authors-manual-backup-$(date +%Y%m%d-%H%M%S).jar
```

✅ **Monitor disk space:**
```bash
# Check available space
df -h /home/azureuser/app

# Clean old backups if needed (keep last 3)
ls -t lms-authors*.jar | tail -n +4 | xargs rm -f
```

✅ **Test health endpoint manually first:**
```bash
# Before Jenkins deployment, verify endpoint works
curl http://4.178.176.171:8090/actuator/health
```

✅ **Use PM2 or systemd for production:**
```bash
# For production, consider using process manager
sudo npm install -g pm2

# Start with PM2
pm2 start java --name lms-authors -- -jar /home/azureuser/app/lms-authors.jar --spring.profiles.active=prod --server.port=8090

# PM2 will auto-restart on crashes
pm2 save
pm2 startup
```

---

## Troubleshooting

### Issue 1: Docker Network Not Found
```bash
# Create network manually
docker network create lms_network

# Verify
docker network ls | grep lms_network
```

### Issue 2: RabbitMQ Connection Failed
```bash
# Check RabbitMQ is running
docker ps | grep rabbitmq

# Restart RabbitMQ
docker restart rabbitmq

# Check logs
docker logs rabbitmq
```

### Issue 3: Port Already in Use
```bash
# Find process using port (e.g., 9001)
sudo lsof -i :9001

# Kill process
kill -9 <PID>

# Or stop all containers
docker-compose down
docker stop $(docker ps -q)
```

### Issue 4: Jenkins Can't Access Docker
```bash
# If Jenkins is in Docker, ensure socket is mounted
docker run -v /var/run/docker.sock:/var/run/docker.sock ...

# Give Jenkins user docker permissions
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

### Issue 5: Maven Build Fails
```bash
# Clear Maven cache
mvn clean
rm -rf ~/.m2/repository

# Force update
mvn clean install -U
```

### Issue 6: SSH Connection to VM Fails
```bash
# Verify SSH key permissions
chmod 600 ~/.ssh/Odsoft_key.pem

# Test SSH connection
ssh -i ~/.ssh/Odsoft_key.pem -v azureuser@4.178.176.171

# Check Azure NSG rules - ensure port 22 is open
```

### Issue 7: Cannot Access VM Services
1. Check Azure NSG inbound rules
2. Verify application is running: `ps aux | grep java`
3. Check firewall on VM: `sudo ufw status`
4. Test locally on VM first: `curl localhost:8090`

### Issue 8: Email Notifications Not Working
1. For Gmail: Enable "Less secure app access" or use App Password
2. Verify SMTP settings in Jenkins
3. Check email credentials
4. Test email configuration: **Manage Jenkins → System → Test Configuration**

---

## Deployment Checklist

### Before First Deployment
- [ ] Docker network `lms_network` created
- [ ] RabbitMQ running and connected to network
- [ ] Jenkins installed and accessible
- [ ] Required Jenkins plugins installed
- [ ] GitHub credentials configured in Jenkins (`github-token`)
- [ ] SSH key configured for VM (if deploying to VM)
- [ ] VM has Docker/Java installed
- [ ] VM firewall rules configured

### For Each Deployment
- [ ] Code committed to GitHub
- [ ] Pipeline job created in Jenkins
- [ ] Build triggers configured (optional)
- [ ] Pipeline runs without errors
- [ ] Services accessible on expected ports
- [ ] RabbitMQ connectivity verified
- [ ] Email notifications received (if configured)

---

## Quick Reference - Useful Commands

### Docker
```bash
# View all containers
docker ps -a

# View networks
docker network ls

# View images
docker images

# Clean up everything
docker system prune -a

# View logs
docker logs <container-name>

# Execute command in container
docker exec -it <container-name> /bin/bash
```

### Docker Compose
```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f

# Rebuild and restart
docker-compose up -d --build

# Scale service
docker-compose up -d --scale service=2
```

### Jenkins
```bash
# Restart Jenkins
sudo systemctl restart jenkins

# View Jenkins logs
sudo journalctl -u jenkins -f

# Jenkins in Docker logs
docker logs jenkins
```

### VM Management
```bash
# SSH into VM
ssh -i ~/.ssh/Odsoft_key.pem azureuser@4.178.176.171

# Copy file to VM
scp -i ~/.ssh/Odsoft_key.pem file.jar azureuser@4.178.176.171:/home/azureuser/app/

# Kill all Java processes
pkill -f java

# View disk usage
df -h

# View running processes
ps aux | grep java
```

---

## Assessment Requirements Checklist

Based on ODSOFT 2025-2026 Project 2:

### ✅ WP1: Environment Setup
- [x] Jenkins server operational
- [x] Version control configured (Git)
- [x] Jenkins credentials configured

### ✅ WP2: Build Automation
- [x] Maven build integration
- [x] Automated JAR packaging
- [x] Docker image building

### ✅ WP3: Automated Testing
- [x] Unit tests execution
- [x] Test reports generation (JUnit)
- [x] Test results published in Jenkins

### ✅ WP4: Static Code Analysis
- [x] Maven validate stage
- [x] Ready for SonarQube integration

### ✅ WP5: Deployment Pipelines
- [x] DEV environment (local JAR)
- [x] STAGING environment (Docker Compose)
- [x] PROD environment (VM deployment)

### ✅ WP6: Pipeline Stages
- [x] Checkout
- [x] Build
- [x] Test
- [x] Package
- [x] Deploy (multiple environments)
- [x] User approval gates

### ✅ WP7: Notifications
- [x] Email notifications on success/failure
- [x] Build status reporting

---

## Support & Resources

- **Jenkins Documentation**: https://www.jenkins.io/doc/
- **Docker Compose**: https://docs.docker.com/compose/
- **Maven**: https://maven.apache.org/guides/
- **Spring Boot**: https://spring.io/guides

---

**Good luck with your deployment! 🚀**
