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
package org.nuxeo.ecm.platform.jbpm.core.service;

import javax.transaction.Synchronization;

import org.jbpm.JbpmContext;

/**
 * @author arussel
 *
 */
public class JbpmSynchronization implements Synchronization {

    private final JbpmContext context;

    public JbpmSynchronization(JbpmContext context) {
        this.context = context;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.Synchronization#afterCompletion(int)
     */
    public void afterCompletion(int arg0) {
        context.close();
        JbpmServiceImpl.contexts.set(null);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.Synchronization#beforeCompletion()
     */
    public void beforeCompletion() {
        context.getSession().flush();
    }

}