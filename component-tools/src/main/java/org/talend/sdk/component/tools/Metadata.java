/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools;

import java.io.IOException;
import java.util.Properties;

public class Metadata {

    private static Metadata inst;

    private Properties meta;

    private Metadata() {
        meta = new Properties() {

            {
                try {
                    load(this.getClass().getClassLoader().getResourceAsStream("metadata.properties"));
                } catch (IOException e) {
                    throw new IllegalStateException("can't find runtime metadata of component-tools");
                }
            }
        };
    }

    public String getVersion() {
        return meta.getProperty("VERSION");
    }

    public static Metadata get() {
        if (inst == null) {
            inst = new Metadata();
        }
        return inst;
    }

}
