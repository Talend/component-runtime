/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio;

import static lombok.AccessLevel.PRIVATE;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.IService;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.INode;
import org.talend.core.runtime.services.IGenericWizardService;
import org.talend.sdk.component.studio.metadata.TaCoKitCache;
import org.talend.sdk.component.studio.service.ComponentService;
import org.talend.sdk.component.studio.service.Configuration;
import org.talend.sdk.component.studio.ui.composite.TaCoKitComposite;
import org.talend.sdk.component.studio.websocket.WebSocketClient;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = PRIVATE)
public class Lookups {

    public static Runnable init() {
        try {
            final Field instance = GlobalServiceRegister.class.getDeclaredField("instance");
            if (!instance.isAccessible()) {
                instance.setAccessible(true);
            }
            final Object originalInstance = instance.get(null);
            instance.set(null, new EnrichedGlobalServiceRegister());
            return () -> {
                try {
                    instance.set(null, originalInstance);
                } catch (final Exception e) {
                    throw new IllegalStateException(e);
                }
            };
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Configuration configuration() {
        return lookup(Configuration.class);
    }

    public static WebSocketClient client() {
        return lookup(WebSocketClient.class);
    }

    public static ComponentService service() {
        return lookup(ComponentService.class);
    }

    public static TaCoKitCache taCoKitCache() {
        return lookup(TaCoKitCache.class);
    }

    private static <T> T lookup(final Class<T> type) {
        final BundleContext context =
                Platform.getBundle("org.talend.sdk.component.studio-integration").getBundleContext();
        final ServiceReference<T> clientRef = context.getServiceReference(type);
        return context.getService(clientRef);
    }

    private static class EnrichedGlobalServiceRegister extends GlobalServiceRegister {

        private volatile IGenericWizardService wizardService;

        @Override
        public IService getService(final Class klass) {
            final IService service = super.getService(klass);
            if (klass == IGenericWizardService.class) {
                if (wizardService == null) {
                    synchronized (this) {
                        if (wizardService == null) {
                            // final WizardRegistry customService = new WizardRegistry();
                            wizardService = IGenericWizardService.class
                                    .cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                            service.getClass().getInterfaces(), (proxy, method, args) -> {
                                                try {
                                                    switch (method.getName()) {
                                                    case "createNodesFromComponentService":
                                                        final Object invoke = method.invoke(service, args);
                                                        // if (args[0] != null) {
                                                        // final Collection<RepositoryNode> nodes = customService
                                                        // .createNodes(RepositoryNode.class.cast(args[0]));
                                                        // Collection.class.cast(invoke).addAll(nodes);
                                                        // }
                                                        return invoke;
                                                    case "creatDynamicComposite":
                                                        if (args[1] != null && args[1] instanceof INode) {
                                                            INode node = (INode) args[1];
                                                            // decide whether it is v0 or v1
                                                            IComponent component = node.getComponent();
                                                            if (component != null
                                                                    && "org.talend.sdk.component.studio.ComponentModel"
                                                                            .equals(component.getClass().getName())) {
                                                                return creatComposite((Composite) args[0],
                                                                        (Element) args[1], (EComponentCategory) args[2],
                                                                        (boolean) args[3]);
                                                            } else { // it is v0 component, so call GenericWizardService
                                                                     // original method
                                                                return method.invoke(service, args);
                                                            }
                                                        }
                                                        return null;
                                                    default:
                                                        return method.invoke(service, args);
                                                    }
                                                } catch (final InvocationTargetException ite) {
                                                    throw ite.getTargetException();
                                                }
                                            }));
                        }
                    }
                }
                return wizardService;
            }
            return service;
        }

        /**
         * Substitutes {@link IGenericWizardService#creatDynamicComposite() original method
         */
        private Composite creatComposite(final Composite parent, final Element element,
                final EComponentCategory category, final boolean isCompactView) {

            return new TaCoKitComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NO_FOCUS, category, element,
                    isCompactView, null);
        }
    }

}
