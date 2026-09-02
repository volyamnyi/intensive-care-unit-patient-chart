package com.superhumans.prosthesismanufacturing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Tripwire for TP-LL-02 — Етапи технологічного процесу нижніх кінцівок (Фаза 1 + Lower Limb Measurement + Phase 3 soft liner).
 * Validates that data-prosth.sql contains the TP-LL-02 template with
 * the expected structure (без Quality Gate): 10 stages / 14 steps / 70 elements
 * (24 legacy + 45 lower-limb measurement form — adapted from measurement-master + 1 Phase 3 soft-liner checkbox).
 */
class TpLl02SeedValidationTest {

    @Test
    void prosthSeed_containsTpLl02Template() throws Exception {
        String sql = sqlText();

        assertThat(sql).contains("'TP-LL-02'");
        assertThat(sql).contains("'generic_lower_limb'");
        assertThat(sql).contains("'ACTIVE', 540");
        assertThat(sql).contains("'c0000003-0000-0000-0000-000000000003'");
        assertThat(sql).contains("'LOWER_LIMB', 'generic_lower_limb', NULL, 'ACTIVE'");
    }

    @Test
    void prosthSeed_containsTpLl02Stages() throws Exception {
        String sql = sqlText();

        // 10 stages for TP-LL-02 (order_index 1..10)
        assertThat(sql).contains("'Виготовлення гіпсового негатива'");
        assertThat(sql).contains("'Виготовлення гіпсової моделі кукси'");
        assertThat(sql).contains("'Виготовлення тренувальної гільзи'");
        assertThat(sql).contains("'Примірка тренувальної гільзи'");
        assertThat(sql).contains("'Складання тренувального протеза'");
        assertThat(sql).contains("'Примірювання та коректування тренувального протеза'");
        assertThat(sql).contains("'Виготовлення пом''якшуючого вкладиша та постійної гільзи'");
        assertThat(sql).contains("'Складання постійного протеза'");
        assertThat(sql).contains("'Примірювання та коректування постійного протеза'");
        assertThat(sql).contains("'Видача протеза'");

        // Stage IDs for TP-LL-02
        assertThat(sql).contains("'d0000012-0000-0000-0000-000000000012'");
        assertThat(sql).contains("'d0000021-0000-0000-0000-000000000021'");
    }

    @Test
    void prosthSeed_containsTpLl02Steps() throws Exception {
        String sql = sqlText();

        // 14 steps
        assertThat(sql).contains("'Зняття та внесення об''ємних розмірів'");
        assertThat(sql).contains("'MEASUREMENT'");
        assertThat(sql).contains("'e0000020-0000-0000-0000-000000000020'");
        assertThat(sql).contains("'e0000033-0000-0000-0000-000000000033'");
        // Conditional insert step must be mandatory false
        assertThat(sql).contains("'Виготовлення пом''якшуючого вкладиша'");
        // Last step must be allow_backward false
        assertThat(sql).contains("'Видача протеза'");
    }

    @Test
    void prosthSeed_containsTpLl02Elements() throws Exception {
        String sql = sqlText();

        // MEASUREMENT numeric inputs (legacy)
        assertThat(sql).contains("'Довжина кукси, см'");
        assertThat(sql).contains("'Обхват кукси, см'");
        assertThat(sql).contains("'NUMERIC_INPUT'");
        assertThat(sql).contains("'см', 0, 200");
        // Lower-limb measurement form (adapted from measurement-master, 45 elements)
        assertThat(sql).contains("'Бланк замірів №'");
        assertThat(sql).contains("'П.І.Б'");
        assertThat(sql).contains("'Стегно, R'");
        assertThat(sql).contains("'Стегно, L'");
        assertThat(sql).contains("'Обхват гомілки'");
        assertThat(sql).contains("'Обхват щиколотки'");
        assertThat(sql).contains("'Коліно, R'");
        assertThat(sql).contains("'Таз R, рівень 2,5'");
        assertThat(sql).contains("'Таз L, рівень 15'");
        assertThat(sql).contains("'Висота каблука'");
        assertThat(sql).contains("'Розмір стопи'");
        assertThat(sql).contains("'Комплектуючі'");
        assertThat(sql).contains("'f0000300-0000-0000-0000-000000000300'");
        assertThat(sql).contains("'f0000344-0000-0000-0000-000000000344'");
        // DATE_PICKER and DROPDOWN for lower limb header
        assertThat(sql).contains("'DATE_PICKER'");
        assertThat(sql).contains("'DROPDOWN'");
        assertThat(sql).contains("'Чоловіча'");
        // CHECKBOX examples
        assertThat(sql).contains("'Гіпсовий негатив виготовлено'");
        assertThat(sql).contains("'Тренувальна гільза виготовлена'");
        assertThat(sql).contains("'Візуальний контроль чистоти: відсутній пил, стружка, забруднення");
        assertThat(sql).contains("'Тактильний контроль поверхні та якість обробки країв. Відсутні задирки");
        assertThat(sql).contains("'На протез нанесено маркування'");
        assertThat(sql).contains("'Супровідна документація оформлена'");
        assertThat(sql).contains("'Протез переданий пацієнту для подальшої експлуатації'");
        // Conditional insert elements are required false (Phase 3: third checkbox)
        assertThat(sql).contains("'f0000214-0000-0000-0000-000000000214'");
        assertThat(sql).contains("'f0000240-0000-0000-0000-000000000240'");
        assertThat(sql).contains("'Пом''якшуючий вкладиш не потрібен'");
    }

    @Test
    void prosthSeed_doesNotContainQualityGateForTpLl02() throws Exception {
        String sql = sqlText();

        // After removal, TP-LL-02 must not have quality_gates / rework_loops entries
        long gateInserts = sql.lines()
                .filter(l -> l.contains("prosthetics_quality_gates") && l.contains("c0000003"))
                .count();
        assertThat(gateInserts).isZero();

        long reworkInserts = sql.lines()
                .filter(l -> l.contains("prosthetics_rework_loops") && l.contains("c0000003"))
                .count();
        assertThat(reworkInserts).isZero();
    }

    @Test
    void prosthSeed_templateIsActiveAndIndexed() throws Exception {
        String sql = sqlText();

        // Verify ON CONFLICT handling
        assertThat(sql).contains("ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description");
        assertThat(sql).contains("'TP-LL-02', 'Етапи технологічного процесу нижніх кінцівок', 1, 'LOWER_LIMB'");
    }

    private static String sqlText() throws Exception {
        return new String(new ClassPathResource("data-prosth.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }
}
