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
package org.talend.sdk.component.tcketl.app.connection;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.talend.sdk.component.tcketl.app.async.AsyncRunner;
import org.talend.sdk.component.tcketl.app.async.Cancellable;
import org.talend.sdk.component.tcketl.app.session.Problems;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;

/**
 * The state of the connection settings dialog: the fields as they are typed, whether they describe a usable
 * endpoint and the outcome of testing it. Editing a field forgets the outcome of a test, which no longer says
 * anything about the new settings.
 *
 * <p>
 * Everything MUST be used on the UI thread.
 */
public final class ConnectionViewModel {

    /** The languages the component server translates its answers to; any other one is served in English. */
    public static final List<String> LANGUAGES = List.of("en", "fr", "zh_CN", "ja", "de");

    public static final List<String> SCHEMES = List.of("http", "https");

    private final ApplicationConfig current;

    private final ConnectionTester tester;

    private final AsyncRunner async;

    private final StringProperty scheme;

    private final StringProperty host;

    private final StringProperty port;

    private final StringProperty basePath;

    private final StringProperty language;

    private final StringBinding validationMessage;

    private final BooleanBinding valid;

    private final StringBinding url;

    private final ReadOnlyBooleanWrapper testing = new ReadOnlyBooleanWrapper();

    private final ReadOnlyBooleanWrapper testSucceeded = new ReadOnlyBooleanWrapper();

    private final ReadOnlyStringWrapper testMessage = new ReadOnlyStringWrapper("");

    private Cancellable test = Cancellable.NONE;

    /** @param current the settings the fields start with, also where what is not editable here is taken from */
    public ConnectionViewModel(final ApplicationConfig current, final ConnectionTester tester,
            final AsyncRunner async) {
        this.current = current;
        this.tester = tester;
        this.async = async;
        final ServerEndpoint server = current.server();
        this.scheme = new SimpleStringProperty(server.scheme());
        this.host = new SimpleStringProperty(server.host());
        this.port = new SimpleStringProperty(Integer.toString(server.port()));
        this.basePath = new SimpleStringProperty(server.basePath());
        this.language = new SimpleStringProperty(current.language());
        this.validationMessage = Bindings.createStringBinding(this::validate, scheme, host, port, basePath);
        this.valid = validationMessage.isEmpty();
        this.url = Bindings.createStringBinding(this::describeUrl, scheme, host, port, basePath);
        for (final StringProperty field : List.of(scheme, host, port, basePath, language)) {
            field.addListener((observed, before, after) -> forgetTest());
        }
    }

    public StringProperty scheme() {
        return scheme;
    }

    public StringProperty host() {
        return host;
    }

    /** The port as typed, so that a half-typed value can be shown and rejected. */
    public StringProperty port() {
        return port;
    }

    public StringProperty basePath() {
        return basePath;
    }

    public StringProperty language() {
        return language;
    }

    /** What is wrong with the settings, blank when they are usable. */
    public StringBinding validationMessage() {
        return validationMessage;
    }

    public BooleanBinding valid() {
        return valid;
    }

    /** The address the settings lead to, or what is typed of it while the settings are not valid. */
    public StringBinding url() {
        return url;
    }

    public ReadOnlyBooleanProperty testing() {
        return testing.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty testSucceeded() {
        return testSucceeded.getReadOnlyProperty();
    }

    /** The outcome of the last test, blank when there is none. */
    public ReadOnlyStringProperty testMessage() {
        return testMessage.getReadOnlyProperty();
    }

    /** Asks the server of the settings who it is. Does nothing while the settings are not valid. */
    public void test() {
        final Optional<ApplicationConfig> candidate = result();
        if (candidate.isEmpty()) {
            return;
        }
        forgetTest();
        testing.set(true);
        testMessage.set("Testing\u2026");
        test = async.submit(() -> tester.test(candidate.get()), status -> {
            testing.set(false);
            testSucceeded.set(true);
            testMessage.set(describe(status));
        }, failure -> {
            testing.set(false);
            testSucceeded.set(false);
            testMessage.set(Problems.describe(failure));
        });
    }

    /** The settings as they are now, empty when they are not valid. */
    public Optional<ApplicationConfig> result() {
        if (!valid.get()) {
            return Optional.empty();
        }
        return Optional.of(current.withServer(endpoint()).withLanguage(language.get()));
    }

    /** Abandons a test in flight, to call when the dialog goes away. */
    public void close() {
        test.cancel();
    }

    private void forgetTest() {
        test.cancel();
        test = Cancellable.NONE;
        testing.set(false);
        testSucceeded.set(false);
        testMessage.set("");
    }

    private String validate() {
        if (host.get() == null || host.get().isBlank()) {
            return "The host is required.";
        }
        if (port.get() == null || port.get().isBlank()) {
            return "The port is required.";
        }
        try {
            endpoint();
            return "";
        } catch (final NumberFormatException e) {
            return "The port must be a number between 1 and 65535.";
        } catch (final IllegalArgumentException e) {
            return capitalized(e.getMessage()) + ".";
        }
    }

    private ServerEndpoint endpoint() {
        return new ServerEndpoint(scheme.get(), host.get(), Integer.parseInt(port.get().strip()), basePath.get());
    }

    private String describeUrl() {
        try {
            return endpoint().toString();
        } catch (final RuntimeException e) {
            return "";
        }
    }

    private static String describe(final ServerStatus status) {
        final String version = status.version().isBlank() ? "" : " " + status.version();
        final String commit = status.commit().isBlank() ? "" : " (commit " + status.commit() + ")";
        return "Connected to component server" + version + commit;
    }

    private static String capitalized(final String message) {
        return message.isEmpty() ? message : Character.toUpperCase(message.charAt(0)) + message.substring(1);
    }
}
