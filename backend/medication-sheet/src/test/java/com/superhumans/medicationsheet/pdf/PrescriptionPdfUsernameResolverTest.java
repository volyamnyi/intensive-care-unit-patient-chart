package com.superhumans.medicationsheet.pdf;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionPdfUsernameResolverTest {

    private static UUID nameUuid(String login) {
        return UUID.nameUUIDFromBytes(login.getBytes(Charset.defaultCharset()));
    }

    @Test
    void executorLoginResolvesOnlyFromPrimaryExecutor() {
        Map<UUID, String> index = PrescriptionPdfUsernameResolver.index(List.of("nurse1", "nurse2"));

        assertThat(index).containsEntry(nameUuid("nurse1"), "nurse1")
                .containsEntry(nameUuid("nurse2"), "nurse2");
        // secondPersonId is not part of this model and cannot become a printed login.
        assertThat(index).doesNotContainKey(UUID.randomUUID());
    }

    @Test
    void collisionAndBlankAreUnresolved() {
        UUID id = UUID.randomUUID();
        Map<UUID, String> index = PrescriptionPdfUsernameResolver.index(List.of("", "  "));

        assertThat(index.getOrDefault(id, "")).isEmpty();
    }
}
