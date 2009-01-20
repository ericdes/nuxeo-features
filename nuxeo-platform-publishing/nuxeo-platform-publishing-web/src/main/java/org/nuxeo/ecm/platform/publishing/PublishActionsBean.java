/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Narcis Paslaru
 *     Florent Guillaume
 *     Thierry Martins
 */

package org.nuxeo.ecm.platform.publishing;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModelTreeNode;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.publishing.api.PublishingInformation;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.ecm.webapp.security.PrincipalListManager;
import org.nuxeo.runtime.api.Framework;

/**
 * This Seam bean manages the publishing tab.
 *
 * @author Narcis Paslaru
 * @author Florent Guillaume
 * @author Thierry Martins
 */
@Name("publishActions")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class PublishActionsBean implements PublishActions, Serializable {
    private PublishingService publishingService;

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PublishActionsBean.class);

    private static final String PUBLISH_DOCUMENT = "PUBLISH_DOCUMENT";

    protected static final String DOMAIN_SECTIONS = "DOMAIN_SECTIONS";

    private static final String DOMAIN_TYPE = "Domain";

    @In(create = true)
    protected transient Principal currentUser;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient VersioningManager versioningManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected transient PrincipalListManager principalListManager;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @RequestParameter
    private String unPublishSectionRef;

    // hierarchy of existing sections
    private transient SelectDataModel sectionsModel;

    // list of selected sections
    private List<DocumentModelTreeNode> selectedSections = new ArrayList<DocumentModelTreeNode>();

    private transient DocumentMessageProducer docMsgProducer;

    private String comment;

    private static Set<String> sectionRootTypes;

    private static Set<String> sectionTypes;

    public Set<String> getSectionRootTypes() {
        if (sectionRootTypes == null) {
            sectionRootTypes = getTypeNamesForFacet("MasterPublishSpace");
            if (sectionRootTypes == null) {
                sectionRootTypes = new HashSet<String>();
                sectionRootTypes.add("SectionRoot");
            }
        }
        return sectionRootTypes;
    }

    public Set<String> getSectionTypes() {
        if (sectionTypes == null) {
            sectionTypes = getTypeNamesForFacet("PublishSpace");
            if (sectionTypes == null) {
                sectionTypes = new HashSet<String>();
                sectionTypes.add("Section");
            }
        }
        return sectionTypes;
    }

    private DocumentMessageProducer getDocumentMessageProducer()
            throws Exception {
        if (docMsgProducer == null) {
            docMsgProducer = DocumentMessageProducerBusinessDelegate.getRemoteDocumentMessageProducer();
        }
        return docMsgProducer;
    }

    /*
     * Called by publish_buttons.xhtml
     */
    public List<Action> getActionsForPublishDocument() {
        return webActions.getActionsList(PUBLISH_DOCUMENT);
    }

    private Set<String> getTypeNamesForFacet(String facetName) {
        TypeService schemaService;
        try {
            // XXX should use getService(SchemaManager.class)
            schemaService = (TypeService) Framework.getRuntime().getComponent(
                    TypeService.NAME);
        } catch (Exception e) {
            log.error("Exception in retrieving publish spaces : ", e);
            return null;
        }

        Set<String> publishRoots = schemaService.getTypeManager().getDocumentTypeNamesForFacet(
                facetName);
        if (publishRoots == null || publishRoots.isEmpty()) {
            return null;
        }
        return publishRoots;
    }

    protected void getSectionsSelectModel() throws ClientException {
        SelectionModelGetter selectionModelGetter = new SelectionModelGetter(
                documentManager, navigationContext.getCurrentDocument(),
                sectionRootTypes, sectionTypes, getSelectedSections(),
                queryModelActions.get("DOMAIN_SECTIONS"));
        selectionModelGetter.runUnrestricted();
        for (DocumentModelTreeNode node : selectionModelGetter.getTreeNodes()) {
            addSelectedSection(node);
        }
        setSectionsModel(selectionModelGetter.getDataModel());
    }

    // TODO move to protected
    public DocumentModelList getProxies(DocumentModel docModel)
            throws ClientException {
        if (docModel == null) {
            return null;
        } else {
            return documentManager.getProxies(docModel.getRef(), null);
        }
    }

    // TODO move to protected
    public List<PublishingInformation> getPublishingInformation(
            DocumentModel docModel) throws ClientException {
        DocumentModelList proxies = getProxies(docModel);
        if (proxies == null) {
            return null;
        }
        List<PublishingInformation> info = new ArrayList<PublishingInformation>(
                proxies.size());
        for (DocumentModel proxy : proxies) {
            DocumentRef parentRef = proxy.getParentRef();
            DocumentModel section = null;
            try {
                section = documentManager.getDocument(parentRef);
            } catch (ClientException e) {
            }
            info.add(new PublishingInformation(proxy, section));
        }
        return info;
    }

    /*
     * Called by action DOCUMENT_PUBLISH.
     */
    public String publishDocument() throws ClientException {
        DocumentModel docToPublish = navigationContext.getCurrentDocument();

        if (documentManager.getLock(docToPublish.getRef()) != null) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "error.document.locked.for.publish"));
            return null;
        }

        List<DocumentModelTreeNode> selectedSections = getSelectedSections();
        if (selectedSections.isEmpty()) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "publish.no_section_selected"));

            return null;
        }

        log.debug("selected " + selectedSections.size() + " sections");

        /*
         * Proxies for which we need a moderation. Let's request the moderation
         * after the document manager session has been saved to avoid conflicts
         * in between sync txn and async txn that can start before the end of
         * the sync txn.
         */
        List<DocumentModel> forModeration = new ArrayList<DocumentModel>();
        List<DocumentModel> alreadyPublished = new ArrayList<DocumentModel>();

        boolean published = false;
        for (DocumentModelTreeNode section : selectedSections) {
            DocumentModel proxy = getPublishedInSection(docToPublish,
                    section.getDocument());
            boolean moderation = proxy == null
                    || publishingService.isPublished(proxy);
            boolean candidate = false;
            if (proxy == null) {
                candidate = true;
                proxy = publishDocument(docToPublish, section.getDocument());
            }
            if (candidate && isReviewer(proxy)) {
                published = true;
            }
            if (moderation && !isReviewer(proxy)) {
                forModeration.add(proxy);
            }
        }

        if (published) {

            // notifyEvent(org.nuxeo.ecm.webapp.helpers.EventNames.
            // DOCUMENT_PUBLISHED,
            // null, comment,
            // null, docToPublish);

            comment = null;
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_published"),
                    resourcesAccessor.getMessages().get(docToPublish.getType()));
        }

        if (!forModeration.isEmpty()) {
            // notifyEvent(org.nuxeo.ecm.webapp.helpers.EventNames.
            // DOCUMENT_SUBMITED_FOR_PUBLICATION,
            // null, comment,
            // null, docToPublish);
            comment = null;
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "document_submitted_for_publication"),
                    resourcesAccessor.getMessages().get(docToPublish.getType()));
            for (DocumentModel proxy : forModeration) {
                notifyEvent(
                        org.nuxeo.ecm.webapp.helpers.EventNames.PROXY_PUSLISHING_PENDING,
                        null, comment, null, proxy);
            }
        }

        setSectionsModel(null);

        return null;
    }

    public boolean isReviewer(DocumentModel dm) throws ClientException {
        // TODO: introduce a specific publication reviewer permission
        return documentManager.hasPermission(dm.getRef(),
                SecurityConstants.WRITE_PROPERTIES);
    }

    // TODO move to protected
    public boolean isAlreadyPublishedInSection(DocumentModel doc,
            DocumentModel section) throws ClientException {
        // XXX We need refetch here for the new one but we should add this to
        // the seam cache somewhere.
        for (PublishingInformation each : getPublishingInformation(doc)) {
            // FIXME do a better comparison using ref directly. Need to test a
            // bit more since it appears to fail.
            // Furthermore, current DocumentModel implementation performs remote
            // call to get the Path.
            if (each.getSection().getPathAsString().equals(
                    section.getPathAsString())) {
                return true;
            }
        }
        return false;
    }

    private DocumentModel getPublishedInSection(DocumentModel doc,
            DocumentModel section) throws ClientException {
        for (PublishingInformation each : getPublishingInformation(doc)) {
            if (each.getSection().getPathAsString().equals(
                    section.getPathAsString())) {
                return each.getProxy();
            }
        }
        return null;
    }

    /*
     * Called by action WORKLIST_PUBLISH.
     */
    public String publishWorkList() throws ClientException {
        return publishDocumentList(DocumentsListsManager.DEFAULT_WORKING_LIST);
    }

    public String publishDocumentList(String listName) throws ClientException {

        List<DocumentModel> docs2Publish = documentsListsManager.getWorkingList(listName);
        DocumentModel target = navigationContext.getCurrentDocument();

        if (!getSectionTypes().contains(target.getType())) {
            return null;
        }

        int nbPublishedDocs = 0;
        for (DocumentModel doc : docs2Publish) {
            if (!documentManager.hasPermission(doc.getRef(),
                    SecurityConstants.READ_PROPERTIES)) {
                continue;
            }

            if (doc.isProxy()) {
                // TODO copy also copies security. just recreate a proxy.
                documentManager.copy(doc.getRef(), target.getRef(),
                        doc.getName());
                nbPublishedDocs++;
            } else {
                if (doc.hasFacet(FacetNames.PUBLISHABLE)) {
                    documentManager.publishDocument(doc, target);
                    nbPublishedDocs++;
                } else {
                    log.info("Attempted to publish non-publishable document "
                            + doc.getTitle());
                }
            }
        }

        Object[] params = { nbPublishedDocs };
        facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 "
                + resourcesAccessor.getMessages().get("n_published_docs"),
                params);

        if (nbPublishedDocs < docs2Publish.size()) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "selection_contains_non_publishable_docs"));
        }

        EventManager.raiseEventsOnDocumentChildrenChange(navigationContext.getCurrentDocument());
        return null;
    }

    /*
     * Called locally and also by FileManageActionsBean.moveWithId
     */
    public DocumentModel publishDocument(final DocumentModel docToPublish,
            final DocumentModel section) throws ClientException {

        // Must have READ on target section
        if (!documentManager.hasPermission(section.getRef(),
                SecurityConstants.READ)) {
            throw new ClientException(
                    "Cannot publish because not enough rights");
        }

        DocumentPublisher documentPublisher = new DocumentPublisher(
                documentManager, docToPublish, section, comment);

        /*
         * If not enough rights to creating content, bypass rights since READ is
         * enough for publishing.
         */
        if (!documentManager.hasPermission(section.getRef(),
                SecurityConstants.ADD_CHILDREN)) {
            documentPublisher.runUnrestricted();
        } else {
            documentPublisher.run();
        }

        return documentManager.getDocument(documentPublisher.proxyRef);
    }

    @Destroy
    public void destroy() {
    }

    /*
     * Called by Seam remoting.
     */
    @WebRemote
    public String processRemoteSelectRowEvent(String docRef, Boolean selection)
            throws ClientException {
        log.debug("Selection processed  : " + docRef);
        List<SelectDataModelRow> sections = getSectionsModel().getRows();

        SelectDataModelRow rowToSelect = null;
        DocumentModelTreeNode node = null;

        for (SelectDataModelRow d : sections) {
            if (((DocumentModelTreeNode) d.getData()).getDocument().getRef().toString().equals(
                    docRef)) {
                node = (DocumentModelTreeNode) d.getData();
                rowToSelect = d;
                break;
            }
        }
        if (node == null) {
            return "ERROR : DataNotFound";
        }

        rowToSelect.setSelected(selection);
        if (selection) {
            getSelectedSections().add(node);
        } else {
            getSelectedSections().remove(node);
        }

        return "OK";
    }

    /*
     * Called by document_publish.xhtml
     */
    @Factory(autoCreate = true, scope = ScopeType.EVENT, value = "currentPublishingSectionsModel")
    public SelectDataModel getSectionsModel() throws ClientException {
        if (sectionsModel == null) {
            getSectionsSelectModel();
        }
        return sectionsModel;
    }

    protected void setSectionsModel(SelectDataModel sectionsModel) {
        this.sectionsModel = sectionsModel;
    }

    @Observer(value = EventNames.DOCUMENT_SELECTION_CHANGED, create = false, inject = false)
    @BypassInterceptors
    public void cancelTheSections() {
        setSectionsModel(null);
        setSelectedSections(null);
        comment = null;
    }

    public List<DocumentModelTreeNode> getSelectedSections() {
        return selectedSections;
    }

    private void addSelectedSection(DocumentModelTreeNode section) {
        Boolean sectionAlreadySelected = false;
        for (DocumentModelTreeNode node : selectedSections) {
            if (node.getDocument().getRef().equals(
                    section.getDocument().getRef())) {
                sectionAlreadySelected = true;
                break;
            }
        }
        if (!sectionAlreadySelected) {
            selectedSections.add(section);
        }
    }

    public void setSelectedSections(List<DocumentModelTreeNode> selectedSections) {
        if (selectedSections != null) {
            this.selectedSections = selectedSections;
        }
    }

    public void notifyEvent(String eventId,
            Map<String, Serializable> properties, String comment,
            String category, DocumentModel dm) throws ClientException {

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }

        properties.put(CoreEventConstants.REPOSITORY_NAME,
                documentManager.getRepositoryName());
        properties.put(CoreEventConstants.SESSION_ID,
                documentManager.getSessionId());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                dm.getCurrentLifeCycleState());

        CoreEvent event = new CoreEventImpl(eventId, dm, properties,
                documentManager.getPrincipal(), category, comment);

        EventMessage message = new DocumentMessageImpl(dm, event);

        try {
            getDocumentMessageProducer().produce(message);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    // TODO move to protected
    public void unPublishDocument(DocumentModel proxy) throws ClientException {
        documentManager.removeDocument(proxy.getRef());
        documentManager.save();
        try {
            notifyEvent(
                    org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_UNPUBLISHED,
                    null, comment, null, navigationContext.getCurrentDocument());
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    // TODO move to protected
    public void unPublishDocuments(List<DocumentModel> documentsList)
            throws ClientException {
        Object[] params = { documentsList.size() };
        List<DocumentRef> documentsRef = new ArrayList<DocumentRef>();
        for (DocumentModel document : documentsList) {
            documentsRef.add(document.getRef());
            if (document.isProxy() && document.getSourceId() != null) {
                try {
                    String proxySourceId = documentManager.getDocument(
                            new IdRef(document.getSourceId())).getSourceId();
                    notifyEvent(
                            org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_UNPUBLISHED,
                            null,
                            null,
                            null,
                            documentManager.getDocument(new IdRef(proxySourceId)));

                } catch (Exception e) {
                    throw new ClientException(e);
                }
            }

        }

        // remove from the current selection list
        documentsListsManager.resetWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION);

        documentManager.removeDocuments(documentsRef.toArray(new DocumentRef[0]));
        documentManager.save();

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("n_unpublished_docs"),
                params);
    }

    /*
     * Called by document_publish.xhtml
     */
    public String unPublishDocument() throws ClientException {

        for (DocumentModelTreeNode section : getSelectedSections()) {

            if (section.getDocument().getRef().toString().equals(
                    unPublishSectionRef)) {

                for (DocumentModel doc : getProxies(navigationContext.getCurrentDocument())) {

                    if (doc.getParentRef().toString().equals(
                            unPublishSectionRef)) {
                        unPublishDocument(doc);
                        facesMessages.add(FacesMessage.SEVERITY_INFO,
                                resourcesAccessor.getMessages().get(
                                        "document_unpublished"),
                                resourcesAccessor.getMessages().get(
                                        doc.getType()));

                    }
                }
            }
        }
        setSectionsModel(null);
        setSelectedSections(null);
        return null;
    }

    /*
     * Called by action CURRENT_SELECTION_UNPUBLISH.
     */
    public void unPublishDocumentsFromCurrentSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION)) {
            unPublishDocuments(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION));
        } else {
            log.debug("No selectable Documents in context to process unpublish on...");
        }
        log.debug("Unpublish the selected document(s) ...");
    }

    /*
     * Called by section_clipboard.xhtml
     */
    public List<Action> getActionsForSectionSelection() {
        return webActions.getUnfiltredActionsList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION
                + "_LIST");
    }

    /*
     * Called by publish_buttons.xhtml
     */
    public String getComment() {
        return comment;
    }

    /*
     * Called by publish_buttons.xhtml
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    public void processSelectRowEvent(SelectDataModelRowEvent event)
            throws ClientException {
        // TODO Auto-generated method stub

    }

}
