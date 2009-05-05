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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.api.adapters;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants;

public class DefaultPictureAdapter extends AbstractPictureAdapter {
    private static final Log log = LogFactory.getLog(DefaultPictureAdapter.class);

    private static final String VIEWS_PROPERTY = "picture:views";

    private static final String VIEW_XPATH = "picture:views/item[%d]/";

    private static final String TITLE_PROPERTY = "title";

    public Boolean createPicture(Blob fileContent, String filename, String title,
            ArrayList<Map<String, Object>> pictureTemplates)
            throws IOException, ClientException {
        this.fileContent = fileContent;
        file = File.createTempFile("tmp", ".jpg");
        fileContent.transferTo(file);
        file.deleteOnExit();
        type = getImagingService().getImageMimeType(file);

        if (type == null) {
            return false;
        }
        if (type.equals("application/octet-stream")) {
            return false;
        }

        setMetadata();
        addViews(pictureTemplates, filename, title);
        return true;
    }

    public void doRotate(int angle) throws ClientException {
        int size = doc.getProperty(VIEWS_PROPERTY).size();
        for (int i = 0; i < size; i++) {
            String xpath = "picture:views/view[" + i + "]/";
            try {
                BlobHolder blob = new SimpleBlobHolder(doc.getProperty(xpath + "content").getValue(
                        Blob.class));
                String type = blob.getBlob().getMimeType();
                if (type != "image/png") {

                    Map<String, Serializable> options = new HashMap<String, Serializable>();
                    options.put(ImagingConvertConstants.OPTION_ROTATE_ANGLE, angle);
                    blob = getConversionService().convert(ImagingConvertConstants.OPERATION_ROTATE , blob, options);
                    doc.getProperty(xpath + "content").setValue(blob.getBlob());
                    Long height = (Long) doc.getProperty(xpath + "height").getValue();
                    Long width = (Long) doc.getProperty(xpath + "width").getValue();
                    doc.getProperty(xpath + "height").setValue(width);
                    doc.getProperty(xpath + "width").setValue(height);
                }
            } catch (Exception e) {
                log.error("Rotation Failed", e);
            }
        }
    }

    public void doCrop(String coords) throws ClientException {
        doc.setPropertyValue("picture:cropCoords", coords);
    }

    public Blob getPictureFromTitle(String title) throws PropertyException, ClientException{
        Collection<Property> views = doc.getProperty(VIEWS_PROPERTY).getChildren();
        for (Property property : views) {
            if (property.getValue(TITLE_PROPERTY).equals(title)) {
                return (Blob) property.getValue(
                        "content");
            }
        }
        return null;
    }

    public String getFirstViewXPath() {
        return getViewXPathFor(0);
    }

    public String getViewXPath(String viewName) {
        try {
            Property views = doc.getProperty(VIEWS_PROPERTY);
            for (int i = 0; i < views.size(); i++) {
                if (views.get(i).getValue(TITLE_PROPERTY).equals(viewName)) {
                    return getViewXPathFor(i);
                }
            }
        } catch (ClientException e) {
            log.error("Unable to get picture views", e);
        }
        return null;
    }

    protected String getViewXPathFor(int index) {
        return String.format(VIEW_XPATH, index);
    }

}
