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

package org.nuxeo.ecm.platform.commandline.executor.service.executors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;

/**
 * Default implementation of the {@link Executor} interface.
 * Use simple shell exec.
 *
 * @author tiry
 *
 */
public class ShellExecutor extends AbstractExecutor implements Executor {

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.platform.commandline.executor.service.executors.Executor#exec(org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor, java.lang.String)
     */
    public ExecResult exec(CommandLineDescriptor cmdDesc, CmdParameters params) {

        long t0 = System.currentTimeMillis();
        List<String> output = new ArrayList<String>();

        String paramString = getParametersString(cmdDesc, params);

        String[] cmd = {
                "/bin/sh",
                "-c",
                cmdDesc.getCommand() + " " + paramString + " 2>&1" };

        if (isWindows()) {
            cmd[0] = "cmd";
            cmd[1] = "/C";
        }

        Process p1 = null;
        try {
            p1 = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            return new ExecResult(e);
        }
        int exitCode = 0;

        if (cmdDesc.getReadOutput()) {
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    p1.getInputStream()));
            String strLine;
            try {
                while ((strLine = stdInput.readLine()) != null) {
                    output.add(strLine);
                }
            } catch (IOException e) {
                return new ExecResult(e);
            }
        }

        try {
            exitCode = p1.waitFor();
        } catch (InterruptedException e) {
            return new ExecResult(e);
        }

        long t1 = System.currentTimeMillis();
        return new ExecResult(output,t1-t0, exitCode);
    }
}
