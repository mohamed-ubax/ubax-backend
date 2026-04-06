// ===================================================================
// UBAX Platform – Pipeline Jenkins CI/CD
// ===================================================================
//
// Branches :
//   develop → image :develop
//   main    → image :latest + :main + :sha-{short}
//
// Credentials Jenkins requis (Manage Jenkins → Credentials) :
//   DOCKER_HUB_CREDENTIALS   → Username/Password (Docker Hub)
//   VPS_SSH_CREDENTIALS      → SSH Username with private key
//   VPS_HOST                 → Secret Text (IP ou domaine VPS)
//   VPS_USER                 → Secret Text (utilisateur SSH VPS)
//   DOCKER_HUB_USERNAME      → Secret Text (nom d'utilisateur Docker Hub)
//   DOCKER_HUB_TOKEN         → Secret Text (token Docker Hub)
//
// ===================================================================

pipeline {

    agent any

    // ─── Paramétrage global ─────────────────────────────────────
    environment {
        DOCKER_IMAGE   = 'ubaxproject/ubax-hub'
        JAVA_VERSION   = '21'
        MAVEN_OPTS     = '-Xmx512m'
    }

    // ─── Options ────────────────────────────────────────────────
    options {
        // Annuler un build si un nouveau commit arriver pendant l'exécution
        disableConcurrentBuilds()
        // Conserver les 10 derniers builds seulement
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Timeout global du pipeline
        timeout(time: 30, unit: 'MINUTES')
        // Horodatage dans les logs
        timestamps()
    }

    // ─── Déclencheurs ───────────────────────────────────────────
    triggers {
        // Polling SCM toutes les 5 minutes (remplacez par Webhook si possible)
        pollSCM('H/5 * * * *')
    }

    stages {

        // ═══════════════════════════════════════════════════════
        // STAGE 0 – Vérification du format Google Java Format
        // ═══════════════════════════════════════════════════════
        stage('📝 Format Check') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo '==> Vérification Google Java Format...'
                sh 'chmod +x mvnw'
                sh './mvnw fmt:check -B --no-transfer-progress'
            }
            post {
                failure {
                    echo '❌ Format check échoué. Exécutez : ./mvnw fmt:apply'
                }
            }
        }

        // ═══════════════════════════════════════════════════════
        // STAGE 1 – Compilation & Tests unitaires
        // ═══════════════════════════════════════════════════════
        stage('🧪 Compile & Test') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo '==> Compilation et exécution des tests...'
                sh './mvnw test -B --no-transfer-progress'
            }
            post {
                always {
                    // Publier les rapports de tests JUnit
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: true
                }
                failure {
                    echo '❌ Tests échoués. Consultez les rapports ci-dessus.'
                }
            }
        }

        // ═══════════════════════════════════════════════════════
        // STAGE 2 – Build du JAR
        // ═══════════════════════════════════════════════════════
        stage('📦 Build JAR') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo '==> Build du JAR (tests ignorés – déjà exécutés)...'
                sh './mvnw package -DskipTests -B --no-transfer-progress'
            }
        }

        // ═══════════════════════════════════════════════════════
        // STAGE 3 – Build & Push image Docker Hub
        // ═══════════════════════════════════════════════════════
        stage('🐳 Build & Push Docker') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    // ── Déterminer les tags selon la branche ──
                    def imageTag
                    def dockerTags = []

                    if (env.BRANCH_NAME == 'main') {
                        def shortSha = sh(
                            script: 'git rev-parse --short HEAD',
                            returnStdout: true
                        ).trim()
                        imageTag = 'latest'
                        dockerTags = [
                            "${DOCKER_IMAGE}:latest",
                            "${DOCKER_IMAGE}:main",
                            "${DOCKER_IMAGE}:sha-${shortSha}"
                        ]
                    } else {
                        imageTag = 'develop'
                        dockerTags = ["${DOCKER_IMAGE}:develop"]
                    }

                    env.IMAGE_TAG = imageTag

                    // ── Login Docker Hub ──
                    withCredentials([
                        string(credentialsId: 'DOCKER_HUB_USERNAME', variable: 'DH_USER'),
                        string(credentialsId: 'DOCKER_HUB_TOKEN',    variable: 'DH_TOKEN')
                    ]) {
                        sh 'echo "$DH_TOKEN" | docker login -u "$DH_USER" --password-stdin'
                    }

                    // ── Build de l'image ──
                    def tagsArgs = dockerTags.collect { "-t ${it}" }.join(' ')
                    sh "docker build ${tagsArgs} -f Dockerfile ."

                    // ── Push de toutes les tags ──
                    dockerTags.each { tag ->
                        sh "docker push ${tag}"
                    }

                    // ── Nettoyage des images locales ──
                    sh "docker rmi ${dockerTags.join(' ')} || true"

                    echo "✅ Image pushée avec les tags : ${dockerTags.join(', ')}"
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        // ═══════════════════════════════════════════════════════
        // STAGE 4 – Déploiement sur le VPS
        // ═══════════════════════════════════════════════════════
        stage('🚀 Deploy to VPS') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    withCredentials([
                        string(credentialsId: 'VPS_HOST', variable: 'VPS_HOST'),
                        string(credentialsId: 'VPS_USER', variable: 'VPS_USER'),
                        sshUserPrivateKey(
                            credentialsId: 'VPS_SSH_CREDENTIALS',
                            keyFileVariable:  'SSH_KEY_FILE',
                            usernameVariable: 'SSH_USERNAME'
                        ),
                        string(credentialsId: 'DOCKER_HUB_USERNAME', variable: 'DH_USER'),
                        string(credentialsId: 'DOCKER_HUB_TOKEN',    variable: 'DH_TOKEN')
                    ]) {
                        def sshOpts = "-i ${SSH_KEY_FILE} -o StrictHostKeyChecking=no"
                        def remote   = "${VPS_USER}@${VPS_HOST}"
                        def tag      = env.IMAGE_TAG

                        // ── Synchroniser les fichiers de config Docker ──
                        sh """
                            scp ${sshOpts} \\
                                docker/docker-compose.vps.yml \\
                                ${remote}:/opt/ubax/docker-compose.vps.yml

                            scp ${sshOpts} \\
                                docker/prometheus/prometheus.vps.yml \\
                                ${remote}:/opt/ubax/prometheus/prometheus.vps.yml

                            scp ${sshOpts} \\
                                docker/nginx/nginx.conf \\
                                ${remote}:/opt/ubax/nginx/nginx.conf

                            scp ${sshOpts} \\
                                docker/postgres/init-db.sh \\
                                ${remote}:/opt/ubax/postgres/init-db.sh

                            scp ${sshOpts} \\
                                docker/pgadmin/servers.json \\
                                ${remote}:/opt/ubax/pgadmin/servers.json
                        """

                        // ── Déploiement distant via SSH ──
                        sh """
                            ssh ${sshOpts} ${remote} bash -s << 'ENDSSH'
                                set -e
                                cd /opt/ubax

                                mkdir -p prometheus grafana/provisioning/datasources nginx postgres pgadmin

                                echo "==> Déploiement tag : ${tag}"
                                sed -i "s/^BACKEND_TAG=.*/BACKEND_TAG=${tag}/" .env

                                echo "${DH_TOKEN}" | docker login -u "${DH_USER}" --password-stdin

                                # Pull avec retry x3
                                pulled=false
                                for attempt in 1 2 3; do
                                    echo "==> Pull tentative \$attempt/3"
                                    if docker pull ubaxproject/ubax-hub:${tag}; then
                                        pulled=true
                                        break
                                    fi
                                    sleep 10
                                done

                                if [ "\$pulled" = "false" ]; then
                                    echo "❌ Échec du pull après 3 tentatives"
                                    exit 1
                                fi

                                docker compose -f docker-compose.vps.yml up -d --no-deps backend

                                # Health check
                                echo "==> Vérification santé backend..."
                                for i in \$(seq 1 12); do
                                    if docker exec ubax-backend-prod wget -qO- http://localhost:8080/api/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
                                        echo "✅ Backend opérationnel"
                                        break
                                    fi
                                    if [ "\$i" -eq 12 ]; then
                                        echo "❌ Backend non opérationnel après 60s"
                                        docker compose -f docker-compose.vps.yml logs --tail=50 backend
                                        exit 1
                                    fi
                                    sleep 5
                                done

                                docker logout
ENDSSH
                        """
                    }
                }
            }
        }
    }

    // ─── Notifications post-pipeline ────────────────────────────
    post {
        success {
            echo "✅ Pipeline terminé avec succès – branche : ${env.BRANCH_NAME} | tag : ${env.IMAGE_TAG ?: 'N/A'}"
        }
        failure {
            echo "❌ Pipeline échoué – branche : ${env.BRANCH_NAME}"
        }
        always {
            // Nettoyage du workspace Jenkins
            cleanWs()
        }
    }
}
