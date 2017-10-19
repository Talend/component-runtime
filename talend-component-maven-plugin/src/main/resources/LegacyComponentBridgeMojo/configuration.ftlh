final java.util.Map<String, String> configuration_<%=cid%> = new java.util.HashMap<>();
<%
    final java.util.Collection<String> configurationKeys = new java.util.HashSet<String>();
    <#list configurationKeys as key>
    configurationKeys.add("${key}");
    </#list>

    final List<IElementParameter> params = List.class.cast(node.getElementParametersWithChildrens());
    if (params != null && !params.isEmpty()) {
        for (final IElementParameter p : params) {
            if (!configurationKeys.contains(p.getName())) {
                continue;
            }
%>
            configuration_<%=cid%>.put(
                "<%=p.getName().replace('$', '.')%>",
                "<%=p.getFieldType() == EParameterFieldType.PASSWORD ? ElementParameterParser.getPasswordValue(node, p.getVariableName()) : ElementParameterParser.getStringElementParameterValue(p)%>");
<%
        }
    }
%>
