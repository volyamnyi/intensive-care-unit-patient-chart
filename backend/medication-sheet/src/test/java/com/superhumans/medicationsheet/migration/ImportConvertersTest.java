package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.migration.ImportConverters.BloodPressure;
import com.superhumans.medicationsheet.migration.ImportConverters.Converted;
import com.superhumans.medicationsheet.migration.ImportConverters.ParentCandidate;
import com.superhumans.medicationsheet.migration.ImportConverters.ParentRef;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ImportConvertersTest {

    @Test
    void documentName_hasNoLegacySuffix() {
        assertThat(ImportConverters.LIST_DOCUMENT_NAME).doesNotContain("(");
        assertThat(ImportConverters.shellDocumentName("1518"))
                .startsWith(ImportConverters.LIST_DOCUMENT_NAME)
                .endsWith("1518)");
    }

    @Test
    void stripRegime_handlesAllShapes() {
        assertThat(ImportConverters.stripRegime(null)).isNull();
        assertThat(ImportConverters.stripRegime("")).isNull();
        assertThat(ImportConverters.stripRegime("   ")).isNull();
        assertThat(ImportConverters.stripRegime(ImportConverters.REGIME_PREFIX)).isNull();
        assertThat(ImportConverters.stripRegime(ImportConverters.REGIME_PREFIX + "x")).isEqualTo("x");
        assertThat(ImportConverters.stripRegime("plain")).isEqualTo("plain");
    }

    @Test
    void normalizeLogin_acceptsLoginsOnly() {
        assertThat(ImportConverters.normalizeLogin("m.zaplatynska"))
                .isEqualTo(Optional.of("m.zaplatynska"));
        assertThat(ImportConverters.normalizeLogin("  m.zaplatynska "))
                .isEqualTo(Optional.of("m.zaplatynska"));
        assertThat(ImportConverters.normalizeLogin("SUPERHUMANS\\m.zaplatynska"))
                .isEqualTo(Optional.of("m.zaplatynska"));
        assertThat(ImportConverters.normalizeLogin("superhumans\\a.b"))
                .isEqualTo(Optional.of("a.b"));
        assertThat(ImportConverters.normalizeLogin(null)).isEmpty();
        assertThat(ImportConverters.normalizeLogin("5")).isEmpty();
        assertThat(ImportConverters.normalizeLogin("room904")).isEmpty();
        assertThat(ImportConverters.normalizeLogin("a.b\nc.d")).isEmpty();
    }

    @Test
    void nameUuid_isStableAndLoginBased() {
        UUID first = ImportConverters.nameUuid("m.zaplatynska");
        assertThat(first).isEqualTo(ImportConverters.nameUuid("m.zaplatynska"));
        assertThat(first).isNotEqualTo(ImportConverters.nameUuid("r.lishchuk"));
        assertThat(first).isEqualTo(UUID.nameUUIDFromBytes(
                "m.zaplatynska".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void convertDoctorName_mapsSingleLogin() {
        Converted<String> result = ImportConverters.convertDoctorName("m.zaplatynska");
        assertThat(result.present()).isTrue();
        assertThat(result.value()).isEqualTo(
                ImportConverters.nameUuid("m.zaplatynska").toString());
        assertThat(ImportConverters.convertDoctorName("").quarantined()).isFalse();
        assertThat(ImportConverters.convertDoctorName(null).present()).isFalse();
        assertThat(ImportConverters.convertDoctorName("5").quarantined()).isTrue();
    }

    @Test
    void convertNurseName_handlesPairs() {
        Converted<String> single = ImportConverters.convertNurseName("l.baran");
        assertThat(single.value())
                .isEqualTo(ImportConverters.nameUuid("l.baran").toString());
        Converted<String> pair = ImportConverters.convertNurseName("n.zakopets\ns.babii");
        assertThat(pair.value()).isEqualTo("n.zakopets/2P:s.babii");
        assertThat(ImportConverters.convertNurseName("room904").quarantined()).isTrue();
        assertThat(ImportConverters.convertNurseName("a.b\n").quarantined()).isTrue();
        assertThat(ImportConverters.convertNurseName("a.b\nc.d\ne.f").quarantined()).isTrue();
        assertThat(ImportConverters.convertNurseName("  ").present()).isFalse();
    }

    @Test
    void parseTemperature_appliesRange() {
        assertThat(ImportConverters.parseTemperature("36.6").value()).isEqualTo(36.6);
        assertThat(ImportConverters.parseTemperature("36,6").value()).isEqualTo(36.6);
        assertThat(ImportConverters.parseTemperature("34.0").present()).isTrue();
        assertThat(ImportConverters.parseTemperature("42.0").present()).isTrue();
        assertThat(ImportConverters.parseTemperature("1").quarantined()).isTrue();
        assertThat(ImportConverters.parseTemperature("110/70").quarantined()).isTrue();
        assertThat(ImportConverters.parseTemperature("33.9").quarantined()).isTrue();
        assertThat(ImportConverters.parseTemperature("").present()).isFalse();
    }

    @Test
    void splitBloodPressure_splitsAndRanges() {
        BloodPressure ok = ImportConverters.splitBloodPressure("120/80").value();
        assertThat(ok.systolic()).isEqualTo(120);
        assertThat(ok.diastolic()).isEqualTo(80);
        assertThat(ImportConverters.splitBloodPressure("120\\80").value())
                .isEqualTo(new BloodPressure(120, 80));
        assertThat(ImportConverters.splitBloodPressure("120 / 80").present()).isTrue();
        assertThat(ImportConverters.splitBloodPressure("").present()).isFalse();
        assertThat(ImportConverters.splitBloodPressure("-").present()).isFalse();
        assertThat(ImportConverters.splitBloodPressure("120.80").quarantined()).isTrue();
        assertThat(ImportConverters.splitBloodPressure("120").quarantined()).isTrue();
        assertThat(ImportConverters.splitBloodPressure("12/80").quarantined()).isTrue();
        assertThat(ImportConverters.splitBloodPressure("125/785").quarantined()).isTrue();
        assertThat(ImportConverters.splitBloodPressure("50/30").present()).isTrue();
    }

    @Test
    void parseBoundedInt_appliesBounds() {
        assertThat(ImportConverters.parseBoundedInt("0", 0, 10).value()).isZero();
        assertThat(ImportConverters.parseBoundedInt("10", 0, 10).value()).isEqualTo(10);
        assertThat(ImportConverters.parseBoundedInt("99", 0, 10).quarantined()).isTrue();
        assertThat(ImportConverters.parseBoundedInt("8-4", 0, 10).quarantined()).isTrue();
        assertThat(ImportConverters.parseBoundedInt("", 0, 10).present()).isFalse();
        assertThat(ImportConverters.parseBoundedInt("98", 50, 100).value()).isEqualTo(98);
    }

    @Test
    void parseDayDate_acceptsIsoOnly() {
        assertThat(ImportConverters.parseDayDate("2025-06-24").value())
                .isEqualTo(LocalDate.of(2025, 6, 24));
        assertThat(ImportConverters.parseDayDate("2026-10-22").present()).isTrue();
        assertThat(ImportConverters.parseDayDate("24.06.2025").quarantined()).isTrue();
        assertThat(ImportConverters.parseDayDate("").present()).isFalse();
    }

    @Test
    void parseCreationTimestamp_parsesLegacyFormat() {
        assertThat(ImportConverters.parseCreationTimestamp("2025-06-24 12:55:33.827").value())
                .isEqualTo(LocalDateTime.of(2025, 6, 24, 12, 55, 33, 827_000_000));
        assertThat(ImportConverters.parseCreationTimestamp("nope").quarantined()).isTrue();
        assertThat(ImportConverters.parseCreationTimestamp("").present()).isFalse();
    }

    @Test
    void resolveParent_picksLatestNotAfter() {
        LocalDateTime first = LocalDateTime.of(2025, 6, 1, 0, 0);
        LocalDateTime second = LocalDateTime.of(2025, 7, 1, 0, 0);
        LocalDateTime third = LocalDateTime.of(2025, 8, 1, 0, 0);
        List<ParentCandidate> candidates = List.of(
                new ParentCandidate(third, "3"),
                new ParentCandidate(first, "1"),
                new ParentCandidate(second, "2"));
        ParentRef at = ImportConverters.resolveParent(
                LocalDateTime.of(2025, 7, 15, 0, 0), candidates);
        assertThat(at.parentRef()).isEqualTo("2");
        assertThat(at.fallback()).isFalse();
    }

    @Test
    void resolveParent_fallsBackToEarliest() {
        List<ParentCandidate> candidates = List.of(
                new ParentCandidate(LocalDateTime.of(2025, 8, 1, 0, 0), "8"),
                new ParentCandidate(LocalDateTime.of(2025, 9, 1, 0, 0), "9"));
        ParentRef result = ImportConverters.resolveParent(
                LocalDateTime.of(2025, 2, 1, 0, 0), candidates);
        assertThat(result.parentRef()).isEqualTo("8");
        assertThat(result.fallback()).isTrue();
    }

    @Test
    void resolveParent_breaksTiesByGreaterRef() {
        LocalDateTime same = LocalDateTime.of(2025, 6, 1, 0, 0);
        ParentRef result = ImportConverters.resolveParent(LocalDateTime.of(2025, 7, 1, 0, 0),
                List.of(new ParentCandidate(same, "99"), new ParentCandidate(same, "100")));
        assertThat(result.parentRef()).isEqualTo("100");
        assertThat(ImportConverters.resolveParent(
            LocalDateTime.of(2025, 7, 1, 0, 0), List.of())).isNull();
    }

    @Test
    void isEmptyJsonValue_coversAllMarkers() {
        assertThat(ImportConverters.isEmptyJsonValue(null)).isTrue();
        assertThat(ImportConverters.isEmptyJsonValue("")).isTrue();
        assertThat(ImportConverters.isEmptyJsonValue("NULL")).isTrue();
        assertThat(ImportConverters.isEmptyJsonValue("null")).isTrue();
        assertThat(ImportConverters.isEmptyJsonValue("[]")).isTrue();
        assertThat(ImportConverters.isEmptyJsonValue("[ ]")).isTrue();
        assertThat(ImportConverters.isEmptyJsonValue("{}")).isFalse();
        assertThat(ImportConverters.isEmptyJsonValue("[1]")).isFalse();
    }
}
