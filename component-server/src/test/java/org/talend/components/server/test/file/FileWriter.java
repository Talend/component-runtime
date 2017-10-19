package org.talend.components.server.test.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.processor.AfterGroup;
import org.talend.component.api.processor.ElementListener;
import org.talend.component.api.processor.Processor;

@Processor(family = "file", name = "output")
public class FileWriter implements Serializable {
    private final File file;
    private transient BufferedWriter writer;

    public FileWriter(@Option("file") final File path) {
        this.file = path;
    }

    @PostConstruct
    public void open() throws IOException {
        writer = new BufferedWriter(new java.io.FileWriter(file));
    }

    @ElementListener
    public void onElement(final Object data) throws IOException {
        writer.write(data.toString() + "\n");
    }

    @AfterGroup
    public void flush() throws IOException {
        writer.flush(); // we are buffered so we need to flush from time to time
    }

    @PreDestroy
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
