/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.runtime.documentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contributor {

    private String id;

    private String name;

    private String description;

    private String gravatar;

    private int commits;

    public static ContributorBuilder builder() {
        return new ContributorBuilder();
    }

    public static class ContributorBuilder {

        private String id;

        private String name;

        private String description;

        private String gravatar;

        private int commits;

        ContributorBuilder() {
        }

        public ContributorBuilder id(final String id) {
            this.id = id;
            return this;
        }

        public ContributorBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public ContributorBuilder description(final String description) {
            this.description = description;
            return this;
        }

        public ContributorBuilder gravatar(final String gravatar) {
            this.gravatar = gravatar;
            return this;
        }

        public ContributorBuilder commits(final int commits) {
            this.commits = commits;
            return this;
        }

        public Contributor build() {
            return new Contributor(id, (name == null || name.isEmpty()) ? id : name, description, gravatar, commits);
        }

        public String toString() {
            return "Contributor.ContributorBuilder(id=" + this.id + ", name=" + this.name + ", description="
                    + this.description + ", gravatar=" + this.gravatar + ", commits=" + this.commits + ")";
        }
    }
}
