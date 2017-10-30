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
package org.talend.component.studio;

import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ServerManagerTest {

    @Test
    public void startServer() throws Exception {
        int port = -1;
        final Thread thread = Thread.currentThread();
        final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try (final URLClassLoader buildLoader = new URLClassLoader(new URL[0], oldLoader) {

            @Override
            public InputStream getResourceAsStream(final String name) {
                if (("META-INF/maven/" + GAV.GROUP_ID + "/" + GAV.ARTIFACT_ID + "/pom.properties").equals(name)) {
                    return new ByteArrayInputStream(
                            ("version = " + System.getProperty("test.version")).getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(name);
            }
        }; final ProcessManager mgr = new ProcessManager(GAV.GROUP_ID, GAV.ARTIFACT_ID, gav -> {
            final String[] segments = gav.substring(gav.lastIndexOf('!') + 1).split("/");
            if (segments[1].startsWith("component-")) { // try in the project
                final File[] root = jarLocation(ServerManagerTest.class).getParentFile().getParentFile().getParentFile()
                        .listFiles((dir, name) -> name.equals(segments[1]));
                if (root != null && root.length == 1) {
                    final File[] jar = new File(root[0], "target").listFiles(
                            (dir, name) -> name.startsWith(segments[1]) && name.endsWith(".jar") && !name.contains("-source")
                                    && !name.contains("-model") && !name.contains("-fat") && !name.contains("-javadoc"));
                    if (jar != null && jar.length == 1) {
                        return jar[0];
                    }
                }
            }
            return new File(System.getProperty("test.m2.repository", System.getProperty("user.home") + "/.m2/repository"),
                    segments[0].replace('.', '/') + '/' + segments[1] + '/' + segments[2] + '/' + segments[1] + '-' + segments[2]
                            + ".jar");
        }, new File("target/conf_missing"))) {
            thread.setContextClassLoader(buildLoader);
            mgr.start();
            mgr.waitForServer();
            port = mgr.getPort();
            assertTrue(isStarted(port));
        } finally {
            thread.setContextClassLoader(oldLoader);
        }
        assertFalse(isStarted(port));
    }

    private boolean isStarted(final int port) throws IOException {
        final URL url = new URL("http://localhost:" + port + "/api/v1/component/index");
        InputStream stream = null;
        try {
            stream = url.openStream();
            final String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
            return "{\"components\":[]}".equals(content); // no component in the test so easy to assert here
        } catch (final ConnectException ioe) {
            return false;
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
