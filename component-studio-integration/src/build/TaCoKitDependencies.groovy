/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.DependencyVisitor

def repositorySession = session.getRepositorySession()
def rs = session.lookup(RepositorySystem.class.getName())

def doCollect = { groupId, artifactId, version, output ->
    def exclusions = [
            'slf4j-api', // slf4j is managed into the studio and findbugs is not needed at runtime
            'findbugs-annotations' // only static analyzis tool, not needed at runtime
    ]

    def collectResult = rs.collectDependencies(repositorySession, new CollectRequest(new Dependency(
            new DefaultArtifact(groupId, artifactId, "jar", version), "compile"), project.getRemoteProjectRepositories()))

    def collect = []
    collectResult.root.accept(new DependencyVisitor() {
        @Override
        boolean visitEnter(DependencyNode node) {
            def aId = node.dependency.artifact.artifactId
            if (!exclusions.contains(aId)) {
                collect.add(node.dependency.artifact)
            }
            return true
        }

        @Override
        boolean visitLeave(DependencyNode node) {
            return true
        }
    })

    def dependencies = new File(project.build.outputDirectory, output)
    dependencies.parentFile.mkdirs()
    def dependenciesOS = dependencies.newOutputStream()
    try {
        collect.collect { "mvn:${it.groupId}/${it.artifactId}/${it.version}" }.toSet().sort { it }.each {
            dependenciesOS << "${it}\n"
        }
    } finally {
        dependenciesOS.close()
    }
}

doCollect(project.groupId, 'component-runtime-manager', project.version, 'TALEND-INF/tacokit.dependencies')
doCollect(project.groupId, 'component-runtime-beam', project.version, 'TALEND-INF/beam.dependencies')
