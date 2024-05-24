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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class SVG2Png implements Runnable {

    private final Path iconsFolder;

    private final Log log;

    private final boolean activeWorkarounds;

    public SVG2Png(final Path iconsFolder, final boolean activeWorkarounds, final Object log) {
        this.iconsFolder = iconsFolder;
        this.activeWorkarounds = activeWorkarounds;
        try {
            this.log = Log.class.isInstance(log) ? Log.class.cast(log) : new ReflectiveLog(log);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void run() {
        try {
            Files.walkFileTree(iconsFolder, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    final String fileName = file.getFileName().toString();
                    if (fileName.endsWith(".svg")) {
                        final Path svg = file
                                .getParent()
                                .resolve(fileName.substring(0, fileName.length() - ".svg".length()) + "_icon32.png");
                        if (!Files.exists(svg)) {
                            createPng(file, svg);
                            log.info("Created " + svg);
                        }
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void createPng(final Path svg, final Path png) {
        final PNGTranscoder transcoder = new PNGTranscoder() {

            @Override
            public void writeImage(final BufferedImage img, final TranscoderOutput output) throws TranscoderException {
                if (activeWorkarounds) {
                    ensureAlphaDiff(img);
                }
                super.writeImage(img, output);
            }

            private void ensureAlphaDiff(final BufferedImage img) {
                // otherwise all web icon are just plain black and studio ignores the alpha in its
                // md5 cache key so we just get only one icon
                for (int x = 1; x < img.getWidth() - 1; x++) {
                    for (int y = 1; y < img.getHeight() - 1; y++) {
                        final Color color = new Color(img.getRGB(x, y), true);
                        if (color.getAlpha() == 0) { // enforce some differences for all black images
                            img.setRGB(x, y, new Color(255, 255, 255, color.getAlpha()).getRGB());
                        }
                    }
                }
            }
        };
        transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, 32f);
        transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, 32f);
        try (final InputStream in = Files.newInputStream(svg); final OutputStream out = Files.newOutputStream(png)) {
            transcoder.transcode(new TranscoderInput(in), new TranscoderOutput(out));
        } catch (final IOException | TranscoderException ioe) {
            throw new IllegalStateException(ioe);
        }
    }
}
