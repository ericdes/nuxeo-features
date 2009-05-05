/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.repository.AbstractRepositoryTestCase;
import org.nuxeo.ecm.platform.annotations.repository.FakeNuxeoPrincipal;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class AnnotationRepositoryTest extends AbstractRepositoryTestCase {

    private static final String SERVER1 = "http://server1.com/nuxeo/";

    private static final String SERVER2 = "http://server2.com/nuxeo/";

    private DocumentModel version1;

    private VersionModel versionModel;

    private DocumentModel section;

    private final NuxeoPrincipal user = new FakeNuxeoPrincipal("bob");

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setUpRepository();
        createSection();
    }

    public void testTest() throws Exception {
        CoreSession session = getCoreSession();
        assertNotNull(session);
        DocumentModel myfileModel = session.createDocumentModel(
                session.getRootDocument().getPathAsString(), "999", "File");
        DocumentModel myfile = session.createDocument(myfileModel);
        assertNotNull(myfile);
        String u1 = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(myfile), true, SERVER1);
        assertNotNull(u1);
        String u2 = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(myfile), true, SERVER2);
        assertNotNull(u2);
        service.addAnnotation(getAnnotation(u1, 1), user, SERVER1);
        sameDocumentFrom2Servers(u1, u2);
        createVersion(session, myfile, "v1");
        newVersionSameAnnotations(session, myfile, u1);
        annotationOnNewVersion(u1);
    }

    private void annotationOnNewVersion(String u1) throws AnnotationException,
            IOException, URISyntaxException {
        annotation = service.addAnnotation(getAnnotation(u1, 2), user, SERVER1);
        assertNotNull(annotation);
        List<Annotation> annotations = service.queryAnnotations(new URI(u1),
                null, user);
        assertEquals(2, annotations.size());
        String versionUrl = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(version1), true, SERVER1);
        annotations = service.queryAnnotations(new URI(versionUrl), null, user);
        assertEquals(1, annotations.size());
    }

    private void newVersionSameAnnotations(CoreSession session,
            DocumentModel myfile, String u1) throws AnnotationException,
            URISyntaxException, ClientException {
        List<Annotation> annotations = service.queryAnnotations(new URI(u1),
                null, user);
        assertEquals(1, annotations.size());
        List<DocumentModel> versions = session.getVersions(myfile.getRef());
        assertEquals(1, versions.size());
        version1 = versions.get(0);
        String versionUrl = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(version1), true, SERVER1);
        assertNotNull(versionUrl);
        annotations = service.queryAnnotations(new URI(versionUrl), null, user);
        assertEquals(1, annotations.size());
    }

    private void createVersion(CoreSession session, DocumentModel myfile,
            String version) throws ClientException {
        versionModel = new VersionModelImpl();
        versionModel.setLabel(version);
        session.checkIn(myfile.getRef(), versionModel);
        session.checkOut(myfile.getRef());
        session.saveDocument(myfile);
        session.save();
        waitForAsyncExec();
    }

    private void createSection() throws ClientException {
        DocumentModel sectionModel = getCoreSession().createDocumentModel(
                getCoreSession().getRootDocument().getPathAsString(), "2",
                "Section");
        assertNotNull(sectionModel);
        section = getCoreSession().createDocument(sectionModel);
    }

    private void sameDocumentFrom2Servers(String u1, String u2)
            throws AnnotationException, URISyntaxException {
        List<Annotation> annotations = service.queryAnnotations(new URI(u1),
                null, user);
        assertEquals(1, annotations.size());
        annotations = service.queryAnnotations(new URI(u2), null, user);
        assertEquals(1, annotations.size());
    }

}
