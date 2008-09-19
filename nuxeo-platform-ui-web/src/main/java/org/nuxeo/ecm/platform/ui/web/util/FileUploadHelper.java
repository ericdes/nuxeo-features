package org.nuxeo.ecm.platform.ui.web.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.restlet.data.Request;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpCall;
import com.noelios.restlet.http.HttpRequest;

/**
 *
 * Helper to encapsulate Multipart requests parsing to extract blobs
 *
 * This helper is needed to provide the indirection between - the Apache file
 * uplaod based solution (5.1) and - the Seam MultiPartFilter bases solution
 * (5.1 / Seam 2.x)
 *
 * @author tiry
 *
 */
public class FileUploadHelper {

    /**
     * Parses a Multipart Restlet Request to extract blobs
     *
     * @param request
     * @return
     * @throws Exception
     */
    public static List<Blob> parseRequest(Request request) throws Exception {
        if (request instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) request;
            HttpCall httpCall = httpRequest.getHttpCall();
            if (httpCall instanceof ServletCall) {
                HttpServletRequest httpServletRequest = ((ServletCall) httpCall)
                        .getRequest();
                return parseRequest(httpServletRequest);
            }
        }
        return null;
    }

    /**
     * Parses a Multipart Servlet Request to extract blobs
     *
     * @param request
     * @return
     * @throws Exception
     */
    public static List<Blob> parseRequest(HttpServletRequest request)
            throws Exception {
        List<Blob> blobs = new ArrayList<Blob>();

// to be enabled only on the 5.2 branch
//        if (request instanceof MultipartRequest) {
//            MultipartRequest seamMPRequest = (MultipartRequest) request;
//
//            Enumeration<String> names = seamMPRequest.getParameterNames();
//            while (names.hasMoreElements()) {
//                String name = names.nextElement();
//                InputStream in = seamMPRequest.getFileInputStream(name);
//                if (in != null) {
//                    Blob blob = new InputStreamBlob(in);
//                    blob.setFilename(seamMPRequest.getFileName(name));
//                    blobs.add(blob);
//                }
//            }
//        } else {
        if (true) {
            // fallback method for non-seam servlet request
            FileUpload fu = new FileUpload(new DiskFileItemFactory());
            String fileNameCharset = request.getHeader("FileNameCharset");
            if (fileNameCharset != null) {
                fu.setHeaderEncoding(fileNameCharset);
            }
            ServletRequestContext requestContext = new ServletRequestContext(
                    request);
            List<FileItem> fileItems = fu.parseRequest(requestContext);
            for (FileItem item : fileItems) {
                Blob blob = StreamingBlob.createFromStream(item.getInputStream()).persist();
                blob.setFilename(item.getName());
                blobs.add(blob);
            }
        }
        return blobs;
    }
}