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
package org.talend.sdk.component.sample;

import java.io.IOException;
import java.io.Serializable;

import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Processor(name = "mapper")
public class PersonToUserMapper implements Serializable {

    // tag::map[]
    @ElementListener
    public User map(final Person person) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Handling {}", person);
        }
        return new User(generateId(person), person.getName());
    }
    // end::map[]

    private String generateId(final Person person) {
        // make it look like an AD convention for the sample
        return "a" + person.getAge() + person.getName();
    }
}
