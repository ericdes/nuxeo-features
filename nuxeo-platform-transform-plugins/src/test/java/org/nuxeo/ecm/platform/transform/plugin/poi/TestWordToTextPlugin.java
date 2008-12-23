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
 * $Id: TestWordToTextPlugin.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.plugin.poi;

import java.io.File;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.DocumentTestUtils;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;

/**
 * Test the PDFBoxplugin for pdf to text transformation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestWordToTextPlugin extends AbstractPluginTestCase {

    private Plugin plugin;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        plugin = service.getPluginByName("word2text_poi");
    }

    @Override
    public void tearDown() throws Exception {
        plugin = null;
        super.tearDown();
    }

    public void testSmallWord2textConversion() throws Exception {
        String path = "test-data/hello.doc";

        SimpleTimer timer = new SimpleTimer();
        timer.start();
        List<TransformDocument> results = plugin.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path)));
        timer.stop();
        System.out.println(timer);

        File textFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "txt");
        assertEquals("text content", "Hello from a Microsoft Word Document!",
                DocumentTestUtils.readContent(textFile));
    }

    // XXX local test as the file is 60 Mo large...
    public void xtestBigWord2textConversion() throws Exception {
        String path = "test-data/BIG-DOC.doc";

        File f = FileUtils.getResourceFileFromContext(path);
        FileBlob blob = new FileBlob(f);

        SimpleTimer timer = new SimpleTimer();
        timer.start();
        List<TransformDocument> results = plugin.transform(null,
                new TransformDocumentImpl(blob));
        timer.stop();
        System.out.println(timer);

        File textFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "txt");
        assertTrue(textFile.length() > 0);

        // Debug => see the output
        // File dst = File.createTempFile("poitest", ".txt");
        // FileUtils.copy(textFile, dst);
        // assertEquals("text content", "Hello from a Microsoft Word Document!",
        // DocumentTestUtils.readContent(textFile));
    }

}
