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
package org.talend.components.sample;

import java.io.IOException;
import java.io.Serializable;

import org.talend.component.api.processor.ElementListener;
import org.talend.component.api.processor.Processor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Processor(name = "mapper")
public class PersonToUserMapper implements Serializable {

    // tag::map[]
    @ElementListener
    public User map(final Person person) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Handling {}", person);
        }
        return new User(generateId(person), person.getName());
    }
    // end::map[]

    private String generateId(final Person person) {
        // make it look like an AD convention for the sample
        return "a" + person.getAge() + person.getName();
    }
}
