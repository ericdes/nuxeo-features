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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.threading.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.BrowsingTask;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.IndexingHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * 
 */
public class UnIndexingBrowseTask extends AbstractTask implements BrowsingTask {

    private static final Log log = LogFactory.getLog(UnIndexingBrowseTask.class);

    public UnIndexingBrowseTask(DocumentRef docRef, String repositoryName) {
        super(docRef, repositoryName);
    }

    public void run() {
        log.debug("Unindexing browse task started");

        try {
            DocumentModel dm = getCoreSession().getDocument(docRef);
            IndexingHelper.recursiveUnindex(dm);
            log.debug("Unindexing browse task done for document: "
                    + dm.getTitle());
        } catch (Exception e) {
            log.error(e);
        }
    }

}
