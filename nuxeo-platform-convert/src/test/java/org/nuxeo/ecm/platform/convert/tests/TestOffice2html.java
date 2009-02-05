package org.nuxeo.ecm.platform.convert.tests;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.runtime.api.Framework;

public class TestOffice2html extends BaseConverterTest {

    protected void doTestHtmlConverter(String srcMT, String fileName)
            throws Exception {

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        String converterName = cs.getConverterName(srcMT, "text/html");
        assertEquals("office2html", converterName);

        ConverterCheckResult check = cs.isConverterAvailable(converterName);
        assertNotNull(check);
        if (!check.isAvailable()) {
            System.out.println("Skipping JOD based converter tests since OOo is not installed");
            System.out.println(" converter check output : " + check.getInstallationMessage());
            System.out.println(" converter check output : " + check.getErrorMessage());
            return;
        }

        BlobHolder hg = getBlobFromPath("test-docs/" + fileName, srcMT);

        BlobHolder result = cs.convert(converterName, hg, null);
        assertNotNull(result);

        String html = result.getBlob().getString();
        System.out.println(html);
        assertTrue(html.contains("Hello"));
    }

    public void testOfficeToHtmlConverter() throws Exception {

        ConversionService cs = Framework.getLocalService(ConversionService.class);
        ConverterCheckResult check = cs.isConverterAvailable("office2html");
        assertNotNull(check);
        if (!check.isAvailable()) {
            System.out.print("Skipping JOD based converter tests since OOo is not installed");
            System.out.print(" converter check output : " + check.getInstallationMessage());
            System.out.print(" converter check output : " + check.getErrorMessage());
            return;
        }

        doTestHtmlConverter("application/vnd.ms-excel", "hello.xls");
        doTestHtmlConverter("application/vnd.sun.xml.writer", "hello.sxw");
        doTestHtmlConverter("application/vnd.oasis.opendocument.text", "hello.odt");
        doTestHtmlConverter("application/vnd.sun.xml.calc", "hello.sxc");
        doTestHtmlConverter("application/vnd.oasis.opendocument.spreadsheet", "hello.ods");
        //doTestHtmlConverter("application/vnd.sun.xml.impress",  "hello.sxi");
        //doTestHtmlConverter("application/vnd.oasis.opendocument.presentation",  "hello.odp");
    }

}
