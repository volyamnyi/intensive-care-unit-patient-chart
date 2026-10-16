package com.superhumans.util;

import java.io.IOException;
import java.io.InputStream;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Single bundled-font loader for all PDF renderers (#427 item 6, #412 item
 * 1). Every renderer used to carry its own fallback chain (classpath DejaVu
 * first, then {@code C:/Windows/Fonts} paths, then Helvetica) — a successful
 * HTTP 200 render with garbage/blank Ukrainian text when the chain fell
 * through to non-Cyrillic Helvetica, silently. Now there is exactly one
 * source (the DejaVu TTFs in classpath {@code fonts/}) and it fails fast with
 * an ERROR + exception when the bundle is absent, never a Helvetica document.
 */
@Slf4j
public final class PdfFonts {

    private PdfFonts() {
    }

    /** Bundled DejaVuSans, IDENTITY_H. Throws when the bundle is absent. */
    public static PdfFont regular() {
        return regular(PdfFonts.class.getClassLoader());
    }

    /** Bundled DejaVuSans-Bold, IDENTITY_H. Throws when the bundle is absent. */
    public static PdfFont bold() {
        return bold(PdfFonts.class.getClassLoader());
    }

    static PdfFont regular(ClassLoader loader) {
        return load(loader, "fonts/DejaVuSans.ttf");
    }

    static PdfFont bold(ClassLoader loader) {
        return load(loader, "fonts/DejaVuSans-Bold.ttf");
    }

    private static PdfFont load(ClassLoader loader, String resource) {
        try (InputStream is = loader.getResourceAsStream(resource)) {
            if (is == null) {
                log.error("Bundled PDF font missing from classpath: {}", resource);
                throw new IllegalStateException("Bundled PDF font missing from classpath: " + resource
                        + " — PDFs with Ukrainian text cannot be rendered without it");
            }
            return PdfFontFactory.createFont(is.readAllBytes(), PdfEncodings.IDENTITY_H);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read bundled PDF font: " + resource, e);
        }
    }
}
