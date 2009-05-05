package org.nuxeo.ecm.platform.preview.converters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;
import org.nuxeo.runtime.api.Framework;

public class HtmlPreviewConverter implements ExternalConverter {

    protected static ConversionService cs;
    protected static Boolean canUsePDF2Html = null;
    protected static Boolean canUseOOo2Html = null;

    protected static ConversionService getConversionService() {
        if (cs == null) {
            cs = Framework.getLocalService(ConversionService.class);
        }
        return cs;
    }

    protected static boolean getCanUsePDF2Html() {
        if (canUsePDF2Html==null) {
            try {
                canUsePDF2Html = getConversionService().isConverterAvailable("pdf2html").isAvailable();
            }
            catch (Exception e) {
                return false;
            }
        }
        return canUsePDF2Html;
    }

    protected static boolean getCanUseOOo2Html() {
        if (canUseOOo2Html==null) {
            try {
                canUseOOo2Html = getConversionService().isConverterAvailable("office2html").isAvailable();
            }
            catch (Exception e) {
                return false;
            }
        }
        return canUseOOo2Html;
    }

    protected List<String> getConverterChain(String srcMT) {
        List<String> subConverters = new ArrayList<String>();

        if (srcMT == null) {
            return null;
        }

        if (srcMT.equals("text/html") || srcMT.equals("text/xml") || srcMT.equals("text/xhtml")) {
            return subConverters;
        }

        if (getCanUsePDF2Html()) {
            if (srcMT.equals("application/pdf")) {
                subConverters.add("pdf2html");
            } else {
                subConverters.add("any2pdf");
                subConverters.add("pdf2html");
            }
        } else {
            if (getCanUsePDF2Html()) {
                subConverters.add("office2html");
            } else {
                return null;
            }
        }

        return subConverters;
    }

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        Blob sourceBlob = null;

        try {
            sourceBlob = blobHolder.getBlob();
        } catch (Exception e) {
            throw new ConversionException("Can not fetch blob", e);
        }

        List<String> subConverters = getConverterChain(sourceBlob.getMimeType());

        if (subConverters == null) {
            throw new ConversionException(
                    "Can not find suitable underlying converters to handle html preview");
        }

        BlobHolder result = blobHolder;

        for (String converterName : subConverters) {
            result = getConversionService().convert(converterName, result,
                    parameters);
        }
        return result;
    }

    public void init(ConverterDescriptor descriptor) {
        // TODO Auto-generated method stub
    }

    public ConverterCheckResult isConverterAvailable() {

        ConverterCheckResult result = new ConverterCheckResult();
        result.setAvailable(getCanUseOOo2Html() || getCanUsePDF2Html());
        return result;
    }

}
