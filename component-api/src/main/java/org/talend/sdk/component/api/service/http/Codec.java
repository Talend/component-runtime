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
package org.talend.sdk.component.api.service.http;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines which implementation payloads (input/output) are serialized with.
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface Codec {

    /**
     * @return the encoder to use to create the payload sent to the server.
     */
    Class<? extends Encoder>[] encoder() default {};

    /**
     * @return the decoder payload used to read the response.
     */
    Class<? extends Decoder>[] decoder() default {};
}
