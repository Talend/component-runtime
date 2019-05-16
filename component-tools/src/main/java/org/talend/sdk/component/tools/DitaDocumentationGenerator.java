/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools;

import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DitaDocumentationGenerator extends DocBaseGenerator {

    private final boolean ignoreType;

    private final boolean ignoreFullPath;

    public DitaDocumentationGenerator(final File[] classes, final Locale locale, final Object log, final File output,
            final boolean ignoreType, final boolean ignoreFullPath) {
        super(classes, locale, log, output);
        this.ignoreType = ignoreType;
        this.ignoreFullPath = ignoreFullPath;
    }

    @Override
    public void run() {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final DocumentBuilderFactory builderFactory = newDocFactory();
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(); // dont write if it fails later
        final Collection<String> directories = new HashSet<>();
        try (final ZipOutputStream zip = new ZipOutputStream(buffer)) {
            components().forEach(it -> {
                try {
                    addDita(it, builderFactory, transformerFactory, zip, directories);
                } catch (final ParserConfigurationException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        ensureParentExists(output);
        try (final OutputStream out = Files
                .newOutputStream(output.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
            out.write(buffer.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        log.info("Generated " + output.getAbsolutePath());
    }

    private void addDita(final ComponentDescription componentDescription, final DocumentBuilderFactory factory,
            final TransformerFactory transformerFactory, final ZipOutputStream zip,
            final Collection<String> directories) throws ParserConfigurationException {
        final String family = componentDescription.getFamily();
        final String name = componentDescription.getName();

        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document xml = builder.newDocument();

        final Element topic = xml.createElement("topic");
        topic.setAttribute("id", "connector-" + family + '-' + name);
        topic.setAttribute("xml:lang", ofNullable(getLocale().getLanguage()).filter(it -> !it.isEmpty()).orElse("en"));
        xml.appendChild(topic);

        final Element title = xml.createElement("title");
        title.setTextContent(name + " parameters");
        topic.appendChild(title);

        final Element shortdesc = xml.createElement("shortdesc");
        shortdesc.setTextContent(componentDescription.getDocumentation().trim());
        topic.appendChild(shortdesc);

        final Element prolog = xml.createElement("prolog");
        final Element metadata = xml.createElement("metadata");
        final Element othermeta = xml.createElement("othermeta");
        othermeta.setAttribute("content", family);
        othermeta.setAttribute("name", "pageid");
        metadata.appendChild(othermeta);
        prolog.appendChild(metadata);
        topic.appendChild(prolog);

        final Element body = xml.createElement("body");
        body.setAttribute("outputclass", "subscription");

        final Element section = xml.createElement("section");
        section.setAttribute("id", "section_" + topic.getAttribute("id"));
        section.setAttribute("outputclass", "subscription");
        body.appendChild(section);

        final Element sectionTitle = xml.createElement("title");
        sectionTitle.setTextContent("Parameters for " + family + " " + name + " component.");
        section.appendChild(sectionTitle);

        if (!componentDescription.getParameters().isEmpty()) {
            final int columnNumber = 5 + 1 - Stream.of(ignoreType, ignoreFullPath).mapToInt(it -> it ? 1 : 0).sum();

            final Element table = xml.createElement("table");
            table.setAttribute("colsep", "1");
            table.setAttribute("frame", "all");
            table.setAttribute("rowsep", "1");

            final Element tgroup = xml.createElement("tgroup");
            tgroup.setAttribute("cols", Integer.toString(columnNumber));
            table.appendChild(tgroup);

            IntStream.rangeClosed(1, columnNumber).forEach(col -> {
                final Element colspec = xml.createElement("colspec");
                colspec.setAttribute("colname", "c" + col);
                colspec.setAttribute("colnum", Integer.toString(col));
                colspec.setAttribute("colwidth", "1*");
                tgroup.appendChild(colspec);
            });

            final Element configurationHead = xml.createElement("thead");
            final Element headRow = xml.createElement("row");
            appendColumn(xml, headRow, "Display Name");
            appendColumn(xml, headRow, "Description");
            appendColumn(xml, headRow, "Default Value");
            appendColumn(xml, headRow, "Enabled If");
            if (!ignoreFullPath) {
                appendColumn(xml, headRow, "Path");
            }
            if (!ignoreType) {
                appendColumn(xml, headRow, "Type");
            }
            configurationHead.appendChild(headRow);
            tgroup.appendChild(configurationHead);

            final Element configurationBody = xml.createElement("tbody");
            componentDescription.parameters().forEach(param -> {
                final Element row = xml.createElement("row");
                appendColumn(xml, row, param.getDisplayName());
                appendColumn(xml, row, param.getDocumentation());
                appendColumn(xml, row, param.getDefaultValue());
                {
                    final Element column = xml.createElement("entry");
                    renderConditions(xml, column, param.getConditions());
                    row.appendChild(column);
                }
                if (!ignoreFullPath) {
                    appendColumn(xml, row, param.getFullPath());
                }
                if (!ignoreType) {
                    appendColumn(xml, row, param.getType());
                }
                configurationBody.appendChild(row);
            });
            tgroup.appendChild(configurationBody);

            section.appendChild(table);
        }

        topic.appendChild(body);

        final StringWriter writer = new StringWriter();
        final StreamResult result = new StreamResult(writer);
        try {
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(xml), result);

            final String rootDir = output.getName().replace(".zip", "");
            if (directories.add(rootDir)) {
                zip.putNextEntry(new ZipEntry(rootDir + '/'));
                zip.closeEntry();
            }
            final String ditaFolder = rootDir + '/' + family;
            if (directories.add(ditaFolder)) {
                zip.putNextEntry(new ZipEntry(ditaFolder + '/'));
                zip.closeEntry();
            }

            final String path = ditaFolder + '/' + name + ".dita";
            zip.putNextEntry(new ZipEntry(path));
            final String content = writer.toString();
            final int topicIdx = content.indexOf("<topic");
            zip
                    .write((content.substring(0, topicIdx)
                            + "<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"topic.dtd\">\n"
                            + content.substring(topicIdx)).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } catch (final IOException | TransformerException e) {
            throw new IllegalStateException(e);
        }
    }

    private void renderConditions(final Document xml, final Element container, final Conditions conditions) {
        switch (conditions.getConditions().size()) {
        case 0:
            container.setTextContent("Always enabled");
            break;
        case 1:
            renderCondition(xml, container, conditions.getConditions().iterator().next());
            break;
        default:
            final Element listWrapper = xml.createElement("ul");
            final Runnable conditionAppender = () -> conditions.getConditions().forEach(cond -> {
                final Element li = xml.createElement("li");
                renderCondition(xml, li, cond);
                listWrapper.appendChild(li);
            });
            switch (conditions.getOperator().toUpperCase(ROOT)) {
            case "OR":
                container.setTextContent("One of these conditions is meet:");
                conditionAppender.run();
                break;
            case "AND":
            default:
                container.setTextContent("All of the following conditions are met:");
                conditionAppender.run();
            }
            container.appendChild(listWrapper);
        }
    }

    private void renderCondition(final Document xml, final Element container,
            final DocBaseGenerator.Condition condition) {
        final Runnable valuesAppender = () -> Stream.of(condition.getValue().split(",")).map(v -> {
            final Element userinput = xml.createElement("userinput");
            userinput.setTextContent(v);
            return userinput;
        }).reduce(container, (wrapper, child) -> {
            if (wrapper.hasChildNodes()) {
                wrapper.setTextContent(" or ");
            }
            wrapper.appendChild(child);
            return wrapper;
        });
        final Runnable appendPath = () -> {
            final Element parmname = xml.createElement("parmname");
            parmname.setTextContent(condition.getPath());
            container.appendChild(parmname);
        };
        switch (ofNullable(condition.getStrategy()).orElse("default").toLowerCase(ROOT)) {
        case "length":
            if (condition.isNegate()) {
                if ("0".equals(condition.getValue())) {
                    appendPath.run();
                    container.setTextContent(" is not empty");
                } else {
                    container.setTextContent("the length of ");
                    appendPath.run();
                    container.setTextContent(" is not ");
                    valuesAppender.run();
                }
            } else if ("0".equals(condition.getValue())) {
                appendPath.run();
                container.setTextContent("is empty");
            } else {
                container.setTextContent("the length of ");
                appendPath.run();
                container.setTextContent(" is ");
                valuesAppender.run();
            }
            break;
        case "contains": {
            appendPath.run();
            if (condition.isNegate()) {
                container.setTextContent(" does not contain ");
            } else {
                container.setTextContent(" contains ");
            }
            valuesAppender.run();
            break;
        }
        case "contains(lowercase=true)": {
            container.setTextContent("the lowercase value of ");
            appendPath.run();
            if (condition.isNegate()) {
                container.setTextContent(" does not contain ");
            } else {
                container.setTextContent(" contains ");
            }
            valuesAppender.run();
            break;
        }
        case "default":
        default:
            appendPath.run();
            if (condition.isNegate()) {
                container.setTextContent(" is not equal to ");
            } else {
                container.setTextContent(" is equal to ");
            }
            valuesAppender.run();
        }
    }

    private void appendColumn(final Document xml, final Element row, final String value) {
        final Element column = xml.createElement("entry");
        if (value != null) {
            column.setTextContent(value.trim());
        }
        row.appendChild(column);
    }

    private DocumentBuilderFactory newDocFactory() {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (final ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
        return factory;
    }
}
