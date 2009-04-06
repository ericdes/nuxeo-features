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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.helper;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 */
public class TaskNotificationHandler extends AbstractJbpmHandlerHelper {

    private static final long serialVersionUID = 1L;

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeoHasStarted()) {
            DocumentModel documentModel = (DocumentModel) getTransientVariable(
                    JbpmService.VariableName.document.name());
            NuxeoPrincipal principal = (NuxeoPrincipal) getTransientVariable(
                    JbpmService.VariableName.principal.name());
            VirtualTaskInstance participant = (VirtualTaskInstance) getTransientVariable(
                    JbpmService.VariableName.participant.name());
            if (participant == null) {
                participant = (VirtualTaskInstance) executionContext.getContextInstance().getVariable(
                        JbpmService.VariableName.participant.name());
            }
            CoreSession coreSession = getCoreSession(principal);
            if (coreSession == null || documentModel == null) {
                return;
            }
            EventProducer eventProducer;
            try {
                eventProducer = Framework.getService(EventProducer.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
            DocumentEventContext ctx = new DocumentEventContext(coreSession,
                    principal, documentModel);
            ctx.setProperty("recipients", participant.getActors().toArray(
                    new String[] {}));
            eventProducer.fireEvent(ctx.newEvent(JbpmEventNames.WORKFLOW_TASK_ASSIGNED));
            closeCoreSession(coreSession);
        }
    }

}
