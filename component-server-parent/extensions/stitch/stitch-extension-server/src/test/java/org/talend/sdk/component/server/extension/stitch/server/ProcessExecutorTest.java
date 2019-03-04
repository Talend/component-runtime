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

import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.extension.stitch.server.configuration.App;
import org.talend.sdk.component.server.extension.stitch.server.execution.ProcessExecutor;

@MonoMeecrowaveConfig
class ProcessExecutorTest {

    @Inject
    private ProcessExecutor executor;

    @App
    @Inject
    private JsonBuilderFactory factory;

    @Test
    void execute() throws ExecutionException, InterruptedException {
        final List<JsonObject> jsons = new ArrayList<>();
        executor
                .execute("ProcessExecutorTest#execute", factory.createObjectBuilder().build(), () -> true,
                        (type, data) -> {
                            final JsonObject record =
                                    factory.createObjectBuilder().add("type", type).add("data", data).build();
                            synchronized (jsons) {
                                jsons.add(record);
                            }
                        }, ProcessExecutor.ProcessOutputMode.LINE, 60000L)
                .toCompletableFuture()
                .get();
        assertEquals(7, jsons.size());
        jsons.sort(comparing(it -> it.getString("type")));
        final Iterator<JsonObject> iterator = jsons.iterator();
        assertEntry(iterator.next(), "exception", "/data/exception", "Traceback (most recent call last):\n\n  "
                + "File \"/usr/local/bin/tap-github\", line 10, in <module>\n    sys.exit(main())\n  File "
                + "\"/usr/local/lib/python3.7/site-packages/singer/utils.py\", line 192, in wrapped\n    return "
                + "fnc(*args, **kwargs)\n  File \"/usr/local/lib/python3.7/site-packages/tap_github.py\", line "
                + "556, in main\n    do_sync(args.config, args.state, catalog)\n  File \"/usr/local/lib/python3"
                + ".7/site-packages/tap_github.py\", line 529, in do_sync\n    state = sync_func(stream_schema, "
                + "repo, state, mdata)\n  File \"/usr/local/lib/python3.7/site-packages/tap_github.py\", line 357,"
                + " in get_all_commits\n    commits = response.json()\n  File \"/usr/local/lib/python3"
                + ".7/site-packages/requests/models.py\", line 897, in json\n    return complexjson.loads(self.text,"
                + " **kwargs)\n  File \"/usr/local/lib/python3.7/site-packages/simplejson/__init__.py\", line 516,"
                + " in loads\n    return _default_decoder.decode(s)\n  File \"/usr/local/lib/python3"
                + ".7/site-packages/simplejson/decoder.py\", line 370, in decode\n    obj, end = self.raw_decode(s)"
                + "\n  File \"/usr/local/lib/python3.7/site-packages/simplejson/decoder.py\", line 400, in "
                + "raw_decode\n    return self.scan_once(s, idx=_w(s, idx).end())\n  File \"/usr/local/lib/python3"
                + ".7/site-packages/simplejson/scanner.py\", line 127, in scan_once\n    return _scan_once(string, "
                + "idx)\n  File \"/usr/local/lib/python3.7/site-packages/simplejson/scanner.py\", line 95, in "
                + "_scan_once\n    return parse_array((string, idx + 1), _scan_once)\n  File "
                + "\"/usr/local/lib/python3.7/site-packages/simplejson/decoder.py\", line 250, in JSONArray\n    "
                + "value, end = scan_once(s, end)\n  File \"/usr/local/lib/python3"
                + ".7/site-packages/simplejson/scanner.py\", line 93, in _scan_once\n    _scan_once, object_hook, "
                + "object_pairs_hook, memo)\n  File \"/usr/local/lib/python3.7/site-packages/simplejson/decoder"
                + ".py\", line 194, in JSONObject\n    value, end = scan_once(s, end)\n  File "
                + "\"/usr/local/lib/python3.7/site-packages/simplejson/scanner.py\", line 93, in _scan_once\n    "
                + "_scan_once, object_hook, object_pairs_hook, memo)\n  File \"/usr/local/lib/python3"
                + ".7/site-packages/simplejson/decoder.py\", line 194, in JSONObject\n    value, end = scan_once(s, "
                + "end)\n  File \"/usr/local/lib/python3.7/site-packages/simplejson/scanner.py\", line 93, in "
                + "_scan_once\n    _scan_once, object_hook, object_pairs_hook, memo)\n  File "
                + "\"/usr/local/lib/python3.7/site-packages/simplejson/decoder.py\", line 194, in JSONObject\n    "
                + "value, end = scan_once(s, end)\n  File \"/usr/local/lib/python3"
                + ".7/site-packages/simplejson/scanner.py\", line 90, in _scan_once\n    return parse_string(string,"
                + " idx + 1, encoding, strict)\n  File \"/usr/local/lib/python3.7/site-packages/simplejson/decoder"
                + ".py\", line 62, in py_scanstring\n    chunks = []");

        assertLog(iterator.next(), "warning",
                "Removed paths list: ['author', 'commit.verification', 'committer', 'node_id']");
        assertLog(iterator.next(), "warning", "Removed 4 paths during transforms:\n" + "        author\n"
                + "        commit.verification\n" + "        committer\n" + "        node_id");
        assertEntry(iterator.next(), "metrics", "/data/metric/value", "300");
        assertEntry(iterator.next(), "record", "/data/sha", "bc5f0a2c4ad411740989c2715625958ef41fbf76");
        assertEntry(iterator.next(), "schema", "/data/properties/_sdc_repository/type/0", "string");
        assertEntry(iterator.next(), "state", "/type", "state");
    }

    private void assertLog(final JsonObject object, final String level, final String message) {
        assertEntry(object, "log", "/data/level", level);
        assertEntry(object, "log", "/data/message", message);
    }

    private void assertEntry(final JsonObject obj, final String type, final String pointer,
            final String expectedValue) {
        assertNotNull(obj);
        assertEquals(type, obj.getString("type"));

        final JsonValue value = Json.createPointer(pointer).getValue(obj);
        assertNotNull(value);
        switch (value.getValueType()) {
        case STRING:
            assertEquals(expectedValue, JsonString.class.cast(value).getString());
            break;
        case NUMBER:
            assertEquals(Long.parseLong(expectedValue), JsonNumber.class.cast(value).longValue());
            break;
        case OBJECT:
            assertEquals(expectedValue, String.valueOf(value));
            break;
        default:
            fail(value.toString());
        }
    }
}
