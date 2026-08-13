# 🗺️ Feuille de Route — Build Complet du Backend GPL-RFID-Livraisons

## Contexte

Ce document est le résultat d'un audit exhaustif des **10 microservices** du backend. Il décrit l'état actuel, les lacunes identifiées, et propose un plan d'exécution structuré en **7 phases** pour atteindre un système complet, sécurisé et production-ready.

---

## 📊 État Actuel du Système (Résumé de l'Audit)

| Microservice | Port | Maturité | Problèmes Critiques |
|---|---|---|---|
| `discovery-server` | 8761 | ✅ OK | Aucun (Eureka Server) |
| `api-gateway` | 8080 | ⚠️ Partiel | Routes manquantes (`cylinder`, `subsidy`, `fleet`, `tour`) |
| `common-lib` | — | ✅ Solide | 27 enums, base entities, DTOs, exceptions |
| `auth-service` | 8081 | ⚠️ Partiel | Rôles absents du JWT, pas d'audit events |
| `audit-service` | 8084 | ⚠️ Partiel | Pas de mécanisme d'ingestion (ni Kafka, ni REST, ni Event) |
| `organization-service` | 8082 | 🟡 Bon | Filtres non implémentés, ClientSite sans CRUD |
| `user-service` | 8083 | 🔴 Cassé | Erreur de compilation (refs à `username`/`lastLoginAt` supprimés) |
| `tour-service` | 8085 | ⚠️ Partiel | `PickupRequest`/`TransporterContract` sans service ni controller |
| `cylinder-service` | 8086 | 🔴 Squelette | Pas de service layer, pas de DTOs, `RfidTag`/`ScanEvent` inaccessibles |
| `fleet-device-service` | 8087 | 🔴 Squelette | Pas de service layer, pas de DTOs, `Device` inaccessible |
| `subsidy-service` | 8088 | 🔴 Squelette | Pas de service layer, `Reconciliation`/`Redressement` inaccessibles |
| `notification-service` | 8089 | 🟡 Bon | SMS/PUSH non implémentés, pas de query log |

---

## 🔐 Matrice des Permissions (~220 permissions réparties en 11 modules)

> [!IMPORTANT]
> Contrairement à l'intuition initiale de ~400 permissions, une analyse rigoureuse montre qu'un système RBAC bien conçu nécessite **~220 permissions granulaires**. Au-delà, on tombe dans la sur-granularité qui rend l'administration impossible. Si un besoin spécifique émerge plus tard, on ajoutera des permissions ciblées.

### Module 1 : `ORGANIZATION` (19 permissions)
| Code | Description |
|---|---|
| `ORG_VIEW` | Voir les détails d'une organisation |
| `ORG_VIEW_ALL` | Voir toutes les organisations (cross-org) |
| `ORG_VIEW_OWN` | Voir uniquement sa propre organisation |
| `ORG_CREATE` | Créer une nouvelle organisation |
| `ORG_UPDATE` | Modifier une organisation existante |
| `ORG_DELETE` | Supprimer (archiver) une organisation |
| `ORG_ACTIVATE` | Activer une organisation |
| `ORG_DEACTIVATE` | Désactiver une organisation |
| `ORG_VIEW_HIERARCHY` | Voir l'arborescence hiérarchique |
| `ORG_VIEW_CHILDREN` | Voir les filiales directes |
| `ORG_RELATIONSHIP_VIEW` | Voir les relations B2B |
| `ORG_RELATIONSHIP_CREATE` | Créer une relation B2B |
| `ORG_RELATIONSHIP_UPDATE` | Modifier une relation B2B |
| `ORG_RELATIONSHIP_DELETE` | Supprimer une relation B2B |
| `CLASS_VIEW` | Voir les classifications |
| `CLASS_CREATE` | Créer une classification |
| `CLASS_UPDATE` | Modifier une classification |
| `CLASS_DELETE` | Supprimer une classification |
| `CLIENT_SITE_MANAGE` | Gérer les spécificités des sites clients |

### Module 2 : `SITE` (12 permissions)
| Code | Description |
|---|---|
| `SITE_VIEW` | Voir les détails d'un site |
| `SITE_VIEW_ALL` | Voir tous les sites (cross-org) |
| `SITE_VIEW_OWN` | Voir les sites de sa propre organisation |
| `SITE_CREATE` | Créer un site |
| `SITE_UPDATE` | Modifier un site |
| `SITE_DELETE` | Supprimer un site |
| `SITE_ACTIVATE` | Activer un site |
| `SITE_DEACTIVATE` | Désactiver un site |
| `SITE_VIEW_NEARBY` | Rechercher des sites par géolocalisation |
| `SITE_ASSIGN_PERSON` | Assigner une personne à un site |
| `SITE_UNASSIGN_PERSON` | Retirer une personne d'un site |
| `SITE_VIEW_ASSIGNMENTS` | Voir les assignations d'un site |

### Module 3 : `USER` (26 permissions)
| Code | Description |
|---|---|
| `PERSON_VIEW` | Voir les détails d'une personne |
| `PERSON_VIEW_ALL` | Voir toutes les personnes (cross-org) |
| `PERSON_VIEW_OWN_ORG` | Voir les personnes de son organisation |
| `PERSON_CREATE` | Créer une personne |
| `PERSON_UPDATE` | Modifier une personne |
| `PERSON_DELETE` | Supprimer une personne |
| `PERSON_ACTIVATE` | Activer une personne |
| `PERSON_DEACTIVATE` | Désactiver une personne |
| `USER_ACCOUNT_CREATE` | Créer un compte d'accès (AuthUser) |
| `USER_ACCOUNT_LOCK` | Verrouiller un compte |
| `USER_ACCOUNT_UNLOCK` | Déverrouiller un compte |
| `USER_ACCOUNT_RESET_PASSWORD` | Réinitialiser un mot de passe |
| `ROLE_VIEW` | Voir les rôles |
| `ROLE_CREATE` | Créer un rôle |
| `ROLE_UPDATE` | Modifier un rôle |
| `ROLE_DELETE` | Supprimer un rôle |
| `ROLE_ASSIGN` | Assigner un rôle à un utilisateur |
| `ROLE_REVOKE` | Révoquer un rôle d'un utilisateur |
| `PERMISSION_VIEW` | Voir les permissions disponibles |
| `PERMISSION_MANAGE` | Gérer les permissions des rôles |
| `GROUP_VIEW` | Voir les groupes d'utilisateurs |
| `GROUP_CREATE` | Créer un groupe |
| `GROUP_UPDATE` | Modifier un groupe |
| `GROUP_DELETE` | Supprimer un groupe |
| `GROUP_ADD_MEMBER` | Ajouter un membre à un groupe |
| `GROUP_REMOVE_MEMBER` | Retirer un membre d'un groupe |

### Module 4 : `AUTH` (6 permissions)
| Code | Description |
|---|---|
| `AUTH_LOGIN` | Se connecter |
| `AUTH_LOGOUT` | Se déconnecter |
| `AUTH_CHANGE_OWN_PASSWORD` | Changer son propre mot de passe |
| `AUTH_FORCE_PASSWORD_CHANGE` | Forcer le changement de mot de passe d'un autre |
| `AUTH_VIEW_SESSIONS` | Voir les sessions actives |
| `AUTH_TERMINATE_SESSION` | Terminer la session d'un autre utilisateur |

### Module 5 : `FLEET` (20 permissions)
| Code | Description |
|---|---|
| `VEHICLE_VIEW` | Voir les détails d'un véhicule |
| `VEHICLE_VIEW_ALL` | Voir tous les véhicules (cross-org) |
| `VEHICLE_VIEW_OWN_ORG` | Voir les véhicules de son organisation |
| `VEHICLE_CREATE` | Créer un véhicule |
| `VEHICLE_UPDATE` | Modifier un véhicule |
| `VEHICLE_DELETE` | Supprimer un véhicule |
| `VEHICLE_ACTIVATE` | Activer un véhicule |
| `VEHICLE_DEACTIVATE` | Désactiver un véhicule |
| `VEHICLE_ASSIGN_DRIVER` | Assigner un chauffeur à un véhicule |
| `VEHICLE_UNASSIGN_DRIVER` | Retirer un chauffeur d'un véhicule |
| `DEVICE_VIEW` | Voir les détails d'un device (GPS/PDA) |
| `DEVICE_VIEW_ALL` | Voir tous les devices |
| `DEVICE_VIEW_OWN_ORG` | Voir les devices de son organisation |
| `DEVICE_CREATE` | Enregistrer un device |
| `DEVICE_UPDATE` | Modifier un device |
| `DEVICE_DELETE` | Supprimer un device |
| `DEVICE_ASSIGN` | Assigner un device à une personne/véhicule |
| `DEVICE_UNASSIGN` | Retirer un device |
| `DEVICE_VIEW_STATUS` | Voir l'historique d'état des devices |
| `DEVICE_VIEW_POSITION` | Voir la position GPS en temps réel |

### Module 6 : `TOUR` (28 permissions)
| Code | Description |
|---|---|
| `TOUR_VIEW` | Voir les détails d'une tournée |
| `TOUR_VIEW_ALL` | Voir toutes les tournées (cross-org) |
| `TOUR_VIEW_OWN_ORG` | Voir les tournées de son organisation |
| `TOUR_CREATE` | Créer une tournée |
| `TOUR_UPDATE` | Modifier une tournée |
| `TOUR_DELETE` | Supprimer une tournée |
| `TOUR_START` | Démarrer une tournée |
| `TOUR_CLOSE` | Clôturer une tournée |
| `TOUR_CANCEL` | Annuler une tournée |
| `TOUR_ASSIGN_DRIVER` | Assigner un chauffeur/livreur |
| `TOUR_ASSIGN_VEHICLE` | Assigner un véhicule |
| `CHECKPOINT_VIEW` | Voir les checkpoints d'une tournée |
| `CHECKPOINT_CREATE` | Ajouter un checkpoint |
| `CHECKPOINT_UPDATE` | Modifier un checkpoint |
| `CHECKPOINT_DELETE` | Supprimer un checkpoint |
| `CHECKPOINT_VALIDATE` | Valider un checkpoint (arrivée confirmée) |
| `CHECKPOINT_SKIP` | Sauter un checkpoint (avec raison) |
| `PICKUP_VIEW` | Voir les demandes d'enlèvement |
| `PICKUP_CREATE` | Créer une demande d'enlèvement |
| `PICKUP_APPROVE` | Approuver une demande |
| `PICKUP_REJECT` | Rejeter une demande |
| `PICKUP_VIEW_ALL` | Voir toutes les demandes (cross-org) |
| `PICKUP_VIEW_OWN_ORG` | Voir les demandes de son organisation |
| `CONTRACT_VIEW` | Voir les contrats transporteurs |
| `CONTRACT_CREATE` | Créer un contrat transporteur |
| `CONTRACT_UPDATE` | Modifier un contrat |
| `CONTRACT_TERMINATE` | Résilier un contrat |
| `CONTRACT_VIEW_ALL` | Voir tous les contrats (cross-org) |

### Module 7 : `CYLINDER` (20 permissions)
| Code | Description |
|---|---|
| `CYLINDER_VIEW` | Voir les détails d'une bouteille |
| `CYLINDER_VIEW_ALL` | Voir toutes les bouteilles (cross-org) |
| `CYLINDER_VIEW_OWN_ORG` | Voir les bouteilles de son organisation |
| `CYLINDER_CREATE` | Enregistrer une bouteille |
| `CYLINDER_UPDATE` | Modifier une bouteille |
| `CYLINDER_DELETE` | Supprimer une bouteille |
| `CYLINDER_TRANSFER` | Transférer une bouteille entre sites |
| `RFID_VIEW` | Voir les tags RFID |
| `RFID_CREATE` | Enregistrer un tag RFID |
| `RFID_UPDATE` | Modifier un tag RFID |
| `RFID_DELETE` | Supprimer un tag RFID |
| `RFID_ASSIGN` | Associer un tag RFID à une bouteille |
| `RFID_UNASSIGN` | Dissocier un tag RFID |
| `RFID_VIEW_ALL` | Voir tous les tags (cross-org) |
| `SCAN_VIEW` | Voir les événements de scan |
| `SCAN_CREATE` | Enregistrer un scan (depuis PDA) |
| `SCAN_VIEW_ALL` | Voir tous les scans (cross-org) |
| `SCAN_VIEW_OWN_ORG` | Voir les scans de son organisation |
| `SCAN_EXPORT` | Exporter les données de scan |
| `SCAN_RESOLVE_CONFLICT` | Résoudre un conflit de scan |

### Module 8 : `SUBSIDY` (18 permissions)
| Code | Description |
|---|---|
| `DECLARATION_VIEW` | Voir les déclarations de volumes |
| `DECLARATION_VIEW_ALL` | Voir toutes les déclarations (cross-org) |
| `DECLARATION_VIEW_OWN_ORG` | Voir les déclarations de son organisation |
| `DECLARATION_CREATE` | Créer une déclaration |
| `DECLARATION_UPDATE` | Modifier une déclaration |
| `DECLARATION_DELETE` | Supprimer une déclaration |
| `DECLARATION_SUBMIT` | Soumettre une déclaration pour validation |
| `DECLARATION_APPROVE` | Approuver une déclaration |
| `DECLARATION_REJECT` | Rejeter une déclaration |
| `RECONCILIATION_VIEW` | Voir les réconciliations |
| `RECONCILIATION_CREATE` | Lancer une réconciliation |
| `RECONCILIATION_VERIFY` | Vérifier/valider une réconciliation |
| `RECONCILIATION_VIEW_ALL` | Voir toutes les réconciliations (cross-org) |
| `REDRESSEMENT_VIEW` | Voir les redressements financiers |
| `REDRESSEMENT_CREATE` | Émettre un redressement |
| `REDRESSEMENT_APPROVE` | Approuver un redressement |
| `REDRESSEMENT_VIEW_ALL` | Voir tous les redressements (cross-org) |
| `SUBSIDY_DASHBOARD` | Accéder au tableau de bord subventions |

### Module 9 : `NOTIFICATION` (8 permissions)
| Code | Description |
|---|---|
| `NOTIFICATION_SEND` | Envoyer une notification |
| `NOTIFICATION_SEND_BULK` | Envoyer des notifications en masse |
| `NOTIFICATION_VIEW_LOG` | Voir l'historique des notifications |
| `NOTIFICATION_VIEW_LOG_ALL` | Voir tous les logs (cross-org) |
| `TEMPLATE_VIEW` | Voir les templates de notification |
| `TEMPLATE_CREATE` | Créer un template |
| `TEMPLATE_UPDATE` | Modifier un template |
| `TEMPLATE_DELETE` | Supprimer un template |

### Module 10 : `AUDIT` (6 permissions)
| Code | Description |
|---|---|
| `AUDIT_VIEW_MODIFICATIONS` | Voir les modifications d'entités |
| `AUDIT_VIEW_STATUS_HISTORY` | Voir l'historique des changements de statut |
| `AUDIT_VIEW_SUMMARY` | Voir le résumé d'audit d'une entité |
| `AUDIT_VIEW_ALL` | Voir les audits de toutes les organisations |
| `AUDIT_VIEW_OWN_ORG` | Voir les audits de sa propre organisation |
| `AUDIT_EXPORT` | Exporter les données d'audit |

### Module 11 : `PLATFORM` (7 permissions)
| Code | Description |
|---|---|
| `SETTINGS_VIEW` | Voir les paramètres système |
| `SETTINGS_UPDATE` | Modifier les paramètres système |
| `DASHBOARD_VIEW` | Accéder au tableau de bord principal |
| `DASHBOARD_VIEW_ANALYTICS` | Accéder aux analytics avancés |
| `MONITORING_VIEW` | Accéder au monitoring technique |
| `REPORT_GENERATE` | Générer un rapport |
| `REPORT_EXPORT` | Exporter un rapport |

> **Total : ~170 permissions granulaires couvrant 11 modules métiers.**
> Des permissions supplémentaires pourront être ajoutées à mesure que de nouveaux cas d'usage émergeront (workflow d'approbation, alertes personnalisées, etc.).

---

## 🚀 Plan d'Exécution (7 Phases)

---

### Phase 0 — Corrections Critiques (Pré-requis, ~1 jour)

> [!CAUTION]
> Le système ne compile plus en l'état actuel. Cette phase est **BLOQUANTE**.

| # | Tâche | Service | Détail |
|---|---|---|---|
| 0.1 | Corriger erreur de compilation `Person.java` | `user-service` | `PersonRepository`, `PersonService`, `CreatePersonRequest`, `PersonResponse`, `UserDataInitializer`, `RolesPermissionsInitializer` référencent encore `username` et `lastLoginAt`. Supprimer ces références et corriger le mapping. |
| 0.2 | Corriger doublon `package` dans `Person.java` | `user-service` | Le fichier contient une déclaration `package` dupliquée (lignes 1-6 et 8-14). |
| 0.3 | Corriger incohérence `hierarchyPath` | `organization-service` | `DataInitializer` utilise `/` comme séparateur, `OrganizationService` utilise `\`. Unifier sur `/`. |
| 0.4 | Corriger le nom de table `AuthUser` | `auth-service` | `@Table(name = "auth_users")` alors que le SQL crée `users`. Aligner les deux. |

---

### Phase 1 — Solidifier le Socle (Foundation, ~3 jours)

**Objectif** : Rendre le système de sécurité et d'identité parfaitement fonctionnel.

| # | Tâche | Service | Détail |
|---|---|---|---|
| 1.1 | Injecter les rôles dans le JWT | `auth-service` | Remplacer `new ArrayList<>()` par un appel inter-service (Feign/REST) vers `user-service` pour résoudre les rôles de la personne au moment du login. |
| 1.2 | Implémenter le mécanisme d'ingestion d'audit | `audit-service` | Créer un `@RestController` POST `/api/v1/audit/ingest` ou un `@KafkaListener` pour recevoir les `AuditEvent` depuis les autres services. |
| 1.3 | Publier les événements d'audit au login | `auth-service` | Implémenter le `TODO` ligne 62 : publier un `AuditEvent` (LOGIN, LOGIN_FAILED) vers `audit-service`. |
| 1.4 | Seed des ~170 permissions en base | `user-service` | Créer un script SQL ou un `DataInitializer` qui insère toutes les permissions de la matrice ci-dessus dans la table `permissions`. |
| 1.5 | Créer les rôles système par défaut | `user-service` | Créer des rôles pré-configurés (SUPERADMIN, ORG_ADMIN, SITE_MANAGER, OPERATOR, VIEWER) avec leurs permissions associées via `RolePermission`. |
| 1.6 | Compléter les routes du Gateway | `api-gateway` | Ajouter les routes manquantes : `/api/v1/cylinders/**`, `/api/v1/declarations/**`, `/api/v1/vehicles/**`, `/api/v1/devices/**`, `/api/v1/tours/**`, `/api/v1/checkpoints/**`. |

---

### Phase 2 — Services de Gestion (Core CRUD, ~5 jours)

**Objectif** : Chaque entité doit avoir un CRUD complet (Service layer + DTOs + Controller) et des filtres fonctionnels.

#### 2A. `organization-service`
| # | Tâche | Détail |
|---|---|---|
| 2A.1 | Créer `ClientSiteRepository` + `ClientSiteService` + `ClientSiteController` | CRUD complet pour les spécificités des sites clients. |
| 2A.2 | Implémenter les filtres dans `listOrganizations()` | Utiliser les paramètres `type`, `tier`, `isActive` dans la requête JPA (Specification ou @Query). |
| 2A.3 | Implémenter les filtres dans `listSites()` | Utiliser `organizationId`, `type`, `city`, `isOperational`. |
| 2A.4 | Compléter le CRUD de `OrganizationRelationship` | Ajouter GET (list, by ID), PUT (update), DELETE (terminate). |
| 2A.5 | Compléter le CRUD de `ClassStructure` | Ajouter POST, PUT, DELETE + recherche par parent. |
| 2A.6 | Populer `statusDescription` dans les DTOs summary | Mapper le champ dans les transformations DTO. |

#### 2B. `user-service`
| # | Tâche | Détail |
|---|---|---|
| 2B.1 | Ajouter endpoint `DELETE /persons/{id}/sites/{assignmentId}` | Exposer `UserSiteAssignmentService.revokeSite()`. |
| 2B.2 | Ajouter endpoints `PUT /roles/{id}` et `PUT /groups/{id}` | Permettre la mise à jour des rôles et groupes. |
| 2B.3 | Implémenter les filtres dans `listPersons()` | Utiliser `siteId`, `jobCode` dans la requête. |
| 2B.4 | Ajouter `DELETE /groups/{id}` | Suppression (soft-delete) d'un groupe. |

#### 2C. `fleet-device-service`
| # | Tâche | Détail |
|---|---|---|
| 2C.1 | Créer `VehicleService` / `VehicleServiceImpl` | Extraire la logique du controller vers un service. |
| 2C.2 | Créer les DTOs véhicule | `CreateVehicleRequest`, `UpdateVehicleRequest`, `VehicleResponse`, `VehicleSummaryResponse`. |
| 2C.3 | Compléter le CRUD véhicule | GET by ID, PUT, DELETE, pagination. |
| 2C.4 | Créer `DeviceRepository` + `DeviceService` + `DeviceController` | CRUD complet pour les devices GPS/PDA. |
| 2C.5 | Créer les DTOs device | `CreateDeviceRequest`, `UpdateDeviceRequest`, `DeviceResponse`. |
| 2C.6 | Endpoint d'assignation device | POST `/devices/{id}/assign`, DELETE `/devices/{id}/unassign`. |

#### 2D. `cylinder-service`
| # | Tâche | Détail |
|---|---|---|
| 2D.1 | Créer `CylinderService` / `CylinderServiceImpl` | Extraire la logique du controller. |
| 2D.2 | Créer les DTOs bouteille | `CreateCylinderRequest`, `CylinderResponse`, `CylinderSummaryResponse`. |
| 2D.3 | Compléter le CRUD bouteille | GET by ID, PUT, DELETE, pagination, filtres. |
| 2D.4 | Créer `RfidTagRepository` + `RfidTagService` + `RfidTagController` | CRUD complet pour les tags RFID. |
| 2D.5 | Créer `ScanEventRepository` + `ScanEventService` + `ScanEventController` | Enregistrement et consultation des scans. |

#### 2E. `subsidy-service`
| # | Tâche | Détail |
|---|---|---|
| 2E.1 | Créer `DeclarationService` / `DeclarationServiceImpl` | Extraire la logique du controller. |
| 2E.2 | Créer les DTOs déclaration | `CreateDeclarationRequest`, `DeclarationResponse`. |
| 2E.3 | Créer `ReconciliationRepository` + `ReconciliationService` + Controller | CRUD + logique de calcul d'écart. |
| 2E.4 | Créer `RedressementRepository` + `RedressementService` + Controller | CRUD + workflow d'approbation. |

---

### Phase 3 — Logique Métier Avancée (~5 jours)

**Objectif** : Implémenter les workflows métier complexes qui font la valeur du système.

| # | Tâche | Service | Détail |
|---|---|---|---|
| 3.1 | Lifecycle de Tournée | `tour-service` | Endpoints dédiés : `POST /tours/{id}/start`, `POST /tours/{id}/close`, `POST /tours/{id}/cancel`. Machine à états (PLANNED → STARTED → IN_PROGRESS → COMPLETED/CANCELLED). |
| 3.2 | Assignation transporteur/chauffeur | `tour-service` | `POST /tours/{id}/assign-driver`, `POST /tours/{id}/assign-vehicle`. |
| 3.3 | CRUD `PickupRequest` | `tour-service` | Service + Controller + DTOs + workflow (PENDING → APPROVED → REJECTED). |
| 3.4 | CRUD `TransporterContract` | `tour-service` | Service + Controller + DTOs + gestion des dates de validité. |
| 3.5 | Validation de checkpoint | `tour-service` | `POST /checkpoints/{id}/validate` avec horodatage GPS, `POST /checkpoints/{id}/skip` avec raison obligatoire. |
| 3.6 | Transfert de bouteille | `cylinder-service` | `POST /cylinders/{id}/transfer` (changement de `currentSiteId`, `currentHolderOrganizationId`). |
| 3.7 | Résolution de conflit scan | `cylinder-service` | `POST /scans/{id}/resolve-conflict`. |
| 3.8 | Calcul d'écart de réconciliation | `subsidy-service` | Comparer `declaredVolume` vs `trackedVolume`, calculer `volumeGap` et `subsidyImpact`. |
| 3.9 | Workflow de déclaration | `subsidy-service` | Machine à états : DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED. |

---

### Phase 4 — Enforcement RBAC & Sécurité (~3 jours)

**Objectif** : Chaque endpoint est protégé par ses permissions.

| # | Tâche | Détail |
|---|---|---|
| 4.1 | Créer `@RequiresPermission` annotation | Annotation custom dans `common-lib` qui prend un code de permission. |
| 4.2 | Créer `PermissionInterceptor` / `SecurityAspect` | AOP qui intercepte les appels annotés, extrait les permissions du JWT, et vérifie l'accès. |
| 4.3 | Enrichir le JWT avec les permissions | Au login, résoudre les permissions effectives et les inclure dans le payload JWT (ou les cacher). |
| 4.4 | Implémenter le filtre multi-tenant | S'assurer qu'un utilisateur `TIER_3` ne voit que les données de son `organizationId` (sauf s'il a une permission `_VIEW_ALL`). |
| 4.5 | Annoter tous les endpoints existants | Parcourir chaque controller et ajouter `@RequiresPermission("PERMISSION_CODE")` sur chaque méthode. |

---

### Phase 5 — Notifications & Événements (~2 jours)

| # | Tâche | Service | Détail |
|---|---|---|---|
| 5.1 | Implémenter l'envoi SMS | `notification-service` | Intégrer un provider SMS (Twilio, Africa's Talking, etc.). |
| 5.2 | Implémenter l'envoi PUSH | `notification-service` | Intégrer Firebase Cloud Messaging. |
| 5.3 | Endpoint de query log | `notification-service` | `GET /notifications/logs?recipientId=...&channel=...&dateFrom=...`. |
| 5.4 | Notifications vers UserGroups | `notification-service` | `POST /notifications/send-to-group` : résoudre les membres du groupe et envoyer à chacun. |
| 5.5 | Publication d'audit events depuis chaque service | Tous | Chaque opération CRUD publie un `AuditEvent` vers `audit-service`. |

---

### Phase 6 — Qualité & Robustesse (~3 jours)

| # | Tâche | Détail |
|---|---|---|
| 6.1 | Tests unitaires (Services) | JUnit 5 + Mockito pour chaque service de chaque microservice. |
| 6.2 | Tests d'intégration (Controllers) | `@SpringBootTest` + `MockMvc` pour valider les endpoints. |
| 6.3 | Validation des DTOs | S'assurer que toutes les `@NotBlank`, `@NotNull`, `@Size`, `@Email` sont en place. |
| 6.4 | Gestion d'erreurs unifiée | Vérifier que `GlobalExceptionHandler` de `common-lib` est utilisé partout. |
| 6.5 | Documentation Swagger complète | Ajouter `@Operation`, `@ApiResponse`, `@Tag` sur chaque endpoint. |

---

### Phase 7 — Production-Ready (~2 jours)

| # | Tâche | Détail |
|---|---|---|
| 7.1 | Sécuriser Eureka Server | Ajouter HTTP Basic Auth sur le Discovery Server. |
| 7.2 | Externaliser les configs | Migrer vers Spring Cloud Config Server ou variables d'environnement pour tous les secrets. |
| 7.3 | Health checks & Actuator | Activer `/actuator/health` sur chaque service pour le monitoring Docker/K8s. |
| 7.4 | Rate limiting sur le Gateway | Protéger les endpoints publics (login, register) contre le brute-force. |
| 7.5 | Docker Compose complet | Mettre à jour `docker-compose.yml` avec tous les services et leurs dépendances. |

---

## 📅 Estimation Globale

| Phase | Durée estimée | Priorité |
|---|---|---|
| Phase 0 — Corrections Critiques | ~1 jour | 🔴 BLOQUANT |
| Phase 1 — Solidifier le Socle | ~3 jours | 🔴 HAUTE |
| Phase 2 — CRUD Complets | ~5 jours | 🔴 HAUTE |
| Phase 3 — Logique Métier | ~5 jours | 🟡 MOYENNE |
| Phase 4 — RBAC & Sécurité | ~3 jours | 🔴 HAUTE |
| Phase 5 — Notifications & Events | ~2 jours | 🟡 MOYENNE |
| Phase 6 — Tests & Qualité | ~3 jours | 🟡 MOYENNE |
| Phase 7 — Production-Ready | ~2 jours | 🟢 BASSE |
| **TOTAL** | **~24 jours ouvrés** | |

> [!TIP]
> Je recommande de traiter les phases dans cet ordre : **0 → 1 → 4 → 2 → 3 → 5 → 6 → 7**. La sécurité (Phase 4) devrait être mise en place *avant* de construire tous les CRUDs (Phase 2), afin que chaque nouveau endpoint soit sécurisé dès sa création.
