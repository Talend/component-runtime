/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.anim.dom.SVGOMSVGElement;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGAnimatedRect;
import org.w3c.dom.svg.SVGRect;

public class SvgValidator {

    private final SAXSVGDocumentFactory factory =
            new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());

    private final Boolean legacy;

    public SvgValidator(final Boolean legacy) {
        this.legacy = legacy;
    }

    public Stream<String> validate(final Path path) {
        final String prefix = "[" + path.getFileName() + "] ";

        final SVGOMSVGElement icon;
        try {
            icon = loadSvg(path);
        } catch (final IllegalStateException e) {
            return Stream.of(prefix + "Invalid SVG: " + e.getMessage());
        }
        return Stream
                .<Function<SVGOMSVGElement, String>> of(this::noEmbedStyle, this::pathsAreClosed, this::noDisplayNone,
                        this::viewportSize)
                .map(fn -> fn.apply(icon))
                .filter(Objects::nonNull)
                .map(error -> prefix + error);
    }

    private SVGOMSVGElement loadSvg(final Path path) {
        try {
            final Document document = factory.createDocument(path.toUri().toASCIIString());
            return SVGOMSVGElement.class.cast(document.getDocumentElement());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String viewportSize(final SVGOMSVGElement icon) {
        final SVGAnimatedRect viewBox = icon.getViewBox();
        final int vb16 = 16;
        final int vb40 = 40;
        if (viewBox == null) {
            return String.format("No viewBox, need one with '0 0 %d %d' (family) or '0 0 %d %d' (connector).", vb16,
                    vb16, vb40, vb40);
        }
        final SVGRect baseVal = viewBox.getBaseVal();
        if (baseVal.getX() != 0 || baseVal.getY() != 0 || (baseVal.getHeight() != vb16 && baseVal.getHeight() != vb40)
                || (baseVal.getWidth() != vb16 && baseVal.getWidth() != vb40)) {
            return String.format("viewBox must be '0 0 %d %d' (family) or '0 0 %d %d' (connector) found '%d %d %d %d'",
                    vb16, vb16, vb40, vb40, (int) baseVal.getX(), (int) baseVal.getY(), (int) baseVal.getWidth(),
                    (int) baseVal.getHeight());
        }
        return null;
    }

    private String pathsAreClosed(final SVGOMSVGElement icon) {
        return browseDom(icon, node -> {
            if ("path".equals(node.getNodeName())) {
                final Node d = node.getAttributes() == null ? null : node.getAttributes().getNamedItem("d");
                if (d == null || d.getNodeValue() == null) {
                    return "Missing 'd' in a path";
                }
                if (!d.getNodeValue().toLowerCase(Locale.ROOT).endsWith("z")) {
                    return "All path must be closed so end with 'z', found value: '" + d.getNodeValue() + '\'';
                }
            }
            return null;
        });
    }

    private String noEmbedStyle(final SVGOMSVGElement icon) {
        return browseDom(icon, node -> node.getNodeName().equals("style") ? "Forbidden <style> in icon" : null);
    }

    private String noDisplayNone(final SVGOMSVGElement icon) {
        return browseDom(icon, node -> {
            final Node display = node.getAttributes() == null ? null : node.getAttributes().getNamedItem("display");
            if (display != null && display.getNodeValue() != null) {
                return display.getNodeValue().replaceAll(" +", "").equalsIgnoreCase("none")
                        ? "'display:none' is forbidden in SVG icons"
                        : null;
            }
            return null;
        });
    }

    private String browseDom(final Node element, final Function<Node, String> errorFactory) {
        {
            final String error = errorFactory.apply(element);
            if (error != null) {
                return error;
            }
        }
        final NodeList childNodes = element.getChildNodes();
        if (childNodes.getLength() > 0) {
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node node = childNodes.item(i);

                {
                    final String error = errorFactory.apply(node);
                    if (error != null) {
                        return error;
                    }
                }

                final String error = browseDom(node, errorFactory);
                if (error != null) {
                    return error;
                }
            }
        }
        return null;
    }
}
