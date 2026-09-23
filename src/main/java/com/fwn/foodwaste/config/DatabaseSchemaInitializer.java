package com.fwn.foodwaste.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureRejectedColumnExists();
        ensureDispatchedColumnExists();
        ensureAcceptedColumnExists();
        ensureUnifiedDonorSchema();
    }

    void ensureRejectedColumnExists() {
        jdbcTemplate.execute(
                "ALTER TABLE IF EXISTS food_waste_item " +
                "ADD COLUMN IF NOT EXISTS rejected BOOLEAN NOT NULL DEFAULT FALSE"
        );
    }

    void ensureDispatchedColumnExists() {
        jdbcTemplate.execute(
                "ALTER TABLE IF EXISTS food_waste_item " +
                "ADD COLUMN IF NOT EXISTS dispatched BOOLEAN NOT NULL DEFAULT FALSE"
        );
    }

    void ensureAcceptedColumnExists() {
        jdbcTemplate.execute(
                "ALTER TABLE IF EXISTS food_waste_item " +
                "ADD COLUMN IF NOT EXISTS accepted BOOLEAN NOT NULL DEFAULT FALSE"
        );
        jdbcTemplate.execute(
            "UPDATE food_waste_item SET accepted = TRUE " +
            "WHERE processed = TRUE AND rejected = FALSE"
        );
        jdbcTemplate.execute(
            "UPDATE food_waste_item SET processed = FALSE " +
            "WHERE accepted = TRUE AND dispatched = FALSE"
        );
    }

    void ensureUnifiedDonorSchema() {
        jdbcTemplate.execute("DO $$ BEGIN " +
            "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'food_waste_item' AND column_name = 'donor_id') " +
            "AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'food_waste_item' AND column_name = 'user_id') " +
            "THEN UPDATE food_waste_item SET user_id = donor_id WHERE user_id IS NULL; " +
            "ALTER TABLE food_waste_item DROP COLUMN donor_id CASCADE; " +
            "ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'food_waste_item' AND column_name = 'donor_id') " +
            "THEN ALTER TABLE food_waste_item RENAME COLUMN donor_id TO user_id; END IF; END $$");
        jdbcTemplate.execute("DO $$ BEGIN " +
            "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'donor_collection_center' AND column_name = 'donor_id') " +
            "AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'donor_collection_center' AND column_name = 'user_id') " +
            "THEN UPDATE donor_collection_center SET user_id = donor_id WHERE user_id IS NULL; " +
            "ALTER TABLE donor_collection_center DROP COLUMN donor_id CASCADE; " +
            "ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'donor_collection_center' AND column_name = 'donor_id') " +
            "THEN ALTER TABLE donor_collection_center RENAME COLUMN donor_id TO user_id; END IF; END $$");
        jdbcTemplate.execute("DROP TABLE IF EXISTS food_donors CASCADE");
    }
}
