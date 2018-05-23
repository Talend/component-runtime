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
package org.talend.sdk.component.server.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.startup.Tomcat;
import org.apache.meecrowave.Meecrowave;

public class OptionalExitOnDestroyMEECROWAVE94 implements LifecycleListener, Meecrowave.InstanceCustomizer {

    @Override
    public void accept(final Tomcat tomcat) {
        tomcat.getServer().addLifecycleListener(this);
    }

    @Override
    public void lifecycleEvent(final LifecycleEvent event) {
        if (Lifecycle.AFTER_DESTROY_EVENT.equals(event.getType()) && Server.class.isInstance(event.getData())
                && Boolean.getBoolean("talend.component.exit-on-destroy")) {
            System.exit(0);
        }
    }
}
