package com.superhumans.prosthesismanufacturing.notification;

import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Builds the subject and plain-text body of the confirmed-brak email
 * (stage 6 of TP-LL-02, training prosthesis).
 *
 * <p>Pure formatting logic: no database access, no mail sending.
 * Missing optional values render as {@code «—»}; an absent note renders
 * as {@code «Без коментаря»}. Patient data is limited to the MIS patient id —
 * no names, contacts or clinical payloads are ever included.
 */
@Component
public class BrakNotificationComposer {

    static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String MISSING = "—";
    private static final String NO_COMMENT = "Без коментаря";
    private static final String YES = "так";
    private static final String NO = "ні";

    // Checkbox labels copied verbatim from the brak dialog in WizardScreen.tsx
    // (\u2019 is the Ukrainian apostrophe used there).
    private static final String SOFT_TISSUE_LABEL =
            "Неправильне розташування м\u2019яких тканин у гільзі";
    private static final String PAIN_LABEL =
            "Наявні больові відчуття і дискомфорт при посадці";

    /**
     * Builds the email subject.
     * Format: {@code Підтверджено брак тренувального протеза — замовлення {orderNumber}
     * (процес {shortId})}, where {@code shortId} is the first 8 characters
     * of the original instance id.
     */
    public String buildSubject(BrakNotificationData data) {
        return "Підтверджено брак тренувального протеза — замовлення "
                + textOrMissing(data.orderNumber())
                + " (процес "
                + shortId(data.originalInstanceId())
                + ")";
    }

    /**
     * Builds the plain-text email body: 11 blocks (event type, patient, order,
     * process, stage, date/time, confirmer, brak reasons, comment, return stage,
     * system link). The system-link block is omitted when no frontend URL
     * is configured.
     */
    public String buildBody(BrakNotificationData data) {
        StringBuilder body = new StringBuilder();
        block(body, "Підтверджено брак тренувального протеза (етап 6).");
        block(body, "Пацієнт (ID): " + textOrMissing(data.patientId()));
        block(body, "Замовлення: " + textOrMissing(data.orderNumber())
                + " (ID: " + textOrMissing(data.orderId()) + ")");
        block(body, "Процес (ID): " + textOrMissing(data.originalInstanceId())
                + "\nНова гілка (ID): " + textOrMissing(data.newInstanceId())
                + "\nШаблон: " + textOrMissing(data.templateName()));
        block(body, "Етап: " + textOrMissing(data.stageLabel())
                + "\nКрок: " + textOrMissing(data.stepLabel())
                + "\n(Технічні ID — етап: " + textOrMissing(data.stageId())
                + ", крок: " + textOrMissing(data.stepId()) + ")");
        block(body, "Дата і час підтвердження: " + formatDateTime(data));
        block(body, "Підтвердив: " + textOrMissing(data.confirmerName())
                + " (логін: " + textOrMissing(data.confirmerLogin()) + ")");
        block(body, SOFT_TISSUE_LABEL + ": " + yesNo(data.softTissueMisalignment())
                + "\n" + PAIN_LABEL + ": " + yesNo(data.painDiscomfort()));
        block(body, "Коментар: " + noteOrDefault(data.note()));
        block(body, "Повернено на етап: " + textOrMissing(data.returnStageName()));
        String link = systemLink(data);
        if (link != null) {
            block(body, "Процес у системі: " + link);
        }
        return body.toString().stripTrailing();
    }

    private void block(StringBuilder body, String text) {
        if (!body.isEmpty()) {
            body.append('\n');
        }
        body.append(text).append('\n');
    }

    private String textOrMissing(String value) {
        return value == null || value.isBlank() ? MISSING : value;
    }

    private String textOrMissing(Object value) {
        return value == null ? MISSING : value.toString();
    }

    private String shortId(Object id) {
        if (id == null) {
            return MISSING;
        }
        String text = id.toString();
        return text.length() <= 8 ? text : text.substring(0, 8);
    }

    private String formatDateTime(BrakNotificationData data) {
        if (data.confirmedAt() == null) {
            return MISSING;
        }
        return data.confirmedAt().format(DATE_TIME_FORMAT);
    }

    private String yesNo(boolean value) {
        return value ? YES : NO;
    }

    private String noteOrDefault(String note) {
        return note == null || note.isBlank() ? NO_COMMENT : note;
    }

    private String systemLink(BrakNotificationData data) {
        if (data.frontendUrl() == null || data.frontendUrl().isBlank()
                || data.originalInstanceId() == null) {
            return null;
        }
        String base = data.frontendUrl().strip();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            return null;
        }
        return base + "/prosthetics/process/" + data.originalInstanceId();
    }
}
