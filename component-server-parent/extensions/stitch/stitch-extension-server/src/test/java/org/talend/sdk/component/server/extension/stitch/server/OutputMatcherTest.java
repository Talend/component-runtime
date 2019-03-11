/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch.server;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonReaderFactory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.talend.sdk.component.server.extension.stitch.server.execution.OutputMatcher;

class OutputMatcherTest {

    private final JsonBuilderFactory builderFactory = Json.createBuilderFactory(emptyMap());

    private final JsonReaderFactory readerFactory = Json.createReaderFactory(emptyMap());

    @ParameterizedTest
    @CsvSource(value = { "{\"type\": \"STATE\", \"value\": {}}|true|DATA|state|{}|null",
            "{\"type\": \"RECORD\", \"stream\": \"commits\", \"record\": {\"sha\": \"dfdfrfrfr\"}}|true|DATA|record|{\"sha\":\"dfdfrfrfr\"}|null",
            "'{\"type\": \"STATE\", \"value\": {}}\nnext one'|true|DATA|state|{}|next one",
            "'Traceback (most recent call last):\n  File \"/usr/local/bin/tap-github\", line 10, in <module>\n    sys.exit(main())\n  File \"/usr/local/lib/python3.7/site-packages/singer/utils.py\", line 192, in wrapped\n    return fnc(*args, **kwargs)\n  File \"/usr/local/lib/python3.7/site-packages/tap_github.py\", line 556, in main\n    do_sync(args.config, args.state, catalog)\n    chunks = []\nKeyboardInterrupt\n'|true|EXCEPTION|exception|'{\"exception\":\"Traceback (most recent call last):\\n\\n  File \\\"/usr/local/bin/tap-github\\\", line 10, in <module>\\n    sys.exit(main())\\n  File \\\"/usr/local/lib/python3.7/site-packages/singer/utils.py\\\", line 192, in wrapped\\n    return fnc(*args, **kwargs)\\n  File \\\"/usr/local/lib/python3.7/site-packages/tap_github.py\\\", line 556, in main\\n    do_sync(args.config, args.state, catalog)\\n    chunks = []\"}'|KeyboardInterrupt",
            "'INFO METRIC: {\"type\": \"counter\", \"metric\": \"record_count\", \"value\": 300 }'|true|METRIC|metrics|{\"metric\":{\"type\":\"counter\",\"metric\":\"record_count\",\"value\":300}}|null",
            "'WARNING Removed 4 paths during transforms:\n\tauthor\n\tcommit.verification\n\tcommitter\n\tnode_id\n'|true|LOG_WARNING|log|{\"level\":\"warning\",\"message\":\"Removed 4 paths during transforms:\\n\\tauthor\\n\\tcommit.verification\\n\\tcommitter\\n\\tnode_id\"}|null",
            "INFO Starting sync|true|LOG_INFO|log|{\"level\":\"info\",\"message\":\"Starting sync\"}|null",
            // no matching
            "dddesdd|false||||" }, delimiter = '|')
    void matches(final String input, final boolean isMatched, final OutputMatcher expectedMatcher, final String type,
            final String data, final String nextLine) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new StringReader(input))) {
            final String line = reader.readLine();
            final Optional<OutputMatcher> matches = OutputMatcher.matches(line);

            assertEquals(isMatched, matches.isPresent());
            if (!isMatched) {
                return;
            }

            final OutputMatcher matcher = matches.orElseThrow(IllegalArgumentException::new);

            assertEquals(expectedMatcher, matcher);
            final OutputMatcher.Data read = matcher.read(line, () -> {
                try {
                    return reader.readLine();
                } catch (final IOException e) {
                    fail(e.getMessage());
                    throw new IllegalStateException(e);
                }
            }, readerFactory, builderFactory.createObjectBuilder());
            assertEquals(type, read.getType());
            assertEquals(data, String.valueOf(read.getObject()));
            assertEquals(nextLine, String.valueOf(read.getNextLine()));
        }
    }
}
