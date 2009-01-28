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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.web;

import java.io.Serializable;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmSecurityPolicy;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.TaskListFilter;
import org.nuxeo.ecm.platform.jbpm.TaskStartDateComparator;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * @author Anahide Tchertchian
 *
 */
@Name("jbpmActions")
@Scope(ScopeType.CONVERSATION)
public class JbpmActionsBean implements JbpmActions {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient JbpmService jbpmService;

    @In(create = true)
    protected transient JbpmHelper jbpmHelper;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected transient Principal currentUser;

    protected Boolean canManageCurrentProcess;

    protected ProcessInstance currentProcess;

    protected String currentProcessInitiator;

    protected List<TaskInstance> currentTasks;

    protected ArrayList<VirtualTaskInstance> currentVirtualTasks;

    protected VirtualTaskInstance newVirtualTask;

    protected Boolean showAddVirtualTaskForm;

    // TODO: use it for notifications
    protected String userComment;

    public boolean getCanCreateProcess() throws ClientException {
        ProcessInstance currentProcess = getCurrentProcess();
        if (currentProcess == null) {
            // check write permissions on current doc
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (currentDoc != null) {
                DocumentRef docRef = currentDoc.getRef();
                return documentManager.hasPermission(docRef,
                        SecurityConstants.WRITE);
            }
        }
        return false;
    }

    public boolean getCanManageProcess() throws ClientException {
        if (canManageCurrentProcess == null) {
            canManageCurrentProcess = false;
            ProcessInstance currentProcess = getCurrentProcess();
            if (currentProcess != null) {
                Boolean canWrite = jbpmService.getPermission(currentProcess,
                        JbpmSecurityPolicy.Action.write,
                        navigationContext.getCurrentDocument(),
                        (NuxeoPrincipal) currentUser);
                if (canWrite != null) {
                    canManageCurrentProcess = canWrite;
                }
            }
        }
        return canManageCurrentProcess;
    }

    public boolean getCanEndTask(TaskInstance taskInstance)
            throws ClientException {
        if (taskInstance != null
                && (!taskInstance.isCancelled() && !taskInstance.hasEnded())) {
            JbpmHelper helper = new JbpmHelper();
            NuxeoPrincipal pal = (NuxeoPrincipal) currentUser;
            return pal.isAdministrator()
                    || pal.getName().equals(getCurrentProcessInitiator())
                    || helper.isTaskAssignedToUser(taskInstance, pal);
        }
        return false;
    }

    public String createProcessInstance(NuxeoPrincipal principal, String pd,
            DocumentModel dm, String endLifeCycle) throws ClientException {
        if (getCanCreateProcess()) {
            Map<String, Serializable> map = null;
            if (endLifeCycle != null && !endLifeCycle.equals("")
                    && !"null".equals(endLifeCycle)) {
                map = new HashMap<String, Serializable>();
                map.put(JbpmService.VariableName.endLifecycleTransition.name(),
                        endLifeCycle);
            }
            jbpmService.createProcessInstance(principal, pd, dm, map, null);

            // TODO: add feedback?

            Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_NEW_STARTED);
            resetCurrentData();
        }
        return null;
    }

    public ProcessInstance getCurrentProcess() throws ClientException {
        if (currentProcess == null) {
            List<ProcessInstance> processes = jbpmService.getProcessInstances(
                    navigationContext.getCurrentDocument(),
                    (NuxeoPrincipal) currentUser, null);
            if (processes != null && !processes.isEmpty()) {
                currentProcess = processes.get(0);
            }
        }
        return currentProcess;
    }

    public String getCurrentProcessInitiator() throws ClientException {
        if (currentProcessInitiator == null) {
            currentProcessInitiator = "";
            ProcessInstance currentProcess = getCurrentProcess();
            if (currentProcess != null) {
                Object initiator = currentProcess.getContextInstance().getVariable(
                        JbpmService.VariableName.initiator.name());
                if (initiator instanceof String) {
                    currentProcessInitiator = (String) initiator;
                    if (currentProcessInitiator.startsWith(NuxeoPrincipal.PREFIX)) {
                        currentProcessInitiator = currentProcessInitiator.substring(NuxeoPrincipal.PREFIX.length());
                    }
                }
            }
        }
        return currentProcessInitiator;
    }

    public List<TaskInstance> getCurrentTasks(String... taskNames)
            throws ClientException {
        if (currentTasks == null) {
            currentTasks = new ArrayList<TaskInstance>();
            ProcessInstance currentProcess = getCurrentProcess();
            if (currentProcess != null) {
                currentTasks.addAll(jbpmService.getTaskInstances(
                        currentProcess.getId(), null, new TaskListFilter(
                                taskNames)));
                Collections.sort(currentTasks, new TaskStartDateComparator());
            }
        }
        return currentTasks;
    }

    public String getVirtualTasksLayoutMode() throws ClientException {
        if (getCanManageProcess()) {
            return BuiltinModes.EDIT;
        }
        return BuiltinModes.VIEW;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<VirtualTaskInstance> getCurrentVirtualTasks()
            throws ClientException {
        if (currentVirtualTasks == null) {
            currentVirtualTasks = new ArrayList<VirtualTaskInstance>();
            ProcessInstance currentProcess = getCurrentProcess();
            if (currentProcess != null) {
                Object participants = currentProcess.getContextInstance().getVariable(
                        JbpmService.VariableName.participants.name());
                if (participants != null && participants instanceof List) {
                    currentVirtualTasks.addAll((List<VirtualTaskInstance>) participants);
                }
            }
        }
        return currentVirtualTasks;
    }

    public boolean getShowAddVirtualTaskForm() throws ClientException {
        if (showAddVirtualTaskForm == null) {
            showAddVirtualTaskForm = false;
            if (getCurrentVirtualTasks().isEmpty()
                    && (currentTasks == null || currentTasks.isEmpty())) {
                showAddVirtualTaskForm = true;
            }
        }
        return showAddVirtualTaskForm;
    }

    public void toggleShowAddVirtualTaskForm(ActionEvent event)
            throws ClientException {
        showAddVirtualTaskForm = !getShowAddVirtualTaskForm();
    }

    public VirtualTaskInstance getNewVirtualTask() throws ClientException {
        if (newVirtualTask == null) {
            newVirtualTask = new VirtualTaskInstance();
        }
        return newVirtualTask;
    }

    public String addNewVirtualTask() throws ClientException {
        ProcessInstance pi = getCurrentProcess();
        if (pi != null && newVirtualTask != null && getCanManageProcess()) {
            List<VirtualTaskInstance> virtualTasks = getCurrentVirtualTasks();
            if (virtualTasks == null) {
                virtualTasks = new ArrayList<VirtualTaskInstance>();
            }
            virtualTasks.add(newVirtualTask);

            pi.getContextInstance().setVariable(
                    JbpmService.VariableName.participants.name(), virtualTasks);
            jbpmService.persistProcessInstance(pi);

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.added.reviewer"));

            // reset so that's reloaded
            newVirtualTask = null;
            currentVirtualTasks = null;
            showAddVirtualTaskForm = null;
            // TODO: refresh process instance?
        }
        return null;
    }

    public String moveDownVirtualTask(int index) throws ClientException {
        ProcessInstance pi = getCurrentProcess();
        if (pi != null && getCanManageProcess()) {
            List<VirtualTaskInstance> virtualTasks = getCurrentVirtualTasks();
            if (virtualTasks != null && index + 1 < virtualTasks.size()) {
                VirtualTaskInstance task = virtualTasks.remove(index);
                virtualTasks.add(index + 1, task);
            }

            pi.getContextInstance().setVariable(
                    JbpmService.VariableName.participants.name(), virtualTasks);
            jbpmService.persistProcessInstance(pi);

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.movedUp.reviewer"));

            // reset so that's reloaded
            currentVirtualTasks = null;
            // TODO: refresh process instance?
        }
        return null;
    }

    public String moveUpVirtualTask(int index) throws ClientException {
        ProcessInstance pi = getCurrentProcess();
        if (pi != null && getCanManageProcess()) {
            List<VirtualTaskInstance> virtualTasks = getCurrentVirtualTasks();
            if (virtualTasks != null && index - 1 < virtualTasks.size()) {
                VirtualTaskInstance task = virtualTasks.remove(index);
                virtualTasks.add(index - 1, task);
            }

            pi.getContextInstance().setVariable(
                    JbpmService.VariableName.participants.name(), virtualTasks);
            jbpmService.persistProcessInstance(pi);

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.movedDown.reviewer"));

            // reset so that's reloaded
            currentVirtualTasks = null;
            // TODO: refresh process instance?
        }
        return null;
    }

    public String removeVirtualTask(int index) throws ClientException {
        ProcessInstance pi = getCurrentProcess();
        if (pi != null && getCanManageProcess()) {
            List<VirtualTaskInstance> virtualTasks = getCurrentVirtualTasks();
            if (virtualTasks != null && index < virtualTasks.size()) {
                virtualTasks.remove(index);
            }

            pi.getContextInstance().setVariable(
                    JbpmService.VariableName.participants.name(), virtualTasks);
            jbpmService.persistProcessInstance(pi);

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.removed.reviewer"));

            // reset so that's reloaded
            currentVirtualTasks = null;
            showAddVirtualTaskForm = null;
            // TODO: refresh process instance?
        }
        return null;
    }

    public void validateTaskDueDate(FacesContext context,
            UIComponent component, Object value) {
        final String DATE_FORMAT = "dd/MM/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        String messageString = null;
        if (value != null) {
            Date today = null;
            Date dueDate = null;
            try {
                dueDate = dateFormat.parse(dateFormat.format((Date) value));
                today = dateFormat.parse(dateFormat.format(new Date()));
            } catch (ParseException e) {
                messageString = "label.workflow.error.date_parsing";
            }
            if (dueDate.before(today)) {
                messageString = "label.workflow.error.outdated_duedate";
            }
        }

        if (messageString != null) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.workflow.error.outdated_duedate"),
                    null);
            ((EditableValueHolder) component).setValid(false);
            context.addMessage(component.getClientId(context), message);
            // also add global message?
            // context.addMessage(null, message);
        }

    }

    protected TaskInstance getStartTask(String taskName) throws ClientException {
        TaskInstance startTask = null;
        if (taskName != null) {
            // get task with that name on current process
            ProcessInstance pi = getCurrentProcess();
            if (pi != null) {
                List<TaskInstance> tasks = jbpmService.getTaskInstances(
                        currentProcess.getId(), null, new TaskListFilter(
                                taskName));
                if (tasks != null && !tasks.isEmpty()) {
                    // take first one found
                    startTask = tasks.get(0);
                }
            }
        }
        if (startTask == null) {
            throw new ClientException(
                    "No start task found on current process with name "
                            + taskName);
        }
        return null;
    }

    public boolean isProcessStarted(String startTaskName)
            throws ClientException {
        TaskInstance startTask = getStartTask(startTaskName);
        return startTask.hasEnded();
    }

    public String startProcess(String startTaskName) throws ClientException {
        if (getCanManageProcess()) {
            TaskInstance startTask = getStartTask(startTaskName);
            if (startTask.hasEnded()) {
                throw new ClientException("Process is already started");
            }
            // optim: pass participants as transient variables to avoid
            // lookup in the process instance
            Map<String, Serializable> transientVariables = new HashMap<String, Serializable>();
            transientVariables.put(
                    JbpmService.VariableName.participants.name(),
                    getCurrentVirtualTasks());
            jbpmService.endTask(startTask.getId(), null, null, null,
                    transientVariables);
            resetCurrentData();
        }
        return null;
    }

    public String validateTask(TaskInstance taskInstance, String transition)
            throws ClientException {
        if (taskInstance != null) {
            // add marker that task was validated
            Map<String, Serializable> taskVariables = new HashMap<String, Serializable>();
            taskVariables.put(JbpmService.TaskVariableName.validated.name(),
                    true);
            jbpmService.endTask(taskInstance.getId(), transition,
                    taskVariables, null, null);

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.task.ended"));

            Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_TASK_COMPLETED);
            resetCurrentData();
        }
        return null;
    }

    public String rejectTask(TaskInstance taskInstance, String transition)
            throws ClientException {
        if (taskInstance != null) {
            // add marker that task was rejected
            Map<String, Serializable> taskVariables = new HashMap<String, Serializable>();
            taskVariables.put(JbpmService.TaskVariableName.validated.name(),
                    false);
            jbpmService.endTask(taskInstance.getId(), transition,
                    taskVariables, null, null);
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.task.ended"));
            Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_TASK_REJECTED);
            resetCurrentData();
        }
        return null;
    }

    public String abandonCurrentProcess() throws ClientException {
        ProcessInstance currentProcess = getCurrentProcess();
        if (currentProcess != null && getCanManageProcess()) {
            // remove wf acls
            Long pid = currentProcess.getId();
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (currentDoc != null) {
                ACP acp = currentDoc.getACP();
                acp.removeACL(AbstractJbpmHandlerHelper.getProcessACLName(pid));
                documentManager.setACP(currentDoc.getRef(), acp, true);
                documentManager.save();
            }

            // end process and tasks
            jbpmService.abandonProcessInstance((NuxeoPrincipal) currentUser,
                    pid);
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.task.ended"));

            Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_ABANDONED);
            resetCurrentData();
        }
        return null;
    }

    public String getUserComment() throws ClientException {
        return userComment;
    }

    public void setUserComment(String comment) throws ClientException {
        this.userComment = comment;
    }

    public void resetCurrentData() throws ClientException {
        canManageCurrentProcess = null;
        currentProcess = null;
        currentProcessInitiator = null;
        currentTasks = null;
        currentVirtualTasks = null;
        newVirtualTask = null;
        showAddVirtualTaskForm = null;
        userComment = null;
    }

}
