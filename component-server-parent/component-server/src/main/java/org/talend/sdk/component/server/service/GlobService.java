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

import static java.util.Optional.ofNullable;

import java.io.File;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GlobService {

    public Stream<File> toFiles(final String path) {
        if (path.endsWith("*") || path.endsWith("*.properties")) {
            final String prefix = path.substring(0, path.lastIndexOf('*'));
            final int lastSlash = prefix.replace(File.separatorChar, '/').lastIndexOf('/');
            final String folder;
            final String filePrefix;
            if (lastSlash > 0) {
                folder = prefix.substring(0, lastSlash);
                filePrefix = prefix.substring(lastSlash + 1);
            } else {
                folder = prefix;
                filePrefix = "";
            }
            return Stream
                    .of(folder)
                    .map(File::new)
                    .flatMap(it -> ofNullable(
                            it.listFiles((dir, name) -> name.startsWith(filePrefix) && name.endsWith(".properties")))
                                    .map(Stream::of)
                                    .orElseGet(Stream::empty));
        }
        return Stream.of(path).map(File::new);
    }
}
