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
package org.talend.sdk.component.runtime.server.vault.proxy.endpoint.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DockerHostNameSanitizerTest {

    @ParameterizedTest
    @CsvSource({
            // valid
            "talend_remote-engine-client_1.talend_default,remote-engine-client",
            "talend_long_remote-engine-client_1.talend_long_default,remote-engine-client",
            "talend_long_remote-engine-client_1_hash****dockercompose123.talend_long_default,remote-engine-client",
            // default to input
            "talend_remote-engine-client_1.talen-d_default,talend_remote-engine-client_1.talen-d_default" })
    void sanitizeDocker(final String input, final String output) {
        assertEquals(output, new DockerHostNameSanitizer().sanitizeDockerHostname(input));
    }

    @ParameterizedTest
    @CsvSource({ "docker_foo_1.weave,foo", "docker_foo_bar_1.weave,foo_bar", "docker_foo-bar_1.weave,foo-bar" })
    void sanitizeWeave(final String input, final String output) {
        assertEquals(output, new DockerHostNameSanitizer().sanitizeWeaveHostname(input));
    }
}
