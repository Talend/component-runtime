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
import org.eclipse.jgit.api.Git
import org.apache.maven.settings.crypto.SettingsDecrypter
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import static java.util.Collections.singleton

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

def ant = new AntBuilder()
ant.copy(todir: workDir.absolutePath, overwrite: true) { fileset(dir: source.absolutePath) }
// versionned version to keep an history - we can need to add version links later on on the main page
// note: this is not yet a need since we'll not break anything for now
ant.copy(todir: new File(workDir, project.version).absolutePath, overwrite: true) { fileset(dir: source.absolutePath) }


git.add().addFilepattern(".").call()
git.commit().setMessage("Updating the website with version ${project.version}").call()
git.status().call()
git.push().setCredentialsProvider(credentialsProvider).add(branch).call()
