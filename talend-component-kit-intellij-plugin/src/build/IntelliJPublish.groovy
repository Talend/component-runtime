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

import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest
import org.apache.maven.settings.crypto.SettingsDecrypter
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory

//
// Helper script to publish a plugin to jetbrains repository using a settings.xml server
//
// It requires to configure a "jetbrains" settings.xml server and these properties:
// - project.properties.idea.pluginZip: the location of the plugin to publish
//

if (project.version.endsWith('-SNAPSHOT') || project.version.contains('M')) {
    log.info("Project is in SNAPSHOT, skipping Jetbrains deployment")
    return
}

// find the plugin to deploy
File pluginZip = new File(project.build.directory, "${project.build.finalName}-plugin.zip")
if (!pluginZip.exists()) {
    log.error("No ${pluginZip} file found, skipping plublish")
    return
}


// grab the credentials
def serverId = project.properties.getProperty('talend.jetbrains.serverId', 'jetbrains')
def serverIt = session.settings.servers.findAll { it.id == serverId }.iterator()
if (!serverIt.hasNext()) { // todo: fail?
    log.error("no server '${serverId}' in your settings.xml, will skip jetbrains publication")
    return
}
def server = serverIt.next()
def decryptedServer = session.container.lookup(SettingsDecrypter).decrypt(new DefaultSettingsDecryptionRequest(server))
server = decryptedServer.server != null ? decryptedServer.server : server

int pluginId = Integer.parseInt(project.properties['idea.plugin.id'].trim())
def repositoryInstance = PluginRepositoryFactory.create(project.properties['idea.intellij.public.url'], server.getPassword())
project.properties['idea.intellij.channel'].trim().split(',').each { channel ->
    try {
        repositoryInstance.uploader
                .uploadPlugin(pluginId, pluginZip, channel == 'default' ? '' : channel, null)
    } catch (final Exception e) { // don't break the release for that, worse case upload it manually
        log.error(e.message, e)
    }
}
