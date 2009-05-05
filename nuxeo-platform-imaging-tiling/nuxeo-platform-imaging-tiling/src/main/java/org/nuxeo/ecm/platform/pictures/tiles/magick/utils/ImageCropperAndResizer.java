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
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.magick.utils;

import org.nuxeo.ecm.platform.pictures.tiles.magick.MagickExecutor;

/**
 * Unit command to crop and resize an picture
 *
 * @author tiry
 *
 */
public class ImageCropperAndResizer extends MagickExecutor {

    public static void cropAndResize(String inputFilePath,
            String outputFilePath, int tileWidth, int tileHeight, int offsetX,
            int offsetY, int targetWidth, int targetHeight) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(streamCmd());
        sb.append(" -map rgb -storage-type char -extract ");
        sb.append(tileWidth);
        sb.append("x");
        sb.append(tileHeight);
        sb.append("+");
        sb.append(offsetX);
        sb.append("+");
        sb.append(offsetY);
        sb.append(" ");
        sb.append(formatFilePath(inputFilePath));
        sb.append(" - | convert -depth 8 -size ");
        sb.append(tileWidth);
        sb.append("x");
        sb.append(tileHeight);
        sb.append(" -resize ");
        sb.append(targetWidth);
        sb.append("x");
        sb.append(targetHeight);
        sb.append("!");
        sb.append(" rgb:- ");
        sb.append(formatFilePath(outputFilePath));

        execCmd(sb.toString());
    }
}
