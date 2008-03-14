/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.comment.api;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public interface CommentManager {

    List<DocumentModel> getComments(DocumentModel docModel) throws ClientException;

    List<DocumentModel> getComments(DocumentModel docModel, DocumentModel parent)
            throws ClientException;

    /**
     * @deprecated CommentManager cannot find the author if invoked remote
     * so one should use CommentManager.createComment(docModel, comment, author)
     */
    @Deprecated
    DocumentModel createComment(DocumentModel docModel,
            String comment) throws ClientException;

    DocumentModel createComment(DocumentModel docModel,
            String comment, String author) throws ClientException;

    DocumentModel createComment(DocumentModel docModel, DocumentModel comment)
            throws ClientException;

    DocumentModel createComment(DocumentModel docModel, DocumentModel parent,
            DocumentModel child) throws ClientException;

    void deleteComment(DocumentModel docModel, DocumentModel comment) throws ClientException;

}