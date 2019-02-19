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

import java.util.UUID;
import java.util.stream.IntStream;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class ConfigurationMain {

    public static void main(final String[] args) {
        if (args.length == 0) {
            usage();
        }

        switch (args[0].trim()) {
        case "--encrypt":
            ensureArgs(args, 3);
            System.out.println("Value: 'secure:" + new PBECipher().encrypt64(args[2], MasterKey.read(args[1])) + "'");
            break;
        case "--decrypt":
            ensureArgs(args, 3);
            System.out.println("Value: '" + new PBECipher().decrypt64(args[2], MasterKey.read(args[1])) + "'");
            break;
        case "--master-key":
            ensureArgs(args, 2, 3);
            MasterKey.write(args[1], args.length == 2 ? UUID.randomUUID().toString() : args[2]);
            System.out.println("Generated '" + args[1] + "'");
            break;
        default:
            usage();
        }
    }

    private static void ensureArgs(final String[] args, final int... len) {
        if (IntStream.of(len).noneMatch(v -> args.length == v)) {
            usage();
        }
    }

    private static void usage() {
        throw new IllegalArgumentException("Usage:\n" + "  java -cp component-server.jar "
                + "org.talend.sdk.component.server.configuration.cipher.CipheredStringConverter \n"
                + " --encrypt master_key_path value_to_encrypt\n" + " --decrypt master_key_path value_to_decrypt\n"
                + " --master-key master_key_path key_value_or_generate_an_uuid\n");
    }
}
