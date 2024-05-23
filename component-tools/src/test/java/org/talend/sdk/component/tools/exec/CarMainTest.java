/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CarMainTest {

    @Test
    void urlDecoderDecode() throws Exception {
        final String expected = "/C:/Users/Cédric/Talend/Talend-Studio-V8.0.1/test-1.55.0.car";
        final String car = new String("file:/C:/Users/C%c3%a9dric/Talend/Talend-Studio-V8.0.1/test-1.55.0.car"
                .getBytes(StandardCharsets.ISO_8859_1));
        final String path = new URL(car).getFile();
        final File file = new File(URLDecoder.decode(path, "UTF-8"));
        assertEquals(expected, file.getPath());
    }
}