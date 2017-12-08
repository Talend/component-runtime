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


import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest
import org.apache.maven.settings.crypto.SettingsDecrypter
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import static java.util.Collections.singleton

if (!new File(project.build.directory, 'generated-adoc/contributors.json').exists()) {
    log.debug('Not yet in deploy phase, should generate the site before')
    return
}

def profile = System.getProperty('github.site.profile', project.properties.getProperty('github.site.profile', 'latest'))
log.info("Site deployment profile: ${profile}")

def source = new File(project.build.directory, project.build.finalName)

def branch = 'refs/heads/gh-pages'

// deploy on github
def workDir = new File(project.build.directory, UUID.randomUUID().toString() + '_' + System.currentTimeMillis())

def url = project.parent.scm.url
def serverId = project.properties['github.global.server']
log.info("Using server ${serverId}")

def server = session.settings.servers.findAll { it.id == serverId }.iterator().next()
def decryptedServer = session.container.lookup(SettingsDecrypter).decrypt(new DefaultSettingsDecryptionRequest(server))
server = decryptedServer.server != null ? decryptedServer.server : server

log.info("Using url=${url}")
log.info("Using user=${server.username}")
log.info("Using branch=${branch}")

def credentialsProvider = new UsernamePasswordCredentialsProvider(server.username, server.password)
def git = Git.cloneRepository()
        .setCredentialsProvider(credentialsProvider)
        .setURI(url)
        .setDirectory(workDir)
        .setBranchesToClone(singleton(branch))
        .setBranch(branch)
        .call()

if ('skip' == profile) {
    log.info("Skipping as requested")
    return
}
def isLatest = 'latest' == profile
def ant = new AntBuilder()

def versions = new StringBuilder()
def root = System.getProperty('jbake.site.rootpath', project.properties.getProperty('jbake.site.rootpath'))
workDir.listFiles(new FilenameFilter() {
    private final Collection<String> excluded = ['css', 'images', 'js', 'presentations', 'tags', 'latest', 'current', 'apidocs']

    @Override
    boolean accept(File dir, String name) {
        return !name.startsWith(".") && !name.endsWith("-SNAPSHOT") && !excluded.contains(name) && new File(dir, name).isDirectory()
    }
}).each {
    versions.append("            <li><a href=\"${root}/${it.name}/index.html\">${it.name}</a></li>\n")
}

def copySite = { to ->
    ant.copy(todir: to.absolutePath, overwrite: true) {
        filterset(begintoken: "<!-- ", endtoken: ' -->') {
            filter(token: 'VERSIONS', value: "${versions.toString()}")
        }
        fileset(dir: source.absolutePath) {
            include(name: '**/*.html')
        }
    }
    ant.copy(todir: to.absolutePath, overwrite: true) {
        fileset(dir: source.absolutePath) {
            exclude(name: '**/*.html')
        }
    }
}

if (isLatest) {
    copySite(new File(workDir, 'latest'))
} else {
    copySite(workDir)
    // versionned version to keep an history - we can need to add version links later on on the main page
    // note: this is not yet a need since we'll not break anything for now
    copySite(new File(workDir, project.version))
}

def message = (isLatest ? 'Updating latest website' : "Updating the website with version ${project.version}") + new Date().toString()
git.add().addFilepattern(".").call()
if (Boolean.getBoolean("component.gh-page.debug")) {
    def status = git.status().call()
    log.info("Status:\n  Changed: ${status.changed}\n  Added: ${status.added}\n  Removed: ${status.removed}")
}
git.commit().setAll(true).setMessage(message).call()
git.status().call()
git.push().setCredentialsProvider(credentialsProvider).add(branch).call()
log.info("Updated the website on ${new Date()}")
