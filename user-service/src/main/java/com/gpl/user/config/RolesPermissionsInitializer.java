package com.gpl.user.config;

import com.gpl.user.model.Permission;
import com.gpl.user.model.Person;
import com.gpl.user.model.Role;
import com.gpl.user.model.RolePermission;
import com.gpl.user.model.UserGroup;
import com.gpl.user.model.UserGroupMembership;
import com.gpl.user.model.UserRoleAssignment;
import com.gpl.user.repository.PermissionRepository;
import com.gpl.user.repository.PersonRepository;
import com.gpl.user.repository.RolePermissionRepository;
import com.gpl.user.repository.RoleRepository;
import com.gpl.user.repository.UserGroupMembershipRepository;
import com.gpl.user.repository.UserGroupRepository;
import com.gpl.user.repository.UserRoleAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Initialisation des rôles systèmes, permissions, affectations et groupes utilisateurs.
 * S'exécute après le UserDataInitializer pour pouvoir référencer les personnes créées.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   10.08.2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test", "local", "default"})
@Order(2)
public class RolesPermissionsInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PersonRepository personRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing roles, permissions, and user assignments...");
        initPermissions();
        initRoles();
        initRolePermissions();
        initUserRoleAssignments();
        initUserGroups();
        log.info("Roles & permissions initialization complete!");
    }

    private void initPermissions() {
        if (permissionRepository.count() > 0) {
            log.info("Permissions already exist, skipping");
            return;
        }
        int order = 0;

        order = seedPermissions("ORGANIZATION", "Gouvernance & organisations", order,
            "ORG_VIEW|Voir les détails d'une organisation",
            "ORG_VIEW_ALL|Voir toutes les organisations (cross-org)",
            "ORG_VIEW_OWN|Voir uniquement sa propre organisation",
            "ORG_CREATE|Créer une nouvelle organisation",
            "ORG_UPDATE|Modifier une organisation existante",
            "ORG_DELETE|Supprimer (archiver) une organisation",
            "ORG_ACTIVATE|Activer une organisation",
            "ORG_DEACTIVATE|Désactiver une organisation",
            "ORG_VIEW_HIERARCHY|Voir l'arborescence hiérarchique",
            "ORG_VIEW_CHILDREN|Voir les filiales directes",
            "ORG_RELATIONSHIP_VIEW|Voir les relations B2B",
            "ORG_RELATIONSHIP_CREATE|Créer une relation B2B",
            "ORG_RELATIONSHIP_UPDATE|Modifier une relation B2B",
            "ORG_RELATIONSHIP_DELETE|Supprimer une relation B2B",
            "CLASS_VIEW|Voir les classifications",
            "CLASS_CREATE|Créer une classification",
            "CLASS_UPDATE|Modifier une classification",
            "CLASS_DELETE|Supprimer une classification",
            "CLIENT_SITE_MANAGE|Gérer les spécificités des sites clients"
        );

        order = seedPermissions("SITE", "Sites & géolocalisation", order,
            "SITE_VIEW|Voir les détails d'un site",
            "SITE_VIEW_ALL|Voir tous les sites (cross-org)",
            "SITE_VIEW_OWN|Voir les sites de sa propre organisation",
            "SITE_CREATE|Créer un site",
            "SITE_UPDATE|Modifier un site",
            "SITE_DELETE|Supprimer un site",
            "SITE_ACTIVATE|Activer un site",
            "SITE_DEACTIVATE|Désactiver un site",
            "SITE_VIEW_NEARBY|Rechercher des sites par géolocalisation",
            "SITE_ASSIGN_PERSON|Assigner une personne à un site",
            "SITE_UNASSIGN_PERSON|Retirer une personne d'un site",
            "SITE_VIEW_ASSIGNMENTS|Voir les assignations d'un site"
        );

        order = seedPermissions("USER", "Identité & accès", order,
            "PERSON_VIEW|Voir les détails d'une personne",
            "PERSON_VIEW_ALL|Voir toutes les personnes (cross-org)",
            "PERSON_VIEW_OWN_ORG|Voir les personnes de son organisation",
            "PERSON_CREATE|Créer une personne",
            "PERSON_UPDATE|Modifier une personne",
            "PERSON_DELETE|Supprimer une personne",
            "PERSON_ACTIVATE|Activer une personne",
            "PERSON_DEACTIVATE|Désactiver une personne",
            "USER_ACCOUNT_CREATE|Créer un compte d'accès (AuthUser)",
            "USER_ACCOUNT_LOCK|Verrouiller un compte",
            "USER_ACCOUNT_UNLOCK|Déverrouiller un compte",
            "USER_ACCOUNT_RESET_PASSWORD|Réinitialiser un mot de passe",
            "ROLE_VIEW|Voir les rôles",
            "ROLE_CREATE|Créer un rôle",
            "ROLE_UPDATE|Modifier un rôle",
            "ROLE_DELETE|Supprimer un rôle",
            "ROLE_ASSIGN|Assigner un rôle à un utilisateur",
            "ROLE_REVOKE|Révoquer un rôle d'un utilisateur",
            "PERMISSION_VIEW|Voir les permissions disponibles",
            "PERMISSION_MANAGE|Gérer les permissions des rôles",
            "GROUP_VIEW|Voir les groupes d'utilisateurs",
            "GROUP_CREATE|Créer un groupe",
            "GROUP_UPDATE|Modifier un groupe",
            "GROUP_DELETE|Supprimer un groupe",
            "GROUP_ADD_MEMBER|Ajouter un membre à un groupe",
            "GROUP_REMOVE_MEMBER|Retirer un membre d'un groupe"
        );

        order = seedPermissions("AUTH", "Authentification", order,
            "AUTH_LOGIN|Se connecter",
            "AUTH_LOGOUT|Se déconnecter",
            "AUTH_CHANGE_OWN_PASSWORD|Changer son propre mot de passe",
            "AUTH_FORCE_PASSWORD_CHANGE|Forcer le changement de mot de passe d'un autre",
            "AUTH_VIEW_SESSIONS|Voir les sessions actives",
            "AUTH_TERMINATE_SESSION|Terminer la session d'un autre utilisateur"
        );

        order = seedPermissions("FLEET", "Flotte & dispositifs IoT", order,
            "VEHICLE_VIEW|Voir les détails d'un véhicule",
            "VEHICLE_VIEW_ALL|Voir tous les véhicules (cross-org)",
            "VEHICLE_VIEW_OWN_ORG|Voir les véhicules de son organisation",
            "VEHICLE_CREATE|Créer un véhicule",
            "VEHICLE_UPDATE|Modifier un véhicule",
            "VEHICLE_DELETE|Supprimer un véhicule",
            "VEHICLE_ACTIVATE|Activer un véhicule",
            "VEHICLE_DEACTIVATE|Désactiver un véhicule",
            "VEHICLE_ASSIGN_DRIVER|Assigner un chauffeur à un véhicule",
            "VEHICLE_UNASSIGN_DRIVER|Retirer un chauffeur d'un véhicule",
            "DEVICE_VIEW|Voir les détails d'un device (GPS/PDA)",
            "DEVICE_VIEW_ALL|Voir tous les devices",
            "DEVICE_VIEW_OWN_ORG|Voir les devices de son organisation",
            "DEVICE_CREATE|Enregistrer un device",
            "DEVICE_UPDATE|Modifier un device",
            "DEVICE_DELETE|Supprimer un device",
            "DEVICE_ASSIGN|Assigner un device à une personne/véhicule",
            "DEVICE_UNASSIGN|Retirer un device",
            "DEVICE_VIEW_STATUS|Voir l'historique d'état des devices",
            "DEVICE_VIEW_POSITION|Voir la position GPS en temps réel",
            "fleet.vehicles.read|Consulter les véhicules (API flotte)",
            "fleet.vehicles.create|Créer un véhicule (API flotte)",
            "fleet.vehicles.write|Modifier un véhicule (API flotte)",
            "fleet.vehicles.manage|Supprimer un véhicule (API flotte)",
            "fleet.devices.read|Consulter les devices (API flotte)",
            "fleet.devices.create|Créer un device (API flotte)",
            "fleet.devices.write|Modifier/assigner un device (API flotte)",
            "fleet.devices.manage|Supprimer un device (API flotte)",
            "TELEMETRY_INGEST|Injecter des données de télémétrie (GPS)",
            "TELEMETRY_VIEW|Consulter la télémétrie et les trajectoires"
        );

        order = seedPermissions("TOUR", "Tournées, checkpoints & livraisons", order,
            "TOUR_VIEW|Voir les détails d'une tournée",
            "TOUR_VIEW_ALL|Voir toutes les tournées (cross-org)",
            "TOUR_VIEW_OWN_ORG|Voir les tournées de son organisation",
            "TOUR_CREATE|Créer une tournée",
            "TOUR_UPDATE|Modifier une tournée",
            "TOUR_DELETE|Supprimer une tournée",
            "TOUR_START|Démarrer une tournée",
            "TOUR_CLOSE|Clôturer une tournée",
            "TOUR_CANCEL|Annuler une tournée",
            "TOUR_ACK|Accuser réception d'une tournée sous-traitée",
            "TOUR_ASSIGN_DRIVER|Assigner un chauffeur/livreur",
            "TOUR_ASSIGN_VEHICLE|Assigner un véhicule",
            "CHECKPOINT_VIEW|Voir les checkpoints d'une tournée",
            "CHECKPOINT_CREATE|Ajouter un checkpoint",
            "CHECKPOINT_UPDATE|Modifier un checkpoint",
            "CHECKPOINT_DELETE|Supprimer un checkpoint",
            "CHECKPOINT_VALIDATE|Valider un checkpoint (arrivée confirmée)",
            "CHECKPOINT_SKIP|Sauter un checkpoint (avec raison)",
            "PICKUP_VIEW|Voir les demandes d'enlèvement",
            "PICKUP_CREATE|Créer une demande d'enlèvement",
            "PICKUP_UPDATE|Modifier une demande d'enlèvement",
            "PICKUP_APPROVE|Approuver une demande",
            "PICKUP_REJECT|Rejeter une demande",
            "PICKUP_DELETE|Supprimer une demande d'enlèvement",
            "PICKUP_VIEW_ALL|Voir toutes les demandes (cross-org)",
            "PICKUP_VIEW_OWN_ORG|Voir les demandes de son organisation",
            "CONTRACT_VIEW|Voir les contrats transporteurs",
            "CONTRACT_CREATE|Créer un contrat transporteur",
            "CONTRACT_UPDATE|Modifier un contrat",
            "CONTRACT_TERMINATE|Résilier un contrat",
            "CONTRACT_DELETE|Supprimer un contrat",
            "CONTRACT_VIEW_ALL|Voir tous les contrats (cross-org)"
        );

        order = seedPermissions("CYLINDER", "Bouteilles, RFID & scans", order,
            "CYLINDER_VIEW|Voir les détails d'une bouteille",
            "CYLINDER_VIEW_ALL|Voir toutes les bouteilles (cross-org)",
            "CYLINDER_VIEW_OWN_ORG|Voir les bouteilles de son organisation",
            "CYLINDER_CREATE|Enregistrer une bouteille",
            "CYLINDER_UPDATE|Modifier une bouteille",
            "CYLINDER_DELETE|Supprimer une bouteille",
            "CYLINDER_TRANSFER|Transférer une bouteille entre sites",
            "RFID_VIEW|Voir les tags RFID",
            "RFID_CREATE|Enregistrer un tag RFID",
            "RFID_UPDATE|Modifier un tag RFID",
            "RFID_DELETE|Supprimer un tag RFID",
            "RFID_ASSIGN|Associer un tag RFID à une bouteille",
            "RFID_UNASSIGN|Dissocier un tag RFID",
            "RFID_VIEW_ALL|Voir tous les tags (cross-org)",
            "SCAN_VIEW|Voir les événements de scan",
            "SCAN_CREATE|Enregistrer un scan (depuis PDA)",
            "SCAN_VIEW_ALL|Voir tous les scans (cross-org)",
            "SCAN_VIEW_OWN_ORG|Voir les scans de son organisation",
            "SCAN_EXPORT|Exporter les données de scan",
            "SCAN_DELETE|Supprimer un événement de scan",
            "SCAN_RESOLVE_CONFLICT|Résoudre un conflit de scan"
        );

        order = seedPermissions("SUBSIDY", "Déclarations & subventions", order,
            "DECLARATION_VIEW|Voir les déclarations de volumes",
            "DECLARATION_VIEW_ALL|Voir toutes les déclarations (cross-org)",
            "DECLARATION_VIEW_OWN_ORG|Voir les déclarations de son organisation",
            "DECLARATION_CREATE|Créer une déclaration",
            "DECLARATION_UPDATE|Modifier une déclaration",
            "DECLARATION_DELETE|Supprimer une déclaration",
            "DECLARATION_SUBMIT|Soumettre une déclaration pour validation",
            "DECLARATION_APPROVE|Approuver une déclaration",
            "DECLARATION_REJECT|Rejeter une déclaration",
            "RECONCILIATION_VIEW|Voir les réconciliations",
            "RECONCILIATION_CREATE|Lancer une réconciliation",
            "RECONCILIATION_VERIFY|Vérifier/valider une réconciliation",
            "RECONCILIATION_VIEW_ALL|Voir toutes les réconciliations (cross-org)",
            "RECONCILIATION_DELETE|Supprimer une réconciliation",
            "REDESSEMENT_VIEW|Voir les redressements financiers",
            "REDESSEMENT_CREATE|Émettre un redressement",
            "REDESSEMENT_UPDATE|Modifier/payer/annuler un redressement",
            "REDESSEMENT_DELETE|Supprimer un redressement",
            "REDESSEMENT_APPROVE|Approuver un redressement",
            "REDESSEMENT_VIEW_ALL|Voir tous les redressements (cross-org)",
            "SUBSIDY_DASHBOARD|Accéder au tableau de bord subventions"
        );

        order = seedPermissions("NOTIFICATION", "Notifications", order,
            "NOTIFICATION_SEND|Envoyer une notification",
            "NOTIFICATION_SEND_BULK|Envoyer des notifications en masse",
            "NOTIFICATION_VIEW_LOG|Voir l'historique des notifications",
            "NOTIFICATION_VIEW_LOG_ALL|Voir tous les logs (cross-org)",
            "TEMPLATE_VIEW|Voir les templates de notification",
            "TEMPLATE_CREATE|Créer un template",
            "TEMPLATE_UPDATE|Modifier un template",
            "TEMPLATE_DELETE|Supprimer un template"
        );

        order = seedPermissions("AUDIT", "Audits & conformité", order,
            "AUDIT_VIEW_MODIFICATIONS|Voir les modifications d'entités",
            "AUDIT_VIEW_STATUS_HISTORY|Voir l'historique des changements de statut",
            "AUDIT_VIEW_SUMMARY|Voir le résumé d'audit d'une entité",
            "AUDIT_VIEW_ALL|Voir les audits de toutes les organisations",
            "AUDIT_VIEW_OWN_ORG|Voir les audits de sa propre organisation",
            "AUDIT_EXPORT|Exporter les données d'audit"
        );

        order = seedPermissions("PLATFORM", "Système & reporting", order,
            "SETTINGS_VIEW|Voir les paramètres système",
            "SETTINGS_UPDATE|Modifier les paramètres système",
            "DASHBOARD_VIEW|Accéder au tableau de bord principal",
            "DASHBOARD_VIEW_ANALYTICS|Accéder aux analytics avancés",
            "MONITORING_VIEW|Accéder au monitoring technique",
            "REPORT_GENERATE|Générer un rapport",
            "REPORT_EXPORT|Exporter un rapport"
        );

        log.info("Created {} permissions", permissionRepository.count());
    }

    /**
     * Initialise les rôles système.
     * Le rôle SUPERADMIN existe déjà dans le plan ; les rôles ORG_ADMIN, SITE_MANAGER,
     * OPERATOR et VIEWER sont ajoutés pour couvrir les rôles par défaut du plan d'attentes.
     */
    private void initRoles() {
        if (roleRepository.count() > 0) {
            log.info("Roles already exist, skipping");
            return;
        }

        createRole("SUPERADMIN", "Super Admin", "Supervision totale — carte ultra-détaillée, tous modules", "REG", "T1", 0);
        createRole("ADMIN", "Administrateur", "Staff CSPH / RH — gestion utilisateurs, agents, marketeurs, rapports", "REG", "T1", 1);
        createRole("SUPERVISOR", "Superviseur", "DevOps / monitoring — Prometheus, Grafana, alertes, scores de risque", "REG", "T1", 2);
        createRole("INTEGRATEUR", "Intégrateur", "Spécialiste domaine — activation, authentification, maintenance matériel PDA+GPS+RFID", "REG", "T1", 3);
        createRole("AGENT", "Agent validateur", "Validateur terrain — suivi marketeurs, reset passwords, validation déclarations", "REG", "T1", 4);
        createRole("MARKETER", "Marketeur", "Société pétrolière — flotte, tournées, quotas, chauffeurs, règles personnalisées", "MKT", "T3", 5);
        createRole("TRANSPORTER", "Transporteur", "Transporteur — flotte, tournées, scans RFID/PDA, points de contrôle et chauffeurs", "TRP", "T3", 6);
        createRole("DRIVER", "Livreur", "Application PDA mobile — missions, scans RFID et livraisons (sans interface web)", "TRP", "T3", 7);

        // Rôles par défaut additionnels du plan d'attentes
        createRole("ORG_ADMIN", "Admin Organisation", "Gestion globale d'une organisation et de ses sites", "REG", "T1", 8);
        createRole("SITE_MANAGER", "Responsable Site", "Gestion opérationnelle des sites et de leurs assignations", "REG", "T1", 9);
        createRole("OPERATOR", "Opérateur", "Exploitation terrain — tournées, scans, checkpoints, livraisons", "REG", "T1", 10);
        createRole("VIEWER", "Lecteur", "Accès en lecture seule à l'ensemble des modules (sans écriture)", "REG", "T1", 11);

        log.info("Created {} roles", roleRepository.count());
    }

    /**
     * Initialise les permissions accordées aux rôles.
     */
    private void initRolePermissions() {
        // Note: NO early-return guard here. `grantPermissionsToRole` is itself
        // idempotent (it skips already-granted permissions). This lets us
        // add new role-permission pairs across deployments without having to
        // wipe the role_permissions table.
        log.info("Reconciling role-permission grants (idempotent)...");

        // SUPERADMIN
        grantAllPermissionsToRole("SUPERADMIN");

        // ADMIN
        grantPermissionsToRole("ADMIN",
            "PERSON_VIEW", "PERSON_UPDATE", "PERSON_CREATE", "PERSON_DELETE",
            "USER_ACCOUNT_CREATE", "USER_ACCOUNT_LOCK", "USER_ACCOUNT_UNLOCK", "USER_ACCOUNT_RESET_PASSWORD",
            "PERSON_ACTIVATE", "PERSON_DEACTIVATE",
            "ROLE_VIEW", "ROLE_CREATE", "ROLE_UPDATE", "ROLE_DELETE", "ROLE_ASSIGN", "ROLE_REVOKE",
            "PERMISSION_VIEW", "PERMISSION_MANAGE",
            "GROUP_VIEW", "GROUP_CREATE", "GROUP_UPDATE", "GROUP_DELETE", "GROUP_ADD_MEMBER", "GROUP_REMOVE_MEMBER",
            "ORG_VIEW", "ORG_VIEW_ALL", "ORG_VIEW_OWN", "ORG_CREATE", "ORG_UPDATE", "ORG_DELETE",
            "ORG_ACTIVATE", "ORG_DEACTIVATE", "ORG_VIEW_HIERARCHY", "ORG_VIEW_CHILDREN",
            "ORG_RELATIONSHIP_VIEW", "ORG_RELATIONSHIP_CREATE", "ORG_RELATIONSHIP_UPDATE", "ORG_RELATIONSHIP_DELETE",
            "CLASS_VIEW", "CLASS_CREATE", "CLASS_UPDATE", "CLASS_DELETE", "CLIENT_SITE_MANAGE",
            "SITE_VIEW", "SITE_VIEW_ALL", "SITE_VIEW_OWN", "SITE_CREATE", "SITE_UPDATE", "SITE_DELETE",
            "SITE_ACTIVATE", "SITE_DEACTIVATE", "SITE_VIEW_NEARBY", "SITE_ASSIGN_PERSON", "SITE_UNASSIGN_PERSON", "SITE_VIEW_ASSIGNMENTS",
            "VEHICLE_VIEW", "VEHICLE_VIEW_ALL", "VEHICLE_CREATE", "VEHICLE_UPDATE", "VEHICLE_DELETE",
            "DEVICE_VIEW", "DEVICE_VIEW_ALL", "DEVICE_CREATE", "DEVICE_UPDATE", "DEVICE_DELETE",
            "fleet.vehicles.read", "fleet.vehicles.create", "fleet.vehicles.write", "fleet.vehicles.manage",
            "fleet.devices.read", "fleet.devices.create", "fleet.devices.write", "fleet.devices.manage",
            "TOUR_VIEW", "TOUR_VIEW_ALL", "TOUR_CREATE", "TOUR_UPDATE", "TOUR_DELETE",
            "CHECKPOINT_VIEW", "CHECKPOINT_CREATE", "CHECKPOINT_UPDATE", "CHECKPOINT_DELETE", "CHECKPOINT_VALIDATE", "CHECKPOINT_SKIP",
            "CYLINDER_VIEW", "CYLINDER_VIEW_ALL", "CYLINDER_CREATE", "CYLINDER_UPDATE", "CYLINDER_TRANSFER",
            "RFID_VIEW", "RFID_CREATE", "RFID_UPDATE", "RFID_DELETE", "RFID_ASSIGN", "RFID_UNASSIGN", "RFID_VIEW_ALL",
            "SCAN_VIEW", "SCAN_CREATE", "SCAN_VIEW_ALL", "SCAN_EXPORT", "SCAN_DELETE", "SCAN_RESOLVE_CONFLICT",
            "PICKUP_VIEW", "PICKUP_CREATE", "PICKUP_UPDATE", "PICKUP_APPROVE", "PICKUP_REJECT", "PICKUP_DELETE", "PICKUP_VIEW_ALL", "PICKUP_VIEW_OWN_ORG",
            "CONTRACT_VIEW", "CONTRACT_CREATE", "CONTRACT_UPDATE", "CONTRACT_TERMINATE", "CONTRACT_DELETE", "CONTRACT_VIEW_ALL",
            "DECLARATION_VIEW", "DECLARATION_VIEW_ALL", "DECLARATION_VIEW_OWN_ORG", "DECLARATION_CREATE", "DECLARATION_UPDATE", "DECLARATION_DELETE",
            "DECLARATION_SUBMIT", "DECLARATION_APPROVE", "DECLARATION_REJECT",
            "RECONCILIATION_VIEW", "RECONCILIATION_CREATE", "RECONCILIATION_VERIFY", "RECONCILIATION_VIEW_ALL", "RECONCILIATION_DELETE",
            "REDESSEMENT_VIEW", "REDESSEMENT_CREATE", "REDESSEMENT_UPDATE", "REDESSEMENT_DELETE", "REDESSEMENT_APPROVE", "REDESSEMENT_VIEW_ALL",
            "SUBSIDY_DASHBOARD",
            "NOTIFICATION_SEND", "NOTIFICATION_SEND_BULK", "NOTIFICATION_VIEW_LOG", "NOTIFICATION_VIEW_LOG_ALL",
            "TEMPLATE_VIEW", "TEMPLATE_CREATE", "TEMPLATE_UPDATE", "TEMPLATE_DELETE",
            "AUDIT_VIEW_MODIFICATIONS", "AUDIT_VIEW_STATUS_HISTORY", "AUDIT_VIEW_SUMMARY", "AUDIT_VIEW_ALL", "AUDIT_VIEW_OWN_ORG", "AUDIT_EXPORT",
            "SETTINGS_VIEW", "SETTINGS_UPDATE", "DASHBOARD_VIEW", "DASHBOARD_VIEW_ANALYTICS", "MONITORING_VIEW", "REPORT_GENERATE", "REPORT_EXPORT",
            "AUTH_VIEW_SESSIONS", "AUTH_TERMINATE_SESSION"
        );

        // SUPERVISOR
        grantPermissionsToRole("SUPERVISOR",
            "MONITORING_VIEW", "DASHBOARD_VIEW", "DASHBOARD_VIEW_ANALYTICS", "REPORT_GENERATE", "REPORT_EXPORT",
            "AUDIT_VIEW_MODIFICATIONS", "AUDIT_VIEW_STATUS_HISTORY", "AUDIT_VIEW_ALL", "AUDIT_VIEW_OWN_ORG", "AUDIT_EXPORT",
            "DEVICE_VIEW", "DEVICE_VIEW_ALL", "DEVICE_VIEW_STATUS", "DEVICE_VIEW_POSITION",
            "VEHICLE_VIEW", "VEHICLE_VIEW_ALL",
            "fleet.vehicles.read", "fleet.devices.read", "TELEMETRY_VIEW",
            "TOUR_VIEW", "TOUR_VIEW_ALL", "TOUR_VIEW_OWN_ORG",
            "CHECKPOINT_VIEW", "SCAN_VIEW", "SCAN_VIEW_ALL",
            "SETTINGS_VIEW"
        );

        // INTEGRATEUR
        grantPermissionsToRole("INTEGRATEUR",
            "DEVICE_VIEW", "DEVICE_UPDATE", "DEVICE_CREATE", "DEVICE_DELETE", "DEVICE_ASSIGN", "DEVICE_UNASSIGN",
            "DEVICE_VIEW_STATUS", "DEVICE_VIEW_POSITION", "DEVICE_VIEW_OWN_ORG",
            "RFID_VIEW", "RFID_CREATE", "RFID_UPDATE", "RFID_DELETE", "RFID_ASSIGN", "RFID_UNASSIGN", "RFID_VIEW_ALL",
            "VEHICLE_VIEW", "VEHICLE_UPDATE", "VEHICLE_VIEW_ALL",
            "fleet.vehicles.read", "fleet.devices.read", "fleet.devices.create", "fleet.devices.write", "fleet.devices.manage",
            "TELEMETRY_INGEST", "TELEMETRY_VIEW",
            "PERSON_VIEW", "SITE_VIEW", "TOUR_VIEW", "CHECKPOINT_VIEW", "SCAN_VIEW",
            "MONITORING_VIEW", "DASHBOARD_VIEW_ANALYTICS", "AUDIT_VIEW_MODIFICATIONS", "AUTH_VIEW_SESSIONS", "SETTINGS_VIEW"
        );

        // AGENT
        grantPermissionsToRole("AGENT",
            "PERSON_VIEW", "USER_ACCOUNT_CREATE", "USER_ACCOUNT_RESET_PASSWORD", "USER_ACCOUNT_LOCK", "USER_ACCOUNT_UNLOCK",
            "ROLE_VIEW", "PERMISSION_VIEW",
            "ORG_VIEW",
            "SITE_VIEW", "SITE_VIEW_OWN",
            "DECLARATION_VIEW", "DECLARATION_UPDATE", "DECLARATION_APPROVE", "DECLARATION_REJECT", "DECLARATION_SUBMIT",
            "RECONCILIATION_VIEW", "RECONCILIATION_VERIFY",
            "REDESSEMENT_VIEW", "REDESSEMENT_APPROVE",
            "CHECKPOINT_VIEW", "CHECKPOINT_VALIDATE",
            "TOUR_VIEW", "PICKUP_VIEW",
            "REPORT_GENERATE", "DASHBOARD_VIEW_ANALYTICS", "MONITORING_VIEW",
            "NOTIFICATION_VIEW_LOG", "AUDIT_VIEW_MODIFICATIONS"
        );

        // DRIVER / LIVREUR — mobile/PDA driver. Carries every permission needed
        // to execute a full tournee from the field (view assigned tournees,
        // start/close the tournee, scan RFID tags at each checkpoint,
        // validate/skip checkpoints). Cross-org capabilities (TOUR_VIEW_ALL,
        // TOUR_CREATE, etc.) remain ADMIN-only by design.
        //
        // Both DRIVER (legacy code) and LIVREUR (newly created users) must
        // carry these grants because PersonService may use either code when
        // a /users/with-auth request specifies `roleName: LIVREUR` (which maps
        // to a LIVREUR role row in user_role_assignments).
        grantPermissionsToRole("DRIVER",
            "TOUR_VIEW", "TOUR_VIEW_OWN_ORG",
            "TOUR_START", "TOUR_CLOSE",
            "CHECKPOINT_VIEW", "CHECKPOINT_CREATE", "CHECKPOINT_UPDATE", "CHECKPOINT_VALIDATE", "CHECKPOINT_SKIP",
            "SCAN_VIEW", "SCAN_CREATE", "SCAN_VIEW_OWN_ORG", "SCAN_EXPORT", "SCAN_RESOLVE_CONFLICT",
            "RFID_VIEW", "RFID_CREATE",
            "PERSON_VIEW",
            "SITE_VIEW", "SITE_VIEW_OWN",
            "VEHICLE_VIEW", "VEHICLE_VIEW_OWN_ORG",
            "DASHBOARD_VIEW_ANALYTICS", "MONITORING_VIEW", "REPORT_GENERATE"
        );
        grantPermissionsToRole("LIVREUR",
            "TOUR_VIEW", "TOUR_VIEW_OWN_ORG",
            "TOUR_START", "TOUR_CLOSE",
            "CHECKPOINT_VIEW", "CHECKPOINT_CREATE", "CHECKPOINT_UPDATE", "CHECKPOINT_VALIDATE", "CHECKPOINT_SKIP",
            "SCAN_VIEW", "SCAN_CREATE", "SCAN_VIEW_OWN_ORG", "SCAN_EXPORT", "SCAN_RESOLVE_CONFLICT",
            "RFID_VIEW", "RFID_CREATE",
            "PERSON_VIEW",
            "SITE_VIEW", "SITE_VIEW_OWN",
            "VEHICLE_VIEW", "VEHICLE_VIEW_OWN_ORG",
            "DASHBOARD_VIEW_ANALYTICS", "MONITORING_VIEW", "REPORT_GENERATE"
        );

        // MARKETER
        grantPermissionsToRole("MARKETER",
            "TOUR_VIEW", "TOUR_VIEW_ALL", "TOUR_VIEW_OWN_ORG",
            "TOUR_CREATE", "TOUR_UPDATE", "TOUR_DELETE",
            "TOUR_START", "TOUR_CLOSE", "TOUR_CANCEL", "TOUR_ACK",
            "TOUR_ASSIGN_DRIVER", "TOUR_ASSIGN_VEHICLE",
            "CHECKPOINT_VIEW", "CHECKPOINT_CREATE", "CHECKPOINT_UPDATE", "CHECKPOINT_VALIDATE", "CHECKPOINT_SKIP",
            "VEHICLE_VIEW", "VEHICLE_VIEW_ALL", "VEHICLE_CREATE", "VEHICLE_UPDATE", "VEHICLE_DELETE",
            "VEHICLE_ASSIGN_DRIVER", "VEHICLE_UNASSIGN_DRIVER", "VEHICLE_VIEW_OWN_ORG",
            "fleet.vehicles.read", "fleet.vehicles.create", "fleet.vehicles.write", "fleet.vehicles.manage",
            "PERSON_VIEW", "PERSON_CREATE", "PERSON_UPDATE",
            "ORG_VIEW", "ORG_VIEW_OWN",
            "SITE_VIEW", "SITE_VIEW_OWN", "SITE_CREATE", "SITE_UPDATE", "SITE_VIEW_ALL",
            "TOUR_VIEW", "TOUR_VIEW_ALL", "TOUR_VIEW_OWN_ORG", "TOUR_CREATE", "TOUR_UPDATE", "TOUR_DELETE",
            "TOUR_ASSIGN_DRIVER", "TOUR_ASSIGN_VEHICLE",
            "CHECKPOINT_VIEW", "CHECKPOINT_CREATE", "CHECKPOINT_UPDATE", "CHECKPOINT_VALIDATE",
            "SCAN_VIEW", "SCAN_CREATE", "SCAN_VIEW_OWN_ORG", "SCAN_EXPORT", "SCAN_RESOLVE_CONFLICT", "SCAN_VIEW_ALL",
            "PICKUP_VIEW", "PICKUP_CREATE", "PICKUP_UPDATE", "PICKUP_APPROVE", "PICKUP_REJECT", "PICKUP_VIEW_ALL", "PICKUP_VIEW_OWN_ORG",
            "CONTRACT_VIEW",
            "DECLARATION_VIEW", "DECLARATION_VIEW_ALL", "DECLARATION_VIEW_OWN_ORG", "DECLARATION_CREATE", "DECLARATION_UPDATE", "DECLARATION_SUBMIT",
            "SUBSIDY_DASHBOARD",
            "CYLINDER_VIEW", "CYLINDER_VIEW_ALL", "CYLINDER_VIEW_OWN_ORG", "CYLINDER_CREATE", "CYLINDER_UPDATE", "CYLINDER_TRANSFER",
            "RFID_VIEW", "RFID_ASSIGN",
            "REPORT_GENERATE", "REPORT_EXPORT", "DASHBOARD_VIEW", "DASHBOARD_VIEW_ANALYTICS", "NOTIFICATION_VIEW_LOG"
        );

        // TRANSPORTER
        grantPermissionsToRole("TRANSPORTER",
            "VEHICLE_VIEW", "VEHICLE_VIEW_ALL", "VEHICLE_VIEW_OWN_ORG", "VEHICLE_CREATE", "VEHICLE_UPDATE", "VEHICLE_DELETE",
            "VEHICLE_ASSIGN_DRIVER", "VEHICLE_UNASSIGN_DRIVER", "VEHICLE_ACTIVATE", "VEHICLE_DEACTIVATE",
            "fleet.vehicles.read", "fleet.vehicles.create", "fleet.vehicles.write", "fleet.vehicles.manage",
            "PERSON_VIEW", "PERSON_CREATE", "PERSON_UPDATE",
            "ORG_VIEW", "ORG_VIEW_OWN",
            "SITE_VIEW", "SITE_VIEW_OWN",
            "TOUR_VIEW", "TOUR_VIEW_OWN_ORG", "TOUR_CREATE", "TOUR_UPDATE", "TOUR_DELETE",
            "TOUR_ACK",
            "TOUR_ASSIGN_DRIVER", "TOUR_ASSIGN_VEHICLE",
            "CHECKPOINT_VIEW", "CHECKPOINT_CREATE", "CHECKPOINT_UPDATE", "CHECKPOINT_DELETE",
            "CHECKPOINT_VALIDATE", "CHECKPOINT_SKIP",
            "SCAN_VIEW", "SCAN_CREATE", "SCAN_VIEW_OWN_ORG", "SCAN_VIEW_ALL", "SCAN_EXPORT", "SCAN_RESOLVE_CONFLICT",
            "PICKUP_VIEW", "PICKUP_VIEW_OWN_ORG",
            "CONTRACT_VIEW",
            "DEVICE_VIEW", "DEVICE_VIEW_STATUS", "DEVICE_VIEW_POSITION", "DEVICE_VIEW_OWN_ORG",
            "fleet.devices.read", "TELEMETRY_VIEW",
            "RFID_VIEW", "RFID_UPDATE", "RFID_ASSIGN", "RFID_VIEW_ALL",
            "CYLINDER_VIEW", "CYLINDER_VIEW_OWN_ORG",
            "DECLARATION_VIEW", "SUBSIDY_DASHBOARD",
            "REPORT_GENERATE", "REPORT_EXPORT", "DASHBOARD_VIEW", "DASHBOARD_VIEW_ANALYTICS",
            "AUDIT_VIEW_MODIFICATIONS"
        );

        // DRIVER
        grantPermissionsToRole("DRIVER",
            "TOUR_VIEW", "TOUR_VIEW_OWN_ORG", "TOUR_UPDATE", "TOUR_ASSIGN_DRIVER",
            "CHECKPOINT_VIEW", "CHECKPOINT_UPDATE", "CHECKPOINT_VALIDATE", "CHECKPOINT_SKIP",
            "SCAN_VIEW", "SCAN_CREATE", "SCAN_VIEW_OWN_ORG", "SCAN_EXPORT", "SCAN_RESOLVE_CONFLICT",
            "RFID_VIEW", "RFID_UPDATE", "RFID_ASSIGN", "RFID_VIEW_ALL",
            "DEVICE_VIEW", "DEVICE_VIEW_STATUS", "DEVICE_VIEW_POSITION",
            "fleet.devices.read", "TELEMETRY_VIEW",
            "PICKUP_VIEW", "PICKUP_VIEW_OWN_ORG",
            "CYLINDER_VIEW", "CYLINDER_VIEW_OWN_ORG",
            "DASHBOARD_VIEW", "AUDIT_VIEW_MODIFICATIONS", "NOTIFICATION_VIEW_LOG"
        );

        // ORG_ADMIN (rôle par défaut du plan)
        grantPermissionsToRole("ORG_ADMIN",
            "ORG_VIEW", "ORG_VIEW_ALL", "ORG_VIEW_OWN", "ORG_CREATE", "ORG_UPDATE", "ORG_DELETE",
            "ORG_ACTIVATE", "ORG_DEACTIVATE", "ORG_VIEW_HIERARCHY", "ORG_VIEW_CHILDREN",
            "ORG_RELATIONSHIP_VIEW", "ORG_RELATIONSHIP_CREATE", "ORG_RELATIONSHIP_UPDATE", "ORG_RELATIONSHIP_DELETE",
            "CLASS_VIEW", "CLASS_CREATE", "CLASS_UPDATE", "CLASS_DELETE", "CLIENT_SITE_MANAGE",
            "SITE_VIEW", "SITE_VIEW_ALL", "SITE_VIEW_OWN", "SITE_CREATE", "SITE_UPDATE", "SITE_DELETE",
            "SITE_ACTIVATE", "SITE_DEACTIVATE", "SITE_VIEW_NEARBY", "SITE_ASSIGN_PERSON", "SITE_UNASSIGN_PERSON", "SITE_VIEW_ASSIGNMENTS"
        );

        // SITE_MANAGER (rôle par défaut du plan)
        grantPermissionsToRole("SITE_MANAGER",
            "SITE_VIEW", "SITE_VIEW_ALL", "SITE_VIEW_OWN", "SITE_CREATE", "SITE_UPDATE", "SITE_DELETE",
            "SITE_ACTIVATE", "SITE_DEACTIVATE", "SITE_VIEW_NEARBY", "SITE_ASSIGN_PERSON", "SITE_UNASSIGN_PERSON", "SITE_VIEW_ASSIGNMENTS",
            "CLASS_VIEW", "ORG_VIEW", "PERSON_VIEW", "TOUR_VIEW", "VEHICLE_VIEW", "DEVICE_VIEW",
            "fleet.vehicles.read", "fleet.devices.read"
        );

        // OPERATOR (rôle par défaut du plan)
        grantPermissionsToRole("OPERATOR",
            "VEHICLE_VIEW", "VEHICLE_VIEW_OWN_ORG", "VEHICLE_UPDATE", "VEHICLE_ASSIGN_DRIVER",
            "DEVICE_VIEW", "DEVICE_VIEW_OWN_ORG", "DEVICE_UPDATE", "DEVICE_VIEW_STATUS", "DEVICE_VIEW_POSITION",
            "fleet.vehicles.read", "fleet.devices.read", "TELEMETRY_VIEW",
            "TOUR_VIEW", "TOUR_VIEW_OWN_ORG", "TOUR_UPDATE", "TOUR_ASSIGN_DRIVER", "TOUR_ASSIGN_VEHICLE",
            "CHECKPOINT_VIEW", "CHECKPOINT_CREATE", "CHECKPOINT_UPDATE", "CHECKPOINT_VALIDATE", "CHECKPOINT_SKIP",
            "SCAN_VIEW", "SCAN_CREATE", "SCAN_VIEW_OWN_ORG", "SCAN_EXPORT", "SCAN_RESOLVE_CONFLICT",
            "CYLINDER_VIEW", "CYLINDER_VIEW_OWN_ORG", "CYLINDER_TRANSFER",
            "RFID_VIEW", "RFID_UPDATE", "RFID_ASSIGN",
            "PICKUP_VIEW", "PICKUP_VIEW_OWN_ORG", "PICKUP_UPDATE", "PICKUP_APPROVE", "PICKUP_REJECT",
            "CONTRACT_VIEW", "CONTRACT_UPDATE",
            "DECLARATION_VIEW", "DECLARATION_VIEW_OWN_ORG", "DECLARATION_UPDATE", "DECLARATION_SUBMIT", "DECLARATION_APPROVE", "DECLARATION_REJECT",
            "RECONCILIATION_VIEW", "RECONCILIATION_VERIFY",
            "REDESSEMENT_VIEW", "AUDIT_VIEW_MODIFICATIONS", "AUDIT_VIEW_STATUS_HISTORY",
            "REPORT_GENERATE", "DASHBOARD_VIEW"
        );

        // VIEWER (rôle par défaut du plan)
        grantPermissionsToRole("VIEWER",
            "PERSON_VIEW", "PERSON_VIEW_ALL", "PERSON_VIEW_OWN_ORG",
            "ORG_VIEW", "ORG_VIEW_ALL", "ORG_VIEW_OWN", "ORG_VIEW_HIERARCHY", "ORG_VIEW_CHILDREN",
            "SITE_VIEW", "SITE_VIEW_ALL", "SITE_VIEW_OWN", "SITE_VIEW_NEARBY", "SITE_VIEW_ASSIGNMENTS",
            "CLASS_VIEW", "CLIENT_SITE_MANAGE",
            "VEHICLE_VIEW", "VEHICLE_VIEW_ALL", "VEHICLE_VIEW_OWN_ORG",
            "DEVICE_VIEW", "DEVICE_VIEW_ALL", "DEVICE_VIEW_OWN_ORG", "DEVICE_VIEW_STATUS", "DEVICE_VIEW_POSITION",
            "fleet.vehicles.read", "fleet.devices.read", "TELEMETRY_VIEW",
            "TOUR_VIEW", "TOUR_VIEW_ALL", "TOUR_VIEW_OWN_ORG",
            "CHECKPOINT_VIEW", "PICKUP_VIEW", "PICKUP_VIEW_ALL", "PICKUP_VIEW_OWN_ORG",
            "CONTRACT_VIEW", "CONTRACT_VIEW_ALL",
            "CYLINDER_VIEW", "CYLINDER_VIEW_ALL", "CYLINDER_VIEW_OWN_ORG",
            "RFID_VIEW", "RFID_VIEW_ALL",
            "SCAN_VIEW", "SCAN_VIEW_ALL", "SCAN_VIEW_OWN_ORG", "SCAN_EXPORT",
            "DECLARATION_VIEW", "DECLARATION_VIEW_ALL", "DECLARATION_VIEW_OWN_ORG",
            "RECONCILIATION_VIEW", "RECONCILIATION_VIEW_ALL",
            "REDESSEMENT_VIEW", "REDESSEMENT_VIEW_ALL",
            "SUBSIDY_DASHBOARD",
            "NOTIFICATION_VIEW_LOG", "NOTIFICATION_VIEW_LOG_ALL", "TEMPLATE_VIEW",
            "AUDIT_VIEW_MODIFICATIONS", "AUDIT_VIEW_STATUS_HISTORY", "AUDIT_VIEW_SUMMARY", "AUDIT_VIEW_ALL", "AUDIT_VIEW_OWN_ORG", "AUDIT_EXPORT",
            "SETTINGS_VIEW", "DASHBOARD_VIEW", "DASHBOARD_VIEW_ANALYTICS", "MONITORING_VIEW", "REPORT_GENERATE", "REPORT_EXPORT"
        );

        log.info("Created {} role permissions", rolePermissionRepository.count());
    }

    /**
     * Crée une catégorie de permissions.
     */
    private int seedPermissions(String module, String moduleDesc, int startOrder, String... entries) {
        int order = startOrder;
        for (String entry : entries) {
            String[] parts = entry.split("\\|");
            Permission p = new Permission();
            p.setCode(parts[0]);
            p.setName(parts[1]);
            p.setDescription(parts[1]);
            p.setModule(module);
            p.setModuleDescription(moduleDesc);
            p.setActive(true);
            p.setSortOrder(order++);
            p.setCreatedBy("SYSTEM_INIT");
            permissionRepository.save(p);
        }
        return order;
    }

    /**
     * Initialise les rôles système.
     */
    private void createRole(String code, String name, String description, String scopeOrgType, String minTier, int sortOrder) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setScopeOrgType(scopeOrgType);
        role.setMinTier(minTier);
        role.setStatus("ACTIVE");
        role.setStatusDescription("Actif");
        role.setSystemRole(true);
        role.setActive(true);
        role.setSortOrder(sortOrder);
        role.setCreatedBy("SYSTEM_INIT");
        roleRepository.save(role);
    }

    /**
     * Accorde toutes les permissions à un rôle spécifique.
     */
    private void grantAllPermissionsToRole(String roleCode) {
        Role role = roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));
        List<Permission> allPerms = permissionRepository.findAll();
        for (Permission perm : allPerms) {
            if (rolePermissionRepository.findByRoleIdAndPermissionId(role.getId(), perm.getId()).isPresent()) {
                continue;
            }
            RolePermission rp = new RolePermission();
            rp.setId(UUID.randomUUID().toString());
            rp.setRoleId(role.getId());
            rp.setPermissionId(perm.getId());
            rp.setGranted(true);
            rp.setGrantedBy("SYSTEM_INIT");
            rp.setGrantedAt(Instant.now());
            rolePermissionRepository.save(rp);
        }
    }

    /**
     * Accorde des permissions spécifiques à un rôle. Crée le rôle à la volée
     * s'il n'existe pas encore (cas des rôles ajoutés dans une nouvelle
     * version mais pas encore seedés en base).
     */
    private void grantPermissionsToRole(String roleCode, String... permissionCodes) {
        Role role = roleRepository.findByCode(roleCode)
            .orElseGet(() -> {
                log.warn("Role {} absent de la base, création à la volée par l'initializer", roleCode);
                Role created = new Role();
                created.setCode(roleCode);
                created.setName(roleCode);
                created.setDescription("Role auto-seeded at startup");
                created.setSystemRole(true);
                created.setSortOrder(50);
                return roleRepository.save(created);
            });
        for (String code : permissionCodes) {
            Permission perm = permissionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + code));
            if (rolePermissionRepository.findByRoleIdAndPermissionId(role.getId(), perm.getId()).isPresent()) {
                continue;
            }
            RolePermission rp = new RolePermission();
            rp.setId(UUID.randomUUID().toString());
            rp.setRoleId(role.getId());
            rp.setPermissionId(perm.getId());
            rp.setGranted(true);
            rp.setGrantedBy("SYSTEM_INIT");
            rp.setGrantedAt(Instant.now());
            rolePermissionRepository.save(rp);
        }
    }

    /**
     * Initialise les affectations utilisateur/rôle.
     */
    private void initUserRoleAssignments() {
        if (userRoleAssignmentRepository.count() > 0) {
            log.info("User role assignments already exist, skipping");
            return;
        }

        assignRoleToUser("superadmin.cspHq", "SUPERADMIN", "CSPH");
        assignRoleToUser("admin.cspHq", "ADMIN", "CSPH");
        assignRoleToUser("superviseur.cspHq", "SUPERVISOR", "CSPH");
        assignRoleToUser("integrateur.cspHq", "INTEGRATEUR", "CSPH");
        assignRoleToUser("gest.gpl", "MARKETER", "MKT-GPL");
        assignRoleToUser("agent.gpl", "AGENT", "MKT-GPL");
        assignRoleToUser("resp.abc", "TRANSPORTER", "TRP-ABC");
        assignRoleToUser("chauffeur.abc1", "DRIVER", "TRP-ABC");
        assignRoleToUser("chauffeur.abc2", "DRIVER", "TRP-ABC");
        assignRoleToUser("resp.industries", "AGENT", "CLT-IND");

        log.info("Created {} user role assignments", userRoleAssignmentRepository.count());
    }

    /**
     * Helper pour affecter un rôle à un utilisateur.
     */
    private void assignRoleToUser(String personId, String roleCode, String organizationId) {
        Optional<Person> personOpt = personRepository.findByPersonId(personId);
        Optional<Role> roleOpt = roleRepository.findByCode(roleCode);

        if (personOpt.isPresent() && roleOpt.isPresent()) {
            Role role = roleOpt.get();

            if (!userRoleAssignmentRepository.findByPersonId(personId).isEmpty()) {
                log.debug("Role assignment for user {} already exists, skipping", personId);
                return;
            }

            UserRoleAssignment ura = new UserRoleAssignment();
            ura.setPersonId(personId);
            ura.setRoleId(role.getId());
            ura.setOrganizationId(organizationId);
            ura.setPrimary(true);
            ura.setActive(true);
            ura.setValidFrom(Instant.now());
            ura.setCreatedBy("SYSTEM_INIT");
            userRoleAssignmentRepository.save(ura);
        } else {
            log.warn("Could not assign role {} to user {}. One or both not found.", roleCode, personId);
        }
    }

    /**
     * Initialise les groupes d'utilisateurs.
     */
    private void initUserGroups() {
        if (userGroupRepository.count() > 0) {
            log.info("User groups already exist, skipping");
            return;
        }

        createUserGroup("GRP-CSPH-DIRECTION", "Direction CSPH", "Direction générale et administration du régulateur", "CSPH",
            Arrays.asList("superadmin.cspHq", "admin.cspHq"));
        createUserGroup("GRP-CSPH-SUPERVISION", "Supervision Terrain CSPH", "Équipe de supervision des opérations terrain", "CSPH",
            Arrays.asList("superviseur.cspHq"));
        createUserGroup("GRP-CSPH-TECHNIQUE", "Équipe Technique CSPH", "Intégration technique, monitoring, maintenance IoT", "CSPH",
            Arrays.asList("integrateur.cspHq"));

        log.info("Created {} user groups", userGroupRepository.count());
    }

    /**
     * Helper pour créer un groupe d'utilisateurs avec ses membres.
     */
    private void createUserGroup(String code, String name, String description, String organizationId, List<String> personIds) {
        if (userGroupRepository.existsByCode(code)) {
            log.debug("User group {} already exists, skipping", code);
            return;
        }
        UserGroup group = new UserGroup();
        group.setCode(code);
        group.setName(name);
        group.setDescription(description);
        group.setOrganizationId(organizationId);
        group.setActive(true);
        group.setSystemGroup(true);
        group.setMemberCount(0);
        group.setCreatedBy("SYSTEM_INIT");

        userGroupRepository.save(group);

        int addedCount = 0;
        for (String personId : personIds) {
            Optional<Person> personOpt = personRepository.findByPersonId(personId);
            if (personOpt.isPresent()) {
                UserGroupMembership membership = new UserGroupMembership();
                membership.setId(UUID.randomUUID().toString());
                membership.setPersonId(personId);
                membership.setGroupId(group.getId());
                membership.setActive(true);
                membership.setJoinedAt(Instant.now());
                membership.setAddedBy("SYSTEM_INIT");
                userGroupMembershipRepository.save(membership);
                addedCount++;
            }
        }

        group.setMemberCount(addedCount);
    }
}
