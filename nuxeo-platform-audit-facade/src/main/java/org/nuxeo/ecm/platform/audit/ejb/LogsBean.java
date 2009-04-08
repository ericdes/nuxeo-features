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
 * $Id:LogsBean.java 1583 2006-08-04 10:26:40Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.ejb;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.api.remote.LogsRemote;
import org.nuxeo.ecm.platform.audit.ejb.local.LogsLocal;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;

/**
 * Stateless bean allowing to query the logs.
 * <p>
 * This class takes advantage of EJBQL.
 *
 * :XXX: http://jira.nuxeo.org/browse/NXP-514
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Stateless
@Local(LogsLocal.class)
@Remote(LogsRemote.class)
public class LogsBean implements Logs {

    @PersistenceContext(unitName = "NXAudit")
    private EntityManager em;

    protected NXAuditEventsService service() {
        return (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
    }

    public void addLogEntries(List<LogEntry> entries) {
        service().addLogEntries(null, entries);
    }

    public List<LogEntry> getLogEntriesFor(String uuid) {
        List<LogEntry> entries = service().getLogEntriesFor(em, uuid);
        return entries;
    }

    public List<LogEntry> getLogEntriesFor(String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        List<LogEntry> entries = service().getLogEntriesFor(em,
                uuid, filterMap, doDefaultSort);
        return entries;
    }

    public List<LogEntry> nativeQueryLogs(String whereClause,
            int pageNb, int pageSize) {
        List<LogEntry> entries = service().nativeQueryLogs(em,whereClause, pageNb, pageSize);
        return entries;
    }

    public List<LogEntry> queryLogsByPage(String[] eventIds,
            Date limit, String category, String path, int pageNb, int pageSize) {
        List<LogEntry> entries = service().queryLogsByPage(em,eventIds,limit,category,path,pageNb,pageSize);
        return entries;
    }

    public List<LogEntry> queryLogsByPage(String[] eventIds,
            String dateRange, String category, String path, int pageNb,
            int pageSize) {
        List<LogEntry> entries =
            service().queryLogsByPage(em, eventIds, dateRange, category, path, pageNb, pageSize);
        return entries;
    }

    public List<LogEntry> queryLogs(String[] eventIds, String dateRange) {
        List<LogEntry> entries =
            service().queryLogs(em, eventIds, dateRange);
        return entries;
    }

    public LogEntry getLogEntryByID(long id) {
        LogEntry entry = service().getLogEntryByID(em, id);
        return entry;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long syncLogCreationEntries(String repoId, String path,
            Boolean recurs) throws ClientException {
        return service().syncLogCreationEntries(em, repoId, path, recurs);
    }

    public void logEvent(Event event) throws AuditException {
        service().logEvent(em, event);
    }

    public void logEvents(EventBundle eventBundle) throws AuditException {
        service().logEvents(em, eventBundle);
    }

 }
