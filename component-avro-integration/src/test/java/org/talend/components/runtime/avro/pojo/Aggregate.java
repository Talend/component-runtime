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
package org.talend.components.runtime.avro.pojo;

import java.util.List;

import lombok.Data;

class Root {
}

@Data
public class Aggregate extends Root {

    private String name;

    private ListWrapper wrapper;

    private List<String> strings1;

    private List<String> strings2;

    @Data
    public static class ListWrapper {

        private List<Root> records;
    }
}
