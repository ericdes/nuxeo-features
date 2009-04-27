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
 * $Id$
 */
package org.nuxeo.ecm.platform.picture.convert.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Laurent Doguin
 *
 */
public class TestImagingConvertPlugin extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.picture.convert");
    }

    private static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return file;
    }

    protected static BlobHolder getBlobFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return new SimpleBlobHolder(new FileBlob(file));
    }

    public void testResizeConverter() throws Exception {

        String converter ="pictureResize";
        String filePath = "test-data/sample.jpeg";

        int resizeWidth = 120;
        int resizeHeight = 120;

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(ImagingConvertConstants.OPTION_RESIZE_WIDTH, resizeWidth);
        options.put(ImagingConvertConstants.OPTION_RESIZE_HEIGHT, resizeHeight);

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        BlobHolder hg = getBlobFromPath(filePath);

        BlobHolder result = cs.convert(converter, hg, options);
        assertNotNull(result);

        BufferedImage resizedImage = ImageIO.read(result.getBlob().getStream());
        assertNotNull("Resized image is null", resizedImage);
        assertEquals("Resized image width", resizeWidth, resizedImage.getWidth());
        assertEquals("Resized image height", resizeHeight, resizedImage.getHeight());
    }

    public void testRotate() throws Exception {

        String converter ="pictureRotation";
        String filePath = "test-data/sample.jpeg";

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(ImagingConvertConstants.OPTION_ROTATE_ANGLE,90);

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        BlobHolder hg = getBlobFromPath(filePath);

        BlobHolder result = cs.convert(converter, hg, options);
        assertNotNull(result);

        BufferedImage image = ImageIO.read(new FileInputStream(
                FileUtils.getResourceFileFromContext(filePath)));
        assertNotNull("Original image is null", image);
        int width = image.getWidth();
        int height = image.getHeight();
        assertTrue("Original image size != (0,0)", width > 0 && height > 0);

        BufferedImage convertedImage = ImageIO.read(result.getBlob().getStream());
        assertNotNull("Rotated image is null", image);
        assertEquals("Ratated image width", height, convertedImage.getWidth());
        assertEquals("Rotated image height", width, convertedImage.getHeight());
    }

    public void testCrop() throws Exception {

        String converter ="pictureCrop";
        String filePath = "test-data/sample.jpeg";

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        options.put(ImagingConvertConstants.OPTION_CROP_X, 5);
        options.put(ImagingConvertConstants.OPTION_CROP_Y, 25);
        options.put(ImagingConvertConstants.OPTION_RESIZE_HEIGHT, 10);
        options.put(ImagingConvertConstants.OPTION_RESIZE_WIDTH, 10);

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        BlobHolder hg = getBlobFromPath(filePath);

        BlobHolder result = cs.convert(converter, hg, options);
        assertNotNull(result);

        BufferedImage image = ImageIO.read(new FileInputStream(
                FileUtils.getResourceFileFromContext(filePath)));
        assertNotNull("Original image is null", image);
        int width = image.getWidth();
        int height = image.getHeight();
        assertTrue("Original image size != (0,0)", width > 0 && height > 0);


        BufferedImage convertedImage = ImageIO.read(result.getBlob().getStream());

        assertNotSame("Ratated image width", height, convertedImage.getWidth());
        assertNotSame("Rotated image height", width, convertedImage.getHeight());
    }

}
