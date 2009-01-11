package org.nuxeo.ecm.platform.transform.compat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.ecm.core.convert.service.MimeTypeTranslationHelper;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;

public class TransformerWrappingConverter extends BaseConverterWrapper
        implements Transformer {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected List<ConverterDescriptor> descriptorsChain;

    public TransformerWrappingConverter(ConverterDescriptor descriptor) {
        super(descriptor);
    }

    public String getMimeTypeDestination() {
        return super.getDestinationMimeType();
    }

    public List<String> getMimeTypeSources() {
        return super.getSourceMimeTypes();
    }

    protected List<ConverterDescriptor> getDescriptorChain() {
        if (descriptorsChain == null) {

            descriptorsChain = new ArrayList<ConverterDescriptor>();

            List<String> steps = descriptor.getSteps();
            if ((steps==null) || (steps.size()==0)) {
                descriptorsChain.add(descriptor);
                return descriptorsChain;
            }

            steps.add(descriptor.getDestinationMimeType());

            String srcMT = "*";
            for (String dstMT : steps) {
                String converterName = MimeTypeTranslationHelper
                        .getConverterName(srcMT, dstMT);
                if (converterName == null) {
                    srcMT = getSourceMimeTypes().get(0);
                    converterName = MimeTypeTranslationHelper.getConverterName(
                            srcMT, dstMT);
                }

                if (converterName != null) {
                    ConverterDescriptor converterDesc = ConversionServiceImpl
                            .getConverterDesciptor(converterName);
                    descriptorsChain.add(converterDesc);
                }
            }
        }
        return descriptorsChain;

    }

    public List<Plugin> getPluginChains() {

        List<Plugin> plugins = new ArrayList<Plugin>();
        for (ConverterDescriptor desc : getDescriptorChain()) {
            plugins.add(new PluginWrappingConverter(desc));
        }
        return plugins;
    }

    public void setDefaultOptions(
            Map<String, Map<String, Serializable>> defaultOptions) {
        // TODO Auto-generated method stub

    }

    public void setPluginChains(List<String> pluginsChain) {
        throw new IllegalStateException("This method is no longer supported");
    }

    public List<TransformDocument> transform(
            Map<String, Map<String, Serializable>> options,
            TransformDocument... sources) {
          Blob[] blobs = new Blob[sources.length];
          for (int i = 0; i < sources.length; i++) {
              blobs[i] = sources[i].getBlob();
          }
          return transform(options, blobs);
    }

    public List<TransformDocument> transform(
            Map<String, Map<String, Serializable>> options, Blob... blobs) {

        List<Blob> blobList = Arrays.asList(blobs);

        BlobHolder bh = new SimpleBlobHolder(blobList);

        try {
            BlobHolder result = getConversionService().convert(descriptor.getConverterName(), bh, null);
            return TransformDocumensFactory.wrap(result);
        }
        catch (Exception e) {
            return null;
        }
    }

    public Map<String, Map<String, Serializable>> getDefaultOptions() {

        Map<String, Map<String, Serializable>> theoptions = new HashMap<String, Map<String,Serializable>>();

        for (ConverterDescriptor desc : getDescriptorChain()) {
            Map<String, Serializable> coptions = new HashMap<String, Serializable>();
            coptions.putAll(desc.getParameters());
            theoptions.put(desc.getConverterName(), coptions);
        }
        return theoptions;
    }

}
