/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front.cxf;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;

@ApplicationScoped
public class CxfExtractor {

    @Inject
    private Bus bus;

    public DestinationRegistry getRegistry() {
        try {
            final HTTPTransportFactory transportFactory = HTTPTransportFactory.class
                    .cast(bus
                            .getExtension(DestinationFactoryManager.class)
                            .getDestinationFactory("http://cxf.apache.org/transports/http" + "/configuration"));
            return transportFactory.getRegistry();
        } catch (final BusException e) {
            throw new IllegalStateException(e);
        }
    }
}
