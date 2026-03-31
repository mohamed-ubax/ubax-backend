#!/bin/bash
# ===================================================================
# UBAX Platform – Script de démarrage Jenkins sur VPS
# ===================================================================
#
# Usage :
#   chmod +x jenkins/setup-jenkins.sh
#   sudo ./jenkins/setup-jenkins.sh
#
# Ce script :
#   1. Crée les répertoires nécessaires
#   2. Vérifie que le réseau Docker ubax-net existe
#   3. Démarre Jenkins via docker compose
#   4. Affiche le mot de passe initial Jenkins
# ===================================================================

set -e

JENKINS_DIR="/opt/jenkins/data"
UBAX_DIR="/opt/ubax"
COMPOSE_FILE="${UBAX_DIR}/docker-compose.jenkins.yml"
NETWORK_NAME="ubax_ubax-net"

echo "==> [1/5] Création des répertoires Jenkins..."
mkdir -p "${JENKINS_DIR}"
chown -R 1000:1000 /opt/jenkins
echo "    ✅ Répertoires créés : ${JENKINS_DIR}"

echo "==> [2/5] Vérification du réseau Docker ubax-net..."
if ! docker network ls --format '{{.Name}}' | grep -q "^${NETWORK_NAME}$"; then
    echo "    ⚠️  Réseau '${NETWORK_NAME}' introuvable."
    echo "    Assurez-vous que le stack principal est démarré :"
    echo "    cd /opt/ubax && docker compose -f docker-compose.vps.yml up -d"
    echo ""
    echo "    Création du réseau manuellement..."
    docker network create "${NETWORK_NAME}" || true
fi
echo "    ✅ Réseau Docker OK"

echo "==> [3/5] Copie du fichier docker-compose.jenkins.yml vers ${UBAX_DIR}..."
if [ ! -f "${COMPOSE_FILE}" ]; then
    echo "    ❌ Fichier non trouvé : ${COMPOSE_FILE}"
    echo "    Copiez docker/docker-compose.jenkins.yml vers /opt/ubax/"
    exit 1
fi
echo "    ✅ Fichier trouvé"

echo "==> [4/5] Démarrage de Jenkins..."
cd "${UBAX_DIR}"
docker compose -f docker-compose.jenkins.yml up -d
echo "    ✅ Jenkins démarré"

echo "==> [5/5] Attente du démarrage de Jenkins (max 90s)..."
for i in $(seq 1 18); do
    if docker exec ubax-jenkins curl -sf http://localhost:8080/login > /dev/null 2>&1; then
        echo "    ✅ Jenkins opérationnel !"
        break
    fi
    if [ "$i" -eq 18 ]; then
        echo "    ⚠️  Jenkins pas encore prêt (normal au 1er démarrage)"
    fi
    sleep 5
done

echo ""
echo "════════════════════════════════════════════════════════════"
echo "  Jenkins est disponible sur : http://$(hostname -I | awk '{print $1}'):8090"
echo ""
echo "  Mot de passe administrateur initial :"
echo "  $(docker exec ubax-jenkins cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null || echo '  (attendez quelques secondes, puis : docker exec ubax-jenkins cat /var/jenkins_home/secrets/initialAdminPassword)')"
echo ""
echo "  Plugins recommandés à installer :"
echo "    - Pipeline"
echo "    - Git"
echo "    - Docker Pipeline"
echo "    - SSH Agent"
echo "    - Credentials Binding"
echo "    - JUnit"
echo "    - Blue Ocean (optionnel, UI améliorée)"
echo "════════════════════════════════════════════════════════════"
