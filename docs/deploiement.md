# Déploiement gratuit sur une VM Oracle Cloud (Always Free)

Toute l'application tourne sur une seule machine avec `docker-compose.prod.yml` :

```
Internet ──443──▶ Caddy (service web) ──┬── /        front Angular (fichiers statiques)
                  HTTPS automatique      ├── /api/*   backend Spring Boot ──▶ PostgreSQL 18
                                         └── /auth/*  Keycloak ─────────────▶ (base "keycloak")
```

- Un seul nom de domaine : le front déduit les URL de l'API et de Keycloak de l'adresse de la page
  (`environment.prod.ts`), il n'y a rien à recompiler pour changer de domaine.
- Seuls les ports 80 et 443 sont publics. PostgreSQL et la console d'administration Keycloak
  n'écoutent que sur `127.0.0.1` de la VM : on y accède par tunnel SSH.
- Le référentiel Ciqual est téléchargé et importé automatiquement au premier démarrage.

## 1. Créer la VM

1. Créer un compte sur [cloud.oracle.com](https://cloud.oracle.com) (offre *Always Free* ; une carte
   bancaire est demandée pour vérification, rien n'est débité tant qu'on reste dans l'offre gratuite).
2. *Compute › Instances › Create instance* :
   - image **Ubuntu 24.04** ;
   - forme **VM.Standard.A1.Flex** (Ampere, ARM) : 2 OCPU et 12 Go de RAM suffisent largement
     (l'offre gratuite va jusqu'à 4 OCPU / 24 Go) ;
   - ajouter votre clé SSH publique, garder l'adresse IP publique.
   Si la région affiche « Out of capacity », réessayer plus tard ou dans un autre domaine de disponibilité.
3. Ouvrir les ports dans le réseau Oracle : *Networking › Virtual cloud networks › (votre VCN) ›
   Security lists › Default* → *Add ingress rules* : source `0.0.0.0/0`, TCP, ports `80,443`
   (et UDP `443` pour HTTP/3, facultatif).

## 2. Nom de domaine gratuit

Sur [duckdns.org](https://www.duckdns.org) : créer un sous-domaine (ex. `mon-repas.duckdns.org`)
et y renseigner l'IP publique de la VM. Tout autre domaine fait l'affaire (enregistrement `A`).

## 3. Préparer la VM

```bash
ssh ubuntu@<IP_PUBLIQUE>

# Les images Ubuntu d'Oracle bloquent tout sauf SSH dans iptables : ouvrir 80 et 443
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p udp --dport 443 -j ACCEPT
sudo netfilter-persistent save

# Docker (dépôt officiel) et accès sans sudo
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
exit   # se reconnecter pour appliquer le groupe docker
```

## 4. Installer l'application

```bash
ssh ubuntu@<IP_PUBLIQUE>
git clone <URL_DU_DEPOT> meal-planner
cd meal-planner
cp .env.prod.example .env.prod
nano .env.prod          # domaine, e-mail, mots de passe (openssl rand -base64 24)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Le premier lancement compile le backend et le front sur la VM (quelques minutes), télécharge Ciqual
puis l'importe. Suivre le démarrage :

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f backend web
```

L'application est prête quand le backend affiche `Started MealPlannerApplication`. Ouvrir
`https://<votre-domaine>` : Caddy obtient le certificat HTTPS au premier accès. Se connecter avec
le compte `admin` (mot de passe `KC_APP_ADMIN_PASSWORD`) ou créer un compte (inscription ouverte).

## 5. Administration

**Console Keycloak** (utilisateurs, rôles, inscription) — fermée sur Internet, ouverte via tunnel :

```bash
ssh -L 8180:localhost:8180 ubuntu@<IP_PUBLIQUE>
# puis, sur votre poste : http://localhost:8180/auth/admin  (KEYCLOAK_ADMIN / KEYCLOAK_ADMIN_PASSWORD)
```

**Base de données** depuis votre poste (DBeaver, psql…) :

```bash
ssh -L 5433:localhost:5432 ubuntu@<IP_PUBLIQUE>
# puis : localhost:5433, base meal_planner, utilisateur DB_USER / DB_PASSWORD
```

## 6. Mettre à jour

```bash
cd meal-planner
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Les migrations Flyway s'appliquent au démarrage du backend.

## 7. Sauvegarder

Sauvegarde quotidienne des deux bases (application et Keycloak), gardée 14 jours :

```bash
mkdir -p ~/backups
crontab -e
# ajouter la ligne :
0 3 * * * cd ~/meal-planner && docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres pg_dumpall -U meal_planner | gzip > ~/backups/meal-planner-$(date +\%F).sql.gz && find ~/backups -name '*.sql.gz' -mtime +14 -delete
```

Copier régulièrement `~/backups` hors de la VM (ex. `scp` vers votre poste) : une sauvegarde
restée sur la machine ne protège pas contre sa perte.

## Bon à savoir

- **Valeurs lues une seule fois.** Les mots de passe de `.env.prod` et `APP_URL` servent à créer
  la base et à importer le realm Keycloak au premier démarrage. Pour les changer ensuite : console
  Keycloak (comptes, *Clients › meal-planner-web › Valid redirect URIs*) et `ALTER USER` dans PostgreSQL.
- **Tester la pile en local** sans domaine : `SITE_ADDRESS=:80`, `APP_URL=http://localhost:8088`,
  `HTTP_PORT=8088`, puis ouvrir http://localhost:8088 (Caddy sert alors en HTTP, sans certificat).
- **Mémoire** : Keycloak (~700 Mo) et le backend (~400 Mo) tiennent largement dans 12 Go.
