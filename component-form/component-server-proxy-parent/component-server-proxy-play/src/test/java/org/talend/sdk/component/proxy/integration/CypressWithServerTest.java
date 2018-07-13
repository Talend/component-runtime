/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.integration;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.openejb.loader.JarLocation.jarLocation;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.OS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.talend.sdk.component.proxy.test.WithServer;

import play.test.TestServer;

@WithServer
// @DisabledIfSystemProperty(named = "component.front.build.skip", matches = "true")
@Disabled("This module will be reworked, no need to run that until we have the final delivery")
class CypressWithServerTest {

    @Test
    @DisplayName("Cypress Integration Tests (E2E)")
    void cypress(final TestServer play) throws Exception {
        if (Boolean.getBoolean("talend.cypress.server.infinite")) { // for local dev iterations
            System.out.println("Waiting for [ENTER], tests will be skipped,\n" + "export CYPRESS_baseUrl="
                    + format("http://localhost:%d", play.port()));
            new Scanner(System.in).nextLine();
            return;
        }
        runCyphress(play.port());
    }

    private void runCyphress(final int port) throws Exception {
        final String browser = System.getProperty("talend.cypress.browser");
        final Integer timeout = Integer.getInteger("talend.cypress.timeout", 600000);
        final boolean headed = Boolean.getBoolean("talend.cypress.headed");
        final boolean debug = Boolean.getBoolean("talend.cypress.debug");
        final boolean video = Boolean.getBoolean("talend.cypress.video");
        final String spec = System.getProperty("talend.cypress.spec");
        // ex of custom options: --config pageLoadTimeout=100000,watchForFileChanges=false for instance
        final String customOptions = System.getProperty("talend.cypress.arguments.custom");
        // todo: --reporter if needed

        final File basedir = jarLocation(CypressWithServerTest.class).getParentFile().getParentFile();
        final File frontendDir = new File(basedir, "src/main/frontend");
        final File nodeBin = new File(basedir, ".node/node");
        final File nodeModulesBin = new File(frontendDir, "node_modules/.bin");

        final boolean isWin = OS.isFamilyWindows();
        final String pathName = isWin ? "Path" : "PATH";
        final String path = createTestPath(nodeBin, nodeModulesBin, pathName);
        final Map<String, String> env = new HashMap<>(System.getenv());
        env.put(pathName, path);
        env.put("CYPRESS_baseUrl", format("http://localhost:%d", port));
        env.put("CYPRESS_video", Boolean.toString(video));
        env.put("CYPRESS_videosFolder", new File(basedir, "target/cypress/videos").getAbsolutePath());
        env.put("CYPRESS_screenshotsFolder", new File(basedir, "target/cypress/screenshots").getAbsolutePath());
        if (debug) {
            env.put("DEBUG", "cypress:launcher");
        }

        final CommandLine command = new CommandLine(new File(nodeBin, "node" + (isWin ? ".exe" : "")));
        command.addArgument(new File(nodeModulesBin, "cypress").getAbsolutePath());
        command.addArgument("run");
        if (browser != null) { // allows to switch the browser
            command.addArgument("--browser");
            command.addArgument(browser);
        }
        if (headed) { // allows to "see" test in the browser
            command.addArgument("--headed");
        }
        if (spec != null) { // allows to run a single test
            command.addArgument("--spec");
            command.addArgument(spec);
        }
        if (customOptions != null) {
            command.addArguments(CommandLine.parse(customOptions).getArguments());
        }

        final DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(frontendDir);
        executor.setWatchdog(new ExecuteWatchdog(timeout));
        executor.execute(command, env);
    }

    private String createTestPath(final File nodeBin, final File nodeModuleBin, final String pathName) {
        return Stream
                .concat(Stream.of(nodeModuleBin.getAbsolutePath(), nodeBin.getAbsolutePath()),
                        Stream.of(System.getenv().getOrDefault(pathName, "").split(File.pathSeparator)))
                .filter(it -> !it.isEmpty())
                .collect(joining(File.pathSeparator));
    }
}
