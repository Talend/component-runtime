/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.components.vault.server;

import java.time.Clock;

import javax.cache.annotation.CacheDefaults;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.components.vault.client.VaultClient;
import org.talend.sdk.components.vault.jcache.VaultCacheKeyGenerator;
import org.talend.sdk.components.vault.jcache.VaultCacheResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@CacheDefaults(cacheKeyGenerator = VaultCacheKeyGenerator.class, cacheResolverFactory = VaultCacheResolver.class)
public class VaultService {

    public static final String TCOMP_UI_CREDENTIAL = "tcomp::ui::credential";

    @Inject
    private VaultClient vault;

    @Inject
    private Clock clock;

    @Inject
    private VaultService self;

    // public CompletionStage<Map<String, String>> decrypt(final Collection<ParameterMeta> properties,
    // final Map<String, String> original, final HttpHeaders headers) {
    // final List<String> cipheredKeys = findCipheredKeys(properties, original);
    // return vault
    // .get(cipheredKeys.stream().map(original::get).collect(toList()), clock.millis(), headers)
    // .thenApply(decrypted -> original
    // .entrySet()
    // .stream()
    // .collect(toMap(Map.Entry::getKey,
    // e -> Optional
    // .of(cipheredKeys.indexOf(e.getKey()))
    // .filter(idx -> idx >= 0)
    // .map(decrypted::get)
    // .map(DecryptedValue::getValue)
    // .orElseGet(() -> original.get(e.getKey())))));
    // }
    //
    // public static List<String> findCipheredKeys(final Collection<ParameterMeta> properties, final Map<String, String>
    // original) {
    // final List<String> credsKeys = properties
    // .stream()
    // .flatMap(VaultService::getNestedConfig)
    // .collect(toList());
    //
    // return original.keySet().stream().filter(key -> credsKeys.contains(key)).collect(toList());
    // }
    //
    // /**
    // *
    // * @param meta
    // * @return Stream of Credentials configuration paths
    // */
    // public static Stream<String> getNestedConfig(final ParameterMeta meta) {
    // return concat(meta
    // .getMetadata()
    // .keySet()
    // .stream()
    // .peek(peeked -> log.warn("[getNestedConfig] {} {}", peeked, meta.getPath()))
    // .filter(k -> k.equals(TCOMP_UI_CREDENTIAL))
    // .map(m -> {
    // return Boolean.parseBoolean(meta.getMetadata().get(TCOMP_UI_CREDENTIAL)) ? meta.getPath() : null;
    // })
    // .filter(Objects::nonNull),
    // ofNullable(meta.getNestedParameters())
    // .map(Collection::stream)
    // .orElseGet(Stream::empty)
    // .flatMap(VaultService::getNestedConfig));
    // }

}
