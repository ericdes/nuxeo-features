/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     rdarlea
 */
package org.nuxeo.webengine.sites.fragments;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.models.WebpageListModel;
import org.nuxeo.webengine.sites.models.WebpageModel;
import org.nuxeo.webengine.sites.utils.SiteConstants;
import org.nuxeo.webengine.sites.utils.SiteQueriesColection;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment related to retrieving a
 * certain number of pages with information about the last modified
 * <b>WebPage</b>-s that are made under an <b>Workspace</b> or <b>WebPage</b>
 * 
 * @author rux
 * 
 */
public class MinisiteLastPublishedFragment extends AbstractFragment {

    private int noPages = 5;

    private int noWordsFromContent = 50;

    /**
     * Retrieves a certain number of pages with information about the last
     * modified <b>WebPage</b>-s that are made under an <b>Workspace</b> or
     * <b>WebPage</b> that is received as parameter.
     */
    @Override
    public Model getModel() throws ModelException {
        WebpageListModel model = new WebpageListModel();
        if (WebEngine.getActiveContext() != null) {
            WebContext ctx = WebEngine.getActiveContext();
            CoreSession session = ctx.getCoreSession();
            DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                    DocumentModel.class);

            WebpageModel webpageModel = null;
            String name = null;
            String path = null;
            String description = null;
            String content = null;
            String author = null;
            String numberComments = null;

            try {
                DocumentModel ws = SiteUtils.getFirstWorkspaceParent(session,
                        documentModel);
                DocumentModelList webPages = SiteQueriesColection.queryLastModifiedPages(
                        session, ws.getPathAsString(), noPages);

                for (DocumentModel webPage : webPages) {
                    if (!webPage.getCurrentLifeCycleState().equals(
                            SiteConstants.DELETED)) {

                        name = SiteUtils.getString(webPage, "dc:title");
                        path = SiteUtils.getPagePath(ws, webPage);
                        description = SiteUtils.getString(webPage,
                                "dc:description");

                        content = SiteUtils.getFistNWordsFromString(
                                SiteUtils.getString(webPage,
                                        SiteConstants.WEBPAGE_CONTENT),
                                noWordsFromContent);
                        author = SiteUtils.getString(webPage, "dc:creator");
                        GregorianCalendar modificationDate = SiteUtils.getGregorianCalendar(
                                webPage, "dc:modified");
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                                "dd MMMM",
                                WebEngine.getActiveContext().getLocale());
                        String formattedString = simpleDateFormat.format(modificationDate.getTime());
                        String[] splittedFormatterdString = formattedString.split(" ");
                        numberComments = Integer.toString(SiteUtils.getNumberCommentsForPage(
                                session, webPage));

                        webpageModel = new WebpageModel(name, path,
                                description, content, author,
                                splittedFormatterdString[0],
                                splittedFormatterdString[1], numberComments);

                        model.addItem(webpageModel);

                    }
                }
            } catch (Exception e) {
                throw new ModelException(e);
            }

        }
        return model;
    }
}
