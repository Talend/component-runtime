/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.xbean;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

public class KnownJarsFilter implements Predicate<String> {

    public static final Predicate<String> INSTANCE = new KnownJarsFilter();

    private final Collection<String> excludes = new HashSet<String>() {

        {
            add("tools.jar");
            add("bootstrap.jar");
            add("brave-");
            add("ApacheJMeter");
            add("XmlSchema-");
            add("activation-");
            add("activeio-");
            add("activemq-");
            add("aether-");
            add("akka-");
            add("animal-sniffer-annotations");
            add("ant-");
            add("antlr-");
            add("aopalliance-");
            add("args4j-");
            add("arquillian-core.jar");
            add("arquillian-testng.jar");
            add("arquillian-junit.jar");
            add("arquillian-transaction.jar");
            add("arquillian-common");
            add("arquillian-config-");
            add("arquillian-core-api-");
            add("arquillian-core-impl-base");
            add("arquillian-core-spi-");
            add("arquillian-container-");
            add("arquillian-junit-");
            add("arquillian-test-api");
            add("arquillian-test-impl-base");
            add("arquillian-test-spi");
            add("arquillian-tomee-");
            add("asciidoctorj-");
            add("async-http-client-");
            add("asm-");
            add("avalon-framework-");
            add("avro-");
            add("awaitility-");
            add("axis-");
            add("axis2-");
            add("batchee-jbatch");
            add("batik-");
            add("bcprov-");
            add("beam-sdks-");
            add("beam-runners-");
            add("bootstrap.jar");
            add("bsf-");
            add("bval-core");
            add("bval-jsr");
            add("c3p0-");
            add("cassandra-driver-core");
            add("catalina-");
            add("catalina.jar");
            add("chill");
            add("cglib-");
            add("charsets.jar");
            add("cldrdata");
            add("commons-");
            add("components-api");
            add("component-api");
            add("component-spi");
            add("component-runtime");
            add("component-server");
            add("component-studio");
            add("compress-lzf");
            add("config");
            add("container-core");
            add("coverage-agent");
            add("cryptacular-");
            add("cssparser-");
            add("curator-");
            add("cxf-");
            add("cdi-");
            add("deploy.jar");
            add("deltaspike-");
            add("derby-");
            add("derbyclient-");
            add("derbynet-");
            add("dnsns");
            add("dom4j");
            add("ecj-");
            add("eclipselink-");
            add("el-api");
            add("ehcache-");
            add("error_prone_annotations");
            add("FastInfoset");
            add("freemarker-");
            add("j2objc-annotations");
            add("jackson-");
            add("jaxb-");
            add("jce.jar");
            add("jets3t");
            add("jfr.jar");
            add("jfxrt.jar");
            add("jmustache");
            add("jna-");
            add("jnr-");
            add("johnzon-");
            add("jolokia-core-");
            add("jolokia-jvm-");
            add("jolokia-serv");
            add("json-simple-");
            add("json4s-");
            add("jsr250-");
            add("fusemq-leveldb-");
            add("geronimo-");
            add("google-");
            add("gpars-");
            add("grpc-");
            add("gragent.jar");
            add("grizzly-");
            add("groovy-");
            add("gson-");
            add("guava-");
            add("guice-");
            add("h2-");
            add("hadoop-");
            add("hamcrest-");
            add("hawtbuf-");
            add("hawtdispatch-");
            add("hawtio-");
            add("hawtjni-runtime");
            add("hibernate-");
            add("howl-");
            add("hsqldb-");
            add("htmlunit-");
            add("httpclient-");
            add("httpcore-");
            add("iban4j-");
            add("icu4j-");
            add("idb-");
            add("idea_rt.jar");
            add("instrumentation-api");
            add("istack-commons-runtime-");
            add("ivy-");
            add("jaccess");
            add("jackson-");
            add("janino-");
            add("jansi-");
            add("jasper.jar");
            add("jasper-el.jar");
            add("jasypt-");
            add("java-atk-wrapper");
            add("java-support-");
            add("javaee-");
            add("javaee-api");
            add("javassist-");
            add("javaws.jar");
            add("javax.");
            add("jaxb-");
            add("jaxp-");
            add("jbake-");
            add("jboss-");
            add("jbossall-");
            add("jbosscx-");
            add("jbossjts-");
            add("jbosssx-");
            add("jcl-over-slf4j-");
            add("jcommander-");
            add("jersey-");
            add("jettison-");
            add("jetty-");
            add("jfairy");
            add("jfxswt");
            add("jline");
            add("jmdns-");
            add("joda-time-");
            add("johnzon-");
            add("jruby-");
            add("jsoup-");
            add("jsonb-api");
            add("json-io");
            add("jsp-api");
            add("jsr166");
            add("jsr299-");
            add("jsr305-");
            add("jsr311-");
            add("jsse.jar");
            add("jul-to-slf4j-");
            add("juli-");
            add("junit-");
            add("junit5-");
            add("gmbal");
            add("kahadb-");
            add("kotlin-runtime");
            add("kryo");
            add("leveldb");
            add("localedata");
            add("log4j-");
            add("logkit-");
            add("lombok");
            add("lucene-analyzers-");
            add("lucene-core-");
            add("lz4");
            add("management-agent.jar");
            add("management-api-");
            add("mapstruct-");
            add("maven-");
            add("mbean-annotation-api-");
            add("meecrowave-");
            add("mesos-");
            add("metrics-");
            add("mimepull-");
            add("mina-");
            add("minlog");
            add("mqtt-client-");
            add("multiverse-core-");
            add("myfaces-api");
            add("myfaces-impl");
            add("mysql-connector-java-");
            add("nashorn");
            add("neethi-");
            add("nekohtml-");
            add("netty-");
            add("objenesis-");
            add("openjpa-");
            add("openmdx-");
            add("opensaml-");
            add("openwebbeans-");
            add("openws-");
            add("ops4j-");
            add("org.eclipse.");
            add("org.junit.");
            add("org.apache.aries.blueprint.noosgi");
            add("org.apache.aries.blueprint.web");
            add("org.osgi.core-");
            add("org.osgi.enterprise");
            add("orient-commons-");
            add("orientdb-core-");
            add("orientdb-nativeos-");
            add("oro-");
            add("paranamer");
            add("pax-url");
            add("PDFBox");
            add("plexus-");
            add("plugin.jar");
            add("poi-");
            add("protobuf-");
            add("py4j-");
            add("pyrolite-");
            add("quartz-2");
            add("qdox-");
            add("quartz-openejb-");
            add("reflectasm-");
            add("resources.jar");
            add("rmock-");
            add("RoaringBitmap-");
            add("rt.jar");
            add("routines");
            add("saaj-");
            add("sac-");
            add("scala");
            add("scalap");
            add("scalatest");
            add("scannotation-");
            add("serializer-");
            add("serp-");
            add("servlet-api-");
            add("sisu-inject");
            add("sisu-guice");
            add("shrinkwrap-");
            add("slf4j-");
            add("smack-");
            add("smackx-");
            add("snakeyaml-");
            add("snappy-");
            add("spark-");
            add("spring-");
            add("sshd-");
            add("stax-api-");
            add("stax2-api-");
            add("stream");
            add("sunec.jar");
            add("sunjce_provider");
            add("sunpkcs11");
            add("surefire-");
            add("swagger-");
            add("swizzle-");
            add("sxc-");
            add("tachyon-");
            add("talend-icon");
            add("testng-");
            add("tomcat-annotations");
            add("tomcat-api");
            add("tomcat-catalina");
            add("tomcat-coyote");
            add("tomcat-dbcp");
            add("tomcat-el");
            add("tomcat-i18n");
            add("tomcat-jasper");
            add("tomcat-jaspic");
            add("tomcat-jdbc");
            add("tomcat-jni");
            add("tomcat-jsp");
            add("tomcat-juli");
            add("tomcat-tribes");
            add("tomcat-servlet");
            add("tomcat-spdy");
            add("tomcat-util");
            add("tomcat-websocket");
            add("tomee-");
            add("tools.jar");
            add("twitter4j-");
            add("uncommons");
            add("unused");
            add("validation-api-");
            add("velocity-");
            add("wagon-");
            add("webbeans-ee");
            add("webbeans-ejb");
            add("webbeans-impl");
            add("webbeans-spi");
            add("websocket-api");
            add("woodstox-core-asl-");
            add("ws-commons-util-");
            add("wsdl4j-");
            add("wss4j-");
            add("wstx-asl-");
            add("xalan-");
            add("xbean-");
            add("xercesImpl-");
            add("xml-apis-");
            add("xml-resolver-");
            add("xmlbeans-");
            add("xmlenc-");
            add("xmlgraphics-");
            add("xmlpull-");
            add("xmlrpc-");
            add("xmlschema-");
            add("xmlsec-");
            add("xmltooling-");
            add("xmlunit-");
            add("xz-");
            add("xstream-");
            add("zipfs.jar");
            add("zipkin-");
            add("ziplock-");
            add("zookeeper-");
        }
    };

    @Override
    public boolean test(final String jarName) {
        return excludes.stream().noneMatch(jarName::startsWith);
    }
}
