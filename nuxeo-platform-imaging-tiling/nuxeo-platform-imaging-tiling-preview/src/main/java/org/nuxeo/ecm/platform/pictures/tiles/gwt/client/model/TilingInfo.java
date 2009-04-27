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

package org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class TilingInfo {

    private final String repoId;

    private final String docId;

    private boolean initialized;

    private int originalImageWidth;

    private int originalImageHeight;

    private double zoom;

    private int tileWidth;

    private int tileHeight;

    private int nbXTiles;

    private int nbYTiles;

    private int maxTiles;

    private long lastModificationDate;

    public TilingInfo(String repoId, String docId) {
        this.repoId = repoId;
        this.docId = docId;
    }

    public TilingInfo(String repoId, String docId, int tileWidth,
            int tileHeight, int maxTiles) {
        this(repoId, docId);
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.maxTiles = maxTiles;
    }

    public TilingInfo(TilingInfo source) {
        docId = source.docId;
        repoId = source.repoId;
        originalImageWidth = source.originalImageWidth;
        originalImageHeight = source.originalImageHeight;
        zoom = source.zoom;
        tileWidth = source.tileWidth;
        tileHeight = source.tileHeight;
        nbXTiles = source.nbXTiles;
        nbYTiles = source.nbYTiles;
        maxTiles = source.maxTiles;
        initialized = source.initialized;
        lastModificationDate = source.lastModificationDate;
    }

    /**
     * @return the repoId.
     */
    public String getRepoId() {
        return repoId;
    }

    /**
     * @return the docId.
     */
    public String getDocId() {
        return docId;
    }

    /**
     * @return the initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @return the originalImageWidth.
     */
    public int getOriginalImageWidth() {
        return originalImageWidth;
    }

    /**
     * @return the originalImageHeight.
     */
    public int getOriginalImageHeight() {
        return originalImageHeight;
    }

    /**
     * @return the zoom.
     */
    public double getZoom() {
        return zoom;
    }

    /**
     * @return the tileWidth.
     */
    public int getTileWidth() {
        return tileWidth;
    }

    /**
     * @return the tileHeight.
     */
    public int getTileHeight() {
        return tileHeight;
    }

    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }

    public void setTileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
    }

    /**
     * @return the nbXTiles.
     */
    public int getNbXTiles() {
        return nbXTiles;
    }

    /**
     * @return the nbYTiles.
     */
    public int getNbYTiles() {
        return nbYTiles;
    }

    /**
     * @return the maxTiles.
     */
    public int getMaxTiles() {
        return maxTiles;
    }

    /**
     * Set the maxTiles.
     */
    public void setMaxTiles(int maxTiles) {
        this.maxTiles = maxTiles;
    }

    public long getLastModificationDate() {
        return lastModificationDate;
    }

    public void updateTilingInfo() {
        updateTilingInfo(null);
    }

    public void updateTilingInfo(final TilingInfoCallback callback) {
        RequestBuilder getRequest = new RequestBuilder(RequestBuilder.GET,
                getBaseUrl() + "?format=json");
        try {
            getRequest.sendRequest(null, new RequestCallback() {

                public void onError(Request arg0, Throwable arg1) {
                    Log.error("Error sending tiling info request: " + arg1);
                }

                public void onResponseReceived(Request arg0, Response resp) {
                    parseResponse(resp.getText());
                    if (callback != null) {
                        callback.tilingInfoUpdated();
                    }
                }
            });
        } catch (RequestException e) {
            Window.alert("Error getting the tiling server: " + e);
        }
    }

    public void parseResponse(String response) {
        if ("".equals(response)) {
            return;
        }
        JSONObject jsonValue = (JSONObject) JSONParser.parse(response);
        JSONObject tileInfo = (JSONObject) jsonValue.get("tileInfo");
        JSONObject originalImage = (JSONObject) jsonValue.get("originalImage");
        JSONNumber zoomFactor = (JSONNumber) tileInfo.get("zoom");
        JSONNumber widthJS = (JSONNumber) originalImage.get("width");
        JSONNumber heightJS = (JSONNumber) originalImage.get("height");
        JSONNumber xTilesJS = (JSONNumber) tileInfo.get("xtiles");
        JSONNumber yTilesJS = (JSONNumber) tileInfo.get("ytiles");
        JSONNumber maxTilesJS = (JSONNumber) tileInfo.get("maxtiles");
        JSONNumber tileWidthJS = (JSONNumber) tileInfo.get("tileWidth");
        JSONNumber tileHeightJS = (JSONNumber) tileInfo.get("tileHeight");

        zoom = zoomFactor.doubleValue();
        originalImageWidth = new Double(widthJS.doubleValue()).intValue();
        originalImageHeight = new Double(heightJS.doubleValue()).intValue();
        nbXTiles = new Double(xTilesJS.doubleValue()).intValue();
        nbYTiles = new Double(yTilesJS.doubleValue()).intValue();
        maxTiles = new Double(maxTilesJS.doubleValue()).intValue();
        tileWidth = new Double(tileWidthJS.doubleValue()).intValue();
        tileHeight = new Double(tileHeightJS.doubleValue()).intValue();

        JSONObject additionalInfo = (JSONObject) jsonValue.get("additionalInfo");
        JSONString lastModificationDateJS = (JSONString) additionalInfo.get("lastModificationDate");
        lastModificationDate = Long.parseLong(lastModificationDateJS.stringValue());

        initialized = true;
    }

    public String getBaseUrl() {
        return "/nuxeo/restAPI/getTiles/" + this.getRepoId() + "/"
                + this.getDocId() + "/" + this.getTileWidth() + "/"
                + this.getTileHeight() + "/" + this.getMaxTiles();
    }

}
