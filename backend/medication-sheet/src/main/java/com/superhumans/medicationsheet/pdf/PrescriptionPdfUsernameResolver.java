package com.superhumans.medicationsheet.pdf;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Inverse of the primary executor key written by PrescriptionExecutionService. */
final class PrescriptionPdfUsernameResolver {
    private PrescriptionPdfUsernameResolver() { }

    static Map<UUID, String> index(List<String> logins) {
        Map<UUID, String> result = new HashMap<>();
        for (String login : logins) {
            if (PrescriptionPdfSnapshot.present(login)) {
                UUID id = UUID.nameUUIDFromBytes(login.getBytes(Charset.defaultCharset()));
                // A collision cannot establish authorship. Keep the ambiguity even on a third match.
                result.merge(id, login, (a, b) -> a.equals(b) ? a : "");
            }
        }
        return Map.copyOf(result);
    }
}
