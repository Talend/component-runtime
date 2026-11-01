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
package org.talend.sdk.component.tcketl.domain.form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reads the JSON text of an array or map default value and spreads it into flat configuration entries. The domain has
 * no JSON library, and defaults are small, so this is a minimal reader: anything it cannot read yields no entry.
 */
final class JsonText {

    private static final int MAX_DEPTH = 32;

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

    private final String text;

    private int index;

    private JsonText(final String text) {
        this.text = text;
    }

    /** {@code ["a","b"]} gives {@code path[0]=a} and {@code path[1]=b}; objects nest as {@code path[0].field}. */
    static Map<String, String> arrayEntries(final String json, final String path) {
        final Map<String, String> entries = new LinkedHashMap<>();
        final Object parsed = parseQuietly(json);
        if (parsed instanceof List<?>) {
            spread(parsed, path, entries);
        }
        return entries;
    }

    /** {@code {"k":"v"}} gives {@code path.key[0]=k} and {@code path.value[0]=v}. */
    static Map<String, String> mapEntries(final String json, final String path) {
        final Map<String, String> entries = new LinkedHashMap<>();
        if (parseQuietly(json) instanceof Map<?, ?> members) {
            int position = 0;
            for (final Map.Entry<?, ?> member : members.entrySet()) {
                entries.put(path + ".key[" + position + ']', String.valueOf(member.getKey()));
                spread(member.getValue(), path + ".value[" + position + ']', entries);
                position++;
            }
        }
        return entries;
    }

    private static void spread(final Object value, final String path, final Map<String, String> entries) {
        if (value instanceof Map<?, ?> members) {
            members.forEach((key, member) -> spread(member, path + '.' + key, entries));
        } else if (value instanceof List<?> elements) {
            for (int position = 0; position < elements.size(); position++) {
                spread(elements.get(position), path + '[' + position + ']', entries);
            }
        } else if (value != null) {
            entries.put(path, value.toString());
        }
    }

    private static Object parseQuietly(final String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            final JsonText reader = new JsonText(json);
            final Object value = reader.value(0);
            reader.skipWhitespace();
            return reader.index == json.length() ? value : null;
        } catch (final IllegalArgumentException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    /** Objects are maps, arrays are lists, every other value is its text; {@code null} stays {@code null}. */
    private Object value(final int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("too deep");
        }
        skipWhitespace();
        switch (peek()) {
            case '{':
                return object(depth);
            case '[':
                return array(depth);
            case '"':
                return string();
            default:
                return literal();
        }
    }

    private Map<String, Object> object(final int depth) {
        final Map<String, Object> members = new LinkedHashMap<>();
        index++;
        skipWhitespace();
        if (peek() == '}') {
            index++;
            return members;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw new IllegalArgumentException("member name expected");
            }
            final String name = string();
            skipWhitespace();
            expect(':');
            members.put(name, value(depth + 1));
            skipWhitespace();
            final char next = text.charAt(index++);
            if (next == '}') {
                return members;
            }
            if (next != ',') {
                throw new IllegalArgumentException("',' or '}' expected");
            }
        }
    }

    private List<Object> array(final int depth) {
        final List<Object> elements = new ArrayList<>();
        index++;
        skipWhitespace();
        if (peek() == ']') {
            index++;
            return elements;
        }
        while (true) {
            elements.add(value(depth + 1));
            skipWhitespace();
            final char next = text.charAt(index++);
            if (next == ']') {
                return elements;
            }
            if (next != ',') {
                throw new IllegalArgumentException("',' or ']' expected");
            }
        }
    }

    private String string() {
        final StringBuilder value = new StringBuilder();
        index++;
        while (true) {
            final char c = text.charAt(index++);
            if (c == '"') {
                return value.toString();
            }
            value.append(c == '\\' ? escape() : c);
        }
    }

    private char escape() {
        final char c = text.charAt(index++);
        switch (c) {
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case 'u':
                return unicode();
            default:
                return c;
        }
    }

    private char unicode() {
        final char decoded = (char) Integer.parseInt(text.substring(index, index + 4), 16);
        index += 4;
        return decoded;
    }

    private String literal() {
        final int start = index;
        while (index < text.length() && ",]} \t\r\n".indexOf(text.charAt(index)) < 0) {
            index++;
        }
        final String literal = text.substring(start, index);
        if ("null".equals(literal)) {
            return null;
        }
        if ("true".equals(literal) || "false".equals(literal) || NUMBER.matcher(literal).matches()) {
            return literal;
        }
        throw new IllegalArgumentException("unexpected '" + literal + "'");
    }

    private void expect(final char expected) {
        if (text.charAt(index++) != expected) {
            throw new IllegalArgumentException("'" + expected + "' expected");
        }
    }

    private char peek() {
        if (index >= text.length()) {
            throw new IllegalArgumentException("unexpected end");
        }
        return text.charAt(index);
    }

    private void skipWhitespace() {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
    }
}
