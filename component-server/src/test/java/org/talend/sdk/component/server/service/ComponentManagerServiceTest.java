package org.talend.sdk.component.server.service;

import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;

import org.apache.meecrowave.junit.MonoMeecrowave;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ComponentManager;

import static java.util.stream.Stream.empty;
import static org.apache.webbeans.util.Asserts.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MonoMeecrowave.Runner.class)
public class ComponentManagerServiceTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Inject
    private ComponentManagerService componentManagerService;

    @Test
    public void deployExistingPlugin() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Container 'the-test-component' already exists");

        componentManagerService.deploy("org.talend.test1:the-test-component:jar:1.2.6:compile");
    }

    @Test //undeploy => deploy
    public void reloadExistingPlugin() {
        String gav = "org.talend.test1:the-test-component:jar:1.2.6:compile";

        String pluginID = getPluginId(gav);
        assertNotNull(pluginID);

        //undeploy
        componentManagerService.undeploy("org.talend.test1:the-test-component:jar:1.2.6:compile");
        Optional<Container> plugin = componentManagerService.manager().findPlugin(pluginID);
        assertFalse(plugin.isPresent());

        //deploy
        componentManagerService.deploy(gav);
        pluginID = getPluginId(gav);
        assertNotNull(pluginID);
        plugin = componentManagerService.manager().findPlugin(pluginID);
        assertTrue(plugin.isPresent());
    }

    @Test
    public void undeployNonExistingPlugin() {
        String gav = "org.talend:non-existing-component:jar:0.0.0:compile";

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("No plugin found using maven GAV: " + gav);

        componentManagerService.undeploy(gav);
    }

    private String getPluginId(final String gav) {
        return componentManagerService.manager()
                .find(c -> gav.equals(c.get(ComponentManager.OriginalId.class).getValue()) ? Stream.of(c.getId())
                        : empty()).findFirst()
                .orElseThrow(() -> new RuntimeException("No plugin found using maven GAV: " + gav));
    }

}
