package com.gpl.fleet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TimescaleDbInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TimescaleDbInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public TimescaleDbInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking and initializing TimescaleDB and PostGIS extensions...");
        
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis;");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS timescaledb;");
            log.info("Extensions postgis and timescaledb are ensured.");

            // Create hypertable if it doesn't exist
            // Using if_not_exists=true to avoid errors on restart
            jdbcTemplate.execute("SELECT create_hypertable('vehicle_telemetry', 'timestamp', if_not_exists => TRUE);");
            log.info("Hypertable vehicle_telemetry is configured.");
        } catch (Exception e) {
            log.warn("Failed to initialize TimescaleDB/PostGIS or create hypertable. It might already be initialized or the DB might not support it yet: {}", e.getMessage());
        }
    }
}
