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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.security.Principal;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActionsBean;

@Name("startupHelper")
@Scope(SESSION)
// TODO @Install(precedence=FRAMEWORK)
public class StartupHelper implements Serializable {

    protected static final String SERVERS_VIEW = "view_servers";

    protected static final String DOMAINS_VIEW = "view_domains";

    protected static final String DASHBOARD_VIEW = "user_dashboard";

    protected static final String DOMAIN_TYPE = "Domain";

    private static final long serialVersionUID = 3248972387619873245L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(StartupHelper.class);

    @In(create = true)
    protected transient RepositoryManager repositoryManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In
    protected transient Context sessionContext;

    @In(create = true)
    ConversationIdGenerator conversationIdGenerator;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    /**
     * Initializes the context with the principal id, and try to connect to the
     * default server if any. If several servers are available, let the user
     * choose.
     *
     * @return the view_id of the contextually computed startup page
     * @throws ClientException
     */
    public String initServerAndFindStartupPage() throws ClientException {

        setupCurrentUser();

        // if more than one repo : display the server selection screen
        if (repositoryManager.getRepositories().size() > 1) {
            return SERVERS_VIEW;
        }

        // we try to select the server to go to the next screen
        if (navigationContext.getCurrentServerLocation() == null) {
            // update location
            RepositoryLocation repLoc = new RepositoryLocation(
                    repositoryManager.getRepositories().iterator().next().getName());
            navigationContext.setCurrentServerLocation(repLoc);
        }

        // the Repository Location is initialized, skip the first screen
        return DOMAINS_VIEW;
    }

    /**
     * Initializes the context with the principal id, and tries to connect to the
     * default server if any then: - if the server has several domains, redirect
     * to the list of domains - if the server has only one domain, select it and
     * redirect to viewId - if the server is empty, create a new domain with
     * title 'domainTitle' and redirect to it on viewId.
     * <p>
     * If several servers are available, let the user choose.
     *
     * @return the view id of the contextually computed startup page
     * @throws ClientException
     */
    @Begin(id = "#{conversationIdGenerator.nextMainConversationId}", join = true)
    public String initDomainAndFindStartupPage(String domainTitle, String viewId)
            throws ClientException {

        // delegate server initialized to the default helper
        String result = initServerAndFindStartupPage();

        if (!DOMAINS_VIEW.equals(result)) {
            // something went wrong during server lookup do not try to go
            // further
            return result;
        }
        if (documentManager == null) {
            documentManager = navigationContext.getOrCreateDocumentManager();
        }

        // get the domains from selected server
        DocumentModel rootDocument = documentManager.getRootDocument();

        if (!documentManager.hasPermission(rootDocument.getRef(),
                SecurityConstants.READ_CHILDREN)) {
            // user cannot see the root but maybe she can see contained
            // documents thus forwarding her to her dashboard
            return DASHBOARD_VIEW;
        }

        FacetFilter facetFilter = new FacetFilter(
                FacetNames.HIDDEN_IN_NAVIGATION, false);
        LifeCycleFilter lcFilter = new LifeCycleFilter(ClipboardActionsBean.DELETED_LIFECYCLE_STATE,false);
        CompoundFilter complexFilter = new CompoundFilter(facetFilter,lcFilter);
        DocumentModelList domains = documentManager.getChildren(
                rootDocument.getRef(), null, SecurityConstants.READ, complexFilter, null);

        if (domains.size() == 1) {
            // select and go to the unique domain
            return navigationContext.navigateToDocument(domains.get(0), viewId);
        }

        // zero or several domains: let the user decide what to do
        navigationContext.navigateToDocument(rootDocument);
        return DOMAINS_VIEW;
    }

    public void setupCurrentUser() {
        Principal currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        sessionContext.set("currentUser", currentUser);
    }

}
