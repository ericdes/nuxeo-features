/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.lifeCycle;

import java.util.Collection;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple test Case for MassLifeCycleChangeListener
 *
 * @author Julien Thimonier <jt@nuxeo.com>
 */
public class BulkLifeCycleChangeListenerTest extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openRepository();
        deployBundle("org.nuxeo.ecm.webapp.core");
    }

    protected void waitForAsyncExec() {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    public void testLifeCycleAPI() throws ClientException {

        DocumentModel folderDoc = getCoreSession().createDocumentModel("/",
                "testFolder", "Folder");
        folderDoc = getCoreSession().createDocument(folderDoc);
        DocumentModel testFile1 = getCoreSession().createDocumentModel(
                folderDoc.getPathAsString(), "testFile1", "File");
        testFile1 = getCoreSession().createDocument(testFile1);
        DocumentModel testFile2 = getCoreSession().createDocumentModel(
                folderDoc.getPathAsString(), "testFile2", "File");
        testFile2 = getCoreSession().createDocument(testFile2);

        getCoreSession().saveDocument(folderDoc);
        getCoreSession().saveDocument(testFile1);
        getCoreSession().saveDocument(testFile2);

        Collection<String> allowedStateTransitions = getCoreSession().getAllowedStateTransitions(
                folderDoc.getRef());
        assertTrue(allowedStateTransitions.contains("approve"));

        assertTrue(getCoreSession().followTransition(folderDoc.getRef(),
                "approve"));

        getCoreSession().save();
        waitForAsyncExec();

        //Check that the MassCycleListener has changed child files to approved
        assertEquals("approved", getCoreSession().getCurrentLifeCycleState(
                testFile1.getRef()));
        assertEquals("approved", getCoreSession().getCurrentLifeCycleState(
                testFile2.getRef()));
    }

}
