// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.server.service;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.components.runtime.manager.ParameterMeta;
import org.talend.components.runtime.manager.reflect.parameterenricher.ValidationParameterEnricher;
import org.talend.components.server.front.model.PropertyValidation;
import org.talend.components.server.front.model.SimplePropertyDefinition;

@ApplicationScoped
public class PropertiesService {

    @Inject
    private PropertyValidationService propertyValidationService;

    public Stream<SimplePropertyDefinition> buildProperties(final Collection<ParameterMeta> meta, final ClassLoader loader,
            final Locale locale) {
        return meta.stream().flatMap(p -> {
            final String path = sanitizePropertyName(p.getPath());
            final String name = sanitizePropertyName(p.getName());
            final String type = p.getType().name();
            final PropertyValidation validation = propertyValidationService.map(p.getMetadata());
            if (p.getType() == ParameterMeta.Type.ENUM) {
                validation.setEnumValues(p.getProposals());
            }
            final Map<String, String> metadata = ofNullable(p.getMetadata())
                    .map(m -> m.entrySet().stream().filter(e -> !e.getKey().startsWith(ValidationParameterEnricher.META_PREFIX))
                            .collect(toMap(e -> e.getKey().replace("tcomp::", ""), Map.Entry::getValue)))
                    .orElse(null);
            return Stream.concat(Stream.of(new SimplePropertyDefinition(path, name,
                    p.findBundle(loader, locale).displayName().orElse(name), type, validation, metadata)),
                    buildProperties(p.getNestedParameters(), loader, locale));
        }).sorted(Comparator.comparing(SimplePropertyDefinition::getPath)); // important cause it is the way you want to see it
    }

    private String sanitizePropertyName(final String path) {
        return path.replace("${index}", "");
    }
}
