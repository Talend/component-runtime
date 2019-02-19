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
package org.talend.sdk.component.configuration.converter.secured;

import static lombok.AccessLevel.PRIVATE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.IntStream;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
class MasterKey {

    private static final int SIZE =
            Integer.getInteger("talend.component.server.configuration.master_key.size", 2 * 1024 * 1024);

    static void write(final String location, final String masterKey) {
        final byte[] key = masterKey.getBytes(StandardCharsets.UTF_8);

        try (final DataOutputStream stream =
                new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(location))))) {
            final int[] indices = new int[key.length];
            final byte[] data = new byte[SIZE];

            final SecureRandom secureRandom = new SecureRandom();
            new Random(System.currentTimeMillis()).nextBytes(data);
            for (int i = 0; i < key.length; i++) {
                final int index = secureRandom.nextInt(SIZE);
                indices[i] = index;
                data[index] = key[i];
            }
            stream.writeInt(key.length);
            for (final int keyByte : indices) {
                stream.writeInt(keyByte);
            }
            stream.write(data);
            final byte[] footer = new byte[secureRandom.nextInt(key.length)];
            secureRandom.nextBytes(footer);
            stream.write(footer);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static byte[] read(final String location) {
        try (final DataInputStream in =
                new DataInputStream(new BufferedInputStream(Files.newInputStream(Paths.get(location))))) {
            final int len = in.readInt();
            final byte[] masterKey = new byte[len];
            final int[] keyIndices = new int[len];
            for (int i = 0; i < len; i++) {
                keyIndices[i] = in.readInt();
            }

            final int maxIdx = IntStream.of(keyIndices).max().orElse(0) + 1;
            final byte[] data = new byte[maxIdx];
            final int nbRead = in.read(data);
            if (nbRead != maxIdx) {
                throw new IllegalStateException("Corrupted master_key: '" + location + "'");
            }
            for (int i = 0; i < masterKey.length; i++) {
                masterKey[i] = data[keyIndices[i]];
            }
            return masterKey;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
