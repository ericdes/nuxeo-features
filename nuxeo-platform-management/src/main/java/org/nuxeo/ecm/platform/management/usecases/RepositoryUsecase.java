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
 *     matic
 */
package org.nuxeo.ecm.platform.management.usecases;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 *
 */
public class RepositoryUsecase implements Usecase {

    public void runCase(CoreSession session) throws ClientException {
        DocumentModel rootDocument = session.getRootDocument();
        DocumentModel model = session.createDocumentModel(
                rootDocument.getPathAsString(),
                RepositoryUsecase.class.getSimpleName(), "File");
        DocumentRef ref = model.getRef();
        model.setProperty("dublincore", "title", "huum");
        model.setProperty("uid", "major_version",1L);
        session.createDocument(model);
        ACP acp = model.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(SecurityConstants.EVERYONE,
                SecurityConstants.EVERYTHING, false));
        session.setACP(ref, acp, true);
        session.save();
        session.removeDocument(model.getRef());
        session.save();
    }

    public void init(Object service) {
    }

}
