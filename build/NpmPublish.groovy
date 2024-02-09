/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.build

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest
import org.apache.maven.settings.crypto.SettingsDecrypter

import javax.json.bind.JsonbBuilder
import javax.json.bind.annotation.JsonbProperty
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream

import static javax.ws.rs.client.Entity.entity
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE

//
// Helper script to publish a folder to npm using a settings.xml server
//
// It requires to configure a "npm" settings.xml server and these properties:
// - packageJson: the location of the package.json, note that it should contain the sources to bundle too
//

class NpmLoginRequest {
    String name
    String password
}

class NpmLoginResponse {
    String token
    boolean ok
}

class NpmAttachment {
    String content_type
    String data
    long length
}

class NpmPublishRequest {
    String _id
    String name
    String description
    Map<String, NpmAttachment> _attachments
    Map<String, Object> versions
    String readme

    @JsonbProperty("dist-tags")
    Map<String, String> distTags
}

class NpmPublishResponse {
    boolean success
    String error
}

class Npm {
    String username
    String password
    String registry = 'https://registry.npmjs.org'

    Client client
    WebTarget target
    String token

    def login() {
        client = ClientBuilder.newClient()
                .register(new JsonbJaxrsProvider())
        target = client.target(registry)
        def response = target.path('/-/user/org.couchdb.user:{username}')
                .resolveTemplate("username", getEncodedUsername())
                .request(APPLICATION_JSON_TYPE)
                .put(entity(new NpmLoginRequest(name: username, password: password), APPLICATION_JSON_TYPE), NpmLoginResponse.class)
        if (!response.ok) {
            throw new IllegalArgumentException("Invalid NPM login for use '${username}'")
        }
        token = response.token
    }

    def publish(pckJson, version) {
        def jsonb = JsonbBuilder.create()
        def json = new File(pckJson)
        try {
            def reader = json.newReader('UTF-8')
            def pck = jsonb.fromJson(reader, Object.class)
            reader.close()

            def base = json.parentFile
            if (!doPublish(base, pck, version, jsonb)) {
                for (int i = 0; i < 100; i++) {
                    if (doPublish(base, pck, "${version}-fix${i}".toString(), jsonb)) {
                        break
                    }
                }
            }

            println("Published ${pck.name} version ${version}")
        } finally {
            jsonb.close()
        }
    }

    def doPublish(base, pck, version, jsonb) {
        pck.version = version // override it since we want to use the build one
        pck.dist = [
                'tarball': "${registry.replace('https://', 'http://')}/${pck.name}/-/${pck.name}-${version}.tgz".toString()
        ]
        pck._id = "${pck.name}@${version}".toString()
        pck._from = '.'
        pck._npmVersion = '3.5.2' // whatever
        pck._nodeVersion = '6.11.4' // whatever
        pck._npmUser = [:]
        pck.readme = 'README.adoc'

        // create a tar from the folder containing pckJson
        def buffer = new ByteArrayOutputStream(8192)
        def tarGz = new TarArchiveOutputStream(new GZIPOutputStream(buffer))
        def exclusions = [
                '.node', 'node_modules', 'target', 'component-kit.js.iml',
                'package-template.json', 'package-lock.json', 'pom.xml', 'npm.lock', 'yarn.lock'
        ]
        try {
            tarGz.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
            for (final String entry : base.list()) {
                if ('package.json' == entry) { // rewritten so updated on the fly
                    def content = jsonb.toJson(pck).getBytes(StandardCharsets.UTF_8)
                    def archiveEntry = new TarArchiveEntry('package/package.json');
                    archiveEntry.setSize(content.length)
                    tarGz.putArchiveEntry(archiveEntry)
                    tarGz.write(content)
                    tarGz.closeArchiveEntry()
                } else if (!exclusions.contains(entry)) {
                    doTarGz(tarGz, new File(base, entry), base.absolutePath)
                }
            }
            def content = """Talend Component Kit Javascript Binding
========

See https://talend.github.io/component-runtime/
""".getBytes(StandardCharsets.UTF_8)
            def archiveEntry = new TarArchiveEntry('package/readme.md');
            archiveEntry.setSize(content.length)
            tarGz.putArchiveEntry(archiveEntry)
            tarGz.write(content)
            tarGz.closeArchiveEntry()
        } finally {
            tarGz.close()
        }

        def tar = buffer.toByteArray()

        def sha = new StringWriter()
        MessageDigest.getInstance('SHA-1').digest(tar).encodeHex().writeTo(sha)
        pck.dist['shasum'] = sha.toString()
        pck._shasum = sha.toString()

        def data = Base64.encoder.encodeToString(tar)

        def request = new NpmPublishRequest(
                _id: pck.name,
                name: pck.name,
                description: pck.description ?: '',
                distTags: [
                        'latest' : version
                ],
                versions: [
                        "${version}": pck
                ],
                readme: pck.readme,
                _attachments: [
                        "${pck.name}-${version}.tgz": new NpmAttachment(
                                content_type: MediaType.APPLICATION_OCTET_STREAM,
                                data: data,
                                length: tar.length
                        )
                ]
        )
        def response = target.path("/${pck.name}")
                .request(APPLICATION_JSON_TYPE)
                .header('Authorization', "Bearer ${token}")
                .put(entity(request, APPLICATION_JSON_TYPE))
        if (response.status != Response.Status.OK.statusCode) {
            def error = response.readEntity(NpmPublishResponse.class)
            if (response.status == Response.Status.FORBIDDEN.statusCode && error.error.startsWith('You cannot publish over the previously published versions')) {
                return false
            }
            throw new IllegalArgumentException("Invalid NPM publish: ${error.error}")
        }
        def value = response.readEntity(String.class)
        def npmResponse = jsonb.fromJson(value, NpmPublishResponse.class)
        if (!npmResponse.success) {
            throw new IllegalStateException("Didn't publish properly the NPM module ${pck.name}: ${npmResponse.error}")
        }
        return true
    }

    def logout() {
        try {
            if (token != null) {
                def response = target.path("-/npm/v1/tokens/token/{token}")
                        .resolveTemplate("token", token)
                        .request()
                        .header('Authorization', "Bearer ${token}")
                        .delete()
                if (response.status != Response.Status.NO_CONTENT.statusCode) {
                    throw new IllegalStateException("Impossible to delete NPM token, got: HTTP ${response.status} / ${response.readEntity(String.class)}")
                }
                token = null
            }
        } finally {
            if (client != null) {
                client.close()
                client = null
            }
        }
    }

    private void doTarGz(tarGz, file, prefix) throws IOException {
        def path = file.getPath().replace(prefix, '').replace(File.separator, '/')
        def archiveEntry = new TarArchiveEntry(file, "package/${path}");
        if (path.endsWith('.sh')) {
            archiveEntry.setMode(0755);
        }
        tarGz.putArchiveEntry(archiveEntry)
        if (file.isDirectory()) {
            tarGz.closeArchiveEntry()
            final File[] files = file.listFiles()
            if (files != null) {
                for (final File child : files) {
                    doTarGz(tarGz, child, prefix)
                }
            }
        } else {
            Files.copy(file.toPath(), tarGz)
            tarGz.closeArchiveEntry()
        }
    }

    private String getEncodedUsername() {
        URLEncoder.encode(username, "UTF-8")
                .replaceAll('\\+', '%20')
                .replaceAll('%21', '!')
                .replaceAll('%27', '\'')
                .replaceAll('%28', '(')
                .replaceAll('%29', ')')
                .replaceAll('%7E', '~')
    }
}


//
// start script
//
if (project.version.endsWith('-SNAPSHOT') || project.version.contains('M')) {
    log.info("Project is in SNAPSHOT, skipping NPM deployment")
    return
}

// grab the credentials
def serverId = project.properties.getProperty('talend.npm.serverId', 'npm')
def serverIt = session.settings.servers.findAll { it.id == serverId }.iterator()
if (!serverIt.hasNext()) {
    log.warn("no server '${serverId}' in your settings.xml, will skip npm publication")
    return
}

def server = serverIt.next()
def decryptedServer = session.container.lookup(SettingsDecrypter).decrypt(new DefaultSettingsDecryptionRequest(server))
server = decryptedServer.server != null ? decryptedServer.server : server

def npm = new Npm(username: server.username, password: server.password)
try {
    npm.login()
    npm.publish(packageJson, project.version)
} finally {
    npm.logout()
}
