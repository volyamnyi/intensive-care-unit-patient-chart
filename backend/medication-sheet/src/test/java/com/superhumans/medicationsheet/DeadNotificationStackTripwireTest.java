package com.superhumans.medicationsheet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tripwire: the dead notification/Telegram stack (NotificationService,
 * LogNotificationService, med-module EmailService/LogEmailService,
 * TelegramSubscription) was fully removed (#408) — its sole caller
 * (PrescriptionSchedulerService) was deleted with the legacy importer
 * (commit b0730f28). This test fails if any reference sneaks back.
 */
class DeadNotificationStackTripwireTest {

    @Test
    void medicationModule_hasNoNotificationOrTelegramReferences() throws IOException {
        Path moduleRoot = Path.of("src", "main", "java");
        assertThat(Files.exists(moduleRoot)).isTrue();

        try (Stream<Path> sources = Files.walk(moduleRoot)) {
            var violations = sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(this::readFile)
                    .filter(content -> content.contains("TelegramSubscription")
                            || content.contains("NotificationService")
                            || content.contains("LogEmailService"))
                    .toList();
            assertThat(violations)
                    .as("the dead notification/Telegram stack must not reappear "
                            + "in medication-sheet main sources")
                    .isEmpty();
        }
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + path, exception);
        }
    }
}
