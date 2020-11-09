package org.talend.sdk.component.tools.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.runtime.manager.reflect.IconFinder;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class MetadataValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    public MetadataValidator(ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(AnnotationFinder finder, List<Class<?>> components) {
        return components.stream().flatMap(component -> {
            final Stream<String> iconErrors = this.findIconsError(component);

            final Stream<String> versionErrors = this.findVersionsError(component);

            return Stream.concat(iconErrors, versionErrors);
        }).filter(Objects::nonNull).sorted();
    }

    private Stream<String> findIconsError(Class<?> component) {
        final IconFinder iconFinder = new IconFinder();
        if (iconFinder.findDirectIcon(component).isPresent()) {
            final Icon icon = component.getAnnotation(Icon.class);
            final List<String> messages = new ArrayList<>();
            messages.add(helper.validateIcon(icon, messages));
            return messages.stream();
        } else if (!iconFinder.findIndirectIcon(component).isPresent()) {
            return Stream.of("No @Icon on " + component);
        }
        return Stream.empty();
    }

    private Stream<String> findVersionsError(Class<?> component) {
        if (!component.isAnnotationPresent(Version.class)) {
            return Stream.of("Component " + component + " should use @Icon and @Version");
        }
        return Stream.empty();
    }
}
