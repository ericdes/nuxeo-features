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

import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.controller.TilingController;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingInfo;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingInfoCallback;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingModel;

import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class TilingMainPanel extends Composite {

    private static class ThumbnailButton extends Image {

        private static String HIDE_THUMBNAIL_IMG = "/nuxeo/img/hide_thumbnail.png";

        private static String SHOW_THUMBNAIL_IMG = "/nuxeo/img/show_thumbnail.png";

        private static int THUMBNAIL_BUTTON_SIZE = 17;

        private boolean showThumbnail = true;

        public ThumbnailButton(
                final TilingPreviewControllerPanel previewControllerPanel) {
            super(HIDE_THUMBNAIL_IMG);

            addMouseListener(new MouseListenerAdapter() {
                @Override
                public void onMouseDown(Widget sender, int x, int y) {
                    Event currentEvent = Event.getCurrentEvent();
                    DOM.eventPreventDefault(currentEvent);
                    currentEvent.cancelBubble(true);
                }
            });

            final ThumbnailButton thumbnailButton = this;
            addClickListener(new ClickListener() {
                public void onClick(Widget arg0) {
                    showThumbnail = !showThumbnail;
                    previewControllerPanel.setVisible(showThumbnail);
                    thumbnailButton.setUrl(showThumbnail ? HIDE_THUMBNAIL_IMG
                            : SHOW_THUMBNAIL_IMG);
                }
            });
        }

        @Override
        public int getWidth() {
            return THUMBNAIL_BUTTON_SIZE;
        }

        @Override
        public int getHeight() {
            return THUMBNAIL_BUTTON_SIZE;
        }

    }

    private static final int TILE_WIDTH = 256;

    private static final int TILE_HEIGHT = 256;

    private static final int PREVIEW_PANEL_BORDER_SIZE = 3;

    private String repoId;

    private String docId;

    public TilingMainPanel() {
        final Dictionary dictionary = Dictionary.getDictionary("serverSetting");

        repoId = dictionary.get("repoId");
        docId = dictionary.get("docId");

        loadControllerPanelTilingInfo();
    }

    private void loadControllerPanelTilingInfo() {
        final TilingInfo tilingInfo = new TilingInfo(repoId, docId, 64, 64, 3);
        tilingInfo.updateTilingInfo(new TilingInfoCallback() {
            public void tilingInfoUpdated() {
                // Continue the loading
                loadModelTilingInfo(tilingInfo);
            }
        });
    }

    private void loadModelTilingInfo(final TilingInfo tilingInfo) {
        // Compute the maximum number of tiles we can display
        int maxTilesW = ((Window.getClientWidth() - PREVIEW_PANEL_BORDER_SIZE * 2) / TILE_WIDTH) + 1;
        int maxTilesH = ((Window.getClientHeight() - PREVIEW_PANEL_BORDER_SIZE * 2) / TILE_HEIGHT) + 1;

        int maxTiles = maxTilesW > maxTilesH ? maxTilesW : maxTilesH;
        maxTiles += 1;
        final TilingInfo currentTilingInfo = new TilingInfo(repoId, docId,
                TILE_WIDTH, TILE_HEIGHT, maxTiles);
        currentTilingInfo.updateTilingInfo(new TilingInfoCallback() {
            public void tilingInfoUpdated() {
                finishLoading(tilingInfo, currentTilingInfo);
            }
        });
    }

    private void finishLoading(TilingInfo sourceTilingInfo,
            TilingInfo currentTilingInfo) {
        // Size of the view area
        final int width = Window.getClientWidth();
        final int height = Window.getClientHeight();
        TilingModel model = new TilingModel(currentTilingInfo, width, height,
                currentTilingInfo.getZoom());

        // Create a controller
        TilingController controller = new TilingController(sourceTilingInfo,
                model);

        // the panels
        AbsolutePanel rootPanel = new AbsolutePanel();
        TilingPreviewPanel previewPanel = new TilingPreviewPanel(controller,
                model);
        rootPanel.add(previewPanel);

        final TilingPreviewControllerPanel controllerPanel = new TilingPreviewControllerPanel(
                sourceTilingInfo, model);
        controllerPanel.addStyleName("thumbnail-panel");
        final int controllerPanelWidth = (int) Math.round(sourceTilingInfo.getOriginalImageWidth()
                * sourceTilingInfo.getZoom());
        final int controllerPanelHeight = (int) Math.round(sourceTilingInfo.getOriginalImageHeight()
                * sourceTilingInfo.getZoom());
        rootPanel.add(controllerPanel, width - controllerPanelWidth - PREVIEW_PANEL_BORDER_SIZE, height
                - controllerPanelHeight - PREVIEW_PANEL_BORDER_SIZE);

        // the button to show / hide the thumbnail
        ThumbnailButton thumbnailButton = new ThumbnailButton(controllerPanel);
        rootPanel.add(thumbnailButton, width - thumbnailButton.getWidth(), 
            height - thumbnailButton.getHeight());

        initWidget(rootPanel);

        // fix bug IE hiding everything when resizing
        rootPanel.getElement().getStyle().setProperty("position", "absolute");

        RootPanel.get("display").add(this);
        // fire event
        model.notifyListeners();
    }

}
