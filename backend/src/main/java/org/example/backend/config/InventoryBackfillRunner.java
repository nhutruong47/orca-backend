package org.example.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Backfill runner for legacy default inventory rows.
 *
 * <p><b>DISABLED.</b> Inventory must not be auto-seeded on application boot.
 * Teams start empty and owners add products through the import / create flow.
 *
 * <p>The class is kept (as a no-op {@link CommandLineRunner}) so that any
 * startup ordering dependencies — e.g. {@code @DependsOn("mockDataInitializer")}
 * elsewhere — remain valid. To re-enable legacy seeding, restore the original
 * body from version control.
 */
@Component
public class InventoryBackfillRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // No-op. Default inventory seeding is disabled.
    }
}
