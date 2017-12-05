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

def serverId = project.properties.getProperty('talend.jira.serverId', 'jira')
def serverIt = session.settings.servers.findAll { it.id == serverId }.iterator()
if (!serverIt.hasNext()) {
    log.warn("no server '${serverId}' in your settings.xml, will skip changelog generation")
    project.properties.setProperty('talend.jira.username', 'skip')
    project.properties.setProperty('talend.jira.password', 'skip')
    return
}

def server = serverIt.next()
def decryptedServer = session.container.lookup(SettingsDecrypter).decrypt(new DefaultSettingsDecryptionRequest(server))
server = decryptedServer.server != null ? decryptedServer.server : server
project.properties.setProperty('talend.jira.username', server.username)
project.properties.setProperty('talend.jira.password', server.password)
log.info("Set the Talend jira credentials into the project properties")
