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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.AnnotationChangeListener;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.AnnotationModel;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.CSSClassManager;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.ImageRangeXPointer;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.NullRangeXPointer;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Point;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.StringRangeXPointer;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Utils;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Visitor;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPathUtil;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPointer;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator.DecoratorVisitor;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator.DecoratorVisitorFactory;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator.ImageDecorator;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class AnnotatedDocument implements AnnotationChangeListener {
    private List<Annotation> annotations = new ArrayList<Annotation>();

    private List<Annotation> decoratedAnnotations = new ArrayList<Annotation>();

    private static XPathUtil xPathUtil = new XPathUtil();

    private final ImageDecorator decorator;

    private AnnotationController controller;

    public AnnotatedDocument(AnnotationController controller) {
        this.controller = controller;
        decorator = new ImageDecorator(controller);
    }

    public void onChange(AnnotationModel model, ChangeEvent ce) {
        annotations = model.getAnnotations();
        Log.debug("On change: annotations.empty? " + annotations.isEmpty());
        if (annotations.isEmpty() || ce == ChangeEvent.annotation) {
            return;
        }

        update();
    }

    public void update() {
        update(false);
    }

    public void update(boolean forceDecorate) {
        Log.debug("Update annotations - forceDecorate: " + forceDecorate);
        if (annotations == null) {
            return;
        }

        if (forceDecorate) {
            decoratedAnnotations.clear();
            removeAllAnnotatedAreas();
        }

        for (Annotation annotation : annotations) {
            if (!decoratedAnnotations.contains(annotation)) {
                Log.debug("Decorate annotation");
                decorate(annotation);
                decoratedAnnotations.add(annotation);
            }
        }

        int selectedAnnotationIndex = getSelectedAnnotationIndex();
        if (selectedAnnotationIndex > -1) {
            updateSelectedAnnotation(selectedAnnotationIndex);
        }

        if (!isAnnotationsVisible()) {
            Log.debug("Hide annotations!");
            hideAnnotations();
            // disable popup listeners in case we just added a new annotation
            controller.disablePopupListeners();
        }
    }

    public void preDecorateDocument() {
        Document document = Document.get();
        Log.debug("preDecorateDocument -- isMultiImage? "
                + controller.isMultiImage());
        preDecorateDocument(document);
    }

    private static void preDecorateDocument(Document document) {
        Log.debug("Predecorate document !");
        NodeList<Element> elements = document.getElementsByTagName("img");
        for (int x = 0; x < elements.getLength(); x++) {
            Element element = elements.getItem(x);
            DivElement divElement = document.createDivElement();
            divElement.getStyle().setProperty("position", "relative");
            divElement.setClassName(AnnotationConstant.IGNORED_ELEMENT);
            String path = xPathUtil.getXPath(element);
            path = XPathUtil.toIdableName(path);
            divElement.setId(path);
            Element nextSibling = element.getNextSiblingElement();
            Element parent = element.getParentElement();
            if (nextSibling == null) {
                parent.appendChild(divElement);
            } else {
                parent.insertBefore(divElement, nextSibling);
            }
            divElement.appendChild(element);
        }
    }

    public void decorate(Annotation annotation) {
        XPointer xpointer = annotation.getXpointer();
        if (xpointer instanceof StringRangeXPointer) {
            decorateStringRange((StringRangeXPointer) xpointer, annotation);
        } else if (xpointer instanceof ImageRangeXPointer) {
            decorateImageRange((ImageRangeXPointer) xpointer, annotation);
        }
    }

    private void decorateImageRange(ImageRangeXPointer xpointer,
            Annotation annotation) {
        ImageElement img = xpointer.getImage(controller.isMultiImage());
        if (img == null) {
            return;
        }
        Point[] points = controller.filterAnnotation(xpointer.getTopLeft(),
                xpointer.getBottomRight());
        if (points == null) {
            return;
        }
        decorator.addAnnotatedArea(points[0].getX(), points[0].getY(),
                points[1].getX(), points[1].getY(), img, annotation, controller);
    }

    private void decorateStringRange(StringRangeXPointer xpointer,
            Annotation annotation) {
        DecoratorVisitor processor = DecoratorVisitorFactory.forAnnotation(
                annotation, controller);
        Visitor visitor = new Visitor(processor);
        visitor.process(xpointer.getOwnerDocument());
    }

    public void updateSelectedAnnotation(int index) {
        Annotation annotation = annotations.get(index);
        BodyElement bodyElement = Document.get().getBody();
        if (!(annotation.getXpointer() instanceof NullRangeXPointer)) {
            NodeList<Element> spans = bodyElement.getElementsByTagName("span");
            NodeList<Element> as = bodyElement.getElementsByTagName("div");
            int scrollTop = Integer.MAX_VALUE;
            int scrollLeft = Integer.MAX_VALUE;
            for (int x = 0; x < spans.getLength(); x++) {
                Element element = spans.getItem(x);
                if (processElement(annotation, element)) {
                    int[] absTopLeft = Utils.getAbsoluteTopLeft(element, Document.get());
                    if (absTopLeft[0] < scrollTop) {
                        scrollTop = absTopLeft[0];
                    }
                    if (absTopLeft[1] < scrollLeft) {
                        scrollLeft = absTopLeft[1];
                    }
                }
            }
            for (int x = 0; x < as.getLength(); x++) {
                Element element = as.getItem(x);
                if (processElement(annotation, element)) {
                    int[] absTopLeft = Utils.getAbsoluteTopLeft(element, Document.get());
                    if (absTopLeft[0] < scrollTop) {
                        scrollTop = absTopLeft[0];
                    }
                    if (absTopLeft[1] < scrollLeft) {
                        scrollLeft = absTopLeft[1];
                    }
                }
            }
            bodyElement.setScrollTop(scrollTop == Integer.MAX_VALUE ? 0
                    : scrollTop);
            bodyElement.setScrollLeft(scrollLeft == Integer.MAX_VALUE ? 0
                    : scrollLeft);
        }
    }

    private boolean processElement(Annotation annotation,
            Element element) {
        CSSClassManager manager = new CSSClassManager(element);
        // remove old
        manager.removeClass(AnnotationConstant.SELECTED_CLASS_NAME);
        // set new
        if (manager.isClassPresent(AnnotationConstant.DECORATE_CLASS_NAME
                + annotation.getId())) {
            manager.addClass(AnnotationConstant.SELECTED_CLASS_NAME);

            return true;
        }
        return false;
    }

    private native int getSelectedAnnotationIndex() /*-{
        if (typeof top['selectedAnnotationIndex'] != "undefined") {
            return top['selectedAnnotationIndex'];
        } else {
            return -1;
        }
    }-*/;

    public void hideAnnotations() {
        BodyElement bodyElement = Document.get().getBody();
        NodeList<Element> spans = bodyElement.getElementsByTagName("span");
        NodeList<Element> divs = bodyElement.getElementsByTagName("div");

        for (int x = 0; x < spans.getLength(); x++) {
            Element element = spans.getItem(x);
            CSSClassManager manager = new CSSClassManager(element);
            if (manager.isClassPresent(AnnotationConstant.DECORATE_CLASS_NAME)) {
                manager.removeClass(AnnotationConstant.DECORATE_CLASS_NAME);
                manager.addClass(AnnotationConstant.DECORATE_NOT_CLASS_NAME);
            }
        }

        for (int x = 0; x < divs.getLength(); x++) {
            Element element = divs.getItem(x);
            CSSClassManager manager = new CSSClassManager(element);
            if (manager.isClassPresent(AnnotationConstant.DECORATE_CLASS_NAME)) {
                manager.removeClass(AnnotationConstant.DECORATE_CLASS_NAME);
                manager.addClass(AnnotationConstant.DECORATE_NOT_CLASS_NAME);
            }
        }
        setAnnotationsShown(false);
    }

    private native void setAnnotationsShown(boolean annotationsShown) /*-{
        top['annotationsShown'] = annotationsShown;
    }-*/;

    public void showAnnotations() {
        BodyElement bodyElement = Document.get().getBody();
        NodeList<Element> spans = bodyElement.getElementsByTagName("span");
        NodeList<Element> divs = bodyElement.getElementsByTagName("div");

        for (int x = 0; x < spans.getLength(); x++) {
            Element element = spans.getItem(x);
            CSSClassManager manager = new CSSClassManager(element);
            if (manager.isClassPresent(AnnotationConstant.DECORATE_NOT_CLASS_NAME)) {
                manager.removeClass(AnnotationConstant.DECORATE_NOT_CLASS_NAME);
                manager.addClass(AnnotationConstant.DECORATE_CLASS_NAME);
            }
            if (manager.isClassPresent(AnnotationConstant.SELECTED_NOT_CLASS_NAME)) {
                manager.removeClass(AnnotationConstant.SELECTED_NOT_CLASS_NAME);
                manager.addClass(AnnotationConstant.SELECTED_CLASS_NAME);
            }
        }

        for (int x = 0; x < divs.getLength(); x++) {
            Element element = divs.getItem(x);
            CSSClassManager manager = new CSSClassManager(element);
            if (manager.isClassPresent(AnnotationConstant.DECORATE_NOT_CLASS_NAME)) {
                manager.removeClass(AnnotationConstant.DECORATE_NOT_CLASS_NAME);
                manager.addClass(AnnotationConstant.DECORATE_CLASS_NAME);
            }
            if (manager.isClassPresent(AnnotationConstant.SELECTED_NOT_CLASS_NAME)) {
                manager.removeClass(AnnotationConstant.SELECTED_NOT_CLASS_NAME);
                manager.addClass(AnnotationConstant.SELECTED_CLASS_NAME);
            }
        }
        setAnnotationsShown(true);
    }

    public native boolean isAnnotationsVisible() /*-{
        if (typeof top['annotationsShown'] != "undefined") {
            return top['annotationsShown'];
        } else {
            return true;
        }
    }-*/;

    private void removeAllAnnotatedAreas() {
        String className = isAnnotationsVisible() ? AnnotationConstant.DECORATE_CLASS_NAME
                : AnnotationConstant.DECORATE_NOT_CLASS_NAME;
        BodyElement bodyElement = Document.get().getBody();
        NodeList<Element> as = bodyElement.getElementsByTagName("div");
        removeAnchorAreas(as, className);
        NodeList<Element> spans = bodyElement.getElementsByTagName("span");
        removeSpanAreas(spans, className);
    }

    private void removeAnchorAreas(NodeList<Element> nodes, String className) {
        List<Element> elements = getElementsToRemove(nodes, className);
        for (Element element : elements) {
            element.getParentElement().removeChild(element);
        }
    }

    private List<Element> getElementsToRemove(NodeList<Element> nodes, String className) {
        List<Element> elementsToRemove = new ArrayList<Element>();
        for (int i = 0; i < nodes.getLength(); ++i) {
            Element element = nodes.getItem(i);
            CSSClassManager manager = new CSSClassManager(element);
            if (manager.isClassPresent(className)) {
                elementsToRemove.add(element);
            }
        }
        return elementsToRemove;
    }

    private void removeSpanAreas(NodeList<Element> nodes, String className) {
        List<Element> elements = getElementsToRemove(nodes, className);
        for (Element element : elements) {
            Element parent = element.getParentElement();
            NodeList<Node> childNodes = element.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); ++i) {
                Node node = childNodes.getItem(i);
                parent.insertBefore(node, element);
            }
            parent.removeChild(element);
        }
    }

    public void decorateSelectedText(Annotation annotation) {
        DecoratorVisitor processor = DecoratorVisitorFactory.forSelectedText(annotation);
        Visitor visitor = new Visitor(processor);
        StringRangeXPointer xpointer = (StringRangeXPointer) annotation.getXpointer();
        visitor.process(xpointer.getOwnerDocument());
    }

    public void removeSelectedTextDecoration(Annotation annotation) {
        BodyElement bodyElement = Document.get().getBody();
        NodeList<Element> spans = bodyElement.getElementsByTagName("span");

        String className = AnnotationConstant.SELECTED_TEXT_CLASS_NAME;
        removeSpanAreas(spans, className);
    }

}
