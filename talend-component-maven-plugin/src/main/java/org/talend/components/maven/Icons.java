/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.maven;

import static lombok.AccessLevel.PRIVATE;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.maven.shared.utils.io.IOUtil;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
final class Icons {

    static byte[] findIcon(final String icon, final boolean downloadIconsFromGithub, final String svgRepository) {
        if (icon == null) {
            return null;
        }
        if (downloadIconsFromGithub) {
            for (int i = 0; i < 2; i++) {
                try {
                    final URL url = new URL(svgRepository + icon + ".svg");
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                    try (final InputStream svgStream = url.openStream()) {
                        IOUtil.copy(svgStream, baos);
                    }
                    return toPng(baos.toByteArray());
                } catch (final UnknownHostException e) {
                    // retry, happens these day where github is not that stable
                } catch (final IOException ioe) {
                    return null;
                }
            }
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream iconStream = loader.getResourceAsStream("icons/" + icon + ".svg")) {
            if (iconStream != null) {
                IOUtil.copy(iconStream, baos);
                baos.flush();
                return toPng(baos.toByteArray());
            }
        } catch (final IOException ioe) {
            // no-op
        }

        // direct png - see SvgIconResolver in the server
        try (final InputStream iconStream = loader.getResourceAsStream(icon + "_icon32.png")) {
            if (iconStream != null) {
                return copy(baos, iconStream);
            }
        } catch (final IOException ioe) {
            // no-op
        }
        try (final InputStream iconStream = loader.getResourceAsStream("icons/" + icon + "_icon32.png")) {
            if (iconStream != null) {
                return copy(baos, iconStream);
            }
        } catch (final IOException ioe) {
            // no-op
        }

        return null;
    }

    private static byte[] copy(final ByteArrayOutputStream baos, final InputStream iconStream) throws IOException {
        IOUtil.copy(iconStream, baos);
        baos.flush();
        return baos.toByteArray();
    }

    private static byte[] toPng(final byte[] svg) throws IOException {
        // convert svf to png at the right size (32x32)
        final PNGTranscoder pngTranscoder = new PNGTranscoder() {

            @Override
            public void writeImage(final BufferedImage img, final TranscoderOutput output) throws TranscoderException {
                // otherwise all web icon are just plain black and studio ignores the alpha in its
                // md5 cache key so we just get only one icon
                forceRGBBasedOnAlpha(img);
                super.writeImage(img, output);
            }
        };
        pngTranscoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, 32f);
        pngTranscoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, 32f);
        final ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        try {
            pngTranscoder.transcode(new TranscoderInput(new ByteArrayInputStream(svg)), new TranscoderOutput(ostream));
        } catch (final TranscoderException e) {
            throw new IllegalStateException(e);
        }
        ostream.close();

        return ostream.toByteArray();
    }

    private static void forceRGBBasedOnAlpha(final BufferedImage img) {
        for (int x = 1; x < img.getWidth() - 1; x++) {
            for (int y = 1; y < img.getHeight() - 1; y++) {
                final Color color = new Color(img.getRGB(x, y), true);
                if (color.getAlpha() == 0) { // enforce some differences for all black images
                    img.setRGB(x, y, new Color(255, 255, 255, color.getAlpha()).getRGB());
                }
            }
        }
    }
}
