/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.service.metrics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.json.bind.annotation.JsonbTransient;

import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.apache.meecrowave.Meecrowave;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.hyperic.jni.ArchLoaderException;
import org.hyperic.jni.ArchNotSupportedException;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarLoader;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;

import lombok.extern.slf4j.Slf4j;

// classloading indirection to not require microprofile-metrics
// todo: move it to geronimo-metrics
@Slf4j
@Vetoed
public class MetricsRegistrar {

    private SigarProxy sigar;

    private Sigar sigarImpl;

    private MetricRegistry registry;

    private Meecrowave meecrowave;

    public void start() {
        registry = CDI.current().select(MetricRegistry.class, new BaseRegistryLiteral()).get();
        meecrowave = CDI.current().select(Meecrowave.class).get();

        registerServerMetrics();

        if (!ensureSigarIsLoaded()) {
            return;
        }

        sigarImpl = new Sigar();
        sigar = SigarProxyCache.newInstance(sigarImpl, SigarProxyCache.EXPIRE_DEFAULT);
        registerMachineMetrics();
    }

    private boolean ensureSigarIsLoaded() {
        final SigarLoader loader = new SigarLoader(Sigar.class);
        try {
            final String systemProp = loader.getPackageName() + ".path";
            final String path = System.getProperty(systemProp);
            if (path == null) {
                final String libraryName = loader.getLibraryName();
                final File output = new File(meecrowave.getConfiguration().getTempDir(), "sigar/" + libraryName);
                if (!output.exists()) {
                    final int dot = libraryName.lastIndexOf('.');
                    final String resourceName = libraryName.substring(0, dot) + "-"
                            + System.getProperty("sigar.version", "1.6.4") + libraryName.substring(dot);
                    try (final InputStream stream =
                            Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
                        if (stream != null) {
                            output.getParentFile().mkdirs();
                            Files.copy(stream, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (final IOException e) {
                        log.error(e.getMessage());
                        return false;
                    }
                }
                try {
                    loader.load(output.exists() ? output.getParentFile().getAbsolutePath() : null);
                    System.setProperty(systemProp, "-");
                } catch (final ArchLoaderException e) {
                    log.error(e.getMessage());
                    return false;
                }
            } else if (!"-".equals(path)) {
                try {
                    loader.load(path);
                    System.setProperty(systemProp, "-");
                } catch (final ArchLoaderException e) {
                    log.error(e.getMessage());
                    return false;
                }
            }
        } catch (final ArchNotSupportedException e) {
            log.debug(e.getMessage());
            return false;
        }
        return true;
    }

    private void registerServerMetrics() {
        Stream
                .of(meecrowave.getTomcat().getServer().findServices())
                .flatMap(Stream::of)
                .map(Service::findConnectors)
                .flatMap(Stream::of)
                .map(Connector::getProtocolHandler)
                .filter(AbstractProtocol.class::isInstance)
                .map(AbstractProtocol.class::cast)
                .forEach(protocol -> {
                    final Executor executor = protocol.getExecutor();
                    final String prefix = "server.executor.port_" + protocol.getPort() + ".";
                    if (java.util.concurrent.ThreadPoolExecutor.class.isInstance(executor)) {
                        final java.util.concurrent.ThreadPoolExecutor pool =
                                java.util.concurrent.ThreadPoolExecutor.class.cast(executor);
                        addGauge(prefix + "queue.size", "Connector Active Count", () -> pool.getQueue().size());
                        addGauge(prefix + "active", "Connector Active Count", pool::getActiveCount);
                        addGauge(prefix + "tasks.completed", "Connector Completed Tasks", pool::getCompletedTaskCount);
                        addGauge(prefix + "tasks.count", "Connector Tasks Count", pool::getTaskCount);
                    }
                    if (ThreadPoolExecutor.class.isInstance(executor)) {
                        final ThreadPoolExecutor pool = ThreadPoolExecutor.class.cast(executor);
                        addGauge(prefix + "submitted", "Connector Submitted Tasks", pool::getSubmittedCount);
                    }
                });
    }

    private void registerMachineMetrics() {
        addGauge("machine.cpu.idle", "CPU idle", () -> sigar.getCpu().getIdle());
        addGauge("machine.cpu.total", "CPU total", () -> sigar.getCpu().getTotal());
        addGauge("machine.cpu.user", "CPU user", () -> sigar.getCpu().getTotal());
        addGauge("machine.mem.ram", "Memory ram", () -> sigar.getMem().getRam());
        addGauge("machine.mem.actual.free", "Memory Actual Free", () -> sigar.getMem().getActualFree());
        addGauge("machine.mem.actual.used", "Memory Actual Used", () -> sigar.getMem().getActualUsed());
        addGauge("machine.mem.used", "Memory Used", () -> sigar.getMem().getUsed());
        addGauge("machine.mem.usedPercent", "Memory Used Percent", () -> sigar.getMem().getUsedPercent());
        addGauge("machine.mem.free", "Memory Used", () -> sigar.getMem().getFree());
        addGauge("machine.mem.freePercent", "Memory Used Percent", () -> sigar.getMem().getFreePercent());
        addGauge("machine.mem.total", "Memory Total", () -> sigar.getMem().getTotal());
        addGauge("machine.net.tcp.bound", "Tcp Bound", () -> sigar.getNetStat().getTcpBound());
        addGauge("machine.net.tcp.close", "Tcp Close", () -> sigar.getNetStat().getTcpBound());
        addGauge("machine.net.tcp.closeWait", "Tcp Close Wait", () -> sigar.getNetStat().getTcpCloseWait());
        addGauge("machine.net.tcp.closing", "Tcp Closing", () -> sigar.getNetStat().getTcpClosing());
        addGauge("machine.net.tcp.idle", "Tcp Idle", () -> sigar.getNetStat().getTcpIdle());
        addGauge("machine.net.tcp.established", "Tcp Established", () -> sigar.getNetStat().getTcpEstablished());
        addGauge("machine.net.tcp.inbound.total", "Tcp Inbound Total", () -> sigar.getNetStat().getTcpInboundTotal());
        addGauge("machine.net.tcp.outbound.total", "Tcp Outbound Total",
                () -> sigar.getNetStat().getTcpOutboundTotal());
        try {
            Stream.of(sigar.getFileSystemList()).forEach(fs -> {
                final String devName = fs.getDevName();
                final String baseName = "machine.net.disk." + devName.replace('/', '_').replaceFirst("^_", "") + ".";
                addGauge(baseName + "read.count", devName + " reads", () -> sigar.getDiskUsage(devName).getReads());
                addGauge(baseName + "read.bytes", devName + " reads", () -> sigar.getDiskUsage(devName).getReadBytes());
                addGauge(baseName + "write.bytes", devName + " reads", () -> sigar.getDiskUsage(devName).getWrites());
                addGauge(baseName + "write.bytes", devName + " reads",
                        () -> sigar.getDiskUsage(devName).getWriteBytes());
            });
        } catch (final SigarException e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void addGauge(final String key, final String name, final ThrowingDoubleSupplier supplier) {
        registry
                .register(new Metadata(key, name, name, MetricType.GAUGE, MetricUnits.MILLISECONDS),
                        new Gauge<Double>() {

                            @Override
                            @JsonbTransient
                            public Double getValue() {
                                try {
                                    return supplier.get();
                                } catch (final Throwable throwable) {
                                    return -1.;
                                }
                            }
                        });
    }

    public void stop() {
        sigarImpl.close();
    }

    private interface ThrowingDoubleSupplier {

        double get() throws Throwable;
    }

    private static class BaseRegistryLiteral extends AnnotationLiteral<RegistryType> implements RegistryType {

        @Override
        public MetricRegistry.Type type() {
            return MetricRegistry.Type.BASE;
        }
    }
}
