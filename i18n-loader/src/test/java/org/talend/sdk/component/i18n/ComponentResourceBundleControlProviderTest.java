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
package org.talend.sdk.component.i18n;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;

public class ComponentResourceBundleControlProviderTest {

    @Rule
    public final TestRule customProvider = new ExternalResource() {

        private Runnable callback;

        @Override
        protected void before() throws Throwable {
            callback = ProviderLocator.instance().register(Thread.currentThread().getContextClassLoader(),
                    new BaseProvider() {

                        @Override
                        protected ResourceBundle createBundle(final String baseName, final Locale locale) {
                            return new BaseBundle() {

                                private int incr = 1;

                                @Override
                                protected Set<String> doGetKeys() {
                                    return Collections.singleton("thekey");
                                }

                                @Override
                                protected Object handleGetObject(final String key) {
                                    return "thekey".equals(key) ? "thevalue" + incr++ : null;
                                }
                            };
                        }

                        @Override
                        protected boolean supports(final String baseName) {
                            return "test".equals(baseName);
                        }
                    });
        }

        @Override
        protected void after() {
            callback.run();
        }
    };

    @Test
    public void useCustomProvider() {
        for (int i = 0; i < 10; i++) {
            final ResourceBundle bundle = ResourceBundle.getBundle("test", Locale.ENGLISH);
            assertNotNull(bundle);
            assertEquals("thevalue" + (i + 1), bundle.getString("thekey"));
        }
    }
}
