/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.tcketl.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Reads and writes the JSON documents owned by the application (settings, pipelines).
 *
 * <p>
 * Reading is strict about the syntax (duplicated keys and trailing content are errors) and tolerant about the
 * structure (unknown fields are ignored, so a newer minor version of a format stays readable). Writing is
 * deterministic and diff friendly (two spaces, sorted map keys, LF, final newline) and replaces the file atomically.
 */
public final class JsonFiles {

    /** Name of the field holding the {@link FormatVersion} of a document. */
    public static final String VERSION_FIELD = "format-version";

    private static final ObjectMapper MAPPER =
            JsonMapper
                    .builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .build();

    private static final ObjectWriter WRITER = MAPPER.writer(prettyPrinter());

    private JsonFiles() {
        // static helpers only
    }

    /** Reads a document that MUST declare a {@link #VERSION_FIELD} supported by this application. */
    public static <T> T read(final Path file, final Class<T> type, final FormatVersion supported)
            throws IOException {
        return read(file, type, supported, true);
    }

    /** Reads a document whose {@link #VERSION_FIELD} is optional, but must be supported when present. */
    public static <T> T readOptionalVersion(final Path file, final Class<T> type, final FormatVersion supported)
            throws IOException {
        return read(file, type, supported, false);
    }

    /**
     * Serializes the document and replaces {@code file} with it, creating the missing parent directories. The previous
     * content stays untouched when anything fails.
     */
    public static void write(final Path file, final Object document) throws IOException {
        final byte[] content = (WRITER.writeValueAsString(document) + "\n").getBytes(StandardCharsets.UTF_8);
        final Path target = file.toAbsolutePath().normalize();
        final Path directory = target.getParent();
        if (directory == null) {
            throw new IOException("Not a file path: " + file);
        }
        Files.createDirectories(directory);
        final Path temporary =
                directory.resolve("." + target.getFileName() + "."
                        + Long.toHexString(ThreadLocalRandom.current().nextLong()) + ".tmp");
        try {
            try (FileChannel channel =
                    FileChannel.open(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                final ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            move(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static <T> T read(final Path file, final Class<T> type, final FormatVersion supported,
            final boolean versionRequired) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            final JsonNode root = MAPPER.readTree(input);
            if (root == null || root.isMissingNode()) {
                throw new FileFormatException("The file is empty");
            }
            if (!root.isObject()) {
                throw new FileFormatException("The file does not contain a JSON object");
            }
            final JsonNode version = root.get(VERSION_FIELD);
            if (versionRequired || version != null) {
                FormatVersion.verify(version == null || version.isNull() ? null : version.asText(), supported);
            }
            return MAPPER.treeToValue(root, type);
        } catch (final JsonProcessingException e) {
            throw new FileFormatException(describe(e), e);
        }
    }

    private static void move(final Path source, final Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (final AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String describe(final JsonProcessingException error) {
        final String reason =
                error.getOriginalMessage() == null ? "" : error.getOriginalMessage().lines().findFirst().orElse("");
        if (error instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            return "Invalid content at '" + path(mapping.getPath()) + "': " + reason;
        }
        if (error.getLocation() != null && error.getLocation().getLineNr() > 0) {
            return "Invalid content at line " + error.getLocation().getLineNr() + ", column "
                    + error.getLocation().getColumnNr() + ": " + reason;
        }
        return "Invalid content: " + reason;
    }

    private static String path(final List<JsonMappingException.Reference> references) {
        final StringBuilder path = new StringBuilder();
        for (final JsonMappingException.Reference reference : references) {
            if (reference.getFieldName() != null) {
                if (path.length() > 0) {
                    path.append('.');
                }
                path.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static DefaultPrettyPrinter prettyPrinter() {
        final DefaultPrettyPrinter printer = new DefaultPrettyPrinter(Separators
                .createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                .withObjectEmptySeparator("")
                .withArrayEmptySeparator(""));
        final DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        return printer;
    }
}
