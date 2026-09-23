package com.fwn.foodwaste.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DatabaseSchemaInitializerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseSchemaInitializer initializer;

    @Test
    void shouldEnsureRejectedColumnExists() {
        initializer.ensureRejectedColumnExists();

        verify(jdbcTemplate).execute(contains("ADD COLUMN IF NOT EXISTS rejected"));
    }
}
