package org.nuxeo.ecm.platform.pictures.tiles.service.test;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilesImpl;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.ecm.platform.pictures.tiles.gimp.GimpExecutor;
import org.nuxeo.ecm.platform.pictures.tiles.gimp.tiler.GimpTiler;
import org.nuxeo.ecm.platform.pictures.tiles.magick.tiler.MagickTiler;
import org.nuxeo.ecm.platform.pictures.tiles.magick.utils.ImageIdentifier;
import org.nuxeo.ecm.platform.pictures.tiles.magick.utils.ImageInfo;
import org.nuxeo.ecm.platform.pictures.tiles.service.GCTask;
import org.nuxeo.ecm.platform.pictures.tiles.service.PictureTilingCacheGCManager;
import org.nuxeo.ecm.platform.pictures.tiles.service.PictureTilingComponent;
import org.nuxeo.ecm.platform.pictures.tiles.tilers.PictureTiler;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.pictures.tiles",
                "OSGI-INF/pictures-tiles-framework.xml");

        PictureTilingComponent.setDefaultTiler(new MagickTiler());
        PictureTilingComponent.endGC();
    }

    public void testLookup() {
        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);
    }

    public void testTilingSimple() throws ClientException {
        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);

        PictureTiles tiles = pts.getTilesFromBlob(image, 255, 255, 20);

        assertNotNull(tiles);
        assertFalse(tiles.getZoomfactor() == 0);
    }

    public void testAdapter() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.pictures.tiles",
                "OSGI-INF/pictures-tiles-adapter-contrib.xml");

    }

    public void testTilingSpead() throws ClientException {
        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);

        GimpExecutor.setUseQuickExec(false);
        image.setFilename("slow.jpg");
        PictureTiles tiles = pts.getTilesFromBlob(image, 255, 255, 20, 0, 0,
                false);
        assertNotNull(tiles);
        assertFalse(tiles.getZoomfactor() == 0);
        System.out.println("ExecTime="
                + tiles.getInfo().get("JavaProcessExecTime"));

        GimpExecutor.setUseQuickExec(true);
        image.setFilename("quick.jpg");
        PictureTiles tiles2 = pts.getTilesFromBlob(image, 255, 255, 20, 0, 0,
                false);
        assertNotNull(tiles2);
        assertFalse(tiles2.getZoomfactor() == 0);
        System.out.println("ExecTime="
                + tiles2.getInfo().get("JavaProcessExecTime"));

        GimpExecutor.setUseQuickExec(false);

    }

    public void testLazy() throws Exception {
        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);

        String wdirPath = System.getProperty("java.io.tmpdir") + "/testMe";
        new File(wdirPath).mkdir();
        pts.setWorkingDirPath(wdirPath);

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);

        image.setFilename("test.jpg");
        PictureTiles tiles = pts.getTilesFromBlob(image, 255, 255, 20, 0, 0,
                false);

        assertNotNull(tiles);

        Blob tile00 = tiles.getTile(0, 0);
        assertNotNull(tile00);

        Blob tile01 = tiles.getTile(0, 1);
        assertNotNull(tile01);

        Blob tile02 = tiles.getTile(0, 2);
        assertNotNull(tile02);

    }

    public void testTilingWithShrink() throws ClientException {
        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);

        PictureTiles tiles = pts.getTilesFromBlob(image, 255, 255, 3);

        assertNotNull(tiles);
        assertFalse(tiles.getZoomfactor() == 0);
    }

    public void testTilingSimpleMagick() throws ClientException {

        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);

        PictureTilingComponent.setDefaultTiler(new MagickTiler());

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);

        PictureTiles tiles = pts.getTilesFromBlob(image, 255, 255, 5);

        assertNotNull(tiles);
        assertFalse(tiles.getZoomfactor() == 0);
    }

    /*public void testTilingBench() throws Exception {

        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);

        benchTiler(pts, new GimpTiler());
        benchTiler(pts, new MagickTiler());

    }*/

    public void testMagick() throws Exception {
        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);
        PictureTilingComponent.setDefaultTiler(new MagickTiler());
        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);
        PictureTiles tiles = pts.getTilesFromBlob(image, 200, 200, 2);
        assertNotNull(tiles);

        tiles.getTile(0, 1);

    }

    public void testMagick2() throws Exception {
        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);
        PictureTilingComponent.setDefaultTiler(new MagickTiler());
        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);
        PictureTiles tiles = pts.getTilesFromBlob(image, 200, 160, 2);
        assertNotNull(tiles);

        tiles.getTile(0, 1);

    }

    /*
     * public void testBig() throws Exception { PictureTilingService pts =
     * Framework.getLocalService(PictureTilingService.class);
     * assertNotNull(pts); PictureTilingComponent.setDefaultTiler(new
     * MagickTiler()); File file = new File("/home/tiry/photos/orion.jpg"); Blob
     * image = new FileBlob(file);
     *
     * // 100% long t0 = System.currentTimeMillis(); PictureTiles tiles =
     * pts.getTilesFromBlob(image, 100, 100, 180); assertNotNull(tiles); long t1
     * = System.currentTimeMillis();
     *
     * System.out.println("Big picture " +
     * tiles.getOriginalImageInfo().getWidth() + "x" +
     * tiles.getOriginalImageInfo().getHeight() + " at zoom " +
     * tiles.getZoomfactor() + " generated in " + (t1-t0) + "ms"); }
     */

    protected void benchTiler(PictureTilingService pts, PictureTiler tiler)
            throws Exception {

        PictureTilingComponent.setDefaultTiler(tiler);
        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);
        long t0 = System.currentTimeMillis();
        int nb = 0;

        for (int maxTiles = 2; maxTiles < 5; maxTiles = maxTiles + 2) {

            long tt0 = System.currentTimeMillis();

            PictureTiles tiles = pts.getTilesFromBlob(image, 200, 200, maxTiles);
            assertNotNull(tiles);

            int nxt = tiles.getXTiles();
            int nyt = tiles.getYTiles();
            int nbt = 0;
            for (int i = 0; i < nxt; i++) {
                for (int j = 0; j < nyt; j++) {
                    nbt++;
                    long t1 = System.currentTimeMillis();
                    Blob tile = tiles.getTile(i, j);
                    long t2 = System.currentTimeMillis();

                    System.out.println("maxTile=" + maxTiles + " " + i + "-"
                            + j + " :" + (t2 - t1));
                }
            }
            nb = nb + nbt;
            long tt1 = System.currentTimeMillis();
            System.out.println("maxTile=" + maxTiles
                    + " total generation time :" + (tt1 - tt0));
            System.out.println("speed " + (nbt + 0.0) / ((tt1 - tt0) / 1000));
        }

        long t3 = System.currentTimeMillis();

        System.out.println("complete run for tiler : " + tiler.getName() + " :"
                + (t3 - t0));
        System.out.println("speed " + (nb + 0.0) / ((t3 - t0) / 1000));
    }

    public void testGC() throws Exception {
        int gcRuns = PictureTilingCacheGCManager.getGCRuns();

        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);
        benchTiler(pts, new MagickTiler());
        testMagick2();
        testMagick2();
        long cacheSize = PictureTilingCacheGCManager.getCacheSizeInKBs();
        System.out.println("CacheSize = " + cacheSize + "KB");
        assertTrue(cacheSize > 0);

        int reduceSize = 500;
        System.out.println("performing GC with " + reduceSize
                + " KB target reduction");
        PictureTilingCacheGCManager.doGC(reduceSize);

        int gcRuns2 = PictureTilingCacheGCManager.getGCRuns();

        assertEquals(1, gcRuns2 - gcRuns);

        long newCacheSize = PictureTilingCacheGCManager.getCacheSizeInKBs();
        System.out.println("new cacheSize = " + newCacheSize + "KB");
        System.out.println("effective delta = " + (cacheSize - newCacheSize)
                + "KB");
        assertTrue(cacheSize - newCacheSize > reduceSize);

    }

    public void testGC2() throws Exception {
        int reduceSize = 500;
        int gcRuns = PictureTilingCacheGCManager.getGCRuns();
        int gcCalls = PictureTilingCacheGCManager.getGCCalls();
        PictureTilingComponent.endGC();

        String maxStr = PictureTilingComponent.getEnvValue(
                PictureTilingCacheGCManager.MAX_DISK_SPACE_USAGE_KEY,
                Long.toString(PictureTilingCacheGCManager.MAX_DISK_SPACE_USAGE_KB));
        PictureTilingComponent.setEnvValue(
                PictureTilingCacheGCManager.MAX_DISK_SPACE_USAGE_KEY,
                Integer.toString(reduceSize));

        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);
        benchTiler(pts, new MagickTiler());

        System.out.println("Tiling run 1");
        testMagick2();
        System.out.println("Tiling run 2");
        testMagick2();
        System.out.println("Tiling run 3");
        testMagick2();

        long cacheSize = PictureTilingCacheGCManager.getCacheSizeInKBs();
        System.out.println("CacheSize = " + cacheSize + "KB");
        assertTrue(cacheSize > 0);

        GCTask.setGCIntervalInMinutes(-100);
        PictureTilingComponent.startGC();

        System.out.println("waiting for GC to run");
        Thread.currentThread().sleep(600);

        int gcRuns2 = PictureTilingCacheGCManager.getGCRuns();
        int gcCalls2 = PictureTilingCacheGCManager.getGCCalls();

        int runs = gcRuns2 - gcRuns;
        int calls = gcCalls2 - gcCalls;
        System.out.println("GC runs = " + runs);
        System.out.println("GC calls = " + calls);

        assertTrue(runs > 0);
        assertTrue(calls > 2);

        PictureTilingComponent.endGC();

        long newCacheSize = PictureTilingCacheGCManager.getCacheSizeInKBs();
        System.out.println("new cacheSize = " + newCacheSize + "KB");
        System.out.println("effective delta = " + (cacheSize - newCacheSize)
                + "KB");
        assertTrue(cacheSize - newCacheSize > reduceSize);

        PictureTilingComponent.setEnvValue(
                PictureTilingCacheGCManager.MAX_DISK_SPACE_USAGE_KEY, maxStr);

    }

    public void testParametersContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.pictures.tiles",
                "OSGI-INF/pictures-tiles-contrib.xml");

        String cacheSize = PictureTilingComponent.getEnvValue(
                PictureTilingCacheGCManager.MAX_DISK_SPACE_USAGE_KEY, "ERROR");

        assertEquals("50000", cacheSize);
    }

    public void testBorderTiles() throws Exception {
        PictureTilingService pts = Framework.getLocalService(PictureTilingService.class);
        assertNotNull(pts);
        PictureTilingComponent.setDefaultTiler(new MagickTiler());
        File file = FileUtils.getResourceFileFromContext("chutes.jpg");
        Blob image = new FileBlob(file);
        PictureTiles tiles = pts.getTilesFromBlob(image, 64, 64, 3);
        assertNotNull(tiles);

        tiles.getTile(2, 1);
        String tilePath = ((PictureTilesImpl) tiles).getTileFilePath(2, 1);
        ImageInfo info = ImageIdentifier.getInfo(tilePath);
        assertEquals(63, info.getWidth());
        assertEquals(64, info.getHeight());

        tiles.getTile(2, 2);
        tilePath = ((PictureTilesImpl) tiles).getTileFilePath(2, 2);
        info = ImageIdentifier.getInfo(tilePath);
        assertEquals(63, info.getWidth());
        assertEquals(15, info.getHeight());
    }
}
