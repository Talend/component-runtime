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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.processor.ElementListener;
import org.talend.component.api.processor.Processor;

@Processor(name = "writer")
public class UserWriter implements Serializable {

    private final File file;

    private final FileService service;

    private transient PrintStream writer;

    public UserWriter(@Option("file") final File file, final FileService service) {
        this.file = file;
        this.service = service;
    }

    // tag::open[]
    @PostConstruct
    public void open() throws FileNotFoundException {
        writer = service.createOutput(file);
    }
    // end::open[]

    // tag::write[]
    @ElementListener
    public void write(final User user) throws IOException {
        writer.println(user.getId() + ";" + user.getName());
    }
    // end::write[]

    // tag::close[]
    @PreDestroy
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
    // end::close[]
}
