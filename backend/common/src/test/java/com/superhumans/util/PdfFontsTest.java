package com.superhumans.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

/**
 * Bundled-font contract (#427 item 6, #412 item 1): the renderers take the
 * DejaVu bundle, and a missing bundle fails fast — never a Helvetica
 * document with blank Ukrainian text.
 */
class PdfFontsTest {

    @Test
    void regular_loadsCyrillicCapableBundledFont() throws Exception {
        var font = PdfFonts.regular();

        assertThat(font.getFontProgram().getFontNames().getFontName())
                .containsIgnoringCase("DejaVu");
    }

    @Test
    void missingBundle_throwsInsteadOfHelvetica() {
        ClassLoader empty = new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return null;
            }
        };

        assertThatThrownBy(() -> PdfFonts.regular(empty))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fonts/DejaVuSans.ttf");
        assertThatThrownBy(() -> PdfFonts.bold(empty))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fonts/DejaVuSans-Bold.ttf");
    }
}
