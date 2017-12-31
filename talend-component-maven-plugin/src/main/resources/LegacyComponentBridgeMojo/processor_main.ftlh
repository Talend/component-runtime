<#--
  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
   Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<%@ jet
    imports="
        org.talend.core.model.process.INode
        org.talend.designer.codegen.config.CodeGeneratorArgument
        org.talend.core.model.process.ElementParameterParser
        org.talend.core.model.process.IElementParameter
        org.talend.core.model.process.EParameterFieldType
        org.talend.core.model.metadata.IMetadataColumn
        org.talend.core.model.metadata.IMetadataTable
        org.talend.core.model.process.IConnection
        org.talend.core.model.process.IConnectionCategory
        org.talend.core.model.process.EConnectionType
        java.util.List
    "
%>
<%
final CodeGeneratorArgument codeGenArgument = CodeGeneratorArgument.class.cast(argument);
final INode node = INode.class.cast(codeGenArgument.getArgument());
final String cid = node.getUniqueName();
%>

<%
for (final IConnection connection : node.getIncomingConnections(EConnectionType.FLOW_MAIN)) {
%>
final java.util.concurrent.atomic.AtomicReference<<%= connection.getName() %>Struct> <%= connection.getName() %>InputRef_<%=cid%> = new java.util.concurrent.atomic.AtomicReference<>(<%= connection.getName() %>);
<% } %>
final org.talend.sdk.component.runtime.output.InputFactory inputs_<%=cid%> = new org.talend.sdk.component.runtime.output.InputFactory() {
    private final java.util.Iterator<java.util.concurrent.atomic.AtomicReference<?>> values = new java.util.ArrayList<java.util.concurrent.atomic.AtomicReference<?>>() {{
        <%
        for (final IConnection connection : node.getIncomingConnections(EConnectionType.FLOW_MAIN)) {
        %>
        // <%= connection.getName() %> Main input
        add(<%= connection.getName() %>InputRef_<%=cid%>);
        <%
        }
        %>
    }}.iterator();

    @Override
    public Object read(final String name) {
        final Object value = values.hasNext() ? values.next().get() : null;
        if (value == null) {
            return null;
        }
        return org.talend.sdk.component.api.processor.data.ObjectMap.class.isInstance(value)?
            org.talend.sdk.component.api.processor.data.ObjectMap.class.cast(value) :
            new org.talend.sdk.component.runtime.output.data.ObjectMapImpl(
                          processor_<%=cid%>.plugin(), value,
                          accessorCache_<%=cid%>);
    }
};

// ensure we can set the row output from the anonymous classes
<%
for (final IConnection connection : node.getOutgoingSortedConnections()) {
    if (connection.getLineStyle() != EConnectionType.FLOW_MAIN) {
        continue;
    }
%>
final java.util.concurrent.atomic.AtomicReference<> <%= connection.getName() %>OutputRef_<%=cid%> = new java.util.concurrent.atomic.AtomicReference<>(<%= connection.getName() %>);
<% } %>

<%/*here the assumption is tcomp only calls create once per output which should be the case*/%>
final org.talend.sdk.component.runtime.output.OutputFactory outputs_<%=cid%> = new org.talend.sdk.component.runtime.output.OutputFactory() {
    <%/* link the studio indexed outputs to the tcomp ones which should rely on names, for now assume ordering is respected (guaranteed by getParameters() */%>
    private final java.util.Iterator<org.talend.sdk.component.api.processor.OutputEmitter> emitters = new java.util.ArrayList<org.talend.sdk.component.api.processor.OutputEmitter>() {{
        <%
        for (final IConnection connection : node.getOutgoingSortedConnections()) {
            if (connection.getLineStyle() != EConnectionType.FLOW_MAIN) {
                continue;
            }
        %>
        // <%= connection.getName() %> Main output
        add(new org.talend.sdk.component.api.processor.OutputEmitter() {
            @Override
            public void emit(final Object value) {
                final org.talend.sdk.component.api.processor.data.ObjectMap objectMap =
                    org.talend.sdk.component.api.processor.data.ObjectMap.class.isInstance(value)?
                    org.talend.sdk.component.api.processor.data.ObjectMap.class.cast(value) :
                    new org.talend.sdk.component.runtime.output.data.ObjectMapImpl(
                                  processor_<%=cid%>.plugin(), value,
                                  accessorCache_<%=cid%>)

                modelMapper_<%=cid%>.map(objectMap, <%= connection.getName() %>OutputRef_<%=cid%>.get());
            }
        });
        <%
        }
        %>
    }}.iterator();

    @Override
    public org.talend.sdk.component.api.processor.OutputEmitter create(final String name) {
        return emitters.hasNext() ? emitters.next() : new org.talend.sdk.component.api.processor.OutputEmitter() {
            @Override
            public void emit(final Object value) {
                System.err.println("Ignoring <" + value + "> since " + processor_<%=cid%>.rootName() + "#" + processor_<%=cid%>.name() + " misses an output");
            }
        };
    }
};
processor_<%=cid%>.onElement(inputs_<%=cid%>, outputs_<%=cid%>);

<%
// once processed update all outputs
for (final IConnection connection : node.getOutgoingSortedConnections()) {
    if (connection.getLineStyle() != EConnectionType.FLOW_MAIN) {
        continue;
    }
%>
<%= connection.getName() %> = <%= connection.getName() %>OutputRef_<%=cid%>.get();
<% } %>

nbLineInput_<%=cid%>++;
globalMap.put("<%=cid %>_NB_LINE", nbLineInput_<%=cid%>);
