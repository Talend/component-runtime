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
package org.talend.sdk.component.server.extension.stitch.server.test;

import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class MainTestImpl {

    public static void main(final String[] args) {
        if (args != null && asList(args).contains("--discover")) {
            System.out
                    .println("{\n" + "  \"streams\": [\n" + "    {\n" + "      \"stream\": \"collaborators\",\n"
                            + "      \"tap_stream_id\": \"collaborators\",\n" + "      \"schema\": {\n"
                            + "        \"type\": [\n" + "          \"null\",\n" + "          \"object\"\n"
                            + "        ],\n" + "        \"additionalProperties\": false,\n"
                            + "        \"properties\": {\n" + "          \"login\": {\n" + "            \"type\": [\n"
                            + "              \"null\",\n" + "              \"string\"\n" + "            ]\n"
                            + "          },\n" + "          \"url\": {\n" + "            \"type\": [\n"
                            + "              \"null\",\n" + "              \"string\"\n" + "            ]\n"
                            + "          }\n" + "        }\n" + "      },\n" + "      \"metadata\": [\n" + "        {\n"
                            + "          \"breadcrumb\": [\n" + "            \"properties\",\n"
                            + "            \"login\"\n" + "          ],\n" + "          \"metadata\": {\n"
                            + "            \"inclusion\": \"available\"\n" + "          }\n" + "        },\n"
                            + "        {\n" + "          \"breadcrumb\": [\n" + "            \"properties\",\n"
                            + "            \"url\"\n" + "          ],\n" + "          \"metadata\": {\n"
                            + "            \"inclusion\": \"available\"\n" + "          }\n" + "        }\n"
                            + "      ],\n" + "      \"key_properties\": [\n" + "        \"id\"\n" + "      ]\n"
                            + "    }\n" + "  ]\n" + "}");
            return;
        }
        System.out.println("{\"type\": \"STATE\", \"value\": {}}");
        System.out
                .println("{\"type\": \"SCHEMA\", \"stream\": \"commits\", \"schema\": {\"selected\": true, "
                        + "\"type\": [\"null\", \"object\"], \"additionalProperties\": false, \"properties\": "
                        + "{\"_sdc_repository\": {\"type\": [\"string\"]}, \"sha\": {\"type\": [\"null\", \"string\"], "
                        + "\"description\": \"The git commit hash\"}, \"url\": {\"type\": [\"null\", \"string\"]}, \"parents\":"
                        + " {\"type\": [\"null\", \"array\"], \"items\": {\"type\": [\"null\", \"object\"], "
                        + "\"additionalProperties\": false, \"properties\": {\"sha\": {\"type\": [\"null\", \"string\"], "
                        + "\"description\": \"The git hash of the parent commit\"}, \"url\": {\"type\": [\"null\", \"string\"],"
                        + " \"description\": \"The URL to the parent commit\"}, \"html_url\": {\"type\": [\"null\", "
                        + "\"string\"], \"description\": \"The HTML URL to the parent commit\"}}}}, \"html_url\": {\"type\": "
                        + "[\"null\", \"string\"], \"description\": \"The HTML URL to the commit\"}, \"comments_url\": "
                        + "{\"type\": [\"null\", \"string\"], \"description\": \"The URL to the commit's comments page\"}, "
                        + "\"commit\": {\"type\": [\"null\", \"object\"], \"additionalProperties\": false, \"properties\": "
                        + "{\"url\": {\"type\": [\"null\", \"string\"], \"description\": \"The URL to the commit\"}, \"tree\": "
                        + "{\"type\": [\"null\", \"object\"], \"additionalProperties\": false, \"properties\": {\"sha\": "
                        + "{\"type\": [\"null\", \"string\"]}, \"url\": {\"type\": [\"null\", \"string\"]}}}, \"author\": "
                        + "{\"type\": [\"null\", \"object\"], \"additionalProperties\": false, \"properties\": {\"date\": "
                        + "{\"type\": [\"null\", \"string\"], \"format\": \"date-time\", \"description\": \"The date the author"
                        + " committed the change\"}, \"name\": {\"type\": [\"null\", \"string\"], \"description\": \"The "
                        + "author's name\"}, \"email\": {\"type\": [\"null\", \"string\"], \"description\": \"The author's "
                        + "email\"}}}, \"message\": {\"type\": [\"null\", \"string\"], \"description\": \"The commit "
                        + "message\"}, \"committer\": {\"type\": [\"null\", \"object\"], \"additionalProperties\": false, "
                        + "\"properties\": {\"date\": {\"type\": [\"null\", \"string\"], \"format\": \"date-time\", "
                        + "\"description\": \"The date the committer committed the change\"}, \"name\": {\"type\": [\"null\", "
                        + "\"string\"], \"description\": \"The committer's name\"}, \"email\": {\"type\": [\"null\", "
                        + "\"string\"], \"description\": \"The committer's email\"}}}, \"comment_count\": {\"type\": [\"null\","
                        + " \"integer\"], \"description\": \"The number of comments on the commit\"}}}}}, \"key_properties\": "
                        + "[\"sha\"]}");
        System.out
                .println("{\"type\": \"RECORD\", \"stream\": \"commits\", \"record\": {\"sha\": "
                        + "\"bc5f0a2c4ad411740989c2715625958ef41fbf76\", \"commit\": {\"author\": {\"name\": \"Romain "
                        + "Manni-Bucau\", \"email\": \"rmannibucau@gmail.com\", \"date\": \"2019-02-26T09:05:15.000000Z\"}, "
                        + "\"committer\": {\"name\": \"Romain Manni-Bucau\", \"email\": \"rmannibucau@gmail.com\", \"date\": "
                        + "\"2019-02-26T09:05:15.000000Z\"}, \"message\": \"minor enhancement of vaul tproxy error handling + "
                        + "dev config\", \"tree\": {\"sha\": \"90cb300c83d1b86fd327bae5d2345115387bf426\", \"url\": "
                        + "\"https://api.github.com/repos/Talend/component-runtime/git/trees"
                        + "/90cb300c83d1b86fd327bae5d2345115387bf426\"}, \"url\": \"https://api.github"
                        + ".com/repos/Talend/component-runtime/git/commits/bc5f0a2c4ad411740989c2715625958ef41fbf76\", "
                        + "\"comment_count\": 0}, \"url\": \"https://api.github"
                        + ".com/repos/Talend/component-runtime/commits/bc5f0a2c4ad411740989c2715625958ef41fbf76\", "
                        + "\"html_url\": \"https://github"
                        + ".com/Talend/component-runtime/commit/bc5f0a2c4ad411740989c2715625958ef41fbf76\", \"comments_url\": "
                        + "\"https://api.github.com/repos/Talend/component-runtime/commits"
                        + "/bc5f0a2c4ad411740989c2715625958ef41fbf76/comments\", \"parents\": [{\"sha\": "
                        + "\"3df0bd287d11901f66aec2cbfc19a786014ec86b\", \"url\": \"https://api.github"
                        + ".com/repos/Talend/component-runtime/commits/3df0bd287d11901f66aec2cbfc19a786014ec86b\", "
                        + "\"html_url\": \"https://github"
                        + ".com/Talend/component-runtime/commit/3df0bd287d11901f66aec2cbfc19a786014ec86b\"}], "
                        + "\"_sdc_repository\": \"talend/component-runtime\"}, \"time_extracted\": \"2019-02-26T09:48:33"
                        + ".874437Z\"}");
        System.err.println("WARNING Removed paths list: ['author', 'commit.verification', 'committer', 'node_id']");
        System.err
                .println("WARNING Removed 4 paths during transforms:\n" + "        author\n"
                        + "        commit.verification\n" + "        committer\n" + "        node_id");
        System.err
                .println("INFO METRIC: {\"type\": \"counter\", \"metric\": \"record_count\", \"value\": 300, "
                        + "\"tags\": {\"endpoint\": \"commits\"}}");
        System.err
                .println("Traceback (most recent call last):\n"
                        + "  File \"/usr/local/bin/tap-github\", line 10, in <module>\n" + "    sys.exit(main())\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/singer/utils.py\", line 192, in wrapped\n"
                        + "    return fnc(*args, **kwargs)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/tap_github.py\", line 556, in main\n"
                        + "    do_sync(args.config, args.state, catalog)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/tap_github.py\", line 529, in do_sync\n"
                        + "    state = sync_func(stream_schema, repo, state, mdata)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/tap_github.py\", line 357, in get_all_commits\n"
                        + "    commits = response.json()\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/requests/models.py\", line 897, in json\n"
                        + "    return complexjson.loads(self.text, **kwargs)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/__init__.py\", line 516, in loads\n"
                        + "    return _default_decoder.decode(s)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/decoder.py\", line 370, in decode\n"
                        + "    obj, end = self.raw_decode(s)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/decoder.py\", line 400, in raw_decode\n"
                        + "    return self.scan_once(s, idx=_w(s, idx).end())\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/scanner.py\", line 127, in scan_once\n"
                        + "    return _scan_once(string, idx)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/scanner.py\", line 95, in _scan_once\n"
                        + "    return parse_array((string, idx + 1), _scan_once)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/decoder.py\", line 250, in JSONArray\n"
                        + "    value, end = scan_once(s, end)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/scanner.py\", line 93, in _scan_once\n"
                        + "    _scan_once, object_hook, object_pairs_hook, memo)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/decoder.py\", line 194, in JSONObject\n"
                        + "    value, end = scan_once(s, end)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/scanner.py\", line 93, in _scan_once\n"
                        + "    _scan_once, object_hook, object_pairs_hook, memo)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/decoder.py\", line 194, in JSONObject\n"
                        + "    value, end = scan_once(s, end)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/scanner.py\", line 93, in _scan_once\n"
                        + "    _scan_once, object_hook, object_pairs_hook, memo)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/decoder.py\", line 194, in JSONObject\n"
                        + "    value, end = scan_once(s, end)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/scanner.py\", line 90, in _scan_once\n"
                        + "    return parse_string(string, idx + 1, encoding, strict)\n"
                        + "  File \"/usr/local/lib/python3.7/site-packages/simplejson/decoder.py\", line 62, in py_scanstring\n"
                        + "    chunks = []");
        System.out.println("KeyboardInterrupt"); // unknown but ok
    }
}
