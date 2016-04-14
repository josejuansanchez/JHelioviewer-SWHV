package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import java.awt.Rectangle;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.GraphDimensionListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpace;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.ValueSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.radio.data.RadioDataManager;

public class ZoomManager implements TimingListener, GraphDimensionListener, PlotAreaSpaceListener, ValueSpaceListener {

    private static ZoomManager instance;
    private DrawController drawController;
    private PlotAreaSpace plotAreaSpace;
    private YAxisElement yAxisElement;
    private RadioDataManager radioDataManager;

    // private ZoomDataConfig zoomDataConfig;
    private boolean isAreaInitialized;
    private Rectangle displaySize;

    private long minX;
    private long maxX;
    private boolean isMinXInitialized;
    private boolean isMaxXInitialized;

    private ZoomManager() {
        isAreaInitialized = false;
        isMinXInitialized = false;
        isMaxXInitialized = false;
        displaySize = new Rectangle();
    }

    public static ZoomManager getSingletonInstance() {
        if (instance == null) {
            instance = new ZoomManager();
            instance.init();
        }
        return instance;
    }

    private void init() {
        drawController = DrawController.getSingletonInstance();
        drawController.addTimingListener(this);
        drawController.addGraphDimensionListener(this);
        plotAreaSpace = PlotAreaSpace.getSingletonInstance();
        radioDataManager = RadioDataManager.getSingletonInstance();
        yAxisElement = radioDataManager.getYAxisElement();
    }

    public void addZoomDataConfig(Interval interval) {
        plotAreaSpace.addPlotAreaSpaceListener(this);
        radioDataManager.getYAxisElement().addValueSpaceListener(this);
        Interval currentInterval = drawController.getSelectedInterval();
        if (currentInterval == null) {
            currentInterval = interval;
        }
        if (interval != null) {
            minX = currentInterval.start;
            maxX = currentInterval.end;
            requestData();
        }
    }

    public DrawableAreaMap getDrawableAreaMap(long startDate, long endDate, int visualStartFrequency, int visualEndFrequency, int imageStartFrequency, int imageEndFrequency, Rectangle area) {
        int sourceX0 = defineXInSourceArea(startDate, startDate, endDate, area);
        int sourceY0 = 0;
        int sourceX1 = defineXInSourceArea(endDate, startDate, endDate, area);
        int sourceY1 = area.height;
        if (sourceY0 == sourceY1) {
            sourceY1 = sourceY0 + 1;
        }
        if (sourceX0 == sourceX1) {
            sourceX1 = sourceX0 + 1;
        }

        int destX0 = defineXInDestinationArea(startDate);
        int destY0 = defineYInDestinationArea(visualStartFrequency, yAxisElement);
        int destX1 = defineXInDestinationArea(endDate);
        int destY1 = defineYInDestinationArea(visualEndFrequency, yAxisElement);
        return new DrawableAreaMap(sourceX0, sourceY0, sourceX1, sourceY1, destX0, destY0, destX1, destY1);
    }

    /**
     * Creates a drawable area map based on start and end date. The source will
     * have the coordinates (0,0,0,0) and are meaningless, the destination
     * coordinates are corresponding with the time interval and the taking the
     * complete height of the plot area.
     *
     * @param startDate
     *            The start date of the interval
     * @param endDate
     *            The end date of the interval
     * @param downloadID
     *            The download id of the request
     * @param plotIdentifier
     *            The plot identifier of the request
     * @return Drawable area map with the correct coordinates
     */
    public DrawableAreaMap getDrawableAreaMap(long startDate, long endDate) {
        int destX0 = defineXInDestinationArea(startDate);
        int destY0 = 0;
        int destX1 = defineXInDestinationArea(endDate);
        int destY1 = displaySize.height;
        return new DrawableAreaMap(0, 0, 0, 0, destX0, destY0, destX1, destY1);
    }

    /**
     * Calculates the available space in the screen size for the requested time
     * interval and frequency interval. The frequency gets the complete height,
     * the time gets the portion of the width of the screen corresponding with
     * the portion of the complete time interval it takes.
     *
     *
     * @param startDate
     *            The start date of the requested time interval
     * @param endDate
     *            The end date of the requested time interval
     * @param startFreq
     *            The start frequency of the requested frequency interval
     * @param endFreq
     *            The end frequency of the requested frequency interval
     * @param downloadId
     *            The download id that requests the space
     * @return A rectangle with the dimensions of the available space for the
     *         requested intervals
     * @throws IllegalArgumentException
     *             If the given start date or end date fall outside the current
     *             interval or the given start frequency or end frequency fall
     *             outside the minimum and maximum frequency.
     */
    public Rectangle getAvailableSpaceForInterval(long startDate, long endDate, int startFreq, int endFreq) {
        Interval currentInterval = drawController.getSelectedInterval();
        double min = yAxisElement.getAvailableRange().min;
        double max = yAxisElement.getAvailableRange().max;

        if (startFreq >= min && startFreq <= max && endFreq >= min && endFreq <= max &&
                currentInterval.containsPointInclusive(startDate) && currentInterval.containsPointInclusive(endDate)) {
            int height = displaySize.height;
            double ratio = displaySize.getWidth() / (currentInterval.end - currentInterval.start);
            int width = (int) Math.round((endDate - startDate) * ratio);
            return new Rectangle(width, height);
        } else {
            return new Rectangle(0, 0);
        }
    }

    private int defineYInDestinationArea(int frequencyToFind, YAxisElement yAxisElement) {
        return displaySize.height - (int) Math.floor((frequencyToFind - yAxisElement.getSelectedRange().min) / (1.0 * (yAxisElement.getSelectedRange().max - yAxisElement.getSelectedRange().min) / displaySize.height));
    }

    private int defineXInDestinationArea(long dateToFind) {
        return displaySize.x + (int) Math.floor((dateToFind - minX) / (1.0 * (maxX - minX) / displaySize.width));
    }

    private int defineXInSourceArea(long dateToFind, long startDateArea, long endDateArea, Rectangle area) {
        long timediff = dateToFind - startDateArea;
        long timeOfArea = endDateArea - startDateArea;
        return (int) Math.floor(timediff / (1.0 * (timeOfArea) / area.width));
    }

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged(boolean keepFullValueRange) {
        Interval newInterval = drawController.getSelectedInterval();
        minX = newInterval.start;
        maxX = newInterval.end;
        isMinXInitialized = true;
        isMaxXInitialized = true;
        requestData();
    }

    /**
     * Remove the zoom manager data from the zoom manager.
     *
     * @param downloadID
     *            The download identifier to remove from the zoom manager
     * @param plotIdentifier
     *            The plot identifier for which the download identifier should
     *            be removed
     */
    public void removeZoomManagerDataConfig() {
        PlotAreaSpace.getSingletonInstance().removePlotAreaSpaceListener(this);
    }

    @Override
    public void graphDimensionChanged() {
        Rectangle newDisplaySize = drawController.getPlotArea();
        if (!displaySize.equals(newDisplaySize)) {
            displaySize = newDisplaySize;
            isAreaInitialized = true;
            requestData();
        }
    }

    @Override
    public void valueSpaceChanged(Range availableRange, Range selectedRange) {
        requestData();
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinTime, double scaledMaxTime, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        requestData();

    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinTime, double oldMaxTime, double newMinTime, double newMaxTime) {
    }

    private void requestData() {
        if (isAreaInitialized && isMinXInitialized && isMaxXInitialized) {
            double xRatio = 1.0 * (maxX - minX) / displaySize.getWidth();
            double yRatio = 1.0 * (yAxisElement.getSelectedRange().max - yAxisElement.getSelectedRange().min) / displaySize.getHeight();
            radioDataManager.requestData(minX, maxX, yAxisElement.getSelectedRange().min, yAxisElement.getSelectedRange().max, xRatio, yRatio);
        }
    }

}
