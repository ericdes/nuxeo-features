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
 *     anguenot
 *
 * $Id: WSAuditBean.java 30185 2008-02-14 17:56:36Z tdelprat $
 */

package org.nuxeo.ecm.platform.audit.ws;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

import static org.nuxeo.ecm.core.api.event.DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;

import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.api.delegate.AuditLogsServiceDelegate;
import org.nuxeo.ecm.platform.audit.ws.api.WSAudit;
import org.nuxeo.ecm.platform.ws.AbstractNuxeoWebService;

/**
 * Audit Web Service bean.
 * 
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 * 
 */
@Stateless
@SerializedConcurrentAccess
@Local(WSAudit.class)
@Remote(WSAudit.class)
@WebService(name = "WSAuditInterface", serviceName = "WSAuditService")
@SOAPBinding(style = Style.DOCUMENT)
public class WSAuditBean extends AbstractNuxeoWebService implements WSAudit {

    private static final long serialVersionUID = 1L;

    private transient Logs logsBean;

    protected final Logs getLogsBean() throws AuditException {
        logsBean = AuditLogsServiceDelegate.getRemoteAuditLogsService();
        if (logsBean == null) {
            throw new AuditException("Cannot find log remote bean...");
        }
        return logsBean;
    }

    @WebMethod
    public ModifiedDocumentDescriptor[] listModifiedDocuments(
            @WebParam(name = "sessionId")
            String sessionId, @WebParam(name = "dataRangeQuery")
            String dateRangeQuery) throws AuditException {

        try {
            initSession(sessionId);
        } catch (ClientException ce) {
            throw new AuditException(ce.getMessage(), ce);
        }

        BatchInfo batchInfo = BatchHelper.getBatchInfo(sessionId,
                dateRangeQuery);

        List<LogEntry> logEntries = getLogsBean().queryLogsByPage(null,
                batchInfo.getPageDateRange(), "eventDocumentCategory", null,
                batchInfo.getNextPage(), batchInfo.getPageSize());
        if (logEntries.size() < batchInfo.getPageSize()) {
            // we are at the end of the batch
            // ==> reset the batch
            BatchHelper.resetBatchInfo(sessionId);
        } else {
            // set the batchInfo ready for next call
            batchInfo.prepareNextCall();
        }

        List<ModifiedDocumentDescriptor> ldocs = new ArrayList<ModifiedDocumentDescriptor>();
        Set<String> uuids = new HashSet<String>();
        for (LogEntry logEntry : logEntries) {
            if (!uuids.contains(logEntry.getDocUUID())) {
                uuids.add(logEntry.getDocUUID());
                ldocs.add(new ModifiedDocumentDescriptor(
                        logEntry.getEventDate(), logEntry.getDocType(),
                        logEntry.getDocUUID()));
            }
        }

        ModifiedDocumentDescriptor[] docs = new ModifiedDocumentDescriptor[ldocs.size()];
        ldocs.toArray(docs);

        return docs;
    }

    @WebMethod
    public ModifiedDocumentDescriptorPage listModifiedDocumentsByPage(
            @WebParam(name = "sessionId")
            String sessionId, @WebParam(name = "dataRangeQuery")
            String dateRangeQuery, @WebParam(name = "docPath")
            String path, @WebParam(name = "pageIndex")
            int page, @WebParam(name = "pageSize")
            int pageSize) throws AuditException {

        try {
            initSession(sessionId);
        } catch (ClientException ce) {
            throw new AuditException(ce.getMessage(), ce);
        }

        List<LogEntry> logEntries = getLogsBean().queryLogsByPage(null,
                dateRangeQuery, "eventDocumentCategory", path, page, pageSize);

        boolean hasMorePage = false;
        if (logEntries.size() < pageSize) {
            hasMorePage = false;
        } else {
            hasMorePage = true;
        }

        List<ModifiedDocumentDescriptor> ldocs = new ArrayList<ModifiedDocumentDescriptor>();
        Set<String> uuids = new HashSet<String>();
        for (LogEntry logEntry : logEntries) {
            if (!uuids.contains(logEntry.getDocUUID())) {
                uuids.add(logEntry.getDocUUID());
                ldocs.add(new ModifiedDocumentDescriptor(
                        logEntry.getEventDate(), logEntry.getDocType(),
                        logEntry.getDocUUID()));
            }
        }

        ModifiedDocumentDescriptor[] docs = new ModifiedDocumentDescriptor[ldocs.size()];
        ldocs.toArray(docs);

        return new ModifiedDocumentDescriptorPage(docs, page, hasMorePage);
    }

    @WebMethod
    public EventDescriptorPage listEventsByPage(@WebParam(name = "sessionId")
    String sessionId, @WebParam(name = "dataRangeQuery")
    String dateRangeQuery, @WebParam(name = "pageIndex")
    int page, @WebParam(name = "pageSize")
    int pageSize) throws AuditException {

        try {
            initSession(sessionId);
        } catch (ClientException ce) {
            throw new AuditException(ce.getMessage(), ce);
        }

        List<LogEntry> logEntries = getLogsBean().queryLogsByPage(null,
                dateRangeQuery, null, null, page, pageSize);
        boolean hasMorePage = false;
        if (logEntries.size() < pageSize) {
            hasMorePage = false;
        } else {
            hasMorePage = true;
        }

        List<EventDescriptor> events = new ArrayList<EventDescriptor>();

        for (LogEntry logEntry : logEntries) {
            events.add(new EventDescriptor(logEntry.getEventId(),
                    logEntry.getEventDate(), logEntry.getDocPath(),
                    logEntry.getDocUUID()));
        }

        EventDescriptor[] evts = new EventDescriptor[events.size()];
        events.toArray(evts);

        return new EventDescriptorPage(evts, page, hasMorePage);
    }

    @WebMethod
    public EventDescriptorPage listDocumentEventsByPage(
            @WebParam(name = "sessionId")
            String sessionId, @WebParam(name = "dataRangeQuery")
            String dateRangeQuery, @WebParam(name = "startDate")
            String startDate, @WebParam(name = "path")
            String path, @WebParam(name = "pageIndex")
            int page, @WebParam(name = "pageSize")
            int pageSize) throws AuditException {

        try {
            initSession(sessionId);
        } catch (ClientException ce) {
            throw new AuditException(ce.getMessage(), ce);
        }
        List<LogEntry> logEntries;
        if (dateRangeQuery != null && dateRangeQuery.length() > 0) {
            logEntries = getLogsBean().queryLogsByPage(null, dateRangeQuery,
                    EVENT_DOCUMENT_CATEGORY, path, page, pageSize);
        } else {
            Date limit = DateParser.parseW3CDateTime(startDate);            
            logEntries = getLogsBean().queryLogsByPage(null, limit,
                    EVENT_DOCUMENT_CATEGORY, path, page, pageSize);
        }
        boolean hasMorePage = false;
        if (logEntries.size() < pageSize) {
            hasMorePage = false;
        } else {
            hasMorePage = true;
        }

        List<EventDescriptor> events = new ArrayList<EventDescriptor>();

        for (LogEntry logEntry : logEntries) {
            events.add(new EventDescriptor(logEntry.getEventId(),
                    logEntry.getEventDate(), logEntry.getDocPath(),
                    logEntry.getDocUUID()));
        }

        EventDescriptor[] evts = new EventDescriptor[events.size()];
        events.toArray(evts);

        return new EventDescriptorPage(evts, page, hasMorePage);
    }

    @WebMethod
    public EventDescriptorPage queryEventsByPage(@WebParam(name = "sessionId")
    String sessionId, @WebParam(name = "whereClause")
    String whereClause, @WebParam(name = "pageIndex")
    int page, @WebParam(name = "pageSize")
    int pageSize) throws AuditException {

        try {
            initSession(sessionId);
        } catch (ClientException ce) {
            throw new AuditException(ce.getMessage(), ce);
        }

        List<LogEntry> logEntries = getLogsBean().nativeQueryLogs(whereClause,
                page, pageSize);
        boolean hasMorePage = false;
        if (logEntries.size() < pageSize) {
            hasMorePage = false;
        } else {
            hasMorePage = true;
        }

        List<EventDescriptor> events = new ArrayList<EventDescriptor>();

        for (LogEntry logEntry : logEntries) {
            events.add(new EventDescriptor(logEntry.getEventId(),
                    logEntry.getEventDate(), logEntry.getDocPath(),
                    logEntry.getDocUUID()));
        }

        EventDescriptor[] evts = new EventDescriptor[events.size()];
        events.toArray(evts);

        return new EventDescriptorPage(evts, page, hasMorePage);
    }

}
