package com.superhumans.mis;

import com.superhumans.mis.dto.DictionaryItemDTO;
import java.util.List;

/**
 * Hardcoded clinical dictionaries shared by both MIS implementations.
 * <p>
 * These dictionaries are application-level constants (order categories, note
 * types, consciousness states) — they do not come from the MIS API, so both
 * implementations must serve byte-identical content. Centralized here and
 * pinned by parity tests in {@code MisParityTest}.
 */
public final class MisDictionaries {

    private MisDictionaries() {
    }

    public static List<DictionaryItemDTO> orderCategories() {
        return List.of(
                new DictionaryItemDTO("MEDICATION", "Медикаменти"),
                new DictionaryItemDTO("INFUSION", "Інфузії"),
                new DictionaryItemDTO("LAB", "Аналізи"),
                new DictionaryItemDTO("PROCEDURE", "Маніпуляції"),
                new DictionaryItemDTO("VENTILATION", "ШВЛ"),
                new DictionaryItemDTO("NUTRITION", "Харчування"),
                new DictionaryItemDTO("OTHER", "Інші"));
    }

    public static List<DictionaryItemDTO> noteTypes() {
        return List.of(
                new DictionaryItemDTO("DOCTOR_NOTE", "Лікарський запис"),
                new DictionaryItemDTO("NURSE_NOTE", "Сестринський запис"),
                new DictionaryItemDTO("SHIFT_REPORT", "Звіт за зміну"));
    }

    public static List<DictionaryItemDTO> consciousness() {
        return List.of(
                new DictionaryItemDTO("CLEAR", "Ясна"),
                new DictionaryItemDTO("STUPOR", "Ступор"),
                new DictionaryItemDTO("SOPOR", "Сопор"),
                new DictionaryItemDTO("COMA", "Кома"),
                new DictionaryItemDTO("SEDATED", "Седація"));
    }
}
