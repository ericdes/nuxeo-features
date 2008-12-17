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
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.plugin.jr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.DocumentTestUtils;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.xml.Xml2TextPluginImpl;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;

public class TestJRTextPlugin extends AbstractPluginTestCase {

    public void testSmallXL2textConversion() throws Exception {
        String path = "test-data/hello.xls";

        ExcelToTextPlugin plugin = (ExcelToTextPlugin) service.getPluginByName("xl2text_jr");
        SimpleTimer timer = new SimpleTimer();
        timer.start();
        List<TransformDocument> results = plugin.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path)));
        timer.stop();
        System.out.println(timer);

        File textFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "txt");
        assertEquals("text content", "Hello from a Microsoft Excel Spreadsheet!",
                DocumentTestUtils.readContent(textFile));
    }

    public void testSmallOO2textConversion() throws Exception {
        String path = "test-data/hello.";

        List<String> extList = new ArrayList<String>();

        extList.add("ods");
        extList.add("odp");
        extList.add("odt");
        extList.add("sxc");
        extList.add("sxi");
        extList.add("sxw");

        for (String ext : extList) {
            OOoSimpleTextExtractor plugin = (OOoSimpleTextExtractor) service.getPluginByName(
                    "oo2text_jr");
            SimpleTimer timer = new SimpleTimer();
            timer.start();
            List<TransformDocument> results = plugin.transform(null,
                    new TransformDocumentImpl(getBlobFromPath(path + ext)));
            timer.stop();
            System.out.println(timer);

            File textFile = getFileFromInputStream(
                    results.get(0).getBlob().getStream(), "txt");
            assertTrue("text content on " + ext,
                    DocumentTestUtils.readContent(textFile).startsWith("Hello from"));
        }
    }

    public void testSmallXML2textConversion() throws Exception {
        String path = "test-data/hello.xml";

        Xml2TextPluginImpl plugin = (Xml2TextPluginImpl) service.getPluginByName("xml2text");
        SimpleTimer timer = new SimpleTimer();
        timer.start();
        List<TransformDocument> results = plugin.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path)));
        timer.stop();
        System.out.println(timer);

        File textFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "txt");
        assertEquals("text content", "Hello from a xml  document !",
                DocumentTestUtils.readContent(textFile));
    }

    public void testSmallHTML2textConversion() throws Exception {
        String path = "test-data/hello2.html";

        HtmlToTextPlugin plugin = (HtmlToTextPlugin) service.getPluginByName("html2text_jr");
        SimpleTimer timer = new SimpleTimer();
        timer.start();
        List<TransformDocument> results = plugin.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path)));
        timer.stop();
        System.out.println(timer);

        File textFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "txt");
        assertEquals("text content", "Hello from a html document",
                DocumentTestUtils.readContent(textFile));
    }

}
