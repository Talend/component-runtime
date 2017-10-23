<#--
  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

final org.talend.components.runtime.manager.ComponentManager mgr_<%=cid%> = org.talend.components.runtime.manager.ComponentManager.instance();

<#include "configuration.ftlh">

final org.talend.components.runtime.input.Mapper mapper_<%=cid%> = mgr_<%=cid%>.findMapper("${mapper.parent.name}", "${mapper.name}", ${mapper.version}, configuration_<%=cid%>)
    .orElseThrow(() -> new IllegalArgumentException("Can't find ${mapper.parent.name}#${mapper.name}"));

int nbLineInput_<%=cid%> = 0;
mapper_<%=cid%>.start();
globalMap.put("mapper_<%=cid%>", mapper_<%=cid%>);

final org.talend.components.runtime.input.Input input_<%=cid%> = mapper_<%=cid%>.create();
input_<%=cid%>.start();
globalMap.put("input_<%=cid%>", mapper_<%=cid%>);

<%
final List<? extends IConnection> connections = node.getOutgoingConnections(EConnectionType.FLOW_MAIN);
if (connections.isEmpty()) {
    throw new RuntimeException("Missing output connection for component ${mapper.parent.name}#${mapper.name}");
}
final String output = connections.iterator().next().getName();
%>

final org.talend.components.runtime.output.data.AccessorCache accessorCache_<%=cid%> = org.talend.components.runtime.output.data.AccessorCache.class.cast(globalMap.computeIfAbsent("__TCOMP_ACCESSOR_CACHE__", org.talend.components.runtime.output.data.AccessorCache::new));
final org.talend.components.runtime.di.ModelMapper modelMapper_<%=cid%> = new org.talend.components.runtime.di.ModelMapper();

Object data_<%=cid%>;
while ((data_<%=cid%> = input_<%=cid%>.next()) != null) {
    nbLineInput_<%=cid%>++;
    globalMap.put("<%=cid %>_NB_LINE", nbLineInput_<%=cid%>);

    // map data_<%=cid%> to the associated structure using the generic object map accessor
    final org.talend.component.api.processor.data.ObjectMap objectMap_<%=cid%> =
        new org.talend.components.runtime.output.data.ObjectMapImpl(
            mapper_<%=cid%>.plugin(), data_<%=cid%>,
            accessorCache_<%=cid%>);
    modelMapper_<%=cid%>.map(objectMap_<%=cid%>, <%=output%>);
