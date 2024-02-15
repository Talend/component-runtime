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
package org.talend.sdk.component.design.extension.flows;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.runtime.output.Branches;

import lombok.AllArgsConstructor;

/**
 * Creates flows for Processor component
 */
@AllArgsConstructor
class ProcessorFlowsFactory implements FlowsFactory {

    private final Class<?> type;

    @Override
    public Collection<String> getInputFlows() {
        return getListener()
                .map(m -> Stream.of(m.getParameters()))
                .orElseGet(() -> getAfterGroup().map(it -> Stream.of(it.getParameters())).orElseGet(Stream::empty))
                .filter(this::isInput)
                .map(this::mapInputName)
                .collect(toList());
    }

    @Override
    public Collection<String> getOutputFlows() {
        return concat(
                getListener()
                        .map(listener -> concat(getReturnedBranches(listener), getOutputParameters(listener)))
                        .orElseGet(Stream::empty),
                getAfterGroup().map(this::getOutputParameters).orElseGet(Stream::empty)).distinct().collect(toList());
    }

    private Optional<Method> getAfterGroup() {
        return of(type.getMethods())
                .filter(m -> m.isAnnotationPresent(AfterGroup.class))
                .filter(it -> it.getParameterCount() > 0)
                .findFirst();
    }

    private Stream<String> getOutputParameters(final Method listener) {
        return of(listener.getParameters())
                .filter(p -> p.isAnnotationPresent(Output.class))
                .map(p -> p.getAnnotation(Output.class).value());
    }

    private Stream<String> getReturnedBranches(final Method listener) {
        return listener.getReturnType().equals(Void.TYPE) ? Stream.empty() : of(Branches.DEFAULT_BRANCH);
    }

    private Optional<Method> getListener() {
        return of(type.getMethods()).filter(m -> m.isAnnotationPresent(ElementListener.class)).findFirst();
    }

    private boolean isInput(final Parameter p) {
        return p.isAnnotationPresent(Input.class) || !p.isAnnotationPresent(Output.class);
    }

    private String mapInputName(final Parameter p) {
        return ofNullable(p.getAnnotation(Input.class)).map(Input::value).orElse(Branches.DEFAULT_BRANCH);
    }
}
