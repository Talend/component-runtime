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
package org.talend.components.server.front.model;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyValidation { // note: we keep names specific per type to ensure we never get any overlap

    private Boolean required;

    // for numbers
    private Integer min;

    // for numbers
    private Integer max;

    // for strings
    private Integer minLength;

    // for strings
    private Integer maxLength;

    // for arrays
    private Integer minItems;

    // for arrays
    private Integer maxItems;

    // for arrays
    private Boolean uniqueItems;

    // for strings
    private String pattern; // for js use http://xregexp.com/

    // for enum
    private Collection<String> enumValues;
}
