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
package org.talend.sdk.component.runtime.manager.xbean;

import java.util.HashSet;
import java.util.Set;

import org.apache.xbean.finder.filter.Filter;

import lombok.Getter;

public class KnownClassesFilter implements Filter { // one easy and efficient solution for fatjars

    public static final Filter INSTANCE = new KnownClassesFilter();

    @Getter
    private final Filter delegateSkip;

    private KnownClassesFilter() {
        final Set<String> excluded = new HashSet<>();
        excluded.add("avro.shaded");
        excluded.add("com.codehale.metrics");
        excluded.add("com.ctc.wstx");
        excluded.add("com.datastax.driver");
        excluded.add("com.fasterxml.jackson");
        excluded.add("com.google.common");
        excluded.add("com.google.thirdparty");
        excluded.add("com.ibm.wsdl");
        excluded.add("com.jcraft.jsch");
        excluded.add("com.kenai");
        excluded.add("com.sun.istack");
        excluded.add("com.sun.xml");
        excluded.add("com.talend.shaded");
        excluded.add("com.thoughtworks");
        excluded.add("io.jsonwebtoken");
        excluded.add("io.netty");
        excluded.add("io.swagger");
        excluded.add("javax");
        excluded.add("jnr");
        excluded.add("junit");
        excluded.add("net.sf.ehcache");
        excluded.add("net.shibboleth");
        excluded.add("org.aeonbits.owner");
        excluded.add("org.apache");
        excluded.add("org.bouncycastle");
        excluded.add("org.codehaus");
        excluded.add("org.cryptacular");
        excluded.add("org.eclipse");
        excluded.add("org.fusesource");
        excluded.add("org.h2");
        excluded.add("org.hamcrest");
        excluded.add("org.hsqldb");
        excluded.add("org.jasypt");
        excluded.add("org.jboss");
        excluded.add("org.joda");
        excluded.add("org.jose4j");
        excluded.add("org.junit");
        excluded.add("org.jvnet");
        excluded.add("org.metatype");
        excluded.add("org.objectweb");
        excluded.add("org.openejb");
        excluded.add("org.opensaml");
        excluded.add("org.slf4j");
        excluded.add("org.swizzle");
        excluded.add("org.terracotta");
        excluded.add("org.tukaani");
        excluded.add("org.yaml");
        excluded.add("serp");

        delegateSkip = new OptimizedExclusionFilter(excluded);
    }

    @Override
    public boolean accept(final String name) {
        return !delegateSkip.accept(name);

    }

    public static class OptimizedExclusionFilter implements Filter {

        @Getter
        private final Set<String> included;

        private OptimizedExclusionFilter(final Set<String> exclusions) {
            included = exclusions;
        }

        @Override
        public boolean accept(final String name) {
            int dot = name.indexOf('.');
            while (dot > 0) {
                if (included.contains(name.substring(0, dot))) {
                    return true;
                }
                dot = name.indexOf('.', dot + 1);
            }
            return included.contains(name);
        }
    }
}
