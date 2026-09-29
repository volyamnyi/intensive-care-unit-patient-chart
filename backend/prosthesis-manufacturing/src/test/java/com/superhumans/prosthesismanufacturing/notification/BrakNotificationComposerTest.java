package com.superhumans.prosthesismanufacturing.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BrakNotificationComposerTest {

    private final BrakNotificationComposer composer = new BrakNotificationComposer();

    private static final UUID INSTANCE_ID = UUID.fromString("a1b2c3d4-0000-0000-0000-000000000001");
    private static final UUID BRANCH_ID = UUID.fromString("b1b2c3d4-0000-0000-0000-000000000002");
    private static final UUID ORDER_ID = UUID.fromString("c1c2c3d4-0000-0000-0000-000000000003");
    private static final UUID STAGE_ID = UUID.fromString("d0000017-0000-0000-0000-000000000017");
    private static final UUID STEP_ID = UUID.fromString("e0000028-0000-0000-0000-000000000028");

    @Test
    void buildSubject_containsBrakOrderNumberAndShortId() {
        BrakNotificationData data = fullData();

        String subject = composer.buildSubject(data);

        assertThat(subject).contains("брак");
        assertThat(subject).contains("ПВ-26-0413");
        assertThat(subject).contains("a1b2c3d4");
        assertThat(subject).doesNotContain(INSTANCE_ID.toString());
    }

    @Test
    void buildBody_containsAllElevenBlocks() {
        BrakNotificationData data = fullData();

        String body = composer.buildBody(data);

        assertThat(body).contains("Підтверджено брак тренувального протеза (етап 6).");
        assertThat(body).contains("Пацієнт (ID): 900001");
        assertThat(body).contains("Замовлення: ПВ-26-0413 (ID: " + ORDER_ID + ")");
        assertThat(body).contains("Процес (ID): " + INSTANCE_ID);
        assertThat(body).contains("Нова гілка (ID): " + BRANCH_ID);
        assertThat(body).contains("Шаблон: TP-LL-02");
        assertThat(body).contains("Етап: Примірювання та коректування тренувального протеза");
        assertThat(body).contains("Крок: Примірювання та коректування тренувального протеза");
        assertThat(body).contains("Дата і час підтвердження: 28.09.2026 11:20");
        assertThat(body).contains("Підтвердив: Олег Романюк (логін: prosthetist1)");
        assertThat(body).contains("Неправильне розташування м’яких тканин у гільзі: так");
        assertThat(body).contains("Наявні больові відчуття і дискомфорт при посадці: ні");
        assertThat(body).contains("Коментар: Гільза тисне в ділянці кукси.");
        assertThat(body).contains("Повернено на етап: Виготовлення тренувальної гільзи");
        assertThat(body).contains(
                "Процес у системі: https://chart.example.invalid/prosthetics/process/" + INSTANCE_ID);
    }

    @Test
    void buildBody_missingOptionalValues_renderedAsDash() {
        BrakNotificationData data = new BrakNotificationData(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, false, false, null, null, null, 1L, List.of());

        String body = composer.buildBody(data);

        assertThat(body).contains("Пацієнт (ID): —");
        assertThat(body).contains("Коментар: Без коментаря");
        assertThat(body).contains("Дата і час підтвердження: —");
        assertThat(body).doesNotContain("Процес у системі:");
    }

    @Test
    void buildBody_blankFrontendUrl_omitsSystemLink() {
        BrakNotificationData data = new BrakNotificationData(
                "900001", "ПВ-26-0413", ORDER_ID, INSTANCE_ID, BRANCH_ID, "TP-LL-02",
                "Етап 6", "Крок 1", STAGE_ID, STEP_ID,
                LocalDateTime.of(2026, 9, 28, 11, 20), "Олег Романюк", "prosthetist1",
                false, false, null, "Виготовлення тренувальної гільзи", "   ",
                1L, List.of());

        String body = composer.buildBody(data);

        assertThat(body).doesNotContain("Процес у системі:");
    }

    @Test
    void buildBody_containsNoForbiddenPatientData() {
        String body = composer.buildBody(fullData());

        assertThat(body).doesNotContain("Сніжко");
        assertThat(body).doesNotContain("+380");
        assertThat(body).doesNotContain("snizhko");
    }

    private BrakNotificationData fullData() {
        return new BrakNotificationData(
                "900001",
                "ПВ-26-0413",
                ORDER_ID,
                INSTANCE_ID,
                BRANCH_ID,
                "TP-LL-02",
                "Примірювання та коректування тренувального протеза",
                "Примірювання та коректування тренувального протеза",
                STAGE_ID,
                STEP_ID,
                LocalDateTime.of(2026, 9, 28, 11, 20),
                "Олег Романюк",
                "prosthetist1",
                true,
                false,
                "Гільза тисне в ділянці кукси.",
                "Виготовлення тренувальної гільзи",
                "https://chart.example.invalid",
                1L,
                List.of());
    }

    @ParameterizedTest
    @ValueSource(longs = {3, 4})
    void buildThresholdSubject_containsNumberOrderAndShortId(long count) {
        BrakNotificationData data = thresholdData(count);

        String subject = composer.buildThresholdSubject(data);

        assertThat(subject).contains("Брак №" + count);
        assertThat(subject).contains("ПВ-26-0413");
        assertThat(subject).contains("a1b2c3d4");
        assertThat(subject).doesNotContain(INSTANCE_ID.toString());
    }

    @Test
    void buildThresholdBody_containsCountAndNumberedHistory() {
        BrakNotificationData data = thresholdData(3L);

        String body = composer.buildThresholdBody(data);

        assertThat(body).contains("Перевищено поріг браків у замовленні (брак №3).");
        assertThat(body).contains("Кількість браків у замовленні: 3");
        assertThat(body).contains("1. Примірювання та коректування тренувального протеза"
                + " — 26.09.2026 10:05 — Олег Романюк");
        assertThat(body).contains("2. Примірювання та коректування тренувального протеза"
                + " — 27.09.2026 14:40 — Ірина Шевчук");
        assertThat(body).contains("3. Примірювання та коректування постійного протеза"
                + " — 28.09.2026 11:20 — Олег Романюк");
        assertThat(body).contains("Пацієнт (ID): 900001");
        assertThat(body).contains("Замовлення: ПВ-26-0413 (ID: " + ORDER_ID + ")");
        assertThat(body).contains("Останній брак — етап: Примірювання та коректування постійного протеза");
        assertThat(body).contains(
                "Процес у системі: https://chart.example.invalid/prosthetics/process/" + INSTANCE_ID);
    }

    @Test
    void buildThresholdBody_emptyHistory_rendersDash() {
        BrakNotificationData data = new BrakNotificationData(
                "900001", "ПВ-26-0413", ORDER_ID, INSTANCE_ID, BRANCH_ID, "TP-LL-02",
                "Етап 6", "Крок 1", STAGE_ID, STEP_ID,
                LocalDateTime.of(2026, 9, 28, 11, 20), "Олег Романюк", "prosthetist1",
                true, false, null, "Виготовлення тренувальної гільзи",
                "https://chart.example.invalid", 3L, List.of());

        String body = composer.buildThresholdBody(data);

        assertThat(body).contains("Кількість браків у замовленні: 3");
        assertThat(body).contains("Історія браків замовлення:\n—");
        assertThat(body).contains("Коментар: Без коментаря");
    }

    private BrakNotificationData thresholdData(long count) {
        return new BrakNotificationData(
                "900001",
                "ПВ-26-0413",
                ORDER_ID,
                INSTANCE_ID,
                BRANCH_ID,
                "TP-LL-02",
                "Примірювання та коректування постійного протеза",
                "Примірювання та коректування постійного протеза",
                UUID.fromString("d0000020-0000-0000-0000-000000000020"),
                UUID.fromString("e0000032-0000-0000-0000-000000000032"),
                LocalDateTime.of(2026, 9, 28, 11, 20),
                "Олег Романюк",
                "prosthetist1",
                true,
                false,
                "Гільза тисне в ділянці кукси.",
                "Виготовлення тренувальної гільзи",
                "https://chart.example.invalid",
                count,
                List.of(
                        new BrakHistoryEntry(
                                "Примірювання та коректування тренувального протеза",
                                "Примірювання та коректування тренувального протеза",
                                LocalDateTime.of(2026, 9, 26, 10, 5),
                                "Олег Романюк",
                                "Виготовлення тренувальної гільзи"),
                        new BrakHistoryEntry(
                                "Примірювання та коректування тренувального протеза",
                                "Примірювання та коректування тренувального протеза",
                                LocalDateTime.of(2026, 9, 27, 14, 40),
                                "Ірина Шевчук",
                                "Виготовлення гіпсового негатива"),
                        new BrakHistoryEntry(
                                "Примірювання та коректування постійного протеза",
                                "Примірювання та коректування постійного протеза",
                                LocalDateTime.of(2026, 9, 28, 11, 20),
                                "Олег Романюк",
                                "Виготовлення тренувальної гільзи")));
    }
}
