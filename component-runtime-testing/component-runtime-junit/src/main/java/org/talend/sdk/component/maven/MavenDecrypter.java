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
package org.talend.sdk.component.maven;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class MavenDecrypter {

    private static final String M2_HOME = "M2_HOME";

    private static final String MAVEN_HOME = "MAVEN_HOME";

    private static final String USER_HOME = "user.home";

    private static final String FILE_SETTINGS = "settings.xml";

    private static final String FILE_SECURITY = "settings-security.xml";

    private final List<File> settings;

    private final File settingsSecurity;

    public MavenDecrypter() {
        this(findSettingsFiles(), new File(getM2(), FILE_SECURITY));
    }

    @Deprecated
    public MavenDecrypter(final File settings, final File settingsSecurity) {
        this(Collections.singletonList(settings), settingsSecurity);
    }

    public MavenDecrypter(final List<File> settings, final File settingsSecurity) {
        this.settings = settings.stream().filter(File::exists).collect(Collectors.toList());
        this.settingsSecurity = settingsSecurity;
    }

    public Server find(final String serverId) {
        final SAXParser parser = newSaxParser();
        if (settings.isEmpty()) {
            throw new IllegalArgumentException(
                    "No " + settings + " found, ensure your credentials configuration is valid");
        }

        final String master;
        if (settingsSecurity.isFile()) {
            final MvnMasterExtractor extractor = new MvnMasterExtractor();
            try (final InputStream is = new FileInputStream(settingsSecurity)) {
                parser.parse(is, extractor);
            } catch (final IOException | SAXException e) {
                throw new IllegalArgumentException(e);
            }
            master = extractor.current == null ? null : extractor.current.toString().trim();
        } else {
            master = null;
        }

        final MvnServerExtractor extractor = new MvnServerExtractor(master, serverId);
        for (final File file : settings) {
            try (final InputStream is = new FileInputStream(file)) {
                parser.parse(is, extractor);
            } catch (final IOException | SAXException e) {
                throw new IllegalArgumentException(e);
            }
            if (extractor.server != null) {
                return extractor.server;
            }
        }

        throw new IllegalArgumentException("Didn't find " + serverId + " in " + settings);
    }

    private static SAXParser newSaxParser() {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        } catch (final ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException ex) {
            // ignore
        }

        final SAXParser parser;
        try {
            parser = factory.newSAXParser();
        } catch (final ParserConfigurationException | SAXException e) {
            throw new IllegalStateException(e);
        }

        try {
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
            // ignore
        }

        return parser;
    }

    private static File getM2() {
        return ofNullable(System.getProperty("talend.maven.decrypter.m2.location"))
                .map(File::new)
                .orElseGet(() -> new File(System.getProperty(USER_HOME), ".m2"));
    }

    private static List<File> findSettingsFiles() {
        return Stream.of(
                new File(getM2(), FILE_SETTINGS),
                findMavenHome(M2_HOME),
                findMavenHome(MAVEN_HOME))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static File findMavenHome(final String mavenHome) {
        return ofNullable(System.getenv(mavenHome))
                .map(File::new)
                .map(it -> new File(it, "conf/" + FILE_SETTINGS))
                .orElse(null);
    }

    public static void main(final String[] args) {
        System.out.println(new MavenDecrypter().find(args[0]));
    }

    private static class MvnServerExtractor extends DefaultHandler {

        private static final Pattern ENCRYPTED_PATTERN = Pattern.compile(".*?[^\\\\]?\\{(.*?[^\\\\])\\}.*");

        private final String passphrase;

        private final String serverId;

        private Server server;

        private String encryptedPassword;

        private boolean done;

        private StringBuilder current;

        private MvnServerExtractor(final String passphrase, final String serverId) {
            this.passphrase = doDecrypt(passphrase, "settings.security");
            this.serverId = serverId;
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName,
                final Attributes attributes) {
            if ("server".equalsIgnoreCase(qName)) {
                if (!done) {
                    server = new Server();
                }
            } else if (server != null) {
                current = new StringBuilder();
            }
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) {
            if (current != null) {
                current.append(new String(ch, start, length));
            }
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) {
            if (done) {
                // decrypt password only when the server is found
                server.setPassword(doDecrypt(encryptedPassword, passphrase));
                return;
            }
            if ("server".equalsIgnoreCase(qName)) {
                if (server.getId().equals(serverId)) {
                    done = true;
                } else if (!done) {
                    server = null;
                    encryptedPassword = null;
                }
            } else if (server != null && current != null) {
                switch (qName) {
                    case "id":
                        server.setId(current.toString());
                        break;
                    case "username":
                        try {
                            server.setUsername(doDecrypt(current.toString(), passphrase));
                        } catch (final RuntimeException re) {
                            server.setUsername(current.toString());
                        }
                        break;
                    case "password":
                        encryptedPassword = current.toString();
                        break;
                    default:
                }
                current = null;
            }
        }

        private String doDecrypt(final String value, final String pwd) {
            if (value == null) {
                return null;
            }

            final Matcher matcher = ENCRYPTED_PATTERN.matcher(value);
            if (!matcher.matches() && !matcher.find()) {
                return value; // not encrypted, just use it
            }

            final String bare = matcher.group(1);
            if (value.startsWith("${env.")) {
                final String key = bare.substring("env.".length());
                return ofNullable(System.getenv(key)).orElseGet(() -> System.getProperty(bare));
            }
            if (value.startsWith("${")) { // all is system prop, no interpolation yet
                return System.getProperty(bare);
            }

            if (pwd == null || pwd.isEmpty()) {
                throw new IllegalArgumentException("Master password can't be null or empty.");
            }

            if (bare.contains("[") && bare.contains("]") && bare.contains("type=")) {
                throw new IllegalArgumentException("Unsupported encryption for " + value);
            }

            final byte[] allEncryptedBytes = Base64.getMimeDecoder().decode(bare);
            final int totalLen = allEncryptedBytes.length;
            final byte[] salt = new byte[8];
            System.arraycopy(allEncryptedBytes, 0, salt, 0, 8);
            final byte padLen = allEncryptedBytes[8];
            final byte[] encryptedBytes = new byte[totalLen - 8 - 1 - padLen];
            System.arraycopy(allEncryptedBytes, 8 + 1, encryptedBytes, 0, encryptedBytes.length);

            try {
                final MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] keyAndIv = new byte[16 * 2];
                byte[] result;
                int currentPos = 0;

                while (currentPos < keyAndIv.length) {
                    digest.update(pwd.getBytes(StandardCharsets.UTF_8));

                    digest.update(salt, 0, 8);
                    result = digest.digest();

                    final int stillNeed = keyAndIv.length - currentPos;
                    if (result.length > stillNeed) {
                        final byte[] b = new byte[stillNeed];
                        System.arraycopy(result, 0, b, 0, b.length);
                        result = b;
                    }

                    System.arraycopy(result, 0, keyAndIv, currentPos, result.length);

                    currentPos += result.length;
                    if (currentPos < keyAndIv.length) {
                        digest.reset();
                        digest.update(result);
                    }
                }

                final byte[] key = new byte[16];
                final byte[] iv = new byte[16];
                System.arraycopy(keyAndIv, 0, key, 0, key.length);
                System.arraycopy(keyAndIv, key.length, iv, 0, iv.length);

                final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

                final byte[] clearBytes = cipher.doFinal(encryptedBytes);
                return new String(clearBytes, StandardCharsets.UTF_8);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static class MvnMasterExtractor extends DefaultHandler {

        private StringBuilder current;

        @Override
        public void startElement(final String uri, final String localName, final String qName,
                final Attributes attributes) {
            if ("master".equalsIgnoreCase(qName)) {
                current = new StringBuilder();
            }
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) {
            if (current != null) {
                current.append(new String(ch, start, length));
            }
        }
    }
}
