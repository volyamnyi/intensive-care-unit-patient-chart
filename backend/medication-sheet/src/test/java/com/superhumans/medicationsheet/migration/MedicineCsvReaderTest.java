package com.superhumans.medicationsheet.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MedicineCsvReaderTest {

    @TempDir
    Path temp;

    private Path write(String name, String content) throws Exception {
        Path file = temp.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    @Test
    void streamRows_skipsHeaderAndSplits() throws Exception {
        Path file = write("a.csv", "A;B;C\n1;2;3\n4;5;6\n");
        List<String[]> rows = new ArrayList<>();
        MedicineCsvReader.streamRows(file, rows::add);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsExactly("1", "2", "3");
        assertThat(rows.get(1)).containsExactly("4", "5", "6");
    }

    @Test
    void streamRows_handlesQuotesMultilinesAndEscapes() throws Exception {
        String content = "A;B\n\"x;y\";\"line1\nline2\"\n\"say \"\"hi\"\"\"; plain\n";
        Path file = write("b.csv", content);
        List<String[]> rows = new ArrayList<>();
        MedicineCsvReader.streamRows(file, rows::add);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsExactly("x;y", "line1\nline2");
        assertThat(rows.get(1)).containsExactly("say \"hi\"", " plain");
    }

    @Test
    void streamRows_stripsBomAndCrlf() throws Exception {
        Path file = write("c.csv", "﻿A;B\r\n1;2\r\n");
        List<String[]> rows = new ArrayList<>();
        MedicineCsvReader.streamRows(file, rows::add);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("1", "2");
    }

    @Test
    void streamRows_ignoresTrailingNewline() throws Exception {
        Path file = write("d.csv", "A\n1\n");
        List<String[]> rows = new ArrayList<>();
        MedicineCsvReader.streamRows(file, rows::add);
        assertThat(rows).hasSize(1);
    }

    @Test
    void streamRows_throwsOnUnterminatedQuote() throws Exception {
        Path file = write("e.csv", "A;B\n\"oops;1\n");
        assertThatThrownBy(() -> MedicineCsvReader.streamRows(file, row -> {
        })).isInstanceOf(java.io.IOException.class);
    }

    @Test
    void readAll_readsHeaderToo() throws Exception {
        Path file = write("f.csv", "A;B\n1;2\n");
        List<String[]> rows = MedicineCsvReader.readAll(file);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("1", "2");
    }

    @Test
    void streamRows_keepsBareQuotesInUnquotedFields() throws Exception {
        Path file = write("g.csv", "A;B\n288;[ \"0\", \"1\", \"2\" ]\n");
        List<String[]> rows = new ArrayList<>();
        MedicineCsvReader.streamRows(file, rows::add);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("288", "[ \"0\", \"1\", \"2\" ]");
    }

    @Test
    void streamRows_survivesChunkBoundaries() throws Exception {
        String cell = "\"a\"\"b\";\"x\ny\";tail";
        Path file = write("h.csv", "H\n" + cell + "\nlast;row\n");
        for (int chunk : new int[]{1, 2, 3, 5, 7, 13}) {
            List<String[]> rows = new ArrayList<>();
            MedicineCsvReader.streamRows(file, rows::add, chunk);
            assertThat(rows).as("chunk=%d", chunk).hasSize(2);
            assertThat(rows.get(0)).as("chunk=%d", chunk)
                    .containsExactly("a\"b", "x\ny", "tail");
            assertThat(rows.get(1)).as("chunk=%d", chunk).containsExactly("last", "row");
        }
    }

    @Test
    void streamRows_handlesClosingQuoteAtChunkEnd() throws Exception {
        Path file = write("i.csv", "H\n\"ab\";1\n\"cd\";2\n");
        for (int chunk = 1; chunk < 12; chunk++) {
            List<String[]> rows = new ArrayList<>();
            MedicineCsvReader.streamRows(file, rows::add, chunk);
            assertThat(rows).as("chunk=%d", chunk).hasSize(2);
            assertThat(rows.get(0)).as("chunk=%d", chunk).containsExactly("ab", "1");
            assertThat(rows.get(1)).as("chunk=%d", chunk).containsExactly("cd", "2");
        }
    }
}
