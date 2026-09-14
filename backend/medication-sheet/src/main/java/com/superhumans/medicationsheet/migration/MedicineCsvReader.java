package com.superhumans.medicationsheet.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal {@code ;}-separated CSV reader for the legacy export files.
 * Handles quoted fields, {@code ""} escapes, embedded newlines and a BOM,
 * streaming row by row (the 400+ MB item file never fits in memory twice).
 * No third-party dependency on purpose (module boundary allowlist).
 */
public final class MedicineCsvReader {

    private MedicineCsvReader() {
    }

    /** Row callback for {@link #streamRows}. */
    public interface RowConsumer {
        void accept(String[] row) throws IOException;
    }

    /** Streams data rows (header skipped) without materializing the file. */
    public static void streamRows(Path path, RowConsumer consumer) throws IOException {
        streamRows(path, consumer, 65536);
    }

    static void streamRows(Path path, RowConsumer consumer, int chunkSize) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Parser parser = new Parser();
            char[] buffer = new char[chunkSize];
            int read;
            boolean firstChunk = true;
            while ((read = reader.read(buffer)) != -1) {
                int offset = 0;
                if (firstChunk) {
                    firstChunk = false;
                    if (read > 0 && buffer[0] == '\uFEFF') {
                        offset = 1;
                    }
                }
                parser.feed(buffer, offset, read, consumer);
            }
            parser.finish(consumer);
        } catch (ImportParseException e) {
            throw new IOException("Cannot parse " + path + ": " + e.getMessage(), e);
        }
    }

    /** Reads all data rows (header skipped; only for the small list file). */
    public static List<String[]> readAll(Path path) throws IOException {
        List<String[]> rows = new ArrayList<>();
        streamRows(path, rows::add);
        return rows;
    }

    private static final class Parser {
        private final List<String> current = new ArrayList<>();
        private final StringBuilder field = new StringBuilder();
        private boolean inQuotes;
        private boolean headerSkipped;
        private boolean rowHasContent;
        private boolean pendingCr;
        private boolean pendingQuote;

        void feed(char[] buffer, int offset, int end, RowConsumer consumer) throws IOException {
            int index = offset;
            if (pendingCr) {
                pendingCr = false;
                if (index < end && buffer[index] == '\n') {
                    index++;
                }
            }
            if (pendingQuote) {
                pendingQuote = false;
                if (index < end && buffer[index] == '"') {
                    field.append('"');
                    index++;
                } else {
                    inQuotes = false;
                }
            }
            while (index < end) {
                char ch = buffer[index];
                if (inQuotes) {
                    if (ch == '"') {
                        if (index + 1 < end && buffer[index + 1] == '"') {
                            field.append('"');
                            index += 2;
                        } else if (index + 1 == end) {
                            pendingQuote = true;
                            return;
                        } else {
                            inQuotes = false;
                            index++;
                        }
                    } else {
                        field.append(ch);
                        index++;
                    }
                } else if (ch == '"' && field.length() == 0) {
                    inQuotes = true;
                    rowHasContent = true;
                    index++;
                } else if (ch == '"') {
                    field.append(ch);
                    rowHasContent = true;
                    index++;
                } else if (ch == ';') {
                    current.add(field.toString());
                    field.setLength(0);
                    rowHasContent = true;
                    index++;
                } else if (ch == '\r' || ch == '\n') {
                    endRow(consumer);
                    if (ch == '\r' && index + 1 < end && buffer[index + 1] == '\n') {
                        index += 2;
                    } else if (ch == '\r' && index + 1 == end) {
                        pendingCr = true;
                        index++;
                    } else {
                        index++;
                    }
                } else {
                    field.append(ch);
                    rowHasContent = true;
                    index++;
                }
            }
        }

        void finish(RowConsumer consumer) throws IOException {
            if (pendingQuote) {
                pendingQuote = false;
                inQuotes = false;
            }
            if (inQuotes) {
                throw new ImportParseException("Unterminated quoted field");
            }
            if (rowHasContent || !current.isEmpty()) {
                endRow(consumer);
            }
        }

        private void endRow(RowConsumer consumer) throws IOException {
            current.add(field.toString());
            field.setLength(0);
            String[] row = current.toArray(new String[0]);
            current.clear();
            rowHasContent = false;
            if (!headerSkipped) {
                headerSkipped = true;
                return;
            }
            consumer.accept(row);
        }
    }
}
