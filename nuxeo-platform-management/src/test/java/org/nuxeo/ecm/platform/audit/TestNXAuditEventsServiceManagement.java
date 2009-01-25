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
 * $Id: TestEventConfService.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.management.AuditEventMetricFactory;
import org.nuxeo.ecm.platform.events.DocumentMessageFactory;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourcePublisherService;

/**
 * Test the event conf service.
 * 
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestNXAuditEventsServiceManagement extends RepositoryOSGITestCase {

    private NXAuditEvents serviceUnderTest;

    protected DocumentModel rootDocument;

    protected DocumentModel source;

    protected DocumentMessage message;

    protected CoreEvent event;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.usermanager");

        deployBundle("org.nuxeo.runtime.management");

        deployBundle("org.nuxeo.ecm.platform.scheduler.core");

        deployBundle("org.nuxeo.ecm.core.event.compat");
        
        deployBundle("org.nuxeo.ecm.platform.audit");

        deployBundle("org.nuxeo.ecm.platform.management");



        NXAuditEventsService.persistenceProvider.setHibernateConfiguration(new TestHibernateConfiguration());

        serviceUnderTest = Framework.getService(NXAuditEvents.class);

        assertNotNull(serviceUnderTest);

        openRepository();
        CoreSession session = getCoreSession();
        rootDocument = getCoreSession().getRootDocument();

        DocumentModel model = session.createDocumentModel(
                rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");
        source = session.createDocument(model);
        session.save();
        event = new CoreEventImpl("documentCreated", source, null,
                session.getPrincipal(), null, null);
        message = DocumentMessageFactory.createDocumentMessage(source, event);
    }

    protected final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    @SuppressWarnings("unchecked")
    protected Set<ObjectName> doQuery(String name) {
        String qualifiedName = ObjectNameFactory.getQualifiedName(name);
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        return mbeanServer.queryNames(objectName, null);

    }

    public void testCount() throws Exception {
        ((NXAuditEventsService) serviceUnderTest).logMessage(getCoreSession(),
                message);
        ObjectName objectName = AuditEventMetricFactory.getObjectName(message.getEventId());
        Long count =  (Long)mbeanServer.getAttribute(objectName, "count");
        assertEquals(new Long(1L), count);
    }

}
