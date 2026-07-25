package com.superhumans.utils;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class JsonCompressor {

    public static byte[] shrink(String json) throws IOException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    public static String unshrink(byte[] compressed) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        try (GZIPInputStream gzip = new GZIPInputStream(bais);
             InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
