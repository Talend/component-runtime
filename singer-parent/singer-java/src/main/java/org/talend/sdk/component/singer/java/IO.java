/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.singer.java;

import java.io.InputStream;
import java.io.PrintStream;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class IO {

    private final InputStream stdin;

    private final PrintStream stdout;

    private final PrintStream stderr;

    public IO() {
        this(System.in, System.out, System.err);
    }

    public void set() {
        System.setIn(stdin);
        System.setOut(stdout);
        System.setErr(stderr);
    }
}
