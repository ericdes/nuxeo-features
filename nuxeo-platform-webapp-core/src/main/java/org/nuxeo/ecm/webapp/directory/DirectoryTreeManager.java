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
 * $Id: DirectoryTreeManager.java 28950 2008-01-11 13:35:06Z tdelprat $
 */
package org.nuxeo.ecm.webapp.directory;

import java.io.Serializable;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.ClientException;
import org.richfaces.component.UITree;
import org.richfaces.event.NodeExpandedEvent;

public interface DirectoryTreeManager extends Serializable {

    DirectoryTreeNode get(String treeName);

    DirectoryTreeNode getSelectedTree();

    List<DirectoryTreeNode> getDirectoryTrees();

    List<String> getDirectoryTreeNames();

    String getSelectedTreeName();

    void setSelectedTreeName(String treeName);

    boolean isInitialized();

    /**
     * Listener for node opening/closing events.
     * <p>
     * Used to not interfere with node state when manually changing open nodes.
     */
    void changeExpandListener(NodeExpandedEvent event);

    /**
     * Returns true if node should be opened according to last selection.
     */
    Boolean adviseNodeOpened(UITree tree);

    @Remove
    @Destroy
    @PermitAll
    void destroy();

}
