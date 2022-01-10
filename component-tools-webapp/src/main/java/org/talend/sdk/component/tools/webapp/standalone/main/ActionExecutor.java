/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools.webapp.standalone.main;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// IMPORTANT: ensure any logger (JUL, Logback, Log4j and Log4j2 are configured to not log anything in stdout/stderr)
@NoArgsConstructor(access = PRIVATE)
public class ActionExecutor {

    public static void main(final String[] args) {
        if (args.length != 6) {
            System.err
                    .println(
                            "Usage\n   app <component_gav> <lang> <action_family> <action_type> <action_name> <payload>");
            return;
        }

        System
                .setProperty("talend.component.manager.m2.repository",
                        System
                                .getProperty("talend.component.manager.m2.repository",
                                        System.getProperty("user.home") + "/.m2/repository"));
        System
                .setProperty("talend.component.manager.classpathcontributor.skip",
                        System.getProperty("talend.component.manager.classpathcontributor.skip", "true"));
        System
                .setProperty("talend.component.manager.localconfiguration.skip",
                        System.getProperty("talend.component.manager.localconfiguration.skip", "true"));
        System
                .setProperty("talend.component.manager.jmx.skip",
                        System.getProperty("talend.component.manager.jmx.skip", "true"));
        System.setProperty("talend.component.impl.mode", System.getProperty("talend.component.impl.mode", "UNSAFE"));

        try (final Jsonb jsonb = JsonbBuilder.create()) {
            try {
                final ComponentManager instance = ComponentManager.instance();
                final String plugin = instance.addPlugin(args[0]);
                final Container container = instance
                        .findPlugin(plugin)
                        .orElseThrow(() -> new IllegalArgumentException("plugin '" + plugin + "' incorrectly started"));

                final Properties props = new Properties();
                try (final StringReader reader = new StringReader(args[5])) {
                    props.load(reader);
                }
                final Map<String, String> runtimeParams = new HashMap<>(1 + props.size());
                runtimeParams.put("$lang", args[1]);
                runtimeParams
                        .putAll(props.stringPropertyNames().stream().collect(toMap(identity(), props::getProperty)));

                final Object result = container
                        .get(ContainerComponentRegistry.class)
                        .getServices()
                        .stream()
                        .flatMap(sm -> sm.getActions().stream())
                        .filter(act -> Objects.equals(args[2], act.getFamily())
                                && Objects.equals(args[3], act.getType()) && Objects.equals(args[4], act.getAction()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Invalid action, not found"))
                        .getInvoker()
                        .apply(runtimeParams);

                onResult(jsonb, result);
            } catch (final RuntimeException re) {
                onError(jsonb, re);
                System.exit(1);
            }
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void onError(final Jsonb jsonb, final RuntimeException re) {
        System.err
                .println(jsonb
                        .toJson(new ErrorPayload("ACTION_ERROR",
                                "Action execution failed with: " + ofNullable(re.getMessage())
                                        .orElseGet(() -> NullPointerException.class.isInstance(re) ? "unexpected null"
                                                : "no error message"))));
    }

    private static void onResult(final Jsonb jsonb, final Object result) {
        System.out.println(jsonb.toJson(result));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorPayload { // copied to fake component-server but not depend on it

        private String code;

        private String description;
    }
}
