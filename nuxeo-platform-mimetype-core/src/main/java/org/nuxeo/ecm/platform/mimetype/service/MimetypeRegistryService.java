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
 * $Id: MimetypeEntry.java 2920 2006-09-15 13:28:15Z janguenot $
 */
package org.nuxeo.ecm.platform.mimetype.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * MimetypeEntry registry service.
 * <p>
 * Singleton holding a registry of mimetype entries and exposes an API to grab
 * information related to these mimetypes. As well, this is possible to ask for
 * a mimetype magic detection from a stream or file using the API.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class MimetypeRegistryService extends DefaultComponent implements
        MimetypeRegistry {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService");

    private static final Log log = LogFactory.getLog(MimetypeRegistryService.class);

    protected Map<String, MimetypeEntry> mimetypeByNormalisedRegistry;

    protected Map<String, MimetypeEntry> mimetypeByExtensionRegistry;

    protected Map<String, ExtensionDescriptor> extensionRegistry;

    private RuntimeContext bundle;

    public MimetypeRegistryService() {
        initializeRegistries();
    }

    protected void initializeRegistries() {
        mimetypeByNormalisedRegistry = new HashMap<String, MimetypeEntry>();
        mimetypeByExtensionRegistry = new HashMap<String, MimetypeEntry>();
        extensionRegistry = new HashMap<String, ExtensionDescriptor>();
    }

    @Override
    public void activate(ComponentContext context) {
        bundle = context.getRuntimeContext();
        initializeRegistries();
    }

    @Override
    public void deactivate(ComponentContext context) {
        mimetypeByNormalisedRegistry = null;
        mimetypeByExtensionRegistry = null;
        extensionRegistry = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            if (contrib instanceof MimetypeDescriptor) {
                MimetypeDescriptor mimetypeDescriptor = (MimetypeDescriptor) contrib;
                registerMimetype(mimetypeDescriptor.getMimetype());
            } else if (contrib instanceof ExtensionDescriptor) {
                registerFileExtension((ExtensionDescriptor) contrib);
            }
        }
    }

    public void registerMimetype(MimetypeEntry mimetype) {
        log.info("registering mimetype with normalized name: "
                + mimetype.getNormalized());
        mimetypeByNormalisedRegistry.put(mimetype.getNormalized(), mimetype);
        for (String extension : mimetype.getExtensions()) {
            mimetypeByExtensionRegistry.put(extension, mimetype);
        }
    }

    public void registerFileExtension(ExtensionDescriptor extensionDescriptor) {
        log.info("Registering file extension: " + extensionDescriptor.getName());
        extensionRegistry.put(extensionDescriptor.getName(),
                extensionDescriptor);
    }

    @Override
    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            if (contrib instanceof MimetypeDescriptor) {
                MimetypeDescriptor mimetypeDescriptor = (MimetypeDescriptor) contrib;
                unregisterMimetype(mimetypeDescriptor.getNormalized());
            } else if (contrib instanceof ExtensionDescriptor) {
                ExtensionDescriptor extensionDescriptor = (ExtensionDescriptor) contrib;
                unregisterFileExtension(extensionDescriptor);
            }
        }
    }

    public void unregisterMimetype(String mimetypeName) {
        log.info("Unregistering mimetype with normalised name: " + mimetypeName);
        MimetypeEntry mimetype = mimetypeByNormalisedRegistry.get(mimetypeName);
        if (mimetype == null) {
            return;
        }
        List<String> extensions = mimetype.getExtensions();
        mimetypeByNormalisedRegistry.remove(mimetypeName);
        for (String extension : extensions) {
            // FIXME: equals always fails because types are incompatible.
            if (mimetype.getNormalized().equals(
                    mimetypeByExtensionRegistry.get(extension))) {
                mimetypeByExtensionRegistry.remove(extension);
            }
        }
    }

    public void unregisterFileExtension(ExtensionDescriptor extensionDescriptor) {
        log.info("Unregistering file extension: "
                + extensionDescriptor.getName());
        extensionRegistry.remove(extensionDescriptor.getName());
    }

    public RuntimeContext getContext() {
        return bundle;
    }

    public List<String> getExtensionsFromMimetypeName(String mimetypeName) {
        List<String> extensions = new ArrayList<String>();
        for (String key : mimetypeByNormalisedRegistry.keySet()) {
            MimetypeEntry mimetypeEntry = mimetypeByNormalisedRegistry.get(key);
            if (mimetypeEntry.getMimetypes().contains(mimetypeName)) {
                extensions.addAll(mimetypeEntry.getExtensions());
            }
        }
        return extensions;
    }

    public MimetypeEntry getMimetypeEntryByName(String name) {
        return mimetypeByNormalisedRegistry.get(name);
    }

    public String getMimetypeFromFile(File file)
            throws MimetypeNotFoundException, MimetypeDetectionException {
        try {
            // Magic magic = new Magic(); // need to be initialized
            MagicMatch match = Magic.getMagicMatch(file, true, false);

            if (match.getSubMatches().isEmpty()) {
                return match.getMimeType();
            } else {
                // Submatches found
                // TODO: we only take the first here
                // what to do with other possible responses ?
                // b.t.w., multiple responses denotes a non-accuracy problem in
                // magic.xml but be careful to nested possible
                // sub-sub-...-submatches make this as recursive ?
                Collection<MagicMatch> possibilities = match.getSubMatches();
                Iterator<MagicMatch> iter = possibilities.iterator();
                MagicMatch m = iter.next();

                String matchingMimetype = m.getMimeType();

                // need to clean for subsequent calls
                possibilities.clear();
                match.setSubMatches(possibilities);

                return matchingMimetype;
            }
        } catch (MagicMatchNotFoundException e) {
            if (file.getAbsolutePath() != null){
                return getMimetypeFromFilename(file.getAbsolutePath());
            }
           throw new MimetypeNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new MimetypeDetectionException(e.getMessage(), e);
        }
    }

    public String getMimetypeFromExtension(String extension)
            throws MimetypeNotFoundException {
        String lowerCaseExtension = extension.toLowerCase();
        ExtensionDescriptor extensionDescriptor = extensionRegistry.get(lowerCaseExtension);
        if (extensionDescriptor == null) {
            // no explicit extension rule, analyse the inverted mimetype
            // registry
            MimetypeEntry mimetype = mimetypeByExtensionRegistry.get(lowerCaseExtension);
            if (mimetype == null) {
                throw new MimetypeNotFoundException(
                        "no registered mimetype has extension: " + lowerCaseExtension);
            } else {
                return mimetype.getNormalized();
            }
        } else {
            if (extensionDescriptor.isAmbiguous()) {
                throw new MimetypeNotFoundException(String.format(
                        "mimetype for %s is ambiguous, binary sniffing needed",
                        lowerCaseExtension));
            } else {
                return extensionDescriptor.getMimetype();
            }
        }
    }

    public String getMimetypeFromFilename(String filename)
            throws MimetypeNotFoundException {
        String[] parts = filename.split("\\.");
        if (parts.length < 2) {
            throw new MimetypeNotFoundException(filename + "has no extension");
        }
        return getMimetypeFromExtension(parts[parts.length - 1]);
    }

    // the stream based detection is deprecated and should be replaced by
    // StreamingBlob detection instead to make serialization efficient for
    // remote call
    @Deprecated
    public String getMimetypeFromStream(InputStream stream)
            throws MimetypeNotFoundException, MimetypeDetectionException {
        File file = null;
        try {
            file = File.createTempFile("NXMimetypeBean", ".bin");
            FileUtils.copyToFile(stream, file);
            return getMimetypeFromFile(file);
        } catch (IOException e) {
            throw new MimetypeDetectionException(e.getMessage(), e);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    /**
     * Finds the mimetype of a stream content and returns provided default if
     * not possible.
     *
     * @param is content to be analyzed
     * @param defaultMimetype default mimetype to be used if no found
     * @return the string mimetype
     * @throws MimetypeDetectionException
     * @author lgodard
     */
    @Deprecated
    // use getMimetypeFromBlobWithDefault instead
    public String getMimetypeFromStreamWithDefault(InputStream is,
            String defaultMimetype) throws MimetypeDetectionException {
        try {
            return getMimetypeFromStream(is);
        } catch (MimetypeNotFoundException e) {
            return defaultMimetype;
        }
    }

    public String getMimetypeFromBlob(Blob blob)
            throws MimetypeNotFoundException, MimetypeDetectionException {
        File file = null;
        try {
            file = File.createTempFile("NXMimetypeBean", ".bin");
            FileUtils.copyToFile(blob.getStream(), file);
            return getMimetypeFromFile(file);
        } catch (IOException e) {
            throw new MimetypeDetectionException(e.getMessage(), e);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    public MimetypeEntry getMimetypeEntryByMimeType(String mimetype) {
        MimetypeEntry mtype = null;
        if (mimetype != null) {
            for (String key : mimetypeByNormalisedRegistry.keySet()) {
                MimetypeEntry entry = mimetypeByNormalisedRegistry.get(key);
                if (mimetype.equals(entry.getNormalized())
                        || entry.getMimetypes().contains(mimetype)) {
                    mtype = entry;
                    break;
                }
              }
        }
        return mtype;
    }

    /**
     * Finds the mimetype of a Blob content and returns provided default if not
     * possible.
     *
     * @param blob content to be analyzed
     * @param defaultMimetype defaultMimeType to be used if no found
     * @return the string mimetype
     * @author lgodard
     * @throws MimetypeDetectionException
     */
    public String getMimetypeFromBlobWithDefault(Blob blob,
            String defaultMimetype) throws MimetypeDetectionException {
        try {
            return getMimetypeFromBlob(blob);
        } catch (MimetypeNotFoundException e) {
            return defaultMimetype;
        }
    }

    /**
     * Finds the mimetype of some content according to its filename and / or
     * binary content.
     *
     * @param filename extension to analyze
     * @param blob content to be analyzed if filename is ambiguous
     * @param defaultMimetype defaultMimeType to be used if no found
     * @return the string mimetype
     * @throws MimetypeDetectionException
     * @author lgodard
     */
    public String getMimetypeFromFilenameAndBlobWithDefault(String filename,
            Blob blob, String defaultMimetype)
            throws MimetypeDetectionException {
        try {
            return getMimetypeFromFilename(filename);
        } catch (MimetypeNotFoundException e) {
            // failed to detect mimetype on extension: fallback to Blob based
            // detection
            try {
                return getMimetypeFromBlob(blob);
            } catch (MimetypeNotFoundException mtnfe) {
                return defaultMimetype;
            }
        }
    }

}