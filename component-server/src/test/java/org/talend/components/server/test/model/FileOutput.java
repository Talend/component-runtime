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
package org.talend.components.server.test.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.processor.ElementListener;
import org.talend.component.api.processor.Processor;

@Processor(family = "chain", name = "file")
public class FileOutput implements Serializable {

    private final File file;

    private transient Writer writer;

    public FileOutput(@Option("file") final File file) {
        this.file = file;
    }

    @PostConstruct
    public void init() throws IOException {
        writer = new FileWriter(file);
    }

    @ElementListener
    public void length(final int data) throws IOException {
        writer.write(Integer.toString(data) + System.lineSeparator());
    }

    @PreDestroy
    public void close() throws IOException {
        if (writer == null) {
            return;
        }
        writer.close();
    }
}
