/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.preview;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.platform.preview.adapter.BlobPostProcessor;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class AnnotationBlobPostProcessor implements BlobPostProcessor {

    private static final Log log = LogFactory.getLog(AnnotationBlobPostProcessor.class);

    protected static final int BUFFER_SIZE = 4096*16;

    protected static final String ANNOTATION_MODULE_JS
        = "<script type=\"text/javascript\" src='/nuxeo/org.nuxeo.ecm.platform.annotations.gwt.AnnotationFrameModule/org.nuxeo.ecm.platform.annotations.gwt.AnnotationFrameModule.nocache.js'></script>";

    protected static final String INTERNET_EXPLORER_RANGE_JS
    = "<script type=\"text/javascript\" src='/nuxeo/scripts/InternetExplorerRange.js'></script>";

    protected Pattern headPattern  = Pattern.compile("(.*)(<head>)(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    protected Pattern htmlPattern  = Pattern.compile("(.*)(<html>)(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    protected Pattern charsetPattern  = Pattern.compile("(.*) charset=(.*?)\"(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public Blob process(Blob blob) {
        try {
            String encoding = null;
            if (blob.getEncoding() == null) {
                Matcher m = charsetPattern.matcher(blob.getString());
                if (m.matches()) {
                    encoding = m.group(2);
                }
            } else {
                encoding = blob.getEncoding();
            }

            String blobAsString = getBlobAsString(blob, encoding);
            String processedBlob = addAnnotationModule(blobAsString);

            byte[] bytes = encoding == null ? processedBlob.getBytes() : processedBlob.getBytes(encoding);
            blob = new ByteArrayBlob(bytes, blob.getMimeType(), encoding);
        } catch(IOException e) {
            log.debug("Unable to process Blob", e);
        }
        return blob;
    }

    protected String getBlobAsString(Blob blob, String encoding) throws IOException {
        if (encoding == null) {
            return blob.getString();
        }
        Reader reader = new InputStreamReader(blob.getStream(), encoding);
        return readString(reader);
    }

    protected String addAnnotationModule(String blob) {
        StringBuilder sb = new StringBuilder();
        Matcher m = headPattern.matcher(blob);
        if (m.matches()) {
            sb.append(m.group(1));
            sb.append(m.group(2));
            sb.append(INTERNET_EXPLORER_RANGE_JS);
            sb.append(ANNOTATION_MODULE_JS);
            sb.append(m.group(3));
        } else {
            m = htmlPattern.matcher(blob);
            if (m.matches()) {
                sb.append(m.group(1));
                sb.append(m.group(2));
                sb.append("<head>");
                sb.append(INTERNET_EXPLORER_RANGE_JS);
                sb.append(ANNOTATION_MODULE_JS);
                sb.append("</head>");
                sb.append(m.group(3));
            } else {
                log.debug("Unable to inject Annotation module javascript");
                sb.append(blob);
            }
        }
        return sb.toString();
    }

    public static String readString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(BUFFER_SIZE);
        try {
            char[] buffer = new char[BUFFER_SIZE];
            int read;
            while ((read = reader.read(buffer, 0, BUFFER_SIZE)) != -1) {
                sb.append(buffer, 0, read);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return sb.toString();
    }

}
