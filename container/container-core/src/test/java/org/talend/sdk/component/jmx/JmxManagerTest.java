/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.jmx;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Date;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.test.Constants;

public class JmxManagerTest {

    @Test
    public void jmx() throws Exception {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final JmxManager manager = new JmxManager("org.talend.test:type=plugin,name=%s", mBeanServer);
        final Container container = new Container("foo.jar", new File("missing/normally").getName(), new String[0],
            ContainerManager.ClassLoaderConfiguration.builder().create(),
            path -> new File(Constants.DEPENDENCIES_LOCATION, path));
        manager.onCreate(container);

        final ObjectName name = new ObjectName("org.talend.test:name=foo.jar,type=plugin");

        try {
            assertTrue(mBeanServer.isRegistered(name));
            assertFalse(Boolean.class.cast(mBeanServer.getAttribute(name, "closed")));

            final Object created = mBeanServer.getAttribute(name, "created");
            assertThat(created, instanceOf(Date.class));
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
