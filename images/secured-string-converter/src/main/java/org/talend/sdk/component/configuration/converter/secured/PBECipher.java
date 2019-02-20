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

import static java.util.Locale.ROOT;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// from plexus-cipher - reformatted for this project rules
class PBECipher {

    private static final int SPICE_SIZE = 16;

    private static final int SALT_SIZE = 8;

    private static final int CHUNK_SIZE = 16;

    private static final String DIGEST_ALG = "SHA-256";

    private static final String KEY_ALG = "AES";

    private static final String CIPHER_ALG = "AES/CBC/PKCS5Padding";

    private final MessageDigest digester;

    private final SecureRandom secureRandom;

    private final boolean onLinux;

    PBECipher() {
        try {
            digester = MessageDigest.getInstance(DIGEST_ALG);
            onLinux = System.getProperty("os.name", "blah").toLowerCase(ROOT).contains("linux");
            if (!onLinux) {
                secureRandom = new SecureRandom();
            } else {
                secureRandom = null;
            }

        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private byte[] getSalt(final int numBytes) {
        if (secureRandom != null) {
            secureRandom.setSeed(System.currentTimeMillis());
            return secureRandom.generateSeed(numBytes);
        }
        final byte[] res = new byte[numBytes];
        new Random(System.currentTimeMillis()).nextBytes(res);
        return res;
    }

    String encrypt64(final String clearText, final byte[] password) {
        try {
            final byte[] clearBytes = clearText.getBytes(StandardCharsets.UTF_8);
            final byte[] salt = getSalt(SALT_SIZE);
            if (secureRandom != null) {
                new SecureRandom().nextBytes(salt);
            }

            final Cipher cipher = createCipher(password, salt, Cipher.ENCRYPT_MODE);
            final byte[] encryptedBytes = cipher.doFinal(clearBytes);
            final int len = encryptedBytes.length;
            final byte padLen = (byte) (CHUNK_SIZE - (SALT_SIZE + len + 1) % CHUNK_SIZE);
            final int totalLen = SALT_SIZE + len + padLen + 1;
            final byte[] allEncryptedBytes = getSalt(totalLen);
            System.arraycopy(salt, 0, allEncryptedBytes, 0, SALT_SIZE);
            allEncryptedBytes[SALT_SIZE] = padLen;
            System.arraycopy(encryptedBytes, 0, allEncryptedBytes, SALT_SIZE + 1, len);

            final byte[] encryptedTextBytes = Base64.getEncoder().encode(allEncryptedBytes);
            return new String(encryptedTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    String decrypt64(final String encryptedText, final byte[] password) {
        try {
            final byte[] allEncryptedBytes = Base64.getDecoder().decode(encryptedText.getBytes());
            final int totalLen = allEncryptedBytes.length;
            final byte[] salt = new byte[SALT_SIZE];
            System.arraycopy(allEncryptedBytes, 0, salt, 0, SALT_SIZE);
            final byte padLen = allEncryptedBytes[SALT_SIZE];
            final byte[] encryptedBytes = new byte[totalLen - SALT_SIZE - 1 - padLen];
            System.arraycopy(allEncryptedBytes, SALT_SIZE + 1, encryptedBytes, 0, encryptedBytes.length);
            final Cipher cipher = createCipher(password, salt, Cipher.DECRYPT_MODE);
            final byte[] clearBytes = cipher.doFinal(encryptedBytes);
            return new String(clearBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Cipher createCipher(final byte[] pwdAsBytes, final byte[] inputSalt, final int mode)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException {
        digester.reset();

        final byte[] keyAndIv = new byte[SPICE_SIZE * 2];
        final byte[] salt;
        if (inputSalt == null || inputSalt.length == 0) {
            salt = null; // no salt, likely bad
        } else {
            salt = inputSalt;
        }

        byte[] result;
        int currentPos = 0;
        while (currentPos < keyAndIv.length) {
            digester.update(pwdAsBytes);

            if (salt != null) {
                digester.update(salt, 0, 8);
            }
            result = digester.digest();

            final int stillNeed = keyAndIv.length - currentPos;
            if (result.length > stillNeed) {
                final byte[] b = new byte[stillNeed];
                System.arraycopy(result, 0, b, 0, b.length);
                result = b;
            }

            System.arraycopy(result, 0, keyAndIv, currentPos, result.length);
            currentPos += result.length;
            if (currentPos < keyAndIv.length) {
                digester.reset();
                digester.update(result);
            }
        }

        final byte[] key = new byte[SPICE_SIZE];
        final byte[] iv = new byte[SPICE_SIZE];
        System.arraycopy(keyAndIv, 0, key, 0, key.length);
        System.arraycopy(keyAndIv, key.length, iv, 0, iv.length);
        final Cipher cipher = Cipher.getInstance(CIPHER_ALG);
        cipher.init(mode, new SecretKeySpec(key, KEY_ALG), new IvParameterSpec(iv));
        return cipher;
    }
}
