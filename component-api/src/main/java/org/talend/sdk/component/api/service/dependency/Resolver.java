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
package org.talend.sdk.component.api.service.dependency;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public interface Resolver {

    /**
     * Creates a classloader from the passed descriptor (dependencies.txt).
     *
     * WARNING: note it is very important to close the descriptor once no more used otherwise
     * you can leak memory.
     *
     * @param descriptor the dependencies.txt InputStream.
     * @return the classloader initialized with the resolved dependencies.
     */
    ClassLoaderDescriptor mapDescriptorToClassLoader(InputStream descriptor);

    /**
     * Alias to load dependencies from a plain list of gav (groupId:artifactId:version).
     *
     * @param gavs the dependencies to use to resolve dependencies.
     * @return the collection of file representing the available dependencies.
     */
    default ClassLoaderDescriptor mapDescriptorToClassLoader(final List<String> gavs) {
        return mapDescriptorToClassLoader(
                new ByteArrayInputStream(String.join("\n", gavs).getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Creates a classloader from the passed classloader configuration and descriptor (dependencies.txt).
     *
     * WARNING: note it is very important to close the descriptor once no more used otherwise
     * you can leak memory.
     *
     * <p>The default implementation of this method is unsupported and will always throw
     * {@link UnsupportedOperationException}. Implementations that support configurable
     * classloader creation must override this method.</p>
     *
     * @param descriptor the dependencies.txt InputStream.
     * @param configuration the classloader configuration to apply when creating the classloader.
     * @return the classloader initialized with the configuration provided and the resolved dependencies.
     * @throws UnsupportedOperationException if this implementation does not support configurable
     * classloader creation.
     */
    default ClassLoaderDescriptor mapDescriptorToClassLoader(InputStream descriptor,
            final ClassLoaderDefinition configuration) {
        throw new UnsupportedOperationException(
                "ClassLoader configuration is not supported by this implementation; "
                        + "override mapDescriptorToClassLoader(InputStream, ClassLoaderDefinition) to provide an implementation");
    }

    default ClassLoaderDescriptor mapDescriptorToClassLoader(final List<String> gavs,
            final ClassLoaderDefinition configuration) {
        return mapDescriptorToClassLoader(
                new ByteArrayInputStream(String.join("\n", gavs).getBytes(StandardCharsets.UTF_8)), configuration);
    }

    /**
     * Resolves the dependencies from the descriptor passed as an InputStream.
     *
     * IMPORTANT: this is to use when you are sure the file is resolvable if you don't have a fallback.
     * In that last case, prefer the <code>mapDescriptorToClassLoader</code>.
     *
     * @param descriptor the dependencies.txt to use to resolve dependencies.
     * @return the collection of file representing the available dependencies.
     */
    Collection<File> resolveFromDescriptor(InputStream descriptor);

    /**
     * Alias to load dependencies from a plain list of gav (groupId:artifactId:version).
     *
     * @param gavs the dependencies to use to resolve dependencies.
     * @return the collection of file representing the available dependencies.
     */
    default Collection<File> resolveFromDescriptor(final List<String> gavs) {
        return resolveFromDescriptor(
                new ByteArrayInputStream(String.join("\n", gavs).getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Abstract a classloader adding the metadata about the resolution done to create it.
     */
    interface ClassLoaderDescriptor extends AutoCloseable {

        /**
         * @return the underlying classloader.
         */
        ClassLoader asClassLoader();

        /**
         * @return the dependencies who matched the resolution and were used to create the classloader.
         */
        Collection<String> resolvedDependencies();
    }
}
