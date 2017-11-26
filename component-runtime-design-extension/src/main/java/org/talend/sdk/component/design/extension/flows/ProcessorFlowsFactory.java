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
package org.talend.sdk.component.design.extension.flows;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.stream.Stream;

import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.runtime.output.Branches;

import lombok.AllArgsConstructor;

/**
 * Creates flows for Processor component
 */
@AllArgsConstructor
class ProcessorFlowsFactory extends FlowsFactory {

    private final Class<?> type;

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getInputFlows() {
        return getListenerParameters() //
                .map(p -> ofNullable(p.getAnnotation(Input.class)) //
                        .map(Input::value) //
                        .orElse(Branches.DEFAULT_BRANCH)) //
                .collect(toSet()); //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getOutputFlows() {
        Method listener = getListener();
        return Stream //
                .concat( //
                        listener.getReturnType().equals(Void.TYPE) ? Stream.empty()
                                : Stream.of(Branches.DEFAULT_BRANCH), //
                        Stream
                                .of(listener.getParameters())
                                .filter(p -> p.isAnnotationPresent(Output.class)) //
                                .map(p -> p.getAnnotation(Output.class).value())) //
                .collect(toSet()); //
    }

    /**
     * Returns Processor class method annotated with {@link ElementListener}
     * 
     * @return listener method
     */
    private Method getListener() {
        return Stream
                .of(type.getMethods()) //
                .filter(m -> m.isAnnotationPresent(ElementListener.class)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException("No @ElementListener method in " + type)); //
    }

    /**
     * Returns all {@link ElementListener} method parameters, which are not
     * annotated with {@link Output}
     * 
     * @return listener method input parameters
     */
    private Stream<Parameter> getListenerParameters() {
        return Stream.of(getListener().getParameters()).filter(
                p -> p.isAnnotationPresent(Input.class) || !p.isAnnotationPresent(Output.class));
    }
}
