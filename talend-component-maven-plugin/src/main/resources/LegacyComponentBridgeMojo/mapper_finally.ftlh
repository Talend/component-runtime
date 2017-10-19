<%@ jet
    imports="
        org.talend.core.model.process.INode
        org.talend.designer.codegen.config.CodeGeneratorArgument
    "
%>
<%
final CodeGeneratorArgument codeGenArgument = CodeGeneratorArgument.class.cast(argument);
final INode node = INode.class.cast(codeGenArgument.getArgument());
final String cid = node.getUniqueName();
%>

final org.talend.components.runtime.input.Mapper mapper_<%=cid%> = org.talend.components.runtime.input.Mapper.class.cast(globalMap.remove("mapper_<%=cid%>"));
final org.talend.components.runtime.input.Input input_<%=cid%> = org.talend.components.runtime.input.Input.class.cast(globalMap.remove("input_<%=cid%>"));

boolean swallowNextException_<%=cid%> = false;
try {
    if (input_<%=cid%> != null) {
        input_<%=cid%>.stop();
    }
} catch (final RuntimeException re) {
    swallowNextException_<%=cid%> = true;
    throw new TalendException(re, currentComponent, globalMap);
} finally {
    try {
        if (mapper_<%=cid%> != null) {
            mapper_<%=cid%>.stop();
        }
    } catch (final RuntimeException re) {
        if (!swallowNextException_<%=cid%>) {
            throw new TalendException(re, currentComponent, globalMap);
        }
    }
}
