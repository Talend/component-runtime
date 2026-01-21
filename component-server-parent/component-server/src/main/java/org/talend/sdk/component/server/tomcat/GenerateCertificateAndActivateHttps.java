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
package org.talend.sdk.component.server.tomcat;

import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.configuration.Configuration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateCertificateAndActivateHttps implements Meecrowave.ConfigurationCustomizer {

    @Override
    public void accept(final Configuration builder) {
        final Config config = ConfigProvider.getConfig();
        if (!config.getOptionalValue("talend.component.server.ssl.active", Boolean.class).orElse(false)) {
            log.debug("Automatic ssl setup is not active, skipping");
            return;
        }

        log.debug("Automatic ssl setup is active");
        final String password =
                config.getOptionalValue("talend.component.server.ssl.password", String.class).orElse("changeit");
        final String location = config
                .getOptionalValue("talend.component.server.ssl.keystore.location", String.class)
                .orElse(new File(System.getProperty("meecrowave.base", "."), "conf/ssl.p12").getAbsolutePath());
        final String alias =
                config.getOptionalValue("talend.component.server.ssl.keystore.alias", String.class).orElse("talend");
        final String keystoreType =
                config.getOptionalValue("talend.component.server.ssl.keystore.type", String.class).orElse("PKCS12");
        final File keystoreLocation = new File(location);
        if (!keystoreLocation.exists() || config
                .getOptionalValue("talend.component.server.ssl.keystore.generation.force", Boolean.class)
                .orElse(false)) {
            final String generateCommand = config
                    .getOptionalValue("talend.component.server.ssl.keystore.generation.command", String.class)
                    .orElse(null);
            if (keystoreLocation.getParentFile() == null
                    || (!keystoreLocation.getParentFile().exists() && !keystoreLocation.getParentFile().mkdirs())) {
                throw new IllegalArgumentException("Can't create '" + keystoreLocation + "'");
            }
            try {
                if (generateCommand != null) {
                    log.debug("Generating certificate for HTTPS using a custom command");
                    doExec(parseCommand(generateCommand));
                } else {
                    log.debug("Generating certificate for HTTPS");
                    doExec(new String[] { findKeyTool(), "-genkey", "-keyalg",
                            config
                                    .getOptionalValue("talend.component.server.ssl.keypair.algorithm", String.class)
                                    .orElse("RSA"),
                            "-alias", alias, "-keystore", keystoreLocation.getAbsolutePath(), "-storepass", password,
                            "-keypass", password, "-noprompt", "-dname",
                            config
                                    .getOptionalValue("talend.component.server.ssl.certificate.dname", String.class)
                                    .orElseGet(
                                            () -> "CN=Talend,OU=www.talend.com,O=component-server,C=" + getLocalId()),
                            "-storetype", keystoreType, "-keysize",
                            config
                                    .getOptionalValue("talend.component.server.ssl.keypair.size", Integer.class)
                                    .map(String::valueOf)
                                    .orElse("2048") });
                }
            } catch (final InterruptedException ie) {
                log.error(ie.getMessage(), ie);
                Thread.currentThread().interrupt();
            } catch (final Exception e) {
                log.error(e.getMessage(), e);
                throw new IllegalStateException(e);
            }
        }

        builder.setSkipHttp(true);
        builder.setSsl(true);
        builder
                .setHttpsPort(config
                        .getOptionalValue("talend.component.server.ssl.port", Integer.class)
                        .orElseGet(builder::getHttpPort));
        builder.setKeystorePass(password);
        builder.setKeystoreFile(keystoreLocation.getAbsolutePath());
        builder.setKeystoreType(keystoreType);
        builder.setKeyAlias(alias);
        log.info("Configured HTTPS using '{}' on port {}", builder.getKeystoreFile(), builder.getHttpsPort());
    }

    private String getLocalId() {
        try {
            return InetAddress.getLocalHost().getHostName().replace('.', '_') /* just in case of a misconfiguration */;
        } catch (final UnknownHostException e) {
            return "local";
        }
    }

    private void doExec(final String[] command) throws InterruptedException, IOException {
        final Process process = new ProcessBuilder(command).start();
        new Thread(new KeyToolStream("stdout", process.getInputStream())).start();
        new Thread(new KeyToolStream("stderr", process.getErrorStream())).start();
        final int status = process.waitFor();
        if (status != 0) {
            throw new IllegalStateException(
                    "Can't generate the certificate, exist code=" + status + ", check out stdout/stderr for details");
        }
    }

    private String findKeyTool() {
        final String ext = System.getProperty("os.name", "ignore").toLowerCase(ROOT).contains("win") ? ".exe" : "";
        final File javaHome = new File(System.getProperty("java.home", "."));
        if (javaHome.exists()) {
            final File keyTool = new File(javaHome, "bin/keytool" + ext);
            if (keyTool.exists()) {
                return keyTool.getAbsolutePath();
            }
            final File jreKeyTool = new File(javaHome, "jre/bin/keytool" + ext);
            if (jreKeyTool.exists()) {
                return jreKeyTool.getAbsolutePath();
            }
        }
        // else check in the path
        final String path = ofNullable(System.getenv("PATH"))
                .orElseGet(() -> System
                        .getenv()
                        .keySet()
                        .stream()
                        .filter(it -> it.equalsIgnoreCase("path"))
                        .findFirst()
                        .map(System::getenv)
                        .orElse(null));
        if (path != null) {
            return Stream
                    .of(path.split(File.pathSeparator))
                    .map(it -> new File(it, "keytool"))
                    .filter(File::exists)
                    .findFirst()
                    .map(File::getAbsolutePath)
                    .orElse(null);
        }
        throw new IllegalStateException("Didn't find keytool");
    }

    // from ant
    private static String[] parseCommand(final String cmd) {
        if (cmd == null || cmd.isEmpty()) {
            return new String[0];
        }

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(cmd, "\"\' ", true);
        final Collection<String> v = new ArrayList<>();
        StringBuffer current = new StringBuffer();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("\'".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                default:
                    if ("\'".equals(nextTok)) {
                        state = inQuote;
                    } else if ("\"".equals(nextTok)) {
                        state = inDoubleQuote;
                    } else if (" ".equals(nextTok)) {
                        if (lastTokenHasBeenQuoted || current.length() != 0) {
                            v.add(current.toString());
                            current = new StringBuffer();
                        }
                    } else {
                        current.append(nextTok);
                    }
                    lastTokenHasBeenQuoted = false;
                    break;
            }
        }
        if (lastTokenHasBeenQuoted || current.length() != 0) {
            v.add(current.toString());
        }
        if (state == inQuote || state == inDoubleQuote) {
            throw new RuntimeException("unbalanced quotes in " + cmd);
        }
        return v.toArray(new String[0]);
    }

    @Slf4j
    private static class KeyToolStream implements Runnable {

        private final String name;

        private final InputStream stream;

        private final ByteArrayOutputStream builder = new ByteArrayOutputStream();

        private KeyToolStream(final String name, final InputStream stream) {
            this.name = name;
            this.stream = stream;
        }

        @Override
        public void run() {
            try {
                final byte[] buf = new byte[64];
                int num;
                while ((num = stream.read(buf)) != -1) { // todo: rework it to handle EOL
                    for (int i = 0; i < num; i++) {
                        if (buf[i] == '\r' || buf[i] == '\n') {
                            doLog();
                            builder.reset();
                        } else {
                            builder.write(buf[i]);
                        }
                    }
                }
                if (builder.size() > 0) {
                    doLog();
                }
            } catch (final IOException e) {
                // no-op
            }
        }

        private void doLog() {
            final String string = builder.toString().trim();
            if (string.isEmpty()) {
                return;
            }
            log.info("[" + name + "] " + string);
        }

    }
}
