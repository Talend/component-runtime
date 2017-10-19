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
package org.talend.components.runtime.manager.sample;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.talend.components.runtime.manager.ComponentManager;
import org.talend.components.runtime.manager.chain.CountingSuccessListener;
import org.talend.components.runtime.manager.chain.ExecutionChainBuilder;
import org.talend.components.runtime.manager.chain.ToleratingErrorHandler;

// here how to write a standalone program with these API
public class Main {

    private Main() {
        // no-op
    }

    public static void main(final String[] args) {
        // specific to the job
        final String jobName = "TestJob";

        // potential configuration (not mandatory in bundles)
        final File m2 = new File(System.getProperty("user.home", ".m2/repository"));

        final CountingSuccessListener successes = new CountingSuccessListener();
        final ToleratingErrorHandler errorHandler = new ToleratingErrorHandler(
                Integer.getInteger("talend.job." + jobName + ".error.max"));
        final long start = System.nanoTime();
        try (final ComponentManager manager = new ComponentManager(m2, "TALEND-INF/dependencies.txt",
                "org.talend.job:type=job,value=" + jobName + ",plugin=%s")) {
            ExecutionChainBuilder.start().withConfiguration(jobName, true)
                    .fromInput("<the input plugin>", "<the input value>", 1, new HashMap<>() /* input config */)
                    .toProcessor("<a processor plugin>", "<a processor value>", 1, new HashMap<>() /* processor config */)
                    .toProcessor(null, "<a processor/output plugin>", "<a processor/output value>", 1,
                            new HashMap<>() /* processor config */)
                    .create(manager, plugin -> new File(".plugin", plugin), successes, errorHandler).get().execute();
        } finally {
            final long end = System.nanoTime();
            System.out.println("Executed in " + TimeUnit.NANOSECONDS.toSeconds(end - start) + "s");
            System.out.println("Success: " + successes.getCurrent());
            System.out.println("Failures: " + errorHandler.getCurrent());
            System.out.println("Total Processed: " + (successes.getCurrent() + errorHandler.getCurrent()));
        }
    }
}
