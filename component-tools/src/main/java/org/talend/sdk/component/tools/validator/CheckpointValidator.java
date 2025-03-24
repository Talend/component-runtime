/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools.validator;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.empty;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.checkpoint.CheckpointAvailable;
import org.talend.sdk.component.api.input.checkpoint.CheckpointData;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

public class CheckpointValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    public CheckpointValidator(final Validators.ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        // get all the classes annotated with @CheckpointData and or @CheckpointAvailable
        final Map<Class<?>, Collection<ParameterMeta>> checkpointingInputs = components
                .stream()
                .filter(this::isSource)
                .filter(this::hasAnyMethod)
                .collect(toMap(identity(), helper::buildOrGetParameters));
        // check if the input implements needed methods
        final Stream<String> missingMethods = checkpointingInputs.keySet()
                .stream()
                .filter(c -> !hasNeededMethods(c))
                .map(c -> "Missing checkpoint method (@CheckpointData or @CheckpointAvailable) for " + c.getName()
                        + ".");
        // check if the checkpoint classes are used in the checkpointing inputs
        final Stream<String> inputsWithoutCheckpoint = checkpointingInputs
                .entrySet()
                .stream()
                // .map(it -> flatten(it.getValue()))
                .filter(it -> flatten(it.getValue()).noneMatch((ParameterMeta prop) -> "checkpoint"
                        .equals(prop.getMetadata().get("tcomp::configurationtype::type"))))
                .map(it -> "The component " + it.getKey().getName()
                        + " is missing a checkpoint in its configuration (see @Checkpoint).")
                .sorted();

        return Stream
                .of(missingMethods, inputsWithoutCheckpoint)
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
    }

    private boolean isSource(final Class<?> component) {
        return component.isAnnotationPresent(PartitionMapper.class) || component.isAnnotationPresent(Emitter.class);
    }

    private boolean hasAnyMethod(final Class<?> component) {
        return Arrays.stream(component.getMethods())
                .anyMatch(m -> m.isAnnotationPresent(CheckpointData.class)
                        || m.isAnnotationPresent(CheckpointAvailable.class));
    }

    private boolean hasNeededMethods(final Class<?> component) {
        return Arrays.stream(component.getMethods()).anyMatch(m -> m.isAnnotationPresent(CheckpointData.class)) &&
                Arrays.stream(component.getMethods()).anyMatch(m -> m.isAnnotationPresent(CheckpointAvailable.class));
    }

    protected static Stream<ParameterMeta> flatten(final Collection<ParameterMeta> options) {
        return options
                .stream()
                .flatMap(it -> Stream
                        .concat(Stream.of(it),
                                it.getNestedParameters().isEmpty() ? empty() : flatten(it.getNestedParameters())));
    }

}
