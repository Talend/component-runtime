#!/usr/bin/env bash

# maven
mvn dependency:resolve dependency:resolve-plugins

# front
for i in component-form/component-form-demo component-starter-server component-tools; do
    cd $i
        mvn frontend:install-node-and-yarn@install-node-and-yarn frontend:yarn@yarn-install
    cd -
done

# studio (p2)
cd component-studio-integration
    mvn gplus:execute@setup-deps
cd -

# documentation - we got some issues on travis so this is a sanity check
cd documentation
    mvn dependency:unpack@unpack-api-javadoc
cd -

