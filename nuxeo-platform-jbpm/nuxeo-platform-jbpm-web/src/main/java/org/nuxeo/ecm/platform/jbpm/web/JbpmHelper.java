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
package org.nuxeo.ecm.platform.jbpm.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
// renamed in META-INF/components.xml for easier configuration
@Name("org.nuxeo.ecm.platform.jbpm.web.JbpmHelper")
@Scope(ScopeType.STATELESS)
public class JbpmHelper {

    private transient JbpmService jbpmService;

    public JbpmService getJbpmService() throws Exception {
        if (jbpmService == null) {
            jbpmService = Framework.getService(JbpmService.class);
        }
        return jbpmService;
    }

    public String createProcessInstance(NuxeoPrincipal principal, String pd,
            DocumentModel dm, String endLifeCycle) throws Exception {
        Map<String, Serializable> map = null;
        if (endLifeCycle != null && !endLifeCycle.equals("")
                && !"null".equals(endLifeCycle)) {
            map = new HashMap<String, Serializable>();
            map.put(JbpmService.VariableName.endLifecycleTransition.name(),
                    endLifeCycle);
        }
        getJbpmService().createProcessInstance(principal, pd, dm, map, null);
        return null;
    }

    public String updateProcessVariable(ProcessInstance pi,
            String variableName, Object variableValue)
            throws NuxeoJbpmException, Exception {
        pi.getContextInstance().setVariable(variableName, variableValue);
        getJbpmService().persistProcessInstance(pi);
        return null;
    }

    @SuppressWarnings("unchecked")
    public boolean processHasRunningTask(ProcessInstance pi, String taskName) {
        Collection<TaskInstance> tis = pi.getTaskMgmtInstance().getTaskInstances();
        for (TaskInstance ti : tis) {
            if (!ti.hasEnded() && ti.getName().equals(taskName)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public boolean isTaskAssignedToUser(TaskInstance task, NuxeoPrincipal user)
            throws ClientException {
        if (task != null && user != null) {
            // task actors
            List<String> taskActors = new ArrayList<String>();
            String taskActorId = task.getActorId();
            if (taskActorId != null) {
                taskActors.add(taskActorId);
            }
            Set pooled = task.getPooledActors();
            if (pooled != null) {
                taskActors.addAll(pooled);
            }

            // user actors
            List<String> actors = new ArrayList<String>();
            List<String> groups = user.getAllGroups();
            String actorId = NuxeoPrincipal.PREFIX + user.getName();
            actors.add(actorId);
            for (String s : groups) {
                actors.add(NuxeoGroup.PREFIX + s);
            }

            // try to match one of the user actors in task actors
            for (String taskActor : taskActors) {
                if (actors.contains(taskActor)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public List<String> getPooledActorIds(TaskInstance task) {
        List<String> res = new ArrayList<String>();
        Set pooledActors = task.getPooledActors();
        for (Object pooledActor : pooledActors) {
            if (pooledActor instanceof PooledActor) {
                res.add(((PooledActor) pooledActor).getActorId());
            }
        }
        return res;
    }

}