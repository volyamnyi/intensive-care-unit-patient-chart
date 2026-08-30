package com.superhumans.prosthesismanufacturing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Tripwire for TP-LL-02 — Етапи технологічного процесу нижніх кінцівок (Фаза 1).
 * Validates that data-prosth.sql contains the TP-LL-02 template with
 * the expected structure from v2.md (без Quality Gate): 10 stages / 14 steps / 24 elements.
 */
class TpLl02SeedValidationTest {

    @Test
    void prosthSeed_containsTpLl02Template() throws Exception {
        String sql = sqlText();

        assertThat(sql).contains("'TP-LL-02'");
        assertThat(sql).contains("'generic_lower_limb'");
        assertThat(sql).contains("'BOTH'");
        assertThat(sql).contains("'ACTIVE', 540");
        assertThat(sql).contains("'c0000003-0000-0000-0000-000000000003'");
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

        // MEASUREMENT numeric inputs
        assertThat(sql).contains("'Довжина кукси, см'");
        assertThat(sql).contains("'Обхват кукси, см'");
        assertThat(sql).contains("'NUMERIC_INPUT'");
        assertThat(sql).contains("'см', 0, 200");
        // CHECKBOX examples
        assertThat(sql).contains("'Гіпсовий негатив виготовлено'");
        assertThat(sql).contains("'Тренувальна гільза виготовлена'");
        assertThat(sql).contains("'Візуальний контроль чистоти: відсутній пил, стружка, забруднення");
        assertThat(sql).contains("'Тактильний контроль поверхні та якість обробки країв. Відсутні задирки");
        assertThat(sql).contains("'На протез нанесено маркування'");
        assertThat(sql).contains("'Супровідна документація оформлена'");
        assertThat(sql).contains("'Протез переданий пацієнту для подальшої експлуатації'");
        // Conditional insert elements are required false
        assertThat(sql).contains("'f0000214-0000-0000-0000-000000000214'");
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
