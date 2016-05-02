package org.helioviewer.jhv.plugins.eveplugin.radio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image.ReaderMode;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ImageCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;

class DownloadedJPXData implements ImageDataHandler {

    private JP2ViewCallisto view;
    private boolean hasData = false;

    private final long startDate;
    private final long endDate;
    private double startFreq;
    private double endFreq;
    private int jp2Width;
    private int jp2Height;

    private BufferedImage bufferedImage;
    private Region region;
    private boolean downloadJPXFailed = false;

    public DownloadedJPXData(long _startDate, long _endDate) {
        startDate = _startDate;
        endDate = _endDate;
    }

    public void init(JP2ViewCallisto _view) {
        view = _view;
        view.setDataHandler(this);

        JP2ImageCallisto image = view.getJP2Image();
        image.setReaderMode(ReaderMode.ONLYFIREONCOMPLETE);
        ResolutionSet resolutionSet = image.getResolutionSet(0);
        jp2Width = resolutionSet.getResolutionLevel(0).width;
        jp2Height = resolutionSet.getResolutionLevel(0).height;
        XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer();
        try {
            hvMetaData.parseXML(image.getXML(0));
            endFreq = hvMetaData.tryGetDouble("STARTFRQ");
            startFreq = hvMetaData.tryGetDouble("END-FREQ");

            long start = JHVDate.parseDateTime(hvMetaData.get("DATE-OBS")).milli;
            long end = JHVDate.parseDateTime(hvMetaData.get("DATE-END")).milli;
            if (startDate != start || endDate != end)
                Log.warn("something is wrong with the jpx dates " + start + " " + end + " " + startDate + " " + endDate);

            hvMetaData.destroyXML();
        } catch (Exception e) {
            Log.error("Some of the metadata could not be read, aborting...");
            return;
        }
        requestData();
    }

    void remove() {
        if (view != null) {
            view.setDataHandler(null);
            view.abolish();
            view = null;
        }
    }

    @Override
    public void handleData(ImageData imageData) {
        if (imageData instanceof SingleChannelByte8ImageData) {
            int w = imageData.getWidth();
            int h = imageData.getHeight();
            if (w < 1 || h < 1) {
                Log.error("width: " + w + " height: " + h);
                return;
            }

            region = imageData.getRegion();
            byte[] data = (byte[]) imageData.getBuffer().array();
            bufferedImage = createBufferedImage(w, h, data);

            if (!hasData) {
                EVEPlugin.ldsm.downloadFinished(EVEPlugin.rdm);
            }
            hasData = true;
            EVEPlugin.dc.fireRedrawRequest();
        }
    }

    private BufferedImage createBufferedImage(int width, int height, byte[] data) {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, EVEPlugin.rdm.getColorModel());
        DataBufferByte dataBuffer = new DataBufferByte(data, width * height);
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

    void requestData() {
        if (view != null) {
            JP2ImageCallisto image = view.getJP2Image();
            Rectangle roi = getROI(EVEPlugin.dc.selectedAxis, EVEPlugin.rdm.getYAxis());

            if (roi.width > 0 && roi.height > 0) {
                image.setRegion(roi);
                view.render(null, null, defineFactor(roi));
            }
        }
    }

    private double defineFactor(Rectangle roi) {
        double visibleImagePercentage = roi.getWidth() * roi.getHeight() / (jp2Width * jp2Height);
        if (visibleImagePercentage <= 0.03125) {
            return 1;
        } else if (visibleImagePercentage <= 0.0625) {
            return 0.5;
        } else if (visibleImagePercentage <= 0.125) {
            return 0.25;
        } else if (visibleImagePercentage <= 0.25) {
            return 0.125;
        } else if (visibleImagePercentage <= 0.5) {
            return 0.0625;
        } else {
            return 0.03125;
        }
    }

    private boolean first = true;

    private Rectangle getROI(TimeAxis xAxis, YAxis yAxis) {
        long imageTimesize = endDate - startDate;
        int imageFrequencySize = (int) (endFreq - startFreq);
        double timePerPix = 1.0 * imageTimesize / jp2Width;
        double freqPerPix = 1.0 * imageFrequencySize / jp2Height;

        long visibleStart = startDate;
        long visibleEnd = endDate;

        if (!first) {
            if (visibleStart <= xAxis.start)
                visibleStart = xAxis.start;

            if (visibleEnd >= xAxis.end)
                visibleEnd = xAxis.end;
        }
        first = false;

        double visibleStartFreq = startFreq;
        double visibleEndFreq = endFreq;
        if (visibleStartFreq < yAxis.start) {
            visibleStartFreq = yAxis.start;
        }
        if (visibleEndFreq > yAxis.end) {
            visibleEndFreq = yAxis.end;
        }

        int x0 = (int) Math.round((visibleStart - startDate) / timePerPix);
        int y0 = (int) Math.round((endFreq - visibleEndFreq) / freqPerPix);
        int width = (int) Math.round((visibleEnd - visibleStart) / timePerPix);
        int height = (int) Math.round((visibleEndFreq - visibleStartFreq) / freqPerPix);
        return new Rectangle(x0, y0, width, height);
    }

    void draw(Graphics2D g, Rectangle ga, TimeAxis xAxis, YAxis yAxis) {
        if (hasData) {
            int sx0 = 0;
            int sx1 = bufferedImage.getWidth();
            int sy0 = 0;
            int sy1 = bufferedImage.getHeight();
            long imStart = (long) (startDate + (endDate - startDate) * region.llx / jp2Width);
            long imEnd = (long) (startDate + (endDate - startDate) * region.urx / jp2Width);

            double freqimStart = (startFreq + (endFreq - startFreq) * region.lly / jp2Height);
            double freqimEnd = (startFreq + (endFreq - startFreq) * region.ury / jp2Height);

            int dx0 = xAxis.value2pixel(ga.x, ga.width, imStart);
            int dx1 = xAxis.value2pixel(ga.x, ga.width, imEnd);

            int dy0 = yAxis.value2pixel(ga.y, ga.height, freqimStart);
            int dy1 = yAxis.value2pixel(ga.y, ga.height, freqimEnd);

            g.drawImage(bufferedImage, dx0, dy0, dx1, dy1, sx0, sy0, sx1, sy1, null);
        }
        else {
            drawNoData(g, ga, xAxis);
        }
    }

    private void drawNoData(Graphics2D g, Rectangle ga, TimeAxis xAxis) {
        int dx0 = xAxis.value2pixel(ga.x, ga.width, Math.max(startDate, xAxis.start));
        int dx1 = xAxis.value2pixel(ga.x, ga.width, Math.min(endDate, xAxis.end));
        int dwidth = dx1 - dx0;
        g.setColor(Color.GRAY);
        g.fillRect(dx0, ga.y, dwidth, ga.height);
        g.setColor(Color.WHITE);

        String text = "Fetching data";
        if (downloadJPXFailed) {
            text = "No data available";
        }

        Rectangle2D r = g.getFontMetrics().getStringBounds(text, g);
        int tWidth = (int) r.getWidth();
        int tHeight = (int) r.getHeight();
        int x = dx0 + dwidth / 2 - tWidth / 2;
        int y = ga.y + ga.height / 2 - tHeight / 2;
        g.drawString(text, x, y);
    }

    void downloadJPXFailed() {
        downloadJPXFailed = true;
    }

    boolean isDownloading() {
        return !hasData && !downloadJPXFailed;
    }

    void changeColormap(ColorModel cm) {
        if (hasData) {
            BufferedImage old = bufferedImage;
            bufferedImage = new BufferedImage(cm, old.getRaster(), false, null);
        }
    }

}