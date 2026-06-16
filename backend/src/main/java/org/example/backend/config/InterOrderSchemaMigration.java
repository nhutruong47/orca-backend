package org.example.backend.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class InterOrderSchemaMigration {

    @Bean
    ApplicationRunner relaxInterOrderBuyerTeamColumn(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE teams ALTER COLUMN factory_image_url TYPE TEXT USING factory_image_url::text");
            } catch (Exception ignored) {
                // Fresh schemas already use TEXT; this only repairs older PostgreSQL deployments.
            }

            try {
                jdbcTemplate.execute(
                        "ALTER TABLE teams ALTER COLUMN factory_images TYPE TEXT USING factory_images::text");
            } catch (Exception ignored) {
                // Fresh schemas already use TEXT; this only repairs older PostgreSQL deployments.
            }

            try {
                jdbcTemplate.execute("ALTER TABLE inter_group_orders ADD COLUMN IF NOT EXISTS buyer_user_id UUID");
            } catch (Exception ignored) {
                // Hibernate creates the column in fresh databases; this is only for older local H2 files.
            }

            try {
                jdbcTemplate.execute("ALTER TABLE inter_group_orders ALTER COLUMN buyer_team_id DROP NOT NULL");
            } catch (Exception ignored) {
                // Some databases already have this nullable, and non-H2 dialects may use different syntax.
            }
        };
    }
}
