package org.nuxeo.ecm.platform.filemanager;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;

public class TestVersionning extends RepositoryOSGITestCase {

    protected DocumentModel destWS;
    protected DocumentModel wsRoot;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployContrib(TestConstants.FILEMANAGER_TEST_BUNDLE,
                "ecm-types-test-contrib.xml");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        deployBundle("org.nuxeo.ecm.platform.versioning.api");
        deployBundle("org.nuxeo.ecm.platform.versioning");
        openRepository();

        createTestDocuments();
    }

    private void createTestDocuments() throws Exception {
        wsRoot = coreSession.getDocument(new PathRef(
                "default-domain/workspaces"));

        DocumentModel ws = coreSession.createDocumentModel(wsRoot
                .getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = coreSession.createDocument(ws);

        destWS = ws;
    }

    public void testVersioning() throws Exception {

        FileManager fm = Framework.getService(FileManager.class);
        Blob blob = new StringBlob("Something");
        blob.setMimeType("something");
        blob.setFilename("mytest.something");
        DocumentModel doc = fm.createDocumentFromBlob(coreSession, blob, destWS
                .getPathAsString(), true, "mytest.something");

        assertEquals("mytest", doc.getTitle());

        VersioningManager vm = Framework
                .getLocalService(VersioningManager.class);
        String vl = vm.getVersionLabel(doc);
        assertEquals("1.0", vl);

        doc = fm.createDocumentFromBlob(coreSession, blob, destWS
                .getPathAsString(), true, "mytest.something");

        String vl2 = vm.getVersionLabel(doc);
        assertEquals("1.1", vl2);

        blob.setFilename("mytest2.something");
        doc = fm.createDocumentFromBlob(coreSession, blob, destWS
                .getPathAsString(), true, "mytest2.something");
        vl = vm.getVersionLabel(doc);
        assertEquals("1.0", vl);

        blob.setFilename("mytxt.txt");
        blob.setMimeType("text/plain");
        doc = fm.createDocumentFromBlob(coreSession, blob, destWS
                .getPathAsString(), true, "mytxt.txt");
        assertEquals("Note", doc.getType());
        vl = vm.getVersionLabel(doc);
        assertEquals("1.0", vl);

        doc = fm.createDocumentFromBlob(coreSession, blob, destWS
                .getPathAsString(), true, "mytxt.txt");
        vl = vm.getVersionLabel(doc);
        assertEquals("1.1", vl);
    }

}
