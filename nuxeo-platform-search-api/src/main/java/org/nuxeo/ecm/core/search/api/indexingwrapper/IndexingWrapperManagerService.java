package org.nuxeo.ecm.core.search.api.indexingwrapper;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface IndexingWrapperManagerService {

	DocumentModel getIndexingWrapper(DocumentModel doc);
}
