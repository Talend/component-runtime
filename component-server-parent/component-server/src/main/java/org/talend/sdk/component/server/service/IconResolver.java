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
package org.talend.sdk.component.server.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.talend.sdk.component.container.Container;

import lombok.Data;

@ApplicationScoped
public class IconResolver {

    public Icon resolve(final Container container, final String icon) {
        if (icon == null) {
            return null;
        }

        Cache cache = container.get(Cache.class);
        if (cache == null) {
            synchronized (container) {
                cache = container.get(Cache.class);
                if (cache == null) {
                    cache = new Cache();
                    container.set(Cache.class, cache);
                }
            }
        }
        return cache.icons.computeIfAbsent(icon, k -> doLoad(container.getLoader(), icon)).orElse(null);
    }

    private static class Cache {

        private final ConcurrentMap<String, Optional<Icon>> icons = new ConcurrentHashMap<>();
    }

    // todo: add support for svg if apps don't embed the Talend/ui/icon bundle
    public Optional<Icon> doLoad(final ClassLoader loader, final String icon) {
        return Stream
                .of(icon + "_icon32.png", "icons/" + icon + "_icon32.png", "icons/svg/" + icon + "_icon32.png",
                        "icons/svg-deprecated/" + icon + "_icon32.png")
                .map(path -> {
                    final InputStream resource = loader.getResourceAsStream(path);
                    if (resource == null) {
                        return null;
                    }
                    return new Icon("image/png", toBytes(resource));
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Data
    public static class Icon {

        private final String type;

        private final byte[] bytes;
    }

    private byte[] toBytes(final InputStream resource) {
        try (final BufferedInputStream stream = new BufferedInputStream(resource)) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(stream.available());
            final byte[] buffer = new byte[1024];
            int read;
            while ((read = stream.read(buffer, 0, buffer.length)) >= 0) {
                if (read > 0) {
                    byteArrayOutputStream.write(buffer, 0, read);
                }
            }
            return byteArrayOutputStream.toByteArray();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
