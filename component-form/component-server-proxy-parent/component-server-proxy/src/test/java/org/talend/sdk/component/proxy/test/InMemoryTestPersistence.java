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
package org.talend.sdk.component.proxy.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;

import org.talend.sdk.component.proxy.api.persistence.OnEdit;
import org.talend.sdk.component.proxy.api.persistence.OnFindById;
import org.talend.sdk.component.proxy.api.persistence.OnPersist;

import lombok.Getter;

@Getter
@ApplicationScoped
public class InMemoryTestPersistence {

    private final Collection<OnPersist> persist = new ArrayList<>();

    private final Collection<OnEdit> edit = new ArrayList<>();

    void on(@ObservesAsync final OnPersist event) {
        persist.add(event.setId(UUID.randomUUID().toString()));
    }

    void on(@ObservesAsync final OnEdit event) {
        edit.add(event);
    }

    void on(@ObservesAsync final OnFindById event) {
        final OnPersist persisted =
                persist.stream().filter(it -> it.getId().equals(event.getId())).findFirst().orElseThrow(
                        () -> new IllegalArgumentException("No persisted entries matching id #" + event.getId()));
        event.setProperties(persisted.getProperties()).setFormId(persisted.getFormId());
    }

    public void clear() {
        persist.clear();
        edit.clear();
    }
}
