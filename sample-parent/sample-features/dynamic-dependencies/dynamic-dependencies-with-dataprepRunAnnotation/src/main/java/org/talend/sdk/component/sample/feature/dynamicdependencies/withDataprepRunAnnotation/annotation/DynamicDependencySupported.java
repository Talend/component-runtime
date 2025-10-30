package org.talend.sdk.component.sample.feature.dynamicdependencies.withDataprepRunAnnotation.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.configuration.type.meta.ConfigurationType;
import org.talend.sdk.component.api.meta.Documentation;

@Target(TYPE)
@Retention(RUNTIME)
@ConfigurationType("configuration")
@Documentation("Copy/past of the annotation from tDataprepRun.")
public @interface DynamicDependencySupported {
    String value() default "default";
}