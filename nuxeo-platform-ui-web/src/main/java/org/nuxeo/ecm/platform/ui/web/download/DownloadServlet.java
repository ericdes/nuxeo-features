package org.nuxeo.ecm.platform.ui.web.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple download servlet used for big files that can not be downloaded from withing the JSF context
 *
 * @author tiry
 *
 */
public class DownloadServlet extends HttpServlet {

    protected static final int BUFFER_SIZE = 1024 * 512;

    /**
     *
     */
    private static final long serialVersionUID = 986876871L;

    private CoreSession getCoreSession(String repoName) throws Exception {

        RepositoryManager rm = Framework.getService(RepositoryManager.class);

        Repository repo = rm.getRepository(repoName);

        if (repo == null) {
            throw new ClientException("Unable to get " + repoName
                    + " repository");
        }
        return repo.open();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String requestURI = req.getRequestURI();

        String filePath = requestURI.replace("/nuxeo/nxbigfile/", "");

        String[] pathParts = filePath.split("/");

        String repoName = pathParts[0];
        String docId = pathParts[1];
        String fieldPath = pathParts[2];
        String fileName = pathParts[3];

        CoreSession session = null;
        InputStream in = null;
        try {
            session = getCoreSession(repoName);

            DocumentModel doc = session.getDocument(new IdRef(docId));
            Blob blob = null;
            if (fieldPath != null) {
                blob = (Blob) DocumentModelUtils.getPropertyValue(doc,
                        DocumentModelUtils.decodePropertyName(fieldPath));
                if (blob == null) {
                    // maybe it's a complex property
                    blob = (Blob) DocumentModelUtils.getComplexPropertyValue(
                            doc, fieldPath);
                }
            } else
                return;

            if (fileName == null || fileName.length() == 0) {
                fileName = "file";
            }
            // url encode filename in UTF-8
            try {
                fileName = URLEncoder.encode(fileName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                fileName = StringUtils.toAscii(fileName);
            }

            String userAgent = req.getHeader("User-Agent");
            if (userAgent != null && userAgent.contains("MSIE")) {
                // XXX AT: IE does not understand the filename* encoding,
                // but decodes correctly the filename this way.
                resp.setHeader("Content-Disposition", "attachment; filename="
                        + fileName + ';');
            } else {
                // follow RFC-2231 for charset setting
                resp.setHeader("Content-Disposition",
                        "attachment; filename*=UTF-8''" + fileName + ';');
            }

            resp.setContentType(blob.getMimeType());

            OutputStream out = resp.getOutputStream();
            in = blob.getStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                out.flush();
            }
            resp.flushBuffer();

        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            if (session != null) {
                try {
                    CoreInstance.getInstance().close(session);
                } catch (ClientException e) {
                    // nothing to do
                }
            }
            if (in != null)
                in.close();
        }

    }
}