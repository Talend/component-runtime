/**
 *  Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.DefaultArtifactHandler
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver
import org.apache.maven.model.Dependency
import org.apache.maven.project.MavenProject

def createDependenciesDescriptor = { rootDependency, output ->
    def dependenciesResolver = session.container.lookup(LifecycleDependencyResolver)

    def tmpProject = new MavenProject(artifactId: 'temp', groupId: 'temp', version: 'temp', packaging: 'pom');
    tmpProject.artifact = new DefaultArtifact(project.groupId, project.artifactId, project.version, 'compile',
            'pom', null, new DefaultArtifactHandler())
    tmpProject.dependencies = [rootDependency] as List
    tmpProject.remoteArtifactRepositories = project.remoteArtifactRepositories

    def scopes = ['compile', 'runtime']
    dependenciesResolver.resolveProjectDependencies(tmpProject, scopes, scopes, session, false, [project.artifact] as Set)

    output.parentFile.mkdirs()
    def out = output.newOutputStream()
    tmpProject.resolvedArtifacts.sort { it.groupId + ':' + it.artifactId }.each {
        out << "${it.groupId}:${it.artifactId}:${it.type}${it.classifier != null && !it.classifier.isEmpty() ? ':' + it.classifier : ''}:${it.baseVersion}:${it.scope}\n"
    }
    out.close()
}

['beam-runners-direct-java', 'beam-runners-spark-3'].each { artifactId ->
    createDependenciesDescriptor(new Dependency(
            groupId: 'org.apache.beam', artifactId: artifactId, version: project.properties['beam.version'], scope: 'compile'),
            new File(project.build.outputDirectory, "TALEND-INF/beam-runner_${artifactId}.dependencies"))
}


