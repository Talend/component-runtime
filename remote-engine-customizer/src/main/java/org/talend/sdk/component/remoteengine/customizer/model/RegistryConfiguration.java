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
package org.talend.sdk.component.remoteengine.customizer.model;

import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.RegistryImage;

import org.tomitribe.crest.api.Option;
import org.tomitribe.crest.api.Options;

@Options
public class RegistryConfiguration {

    private final String username;

    private final String password;

    private final String basePath;

    public RegistryConfiguration(@Option("username") final String username, @Option("password") final String password,
            @Option("baseUrl") final String baseUrl) {
        this.username = username;
        this.password = password;
        this.basePath = baseUrl;
    }

    public RegistryImage toImage(final String toConnectorsImage) throws InvalidImageReferenceException {
        final RegistryImage named = RegistryImage.named(basePath + '/' + toConnectorsImage);
        if (username != null) {
            named.addCredential(username, password);
        }
        return named;
    }
}
