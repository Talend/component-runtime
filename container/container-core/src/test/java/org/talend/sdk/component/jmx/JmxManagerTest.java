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
package org.talend.sdk.component.jmx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Date;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.test.Constants;

class JmxManagerTest {

    @Test
    void jmx() throws Exception {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final JmxManager manager = new JmxManager("org.talend.test:type=plugin,name=%s", mBeanServer);
        final Container container = new Container("foo.jar", new File("missing/normally").getName(), new Artifact[0],
                ContainerManager.ClassLoaderConfiguration.builder().create(),
                path -> new File(Constants.DEPENDENCIES_LOCATION, path), null);
        manager.onCreate(container);

        final ObjectName name = new ObjectName("org.talend.test:name=foo.jar,type=plugin");

        try {
            assertTrue(mBeanServer.isRegistered(name));
            assertFalse(Boolean.class.cast(mBeanServer.getAttribute(name, "closed")));

            final Object created = mBeanServer.getAttribute(name, "created");
            assertTrue(Date.class.isInstance(created));
            // ensure date is stable until reloading
            assertEquals(created, created);

            mBeanServer.invoke(name, "reload", new Object[0], new String[0]);
            assertNotSame(created, mBeanServer.getAttribute(name, "created"));
        } finally {
            manager.onClose(container);
        }

        assertFalse(mBeanServer.isRegistered(name));
    }
}
