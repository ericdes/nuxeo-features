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

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;

/**
 * Test the event conf service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestNXAuditEventsService extends RepositoryOSGITestCase {

    private NXAuditEvents serviceUnderTest;

    // protected final MBeanServer mbeanServer =
    // ManagementFactory.getPlatformMBeanServer();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.platform.audit");
        // deployBundle("org.nuxeo.runtime.management");
        // deployBundle("org.nuxeo.ecm.platform.management");

        NXAuditEventsService.persistenceProvider.setHibernateConfiguration(new TestHibernateConfiguration());
        serviceUnderTest = Framework.getService(NXAuditEvents.class);
        assertNotNull(serviceUnderTest);

        openRepository();
    }

    protected DocumentModel doCreateDocument() throws ClientException {
        DocumentModel rootDocument = coreSession.getRootDocument();
        DocumentModel model = coreSession.createDocumentModel(
                rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");
        DocumentModel source = coreSession.createDocument(model);
        coreSession.save();
        waitForEventsDispatched();
        return source;
    }

    public void testLogMessage() throws ClientException {
        DocumentModel source = doCreateDocument();
        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(source.getId());
        assertEquals(1, entries.size());

        LogEntry entry = entries.get(0);
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("project", entry.getDocLifeCycle());
        assertEquals("/youps", entry.getDocPath());
        assertEquals("File", entry.getDocType());
        assertEquals("documentCreated", entry.getEventId());
        assertEquals("Administrator", entry.getPrincipalName());
    }

    public void testsyncLogCreation() throws AuditException, ClientException {
        doCreateDocument();
        DocumentModel rootDocument = coreSession.getRootDocument();
        long count = serviceUnderTest.syncLogCreationEntries(
                getRepository().getName(), rootDocument.getPathAsString(), true);
        assertEquals(2, count);

        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(rootDocument.getId());
        assertEquals(1, entries.size());

        LogEntry entry = entries.get(0);
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertNull(entry.getComment());
        assertEquals("project", entry.getDocLifeCycle());
        assertEquals("/", entry.getDocPath());
        assertEquals("Root", entry.getDocType());
        assertEquals("documentCreated", entry.getEventId());
        assertEquals("system", entry.getPrincipalName());
    }

    // protected Set<ObjectName> doQuery(String name) {
    // String qualifiedName = ObjectNameFactory.getQualifiedName(name);
    // ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
    // return mbeanServer.queryNames(objectName, null);
    // }

    public void TODOtestCount() throws Exception {
        // CoreSession session = getCoreSession();
        // DocumentModel rootDocument = getCoreSession().getRootDocument();
        // DocumentModel model = session.createDocumentModel(
        // rootDocument.getPathAsString(), "youps", "File");
        // model.setProperty("dublincore", "title", "huum");
        // session.createDocument(model);
        // session.save();
        // waitForEventsDispatched();
        // ObjectName objectName =
        // AuditEventMetricFactory.getObjectName("documentCreated");
        // Long count = (Long) mbeanServer.getAttribute(objectName, "count");
        // assertEquals(new Long(1L), count);
    }

}
