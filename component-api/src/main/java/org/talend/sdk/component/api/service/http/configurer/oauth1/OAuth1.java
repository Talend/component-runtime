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
package org.talend.sdk.component.api.service.http.configurer.oauth1;

import static lombok.AccessLevel.PRIVATE;

import java.security.PrivateKey;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.talend.sdk.component.api.meta.Internal;
import org.talend.sdk.component.api.meta.Partial;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Partial("This doesn't fully implement oauth1 yet but is a good example of configurer entry point")
@NoArgsConstructor(access = PRIVATE)
public final class OAuth1 {

    public static class Configurer implements org.talend.sdk.component.api.service.http.Configurer {

        @Override
        public void configure(final Connection connection, final Configurer.ConfigurerConfiguration configuration) {
            loadProvider().newConfigurer().configure(connection, configuration);
        }
    }

    public Map<String, String> buildParameters(final String method, final String url, final byte[] payload,
            final OAuth1.Configuration oauth1Config) {
        return loadProvider().buildParameters(method, url, payload, oauth1Config);
    }

    private static OAuth1Provider loadProvider() {
        final Iterator<OAuth1Provider> iterator = ServiceLoader.load(OAuth1Provider.class).iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("No registered implementation of OAuth1Provider");
        }
        return iterator.next();
    }

    /**
     * This is the SPI to load the implementation, WARNING: this is an internal API.
     */
    @Internal
    public interface OAuth1Provider {

        Map<String, String> buildParameters(String method, String url, byte[] payload,
                OAuth1.Configuration oauth1Config);

        org.talend.sdk.component.api.service.http.Configurer newConfigurer();
    }

    @Data
    @Builder
    public static class Configuration {

        /**
         * The header name to set, by default it uses Authorization.
         */
        private String header;

        /**
         * The prefix to preppend to the header value, by default it uses "OAuth".
         */
        private String headerPrefix;

        /**
         * The payload hashing algorithm, default to null (ignored).
         */
        private String payloadHashAlgorithm;

        /**
         * The signing algorithm, default to HmacSHA1.
         */
        private String algorithm;

        /**
         * When using a hmac algorithm the key, if null default one is composed based on consumer key and token secret.
         */
        private byte[] signingHmacKey;

        /**
         * When using a signature algorithm the private key to use.
         */
        private PrivateKey signingSignatureKey;

        /**
         * Additional oauth parameters (not encoded).
         */
        private Map<String, String> oauthParameters;

        /**
         * oauth_token if set, otherwise ignored.
         */
        private String token;

        /**
         * OAuth token secret.
         */
        private String tokenSecret;

        /**
         * OAuth consumer key.
         */
        private String consumerKey;

        /**
         * OAuth consumer secret.
         */
        private String consumerSecret;

        /**
         * The nonce to use if set, otherwise it is generated.
         */
        private String nonce;

        /**
         * The timestamp to use if set, otherwise it is generated.
         */
        private Long timestamp;
    }
}
