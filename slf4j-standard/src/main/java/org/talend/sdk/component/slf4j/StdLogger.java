/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.slf4j;

import static java.util.Locale.ROOT;

import java.io.PrintStream;

import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import lombok.Getter;

public class StdLogger extends MarkerIgnoringBase {

    @Getter
    private final String name;

    private final boolean trace;

    private final boolean debug;

    private final boolean info;

    private final boolean warn;

    private final boolean error;

    StdLogger(final String name) {
        this.name = name;

        final String level = System.getProperty(getClass().getName() + ".level", "info");
        switch (level.toLowerCase(ROOT)) {
        case "trace":
            trace = debug = info = warn = error = true;
            break;
        case "debug":
            trace = false;
            debug = info = warn = error = true;
            break;
        case "info":
            trace = debug = false;
            info = warn = error = true;
            break;
        case "warn":
            trace = debug = info = false;
            warn = error = true;
            break;
        case "error":
            trace = debug = info = warn = false;
            error = true;
            break;
        default:
            trace = debug = false;
            info = warn = error = true;
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return trace;
    }

    @Override
    public void trace(final String msg) {
        if (!trace) {
            return;
        }
        log("TRACE", msg, null, System.out);
    }

    @Override
    public void trace(final String format, final Object arg) {
        if (!trace) {
            return;
        }
        log("TRACE", MessageFormatter.format(format, arg).getMessage(), null, System.out);
    }

    @Override
    public void trace(final String format, final Object arg1, final Object arg2) {
        if (!trace) {
            return;
        }
        log("TRACE", MessageFormatter.format(format, arg1, arg1).getMessage(), null, System.out);
    }

    @Override
    public void trace(final String format, final Object... arguments) {
        if (!trace) {
            return;
        }
        log("TRACE", MessageFormatter.arrayFormat(format, arguments).getMessage(), null, System.out);
    }

    @Override
    public void trace(final String msg, final Throwable throwable) {
        if (!trace) {
            return;
        }
        log("TRACE", msg, throwable, System.out);
    }

    @Override
    public boolean isDebugEnabled() {
        return debug;
    }

    @Override
    public void debug(final String msg) {
        if (!debug) {
            return;
        }
        log("DEBUG", msg, null, System.out);
    }

    @Override
    public void debug(final String format, final Object arg) {
        if (!debug) {
            return;
        }
        log("DEBUG", MessageFormatter.format(format, arg).getMessage(), null, System.out);
    }

    @Override
    public void debug(final String format, final Object arg1, final Object arg2) {
        if (!debug) {
            return;
        }
        log("DEBUG", MessageFormatter.format(format, arg1, arg1).getMessage(), null, System.out);
    }

    @Override
    public void debug(final String format, final Object... arguments) {
        if (!debug) {
            return;
        }
        log("DEBUG", MessageFormatter.arrayFormat(format, arguments).getMessage(), null, System.out);
    }

    @Override
    public void debug(final String msg, final Throwable throwable) {
        if (!debug) {
            return;
        }
        log("DEBUG", msg, throwable, System.out);
    }

    @Override
    public boolean isInfoEnabled() {
        return info;
    }

    @Override
    public void info(final String msg) {
        if (!info) {
            return;
        }
        log("INFO", msg, null, System.out);
    }

    @Override
    public void info(final String format, final Object arg) {
        if (!info) {
            return;
        }
        log("INFO", MessageFormatter.format(format, arg).getMessage(), null, System.out);
    }

    @Override
    public void info(final String format, final Object arg1, final Object arg2) {
        if (!info) {
            return;
        }
        log("INFO", MessageFormatter.format(format, arg1, arg1).getMessage(), null, System.out);
    }

    @Override
    public void info(final String format, final Object... arguments) {
        if (!info) {
            return;
        }
        log("INFO", MessageFormatter.arrayFormat(format, arguments).getMessage(), null, System.out);
    }

    @Override
    public void info(final String msg, final Throwable throwable) {
        if (!info) {
            return;
        }
        log("INFO", msg, throwable, System.out);
    }

    @Override
    public boolean isWarnEnabled() {
        return warn;
    }

    @Override
    public void warn(final String msg) {
        if (!warn) {
            return;
        }
        log("WARN", msg, null, System.out);
    }

    @Override
    public void warn(final String format, final Object arg) {
        if (!warn) {
            return;
        }
        log("WARN", MessageFormatter.format(format, arg).getMessage(), null, System.out);
    }

    @Override
    public void warn(final String format, final Object arg1, final Object arg2) {
        if (!warn) {
            return;
        }
        log("WARN", MessageFormatter.format(format, arg1, arg1).getMessage(), null, System.out);
    }

    @Override
    public void warn(final String format, final Object... arguments) {
        if (!warn) {
            return;
        }
        log("WARN", MessageFormatter.arrayFormat(format, arguments).getMessage(), null, System.out);
    }

    @Override
    public void warn(final String msg, final Throwable throwable) {
        if (!warn) {
            return;
        }
        log("WARN", msg, throwable, System.out);
    }

    @Override
    public boolean isErrorEnabled() {
        return error;
    }

    @Override
    public void error(final String msg) {
        if (!error) {
            return;
        }
        log("ERROR", msg, null, System.err);
    }

    @Override
    public void error(final String format, final Object arg) {
        if (!error) {
            return;
        }
        log("ERROR", MessageFormatter.format(format, arg).getMessage(), null, System.err);
    }

    @Override
    public void error(final String format, final Object arg1, final Object arg2) {
        if (!error) {
            return;
        }
        log("ERROR", MessageFormatter.format(format, arg1, arg1).getMessage(), null, System.err);
    }

    @Override
    public void error(final String format, final Object... arguments) {
        if (!error) {
            return;
        }
        log("ERROR", MessageFormatter.arrayFormat(format, arguments).getMessage(), null, System.err);
    }

    @Override
    public void error(final String msg, final Throwable throwable) {
        if (!error) {
            return;
        }
        log("ERROR", msg, throwable, System.err);
    }

    private void log(final String level, final String message, final Throwable throwable, final PrintStream out) {
        final StringBuilder builder = new StringBuilder(message.length() + level.length() + 3)
                .append('[')
                .append(level)
                .append("] ")
                .append(message);
        out.println(builder);
        if (throwable != null) {
            throwable.printStackTrace(out);
        }
        out.flush();
    }
}
