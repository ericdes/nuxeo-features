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

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.util.List;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import static org.jboss.seam.ScopeType.EVENT;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.interfaces.ejb.ECServer;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.w3c.dom.Element;

@Name("browseRestlet")
@Scope(EVENT)
public class BrowseRestlet extends BaseNuxeoRestlet {

    @In(create = true)
    protected NavigationContext navigationContext;

    protected CoreSession documentManager;

    @In(required = true, create = true)
    protected ECServer ecServer;

    @Override
    public void handle(Request req, Response res)  {
        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        DocumentModel dm=null;
        DOMDocumentFactory domfactory = new DOMDocumentFactory();

        DOMDocument result = (DOMDocument) domfactory.createDocument();
        //Element root = result.createElement("browse");
        //result.setRootElement((org.dom4j.Element) root);

        if (repo == null || repo.equals("*")) {
            List<RepositoryLocation> repos = ecServer.getAvailableRepositoryLocations();

            Element serversNode = result.createElement("avalaibleServers");
            result.setRootElement((org.dom4j.Element) serversNode);

            for (RepositoryLocation availableRepo : repos) {
                Element server = result.createElement("server");
                server.setAttribute("title", availableRepo.getName());
                server.setAttribute("url", getRelURL(availableRepo.getName(), "*"));
                serversNode.appendChild(server);
            }
        } else {
            try {
                navigationContext.setCurrentServerLocation(new RepositoryLocation(repo));
                documentManager = navigationContext.getOrCreateDocumentManager();
                if (docid == null || docid.equals("*")) {
                    dm = documentManager.getRootDocument();
                } else {
                    dm = documentManager.getDocument(new IdRef(docid));
                }
            } catch (ClientException e) {
                handleError(result, res, e);
                return;
            }

            Element current = result.createElement("document");
            current.setAttribute("title", dm.getTitle());
            current.setAttribute("type", dm.getType());
            current.setAttribute("id", dm.getId());
            current.setAttribute("url", getRelURL(repo, dm.getRef().toString()));
            result.setRootElement((org.dom4j.Element) current);

            if (dm.isFolder()) {
                //Element childrenElem = result.createElement("children");
                //root.appendChild(childrenElem);

                DocumentModelList children;
                try {
                    children = documentManager.getChildren(dm.getRef());
                } catch (ClientException e) {
                    handleError(result, res, e);
                    return;
                }

                for (DocumentModel child : children) {
                    Element el = result.createElement("document");
                    el.setAttribute("title", child.getTitle());
                    el.setAttribute("type", child.getType());
                    el.setAttribute("id", child.getId());
                    el.setAttribute("url", getRelURL(repo, child.getRef().toString()));
                    current.appendChild(el);
                }
            }
        }
        res.setEntity(result.asXML(), MediaType.TEXT_XML);
    }

    private static String getRelURL(String repo, String uuid) {
        return '/' + repo + '/' + uuid;
    }

}