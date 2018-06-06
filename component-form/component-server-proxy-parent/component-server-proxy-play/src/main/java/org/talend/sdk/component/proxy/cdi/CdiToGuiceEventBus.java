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
package org.talend.sdk.component.proxy.cdi;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import com.google.common.eventbus.EventBus;

import org.talend.sdk.component.proxy.api.persistence.OnEdit;
import org.talend.sdk.component.proxy.api.persistence.OnFindById;
import org.talend.sdk.component.proxy.api.persistence.OnPersist;

import play.Application;

/**
 * Loose coupling on extension points for the application base don Guice events.
 */
@ApplicationScoped
public class CdiToGuiceEventBus {

    private EventBus eventBus;

    @Inject
    private Collection<Application> guiceApplication;

    void eagerInit(@Observes @Initialized(ApplicationScoped.class) final Object init) {
        eventBus = guiceApplication.stream().map(a -> {
            try {
                return a.injector().instanceOf(EventBus.class);
            } catch (final RuntimeException re) {
                return null;
            }
        }).findFirst().orElseThrow(() -> new IllegalStateException("No guice EventBus available"));
    }

    public void onPersist(@ObservesAsync final OnPersist onPersist) {
        eventBus.post(onPersist);
    }

    public void onEdit(@ObservesAsync final OnEdit onEdit) {
        eventBus.post(onEdit);
    }

    public void onFindById(@ObservesAsync final OnFindById onFindById) {
        eventBus.post(onFindById);
    }
}
