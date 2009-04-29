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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.pictures.tiles.gwt.client.view;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.controller.TilingController;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingModel;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingModelListener;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingModel.TilingModelEvent;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.util.Point;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.util.Rectangle;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class TilingPreviewPanel extends Composite implements
        TilingModelListener {

    private static final String DEFAULT_CLASS_NAME = "tilingPreviewPanel";

    private AbsolutePanel tilingViewPanel;

    private AbsolutePanel tilingView;

    private final TilingController controller;

    private final TilingModel model;

    private final Map<String, Image> images = new HashMap<String, Image>();

    public TilingPreviewPanel(TilingController tilingController,
            TilingModel tilingModel) {
        controller = tilingController;
        model = tilingModel;
        model.addListener(this);

        // create the panel to show the part of the image
        tilingViewPanel = new AbsolutePanel();
        tilingViewPanel.setPixelSize(model.getViewAreaWidth(),
                model.getViewAreaHeight());
        tilingView = new AbsolutePanel();
        tilingViewPanel.add(tilingView);

        tilingViewPanel.addStyleName("tilingViewPanel");

        AbsolutePanel rootPanel = new AbsolutePanel();
        rootPanel.setPixelSize(model.getViewAreaWidth(),
                model.getViewAreaHeight());
        rootPanel.addStyleName("tilingMasterContainer");
        rootPanel.add(tilingViewPanel, 0, 0);
        
        HorizontalPanel hPanel = createZoomButtons();
        rootPanel.add(hPanel, 20, 20);
        
        initWidget(rootPanel);

        setStyleName(DEFAULT_CLASS_NAME);
    }

    private HorizontalPanel createZoomButtons() {
        HorizontalPanel buttons = new HorizontalPanel();

        Button resetZoomButton = new Button();
        resetZoomButton.addClickListener(new ClickListener() {
            public void onClick(Widget arg0) {
                Window.Location.assign(Window.Location.getHref());
                decorate();
            }
        });
        resetZoomButton.addStyleName("resetZoomButton");
        resetZoomButton.setTitle("Default size");
        buttons.add(resetZoomButton);
        
        Button zoomInButton = new Button();
        zoomInButton.addClickListener(new ClickListener() {
            public void onClick(Widget arg0) {
                model.zoomIn();
            }
        });
        zoomInButton.addStyleName("zoomInButton");
        zoomInButton.setTitle("Zoom in");
        buttons.add(zoomInButton);

        Button zoomOutButton = new Button();
        zoomOutButton.addClickListener(new ClickListener() {
            public void onClick(Widget arg0) {
                model.zoomOut();
            }
        });
        zoomOutButton.addStyleName("zoomOutButton");
        zoomOutButton.setTitle("Zoom out");
        buttons.add(zoomOutButton);

        return buttons;
    }

    public void onModelChange(TilingModelEvent event, TilingModel model) {
        switch (event) {
        case MOVE_EVENT:
            // update the view (move)
            renderView();
            break;
        case TILING_INFO_UPDATED:
            // clean and reload the view
            resetView();
            renderView();
            break;
        }
    }

    private void renderView() {
        int widthInTiles = model.getWidthInTiles();
        int heightInTiles = model.getHeightInTiles();

        int top = model.getViewAreaTop();
        int left = model.getViewAreaLeft();
        int width = model.getViewAreaWidth();
        int height = model.getViewAreaHeight();

        Rectangle viewableArea = new Rectangle(new Point(left, top), width,
                height);

        tilingView.clear();
        for (int y = 0; y < heightInTiles; ++y) {
            for (int x = 0; x < widthInTiles; ++x) {
                String imageUrl = model.getBaseUrl() + "?x=" + x + "&y=" + y;
                imageUrl += "&date=" + model.getLastModificationDate();

                int imageLeft = x * model.getTileWidth();
                int imageTop = y * model.getTileHeight();
                int imageRight = imageLeft + model.getTileWidth();
                int imageBottom = imageTop + model.getTileHeight();

                Point imageTopLeft = new Point(imageLeft, imageTop);
                Point imageTopRight = new Point(imageRight, imageTop);
                Point imageBottomLeft = new Point(imageLeft, imageBottom);
                Point imageBottomRight = new Point(imageRight, imageBottom);

                if (!viewableArea.containsAny(imageTopLeft, imageBottomLeft,
                        imageTopRight, imageBottomRight)) {
                    // Always draw the first image. It's used to display the
                    // annotations
                    if (!(x == 0 && y == 0)) {
                        continue;
                    }
                }

                Log.debug("Getting image: " + imageUrl);
                Image image = getImage(imageUrl);

                if (x == 0 && y == 0) {
                    image.getElement().setId("annotationRootImage");
                }

                tilingView.add(image, imageLeft, imageTop);
            }
        }
        tilingViewPanel.add(tilingView, -left, -top);

        decorate();
    }

    private Image getImage(String imageUrl) {
        Image image = images.get(imageUrl);
        if (image == null) {
            image = new Image(imageUrl);
            images.put(imageUrl, image);
        }
        return image;
    }

    private void decorate() {
        try {
            controller.updateAnnotationDecoration();
        } catch (Exception e) {
            // do nothing
        }
    }

    private void resetView() {
        images.clear();
        tilingView.clear();
        tilingView.setPixelSize(model.getTotalWidth(), model.getTotalHeight());
    }

}
