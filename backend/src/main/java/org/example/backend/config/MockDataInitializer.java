package org.example.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Legacy seeder that populated the marketplace with 20 demo factories and
 * default inventory rows on an empty database.
 *
 * <p><b>DISABLED.</b> The marketplace must reflect real data only. Demo
 * factories and auto-generated inventory rows are no longer inserted.
 *
 * <p>The class is kept as a no-op {@link CommandLineRunner} so that any
 * startup ordering dependencies elsewhere — e.g. {@code @DependsOn}
 * annotations — remain valid.
 */
@Component
@DependsOn("defaultAdminInitializer")
public class MockDataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // No-op. Demo data seeding is disabled.
    }
}
