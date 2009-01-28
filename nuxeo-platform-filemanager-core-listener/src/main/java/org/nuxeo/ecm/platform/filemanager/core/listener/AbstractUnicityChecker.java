package org.nuxeo.ecm.platform.filemanager.core.listener;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractUnicityChecker {

    protected FileManager fileManager;

    protected static Boolean unicityCheckEnabled = null;

    protected static final String DUPLICATED_FILE = "duplicatedFile";


    private static final Log log = LogFactory.getLog(AbstractUnicityChecker.class);

    protected void doUnicityCheck(DocumentModel doc2Check, CoreSession session, Event event) {

        List<String> xpathFields = null;

        try {
            xpathFields = getFileManagerService().getFields();
        }
        catch (ClientException e) {
            log.error("Error while getting xpaths config from FileManager", e);
            return;
        }

        if (xpathFields==null || xpathFields.size()==0) {
            unicityCheckEnabled=false;
            log.info("Unicity check has been automatically disabled");
            return;
        }

        Blob blob =null;

        for (String field : xpathFields) {

            try {
                blob = (Blob) doc2Check.getPropertyValue(field);
            }
            catch (PropertyNotFoundException pnfe) {
                continue;
            } catch (ClientException e) {
                log.error("Error while getting property", e);
                continue;
            }

            String digest = blob.getDigest();
            if (digest==null) {
                log.debug("Blob has no disgest, can not check for unicity");
                continue;
            }


            List<DocumentLocation> existingDocuments=null;

            try {
            existingDocuments = fileManager.findExistingDocumentWithFile(
                    session, doc2Check.getPathAsString(), digest,
                    session.getPrincipal());
            }
            catch (Exception e) {
                log.error("Error in FileManager unicity check execution",e);
                continue;
            }

            if (!existingDocuments.isEmpty()) {
                Iterator<DocumentLocation> existingDocumentsIterator = existingDocuments.iterator();
                while (existingDocumentsIterator.hasNext()) {
                    if (existingDocumentsIterator.next().getDocRef() == doc2Check.getRef()) {
                        existingDocumentsIterator.remove();
                    }
                }
                log.debug("Existing Documents[" + existingDocuments.size()
                        + "]");

                onDuplicatedDoc(session, session.getPrincipal(), doc2Check, existingDocuments, event);
        }


        }
    }


    protected abstract void onDuplicatedDoc(CoreSession session, Principal principal, DocumentModel newDoc, List<DocumentLocation> existingDocs, Event event);


    protected void raiseDuplicatedFileEvent(CoreSession session, Principal principal,
            DocumentModel newDoc, List<DocumentLocation> existingDocs) {

        DocumentEventContext ctx = new DocumentEventContext(session,principal, newDoc);

        Map<String, Serializable> props = new HashMap<String, Serializable>();

        props.put("category", DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY);
        props.put("duplicatedDocLocation", (Serializable) existingDocs);

        Event event = ctx.event(DUPLICATED_FILE);

        try {
            EventProducer producer = Framework.getService(EventProducer.class);
            producer.fireEvent(event);
        } catch (Exception e) {
            log.error("Error while sending duplication message", e);
        }
    }

    protected boolean isUnicityCheckEnabled(){
        if (unicityCheckEnabled==null) {
            try {
                unicityCheckEnabled = getFileManagerService().isUnicityEnabled();
            } catch (ClientException e) {
                log.error("Unable to get FileManagerService", e);
                unicityCheckEnabled=false;
            }
        }
        return unicityCheckEnabled;
    }

    private FileManager getFileManagerService() throws ClientException {
        if (fileManager == null) {
            fileManager = Framework.getRuntime().getService(FileManager.class);
        }
        if (fileManager == null) {
            log.error("Unable to get FileManager runtime service");
            throw new ClientException(
                    "Unable to get FileManager runtime service");
        }
        return fileManager;
    }
}
