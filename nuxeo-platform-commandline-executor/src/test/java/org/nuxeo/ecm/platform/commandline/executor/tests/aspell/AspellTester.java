package org.nuxeo.ecm.platform.commandline.executor.tests.aspell;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

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

/**
 *
 * Test Aspell command line
 *
 *
 */
public class AspellTester extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployContrib("org.nuxeo.ecm.platform.commandline.executor",
                "OSGI-INF/commandline-aspell-test-contribs.xml");

    }

    public void testAspellExec() throws Exception {
        CommandLineExecutorService cles = Framework
                .getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        CommandAvailability ca =  cles.getCommandAvailability("aspell");

        if (!ca.isAvailable()) {
            System.out.println("Aspell is not avalaible, skipping test");
            return;
        }

        CmdParameters params = new CmdParameters();
        params.addNamedParameter("lang", "fr_FR");
        params.addNamedParameter("encoding", "utf-8");

        String text2Check = "ceci est un test avec une fautte \n et encor une autre \n et une derniere pour la route";
        File file2Check = File.createTempFile("nuxeo-spell-check-in", "txt");
        PrintWriter printout = new PrintWriter(new BufferedWriter(
                new FileWriter(file2Check)));
        printout.print(text2Check);
        printout.flush();
        printout.close();

        params.addNamedParameter("textFile", file2Check);

        ExecResult result =  cles.execCommand("aspell", params);

        assertTrue(result.isSuccessful());
        assertTrue(result.getReturnCode()==0);

        List<String> lines = result.getOutput();

        System.out.println(lines);

        assertTrue(checkOutput(lines));

        params.addNamedParameter("textFile", "");
        result =  cles.execCommand("aspell", params);
        assertFalse(result.isSuccessful());
        assertTrue(result.getReturnCode()!=0);
        System.out.println(result.getOutput());


    }

    protected boolean checkOutput(List<String> lines) {
        for (String line:lines) {
            if (line.contains("& fautte 11 26")) {
                return true;
            }
        }
        return false;
    }
}
