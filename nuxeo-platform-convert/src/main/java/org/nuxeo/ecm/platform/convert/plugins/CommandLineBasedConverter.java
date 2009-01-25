/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Base class to implement {@link Converter} based on {@link CommandLineExecutorService}
 *
 * @author tiry
 *
 */
public abstract class CommandLineBasedConverter implements ExternalConverter {


    protected static final String CMD_NAME_PARAMETER = "CommandLineName";

    protected static final String TMP_PATH_PARAMETER = "TmpDirectory";

    protected Map<String, String> initParameters=null;

    protected CommandLineExecutorService cls;

    protected CommandLineExecutorService getCommandeLineService() {
        if (cls==null) {
            cls = Framework.getLocalService(CommandLineExecutorService.class);
        }
        return cls;
    }


    public String getTmpDirectory(Map<String, Serializable> parameters) {
        String tmp = initParameters.get(TMP_PATH_PARAMETER);
        if ((parameters!=null) &&(parameters.containsKey(TMP_PATH_PARAMETER))) {
            tmp = (String) parameters.get(TMP_PATH_PARAMETER);
        }
        if (tmp==null) {
            tmp = System.getProperty("java.io.tmpdir");
        }
        return tmp;
    }

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        String commandName = getCommandName(blobHolder, parameters);
        if (commandName==null) {
            throw new ConversionException("Unable to determine target CommandLine name");
        }

        Map<String, Blob> blobParams = getCmdBlobParameters(blobHolder, parameters);
        Map<String, String> strParams = getCmdStringParameters(blobHolder, parameters);

        CmdReturn result = execOnBlob(commandName, blobParams, strParams);

        return buildResult(result.output, result.params);
    }


    protected String getCommandName(BlobHolder blobHolder, Map<String, Serializable> parameters) {
        String commandName = initParameters.get(CMD_NAME_PARAMETER);
        if ((parameters!=null) && (parameters.containsKey(CMD_NAME_PARAMETER))) {
            commandName = (String)parameters.get(CMD_NAME_PARAMETER);
        }
        return commandName;
    }


    /**
     * Extracts BlobParameters
     *
     * @param blobHolder
     * @param parameters
     * @return
     */
    protected abstract Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException;

    /**
     * Extract String parameters
     * @param blobHolder
     * @param parameters
     * @return
     */
    protected abstract Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException;

    /**
     * Build result from commandLine output buffer
     *
     * @param cmdOutput
     * @return
     */
    protected abstract BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) throws ConversionException;


    protected class CmdReturn {
        protected CmdParameters params;
        protected List<String> output;

        public CmdReturn(CmdParameters params, List<String> output) {
            this.params=params;
            this.output=output;
        }

    }

    protected CmdReturn execOnBlob(String commandName, Map<String, Blob> blobParameters, Map<String, String> parameters) throws ConversionException {
        CmdParameters params = new CmdParameters();
        List<String> filesToDelete = new ArrayList<String>();

        try {
            if (blobParameters!=null) {
                for (String blobParamName : blobParameters.keySet()) {
                    Blob blob = blobParameters.get(blobParamName);
                    File file = File.createTempFile("cmdLineBasedConverter", blob.getFilename());
                    blob.transferTo(file);
                    params.addNamedParameter(blobParamName, file);
                    filesToDelete.add(file.getAbsolutePath());
                }
            }

            if (parameters!=null) {
                for (String paramName : parameters.keySet()) {
                    params.addNamedParameter(paramName, parameters.get(paramName));
                }
            }

            ExecResult result = cls.execCommand(commandName, params);

            if (!result.isSuccessful()) {
                throw new ConversionException("CommandLine returned code " + result.getReturnCode() + " : ", result.getError());
            }

            return new CmdReturn(params, result.getOutput());
        }
        catch (CommandNotAvailable e) {
            // XXX bubble installation instructions
            throw new ConversionException("Unable to find targetCommand",e);
        }
        catch (IOException e) {
            throw new ConversionException("Error while converting via CommandLineService", e);

        }
        finally {
            if (filesToDelete!=null) {
                for (String fileToDelete : filesToDelete) {
                    new File(fileToDelete).delete();
                }
            }
        }
    }

    public void init(ConverterDescriptor descriptor) {
        initParameters =  descriptor.getParameters();
        if (initParameters==null) {
            initParameters = new HashMap<String, String>();
        }
        getCommandeLineService();

    }

    public ConverterCheckResult isConverterAvailable() {

        String commandName = getCommandName(null, null);
        if (commandName==null) {
            // can not check
            return new ConverterCheckResult();
        }

        CommandAvailability ca = cls.getCommandAvailability(commandName);

        if (ca.isAvailable()) {
            return new ConverterCheckResult();
        }
        else {
            return new ConverterCheckResult(ca.getInstallMessage(), ca.getErrorMessage());
        }
    }
}