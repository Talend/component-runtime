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
package org.talend.components.runtime.avro;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.talend.component.api.service.schema.Type;
import org.talend.components.runtime.input.Input;
import org.talend.components.runtime.input.Mapper;
import org.talend.components.runtime.manager.ComponentManager;
import org.talend.components.runtime.manager.ContainerComponentRegistry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchemaDiscoverer {

    public Schema populateSchema(final String plugin, final String family, final String action, final String type,
            final String identifier, final Map<String, String> config) {

        final ComponentManager componentManager = ComponentManager.instance();
        synchronized (componentManager) {
            if (!componentManager.findPlugin(plugin).isPresent()) {
                componentManager.addPlugin(plugin);
            }
        }
        final Object result = componentManager.findPlugin(plugin)
                .orElseThrow(() -> new IllegalArgumentException("No component " + plugin)).get(ContainerComponentRegistry.class)
                .getServices().stream().flatMap(s -> s.getActions().stream())
                .filter(a -> a.getFamily().equals(family) && a.getAction().equals(action) && a.getType().equals(type)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No action " + family + "#" + type + "#" + action)).getInvoker()
                .apply(config);
        if (!org.talend.component.api.service.schema.Schema.class.isInstance(result)) {
            throw new IllegalArgumentException("Result of " + family + "#" + type + "#" + action + " is not a schema");
        }
        final org.talend.component.api.service.schema.Schema compSchema = org.talend.component.api.service.schema.Schema.class
                .cast(result);
        final List<Schema.Field> avroFields = new ArrayList<>(
                compSchema.getEntries() == null ? 0 : compSchema.getEntries().size());
        if (compSchema.getEntries() != null) {
            compSchema.getEntries().forEach(e -> { // todo: enrich schema model with actual need like nested records?
                final Type eType = e.getType();
                final Schema eSchema;
                switch (eType) {
                case INT:
                    eSchema = SchemaBuilder.builder().unionOf().nullType().and().intType().endUnion();
                    break;
                case BOOLEAN:
                    eSchema = SchemaBuilder.builder().unionOf().nullType().and().booleanType().endUnion();
                    break;
                case STRING:
                    eSchema = SchemaBuilder.builder().unionOf().nullType().and().stringType().endUnion();
                    break;
                case DOUBLE:
                    eSchema = SchemaBuilder.builder().unionOf().nullType().and().doubleType().endUnion();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type: " + eType);
                }
                avroFields.add(new Schema.Field(e.getName(), eSchema, null, (Object) null));
            });
        }
        return Schema.createRecord(identifier, null, "talend.component", false, avroFields);
    }

    // find from a mapper the first record to guess the schema
    public Schema find(final String plugin, final String family, final String component, final int version,
            final Map<String, String> config) {

        final ComponentManager instance = ComponentManager.instance();
        synchronized (instance) {
            if (!instance.findPlugin(plugin).isPresent()) {
                instance.addPlugin(plugin);
            }
        }
        final Mapper mapper = instance.findMapper(family, component, version, config)
                .orElseThrow(() -> new IllegalArgumentException("No component " + family + "#" + component));
        try {
            mapper.start();
            try {
                final Input input = mapper.create();
                input.start();
                try {
                    final Object record = input.next();
                    if (record == null) {
                        return emptySchema();
                    }
                    return new ComponentModelToIndexeredRecordConverter().map(record).getSchema();
                } finally {
                    input.stop();
                }
            } finally {
                mapper.stop();
            }
        } catch (final Exception e) {
            log.warn(e.getMessage());
            return emptySchema();
        }
    }

    private Schema emptySchema() {
        return SchemaBuilder.builder().record("empty_record").fields().endRecord();
    }
}
