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
package org.talend.component.server.test.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.configuration.action.Checkable;
import org.talend.component.api.configuration.constraint.Max;
import org.talend.component.api.configuration.constraint.Min;
import org.talend.component.api.configuration.constraint.Required;
import org.talend.component.api.configuration.constraint.Uniques;
import org.talend.component.api.configuration.type.DataSet;
import org.talend.component.api.input.Emitter;
import org.talend.component.api.input.Producer;

import lombok.Getter;

@Emitter(family = "chain", name = "list")
public class ListInput implements Serializable {

    private final Collection<String> list;

    private transient Iterator<String> iterator;

    public ListInput(@Option("remote") final MyDataSet dataSet, @Option("values") final List<String> list) {
        this.list = list;
    }

    @Producer
    public String data() {
        if (iterator == null) {
            iterator = list.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    @Getter
    @DataSet("thechaindataset")
    @Checkable
    public static class MyDataSet {

        @Option
        @Min(1)
        @Uniques
        private Collection<String> urls;

        @Option
        private MyUser user;
    }

    @Getter
    public static class MyUser {

        @Option
        @Min(2)
        private String user;

        @Option
        @Max(8)
        @Required
        private String password;
    }
}
