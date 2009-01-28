/*
// * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmConfiguration;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * The entry point for the JbpmService.
 * <p>
 * The returned value are detached from their hibernate session.
 * <p>
 * The methods on document assume a single document attached to a process. The
 * variables 'documentId' and 'documentRepositoryName' should exists. Security
 * is only implemented in such a case.
 * <p>
 * Warning: The returned result of most of the methods are list issue from
 * hibernate query. Also the object are fully loaded, their dependent objects
 * might not have been fetch. If you need dependent object, use the
 * executeJbpmOperation making sure those object are properly loaded inside the
 * operation.
 *
 *
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public interface JbpmService {

    public enum VariableName {
        documentId, documentRepositoryName, endLifecycleTransition, initiator, participants
    }

    public enum TaskVariableName {
        directive, validated
    }

    public enum HibernateQueries {
        NuxeoHibernateQueries_getProcessInstancesForDoc, NuxeoHibernateQueries_getTaskInstancesForDoc
    }

    /**
     * Marker for acls managed by workflow
     */
    public static String ACL_PREFIX = "WORKFLOW_ACL_";

    /**
     * Return the configuration.
     */
    JbpmConfiguration getConfiguration();

    /**
     * Execute a JbpmOperation.
     *
     * @param operation
     * @return The object returned by the run method of the JbpmOperation.
     * @throws NuxeoJbpmException
     */
    Serializable executeJbpmOperation(JbpmOperation operation)
            throws NuxeoJbpmException;

    // TASK
    /**
     * Return a list of task instances assigned to this user or one of its
     * group.
     *
     * @param currentUser the user.
     * @return A list of task instances.
     * @throws IllegalStateException If the currentUser is null.
     */
    List<TaskInstance> getCurrentTaskInstances(
            final NuxeoPrincipal currentUser, JbpmListFilter filter)
            throws NuxeoJbpmException;

    /**
     * Return a list of task instance for this process instance.
     *
     * @param processInstanceId the id of the process instance.
     * @param principal
     * @return
     */
    List<TaskInstance> getTaskInstances(Long processInstanceId,
            NuxeoPrincipal principal, JbpmListFilter filter)
            throws NuxeoJbpmException;

    /**
     * End the task following the transition. If transition is null, follow the
     * default transition.
     *
     * @param taskInstanceId
     * @param transition
     * @param taskVariables TODO
     * @param variables A list a variables to add to the process instance.
     * @param transientVariables the list of transient variables.
     * @throws NuxeoJbpmSecurityRuntimeException If a document is attached to
     *             the process and the transition is not allowed for user.
     */
    void endTask(Long taskInstanceId, String transition,
            Map<String, Serializable> taskVariables,
            Map<String, Serializable> variables,
            Map<String, Serializable> transientVariables)
            throws NuxeoJbpmException;

    /**
     * Return a list of of possible transition for this user. (If a document is
     * attached to the process, then return only the allowed transition)
     *
     * @param taskInstanceId the id of the taskInstance.
     * @param principal the user
     * @return
     */
    List<String> getAvailableTransitions(Long taskInstanceId,
            NuxeoPrincipal principal) throws NuxeoJbpmException;

    /**
     * Saves the task instances.
     * <p>
     * Tasks are created or updated if they already exist.
     *
     * @param taskInstances
     */
    void saveTaskInstances(List<TaskInstance> taskInstances)
            throws NuxeoJbpmException;

    // PROCESS

    /**
     * End the process instance and cancel all related tasks.
     *
     * @param principal the canceller.
     */
    public void abandonProcessInstance(NuxeoPrincipal principal, Long processId)
            throws NuxeoJbpmException;

    /**
     * Return a list of all current process instances for this user.
     *
     * <p>
     * A current process instance is an unfinished process. The user is the
     * process instances initiator.
     *
     * @param principal
     * @return A list of ProcessInstance
     * @throws IllegalStateException If the principal is null.
     */
    List<ProcessInstance> getCurrentProcessInstances(NuxeoPrincipal principal,
            JbpmListFilter filter) throws NuxeoJbpmException;

    /**
     * Return the process instance.
     *
     * @param processInstanceId the id of the process instance.
     * @return
     */
    ProcessInstance getProcessInstance(final Long processInstanceId)
            throws NuxeoJbpmException;

    /**
     * Return the created and started process instance.
     *
     * @param processInstanceName the name of the process
     * @param user the initiator of the process.
     * @param dm the document attached to this process.
     * @param variables A list a variables to add to the process instance.
     * @param transientVariables the list of transient variables.
     * @return the created process instance.
     * @throws NuxeoJbpmException
     * @throws NuxeoJbpmSecurityException If the user is not allowed to create a
     *             process instance for this document.
     */
    ProcessInstance createProcessInstance(NuxeoPrincipal user,
            String processInstanceName, DocumentModel dm,
            Map<String, Serializable> variables,
            Map<String, Serializable> transientVariables)
            throws NuxeoJbpmException;

    /**
     * Return a list of available Process Definition Name available for this
     * document and user. All process definition if dm is <code>null</code>.
     * The returned process definition is always the latest.
     *
     * @param user the caller.
     * @param dm the document concerned by the process
     * @return A list of process definition
     */
    List<ProcessDefinition> getProcessDefinitions(NuxeoPrincipal user,
            DocumentModel dm, JbpmListFilter filter) throws NuxeoJbpmException;

    /**
     * Return the latest process definitions attached to a type.
     *
     * Matching between type and process definition is done in the typeFilter
     * extension point of the service.
     *
     * @param type the type.
     * @return A list of process definition.
     */
    List<ProcessDefinition> getProcessDefinitionsByType(String type)
            throws NuxeoJbpmException;

    /**
     * Return the latest process definition with this name.
     *
     * @param name the Name.
     * @return the process definition.
     */
    ProcessDefinition getProcessDefinitionByName(String name)
            throws NuxeoJbpmException;

    /**
     * Terminate this process and all the tokens in it.
     *
     * @param processId the id of the process instance.
     * @throws NuxeoJbpmException
     */
    void endProcessInstance(Long processId) throws NuxeoJbpmException;

    /**
     * Persist a process instance.
     */
    void persistProcessInstance(ProcessInstance pi) throws NuxeoJbpmException;

    // DOCUMENT

    /**
     * Return the document used in this task.
     *
     * @param ti the task.
     * @param user the user.
     * @return a list of DocumentModel.
     */
    DocumentModel getDocumentModel(TaskInstance ti, NuxeoPrincipal user)
            throws NuxeoJbpmException;

    /**
     * Return a list of document used in this process.
     *
     * @param pi the process instance.
     * @param user the user.
     * @return a list of DocumentModel.
     * @throws NuxeoJbpmException
     */
    DocumentModel getDocumentModel(ProcessInstance pi, NuxeoPrincipal user)
            throws NuxeoJbpmException;

    /**
     * Return the list of task instances associated with this document for which
     * the user is the actor or belongs to the pooled actor list.
     *
     * If the user is null, then it returns all task instances for the document.
     *
     * @param dm the document.
     * @param user
     * @return
     * @throws NuxeoJbpmException
     */
    List<TaskInstance> getTaskInstances(DocumentModel dm, NuxeoPrincipal user,
            JbpmListFilter jbpmListFilter) throws NuxeoJbpmException;

    /**
     * Return the list of process instances associated with this document.
     *
     * @param dm
     * @param user
     * @return
     * @throws NuxeoJbpmException
     */
    List<ProcessInstance> getProcessInstances(DocumentModel dm,
            NuxeoPrincipal user, JbpmListFilter jbpmListFilter)
            throws NuxeoJbpmException;

    Map<String, List<String>> getTypeFilterConfiguration();

    Boolean getPermission(ProcessInstance pi, JbpmSecurityPolicy.Action action,
            DocumentModel dm, NuxeoPrincipal principal)
            throws NuxeoJbpmException;

}
