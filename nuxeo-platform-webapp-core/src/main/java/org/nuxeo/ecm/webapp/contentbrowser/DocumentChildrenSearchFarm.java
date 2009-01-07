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
 * $Id$
 */
package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.pagination.Pages;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;

/**
 * Creates ResultsProvider for the children of the current document using
 * SearchService.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@Name("documentChildrenSearchFarm")
@Scope(SESSION)
public class DocumentChildrenSearchFarm extends InputController implements
        ResultsProviderFarm, Serializable {

    private static final long serialVersionUID = 8331654530334881666L;

    // Result providers
    // public static final String CHILDREN_BY_SEARCH =
    // "CURRENT_DOC_CHILDREN_BY_SEARCH";

    private static final String FILTER_SCHEMA_NAME = "browsing_filters";

    private static final String FILTER_FIELD_NAME_PARENT_ID = "query_parentId";

    @In(create = true)
    private transient QueryModelActions queryModelActions;

    @In(create = true)
    private transient ResultsProvidersCache resultsProvidersCache;

    public Pages<DocumentModel> getResultsProvider(String name)
            throws ClientException {
        return getResultsProvider(name, null);
    }

    public Pages<DocumentModel> getResultsProvider(String name,
            SortInfo sortInfo) throws ClientException {
        final DocumentModel currentDoc = navigationContext.getCurrentDocument();

        if (DocumentChildrenStdFarm.CHILDREN_BY_COREAPI.equals(name)) {
            Pages<DocumentModel> provider = getChildrenResultsProviderQMPattern(name, currentDoc, sortInfo);
            provider.setName(name);
            return provider;
        } else {
            throw new ClientException("Unknown provider: " + name);
        }
    }

    /**
     * Usable with a queryModel that defines a pattern NXQL.
     */
    private Pages<DocumentModel> getChildrenResultsProviderQMPattern(
            String queryModelName, DocumentModel parent, SortInfo sortInfo) throws ClientException {

        final String parentId = parent.getId();
        Object[] params = { parentId };
        return getResultsProvider(queryModelName, params, sortInfo);
    }

    private Pages<DocumentModel> getResultsProvider(String qmName,
            Object[] params, SortInfo sortInfo) throws ClientException {
        try {
            QueryModel qm = queryModelActions.get(qmName);
            return qm.getResultsProvider(params, sortInfo);
        } catch (QueryException e) {
            throw new ClientException(String.format("Invalid search query. "
                    + "Check the '%s' QueryModel configuration", qmName), e);
        }
    }

    /**
     * Usable with a queryModel that defines a WhereClause with predicates.
     */
    protected Pages<DocumentModel> getChildrenResultsProviderQMPred(
            String queryModelName, DocumentModel currentDoc)
            throws ClientException {
        QueryModel qm = queryModelActions.get(queryModelName);
        if (qm == null) {
            throw new ClientException("no QueryModel registered under name: "
                    + queryModelName);
        }

        // Invalidation code. TODO Would be better to listen to an event
        String currentRef = currentDoc.getRef().toString();
        if (!currentRef.equals(qm.getProperty(FILTER_SCHEMA_NAME,
                FILTER_FIELD_NAME_PARENT_ID))) {
            qm.setProperty(FILTER_SCHEMA_NAME, FILTER_FIELD_NAME_PARENT_ID,
                    currentRef);
            resultsProvidersCache.invalidate(queryModelName);
        }

        return resultsProvidersCache.get(queryModelName);
    }

}
