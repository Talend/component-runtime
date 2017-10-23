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

final org.talend.component.runtime.manager.ComponentManager mgr_<%=cid%> = org.talend.component.runtime.manager.ComponentManager.instance();

<#include "configuration.ftlh">

final org.talend.component.runtime.output.data.AccessorCache accessorCache_<%=cid%> = org.talend.component.runtime.output.data.AccessorCache.class.cast(globalMap.computeIfAbsent("__TCOMP_ACCESSOR_CACHE__", org.talend.component.runtime.output.data.AccessorCache::new));
final org.talend.component.runtime.di.ModelMapper modelMapper_<%=cid%> = new org.talend.component.runtime.di.ModelMapper();

final String chunkSizeStr_<%=cid%> = "<%=ElementParameterParser.getStringElementParameterValue(node.getElementParameter("CHUNK_SIZE"))%>";
int chunkSize_<%=cid%> = chunkSizeStr_<%=cid%> == null || chunkSizeStr_<%=cid%>.trim().isEmpty() ? 100 : Integer.parseInt(chunkSizeStr_<%=cid%>.trim());

final org.talend.component.runtime.output.Processor processorImpl_<%=cid%> = mgr_<%=cid%>.findProcessor("${processor.parent.name}", "${processor.name}", ${processor.version}, configuration_<%=cid%>)
                                                                             .orElseThrow(() -> new IllegalArgumentException("Can't find ${processor.parent.name}#${processor.name}"));
final org.talend.component.runtime.di.AutoChunkProcessor processor_<%=cid%> = new org.talend.component.runtime.di.AutoChunkProcessor(chunkSize_<%=cid%>, processorImpl_<%=cid%>);

processor_<%=cid%>.start();
globalMap.put("processor_<%=cid%>", processor_<%=cid%>);

int nbLineInput_<%=cid%> = 0;
