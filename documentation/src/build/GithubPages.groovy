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


import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest
import org.apache.maven.settings.crypto.SettingsDecrypter
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import static java.util.Collections.singleton

if (!new File(project.build.directory, "${project.build.finalName}/main/1.0.1").exists()) {
    log.info('Not yet in deploy phase, should generate the site before')
    return
}

def profile = System.getProperty('github.site.profile', project.properties.getProperty('github.site.profile', 'deploy'))
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

def credentialsProvider = new UsernamePasswordCredentialsProvider(server.password, "")
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
new AntBuilder().copy(todir: workDir.absolutePath, overwrite: true) {
    fileset(dir: source.absolutePath)
}

def message = "Updating the website with version ${project.version} // " + new Date().toString()
git.add().addFilepattern(".").call()
if (Boolean.getBoolean("component.gh-page.debug")) {
    def status = git.status().call()
    log.info("Status:\n  Changed: ${status.changed}\n  Added: ${status.added}\n  Removed: ${status.removed}")
}
git.commit().setAll(true).setMessage(message).call()
git.status().call()
git.push().setCredentialsProvider(credentialsProvider).add(branch).call()
log.info("Updated the website on ${new Date()}")
