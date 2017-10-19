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

final org.talend.components.runtime.output.data.AccessorCache accessorCache_<%=cid%> = org.talend.components.runtime.output.data.AccessorCache.class.cast(globalMap.computeIfAbsent("__TCOMP_ACCESSOR_CACHE__", org.talend.components.runtime.output.data.AccessorCache::new));
final org.talend.components.runtime.di.ModelMapper modelMapper_<%=cid%> = new org.talend.components.runtime.di.ModelMapper();

final String chunkSizeStr_<%=cid%> = "<%=ElementParameterParser.getStringElementParameterValue(node.getElementParameter("CHUNK_SIZE"))%>";
int chunkSize_<%=cid%> = chunkSizeStr_<%=cid%> == null || chunkSizeStr_<%=cid%>.trim().isEmpty() ? 100 : Integer.parseInt(chunkSizeStr_<%=cid%>.trim());

final org.talend.components.runtime.output.Processor processorImpl_<%=cid%> = mgr_<%=cid%>.findProcessor("${processor.parent.name}", "${processor.name}", ${processor.version}, configuration_<%=cid%>)
                                                                             .orElseThrow(() -> new IllegalArgumentException("Can't find ${processor.parent.name}#${processor.name}"));
final org.talend.components.runtime.di.AutoChunkProcessor processor_<%=cid%> = new org.talend.components.runtime.di.AutoChunkProcessor(chunkSize_<%=cid%>, processorImpl_<%=cid%>);

processor_<%=cid%>.start();
globalMap.put("processor_<%=cid%>", processor_<%=cid%>);

int nbLineInput_<%=cid%> = 0;
