/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.runtime.documentation;

import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;
import org.sonatype.plexus.components.cipher.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.Data;
import lombok.NoArgsConstructor;

// NOTE: in progress until we find a way to have confluence 5 REST API
/**
 * create in ~/.m2/settings.xml a server with the id "talend-confluence" to
 * ensure this main can work.
 */
// note: confluence 4.2.12 compatible
public class ConfluenceUpdater implements Runnable {

    @Option(name = "--source-base", usage = "Source folder to synchronize", required = true)
    private String sourceFolder;

    @Option(name = "--confluence-parent-page", usage = "Confluence parent page", required = true)
    private String parentPage;

    @Option(name = "--documentation-version", usage = "Project version", required = true)
    private String version;

    @Option(name = "--confluence-base", usage = "Confluence base URL")
    private String confluenceBase = "https://wiki.talend.com/";

    @Option(name = "--confluence-space", usage = "Confluence space")
    private String space = "rd";

    @Option(name = "--settings-security-location",
            usage = "Where confluence credentials master key is stored, default to ~/.m2/settings-security.xml")
    private String settingsSecurityLocation =
            new File(System.getProperty("user.home"), ".m2/settings-security.xml").getAbsolutePath();

    @Option(name = "--settings-location",
            usage = "Where confluence credentials are read, default to ~/.m2/settings.xml")
    private String settingsLocation = new File(System.getProperty("user.home"), ".m2/settings.xml").getAbsolutePath();

    @Option(name = "--maven-settings-server-id", usage = "~/.m2/settings.xml server id with confluence credentials")
    private String settingsServerId = "talend-confluence";

    @Override
    public void run() {
        final Server server = findCredentials();
        final Client client = ClientBuilder.newClient();
        try {
            final WebTarget baseWebTarget = client.target(confluenceBase);
        } finally {
            client.close();
        }
    }

    private Server findCredentials() {
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(false);
        saxParserFactory.setValidating(false);
        final SAXParser parser;
        try {
            parser = saxParserFactory.newSAXParser();
        } catch (final ParserConfigurationException | SAXException e) {
            throw new IllegalStateException(e);
        }
        final File file = new File(settingsLocation);
        if (!file.exists()) {
            throw new IllegalArgumentException(
                    "No " + settingsLocation + " found, ensure your credentials configuration is valid");
        }

        final File securityFile = new File(settingsSecurityLocation);
        final String master;
        if (securityFile.isFile()) {
            final MvnMasterExtractor extractor = new MvnMasterExtractor();
            try (final InputStream is = new FileInputStream(securityFile)) {
                parser.parse(is, extractor);
            } catch (final IOException | SAXException e) {
                throw new IllegalArgumentException(e);
            }
            master = extractor.current == null ? null : extractor.current.toString().trim();
        } else {
            master = null;
        }

        final MvnServerExtractor extractor = new MvnServerExtractor(master, settingsServerId);
        try (final InputStream is = new FileInputStream(file)) {
            parser.parse(is, extractor);
        } catch (final IOException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
        return extractor.server;
    }

    @NoArgsConstructor(access = PRIVATE)
    public static class Main {

        public static void main(final String[] args) {
            final ConfluenceUpdater confluenceUpdater = new ConfluenceUpdater();
            final CmdLineParser parser =
                    new CmdLineParser(confluenceUpdater, ParserProperties.defaults().withUsageWidth(80));

            try {
                parser.parseArgument(args);
                confluenceUpdater.run();
            } catch (final CmdLineException e) {
                if (Boolean.getBoolean("debug")) {
                    e.printStackTrace();
                }
                System.err.println("java " + Main.class.getName() + " [options...] arguments...");
                parser.printUsage(System.err);
                System.err.println();
                System.err.println("  Example: java SampleMain" + parser.printExample(OptionHandlerFilter.ALL));
            }
        }
    }

    private static class MvnServerExtractor extends DefaultHandler {

        private static final Pattern ENCRYPTED_PATTERN = Pattern.compile(".*?[^\\\\]?\\{(.*?[^\\\\])\\}.*");

        private final String passphrase;

        private final String serverId;

        private Server server;

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
                return;
            }
            if ("server".equalsIgnoreCase(qName)) {
                if (server.id.equals(serverId)) {
                    done = true;
                } else if (!done) {
                    server = null;
                }
            } else if (server != null && current != null) {
                switch (qName) {
                case "id":
                    server.id = current.toString();
                    break;
                case "username":
                    server.username = current.toString();
                    break;
                case "password":
                    server.password = doDecrypt(current.toString(), passphrase);
                    break;
                default:
                }
                current = null;
            }
        }

        private String doDecrypt(final String value, final String pwd) {
            final Matcher matcher = ENCRYPTED_PATTERN.matcher(value);
            if (!matcher.matches() && !matcher.find()) {
                return value; // not encrypted, just use it
            }

            final String bare = matcher.group(1);
            if (bare.contains("[") && bare.contains("]") && bare.contains("type=")) {
                throw new IllegalArgumentException("Unsupported encryption for " + value);
            }

            final byte[] allEncryptedBytes = Base64.decodeBase64(value.getBytes(StandardCharsets.UTF_8));
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

    @Data
    private static class Server {

        private String id;

        private String username;

        private String password;
    }
}
