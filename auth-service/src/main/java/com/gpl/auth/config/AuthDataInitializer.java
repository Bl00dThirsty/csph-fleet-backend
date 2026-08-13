package com.gpl.auth.config;

import com.gpl.auth.model.AuthUser;
import com.gpl.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AuthDataInitializer
 *
 * @author GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since 10.08.2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test", "local", "default"})
public class AuthDataInitializer implements CommandLineRunner {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "Password123!";

    /*
     * run
     */
    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing test data for auth users...");

        if (authUserRepository.count() > 0) {
            log.info("Auth users already exist, skipping initialization");
            return;
        }

        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

        createAuthUser("admin.cspHq", "admin.cspHq@cspHq.cm", encodedPassword, "CSPH", "CSPH");
        createAuthUser("operateur.dla", "jean.kouam@depot-dla.cm", encodedPassword, "DEP-DLA", "DEP-DLA");
        createAuthUser("emplisseur.dla", "marie.ngono@depot-dla.cm", encodedPassword, "DEP-DLA", "DEP-DLA");
        createAuthUser("operateur.yde", "paul.mvondo@depot-yde.cm", encodedPassword, "DEP-YDE", "DEP-YDE");
        createAuthUser("gest.gpl", "alice.fouda@gpl.cm", encodedPassword, "MKT-GPL", "MKT-GPL");
        createAuthUser("chauffeur.abc1", "pierre.essomba@abctransport.cm", encodedPassword, "TRP-ABC", "TRP-ABC");
        createAuthUser("chauffeur.abc2", "samuel.ondoa@abctransport.cm", encodedPassword, "TRP-ABC", "TRP-ABC");
        createAuthUser("resp.industries", "christelle.moukoko@industries.cm", encodedPassword, "CLT-IND", "CLT-IND");
        createAuthUser("superviseur.cspHq", "robert.atangana@cspHq.cm", encodedPassword, "CSPH", "CSPH");
        createAuthUser("superadmin.cspHq", "emmanuel.mbarga@cspHq.cm", encodedPassword, "CSPH", "CSPH");
        createAuthUser("integrateur.cspHq", "fabrice.ndjock@cspHq.cm", encodedPassword, "CSPH", "CSPH");
        createAuthUser("resp.abc", "jacques.tabi@abctransport.cm", encodedPassword, "TRP-ABC", "TRP-ABC");
        createAuthUser("agent.gpl", "diane.ekotto@gpl.cm", encodedPassword, "MKT-GPL", "MKT-GPL");

        log.info("Created {} auth users with default password: {}", authUserRepository.count(), DEFAULT_PASSWORD);
        log.info("Auth test data initialization complete!");
    }

    /*
     * createAuthUser
     */
    private void createAuthUser(String username, String email, String passwordHash,
                                String organizationId, String orgId) {
        
        String personId = username; // Using username as personId for simplicity in test data

        if (authUserRepository.existsByUsername(username)) {
            log.debug("Auth user {} already exists, skipping", username);
            return;
        }

        AuthUser user = AuthUser.builder()
                .personId(personId)
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .organizationId(organizationId)
                .orgId(orgId)
                .isLocked(false)
                .failedLoginAttempts(0)
                .mustChangePassword(false)
                .twoFactorEnabled(false)
                .build();
        user.setCreatedBy("SYSTEM_INIT");

        authUserRepository.save(user);
    }
}