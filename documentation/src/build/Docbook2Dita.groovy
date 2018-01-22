/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdl.component.build

import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

if (Boolean.getBoolean("talend.component.documentation.dita.skip")) {
    return
}

def sourceFolder = new File(project.build.directory, 'documentation/docbook')

/* doconv xslt is not good enough
def doconvTarGz = new File(project.build.directory, 'doconv/doconv.tar.gz')
doconvTarGz.parentFile.mkdirs()
def localCopy = doconvTarGz.newOutputStream()
localCopy << new URL(doconvArchive).openStream()
localCopy.close()

def localXslt = new File(project.build.directory, 'doconv/xslt')

def  archiveStream = new TarArchiveInputStream(new GZIPInputStream(doconvTarGz.newInputStream()))
def entry
while ((entry = archiveStream.nextTarEntry) != null) {
    if (!entry.directory && entry.name.startsWith("doconv-${doconvVersion}/doconv/plugin/docbooktodita") && !entry.name.endsWith('.py')) {
        def target = new File(localXslt, entry.name)
        target.parentFile.mkdirs()
        def out = target.newOutputStream()
        out << archiveStream
        out.close()
    }
}
archiveStream.close()
log.info("Extracting doconv in ${localXslt}")
*/

def xslt = null // todo
if (xslt == null) {
    log.warn("No XSLT to convert docbook to dita, skipping")
    return
}

final TransformerFactory factory = TransformerFactory.newInstance()
final Transformer transformer = factory.newTransformer(new StreamSource(xslt))

sourceFolder.listFiles(new FilenameFilter() {
    @Override
    boolean accept(File dir, String name) {
        return name.endsWith('.xml')
    }
}).each {adoc ->
    log.info("${adoc}")
    def stream = adoc.newInputStream()
    transformer.transform(new StreamSource(stream), new StreamResult(System.out))
    stream.close()
}
