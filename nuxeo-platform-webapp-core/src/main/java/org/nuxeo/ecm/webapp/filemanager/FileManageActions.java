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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.filemanager;

import java.io.InputStream;

import javax.ejb.Remote;

import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.ui.web.api.SimpleFileManager;

@Remote
public interface FileManageActions extends SimpleFileManager {

    String display();

    /**
     * Adds a new File.
     *
     * @return the page that displays the documents
     * @throws ClientException
     */
    String addFile() throws ClientException;

    void setFileUpload(InputStream fileUpload);

    InputStream getFileUpload();

    void setFileName(String fileName);

    String getFileName();

    @WebRemote
    String addFileFromPlugin(String content, String mimetype, String fullName,
            String morePath, Boolean UseBase64) throws ClientException;

    @WebRemote
    String addBinaryFileFromPlugin(byte[] content, String mimetype,
            String fullName, String morePath) throws ClientException;

    @WebRemote
    String addFolderFromPlugin(String fullName, String morePath)
            throws ClientException;

    @WebRemote
    boolean canWrite() throws ClientException;

    @WebRemote
    String moveWithId(String docId, String containerId) throws ClientException;

    @WebRemote
    String copyWithId(String docId) throws ClientException;

    @WebRemote
    String pasteWithId(String docId) throws ClientException;

}
