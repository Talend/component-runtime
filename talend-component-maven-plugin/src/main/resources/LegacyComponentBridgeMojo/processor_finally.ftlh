<%@ jet
    imports="
        org.talend.core.model.process.INode
        org.talend.designer.codegen.config.CodeGeneratorArgument
    "
%>
<%
final String cid = INode.class.cast(CodeGeneratorArgument.class.cast(argument).getArgument()).getUniqueName();
%>

final org.talend.components.runtime.di.AutoChunkProcessor processor_<%=cid%> = org.talend.components.runtime.di.AutoChunkProcessor.class.cast(globalMap.remove("processor_<%=cid%>"));
try {
    if (processor_<%=cid%> != null) {
        processor_<%=cid%>.stop();
    }
} catch (final RuntimeException re) {
    throw new TalendException(re, currentComponent, globalMap);
}
