/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestTransformComponent.java 2692 2006-09-08 16:13:51Z janguenot $
 */

package org.nuxeo.ecm.platform.events;

import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.EventListener;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test the JMS producer service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestJMSProducerService extends NXRuntimeTestCase {

    private static final String JMS_LISTENER_NAME = "jmslistener";

    private DocumentMessageProducer service;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        /** NXCore descriptors * */
        deployContrib("org.nuxeo.ecm.platform.events.tests", "CoreService.xml");
        deployContrib("org.nuxeo.ecm.platform.events.tests", "TypeService.xml");
        deployContrib("org.nuxeo.ecm.platform.events.tests",
                "RepositoryService.xml");
        deployContrib("org.nuxeo.ecm.platform.events.tests",
                "CoreExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.events.tests",
                "CoreTestExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.events.tests",
                "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.events.tests",
                "CoreEventListenerService.xml");

        deployContrib("org.nuxeo.ecm.platform.events",
                "OSGI-INF/nxevents-jms-service.xml");

        service = DocumentMessageProducerBusinessDelegate.getLocalDocumentMessageProducer();
    }

    private static CoreEventListenerService getRepositoryListenerService() {
        return Framework.getLocalService(CoreEventListenerService.class);
    }

    public void testServiceRegistration() {
        assertNotNull(service);
    }

    public void testJMSListenerRegistration() {
        CoreEventListenerService listenerService = getRepositoryListenerService();
        EventListener jmsListener = listenerService.getEventListenerByName(JMS_LISTENER_NAME);
        assertNotNull(jmsListener);
    }

}
