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
package org.talend.sdk.component.form.internal.validation;

import java.util.function.Predicate;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class JavascriptRegex implements Predicate<CharSequence> {

    private final String regex;

    private final String indicators;

    public JavascriptRegex(final String regex) {
        if (regex.startsWith("/") && regex.length() > 1) {
            final int end = regex.lastIndexOf('/');
            if (end < 0) {
                this.regex = regex;
                indicators = "";
            } else {
                this.regex = regex.substring(1, end);
                indicators = regex.substring(end + 1);
            }
        } else {
            this.regex = regex;
            indicators = "";
        }
    }

    @Override
    public boolean test(final CharSequence text) {
        final String script = "new RegExp(regex, indicators).test(text)";
        final Context context = Context.enter();
        try {
            final Scriptable scope = context.initStandardObjects();
            scope.put("text", scope, text);
            scope.put("regex", scope, regex);
            scope.put("indicators", scope, indicators);
            return Context.toBoolean(context.evaluateString(scope, script, "test", 0, null));
        } catch (final Exception e) {
            return false;
        } finally {
            Context.exit();
        }
    }

    @Override
    public String toString() {
        return "JavascriptRegex{/" + regex + "/" + indicators + '}';
    }

}