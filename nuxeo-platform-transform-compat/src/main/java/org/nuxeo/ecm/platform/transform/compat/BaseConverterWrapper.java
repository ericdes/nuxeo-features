package org.nuxeo.ecm.platform.transform.compat;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.runtime.api.Framework;

public class BaseConverterWrapper {

    protected ConverterDescriptor descriptor;
    protected Map<String, Serializable> specOptions;

    protected ConversionService cs;

    public BaseConverterWrapper(ConverterDescriptor descriptor) {
        this.descriptor=descriptor;
    }


    protected ConversionService getConversionService() throws Exception {
        if(cs==null) {
            cs = Framework.getService(ConversionService.class);
        }
        if (cs==null) {
            throw new ClientException("Unable to locale ConversionService");
        }
        return cs;
    }

    public String getDestinationMimeType() {
        return descriptor.getDestinationMimeType();
    }

    public String getName() {
        return descriptor.getConverterName();
    }

    public List<String> getSourceMimeTypes() {
        return descriptor.getSourceMimeTypes();
    }

    public boolean isSourceCandidate(TransformDocument doc) {
        return isSourceCandidate(doc.getBlob());
    }

    public boolean isSourceCandidate(Blob blob) {
        String mt = blob.getMimeType();
        if (mt==null) {
            return false;
        }
        if (getSourceMimeTypes().contains(mt)) {
            return true;
        }
        return false;
    }


    public void setDestinationMimeType(String destinationMimeType) {
        throw new IllegalStateException("This method is no longer supported");
    }

    public void setName(String name) {
        throw new IllegalStateException("This method is no longer supported");
    }

    public void setSourceMimeTypes(List<String> sourceMimeTypes) {
        throw new IllegalStateException("This method is no longer supported");
    }

    public void setSpecificOptions(Map<String, Serializable> options) {
        this.specOptions=options;
    }

    protected Map<String,Serializable> buildParameters(Map<String, Serializable> options) {

        Map<String,Serializable> params = new HashMap<String, Serializable>();

        if (options!=null) {
            params.putAll(options);
        }
        if (specOptions!=null) {
            params.putAll(specOptions);
        }
        return params;
    }


}