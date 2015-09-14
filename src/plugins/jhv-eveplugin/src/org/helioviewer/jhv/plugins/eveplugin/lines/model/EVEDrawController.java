package org.helioviewer.jhv.plugins.eveplugin.lines.model;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.plugins.eveplugin.base.Range;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpace;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.BandControllerListener;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVECacheController;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVECacheControllerListener;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVEValues;
import org.helioviewer.jhv.plugins.eveplugin.lines.gui.EVEDrawableElement;

/**
 * @author Stephan Pagel
 * */
public class EVEDrawController implements BandControllerListener, TimingListener, EVECacheControllerListener, PlotAreaSpaceListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private final Map<YAxisElement, Map<Band, EVEValues>> dataMapPerUnitLabel = new HashMap<YAxisElement, Map<Band, EVEValues>>();
    private final Map<YAxisElement, Range> selectedRangeMap = new HashMap<YAxisElement, Range>();
    private final Map<YAxisElement, Range> availableRangeMap = new HashMap<YAxisElement, Range>();
    private final DrawController drawController;

    private final Map<YAxisElement, EVEDrawableElement> eveDrawableElementMap;
    private final Map<Band, YAxisElement> yAxisElementMap;
    private final Map<YAxisElement, List<Band>> bandsPerYAxis;
    private final PlotAreaSpace plotAreaSpace;
    private static EVEDrawController instance;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    private EVEDrawController() {

        BandController.getSingletonInstance().addBandControllerListener(this);
        DrawController.getSingletonInstance().addTimingListener(this);
        EVECacheController.getSingletonInstance().addControllerListener(this);

        drawController = DrawController.getSingletonInstance();
        eveDrawableElementMap = new HashMap<YAxisElement, EVEDrawableElement>();
        yAxisElementMap = new HashMap<Band, YAxisElement>();
        bandsPerYAxis = new HashMap<YAxisElement, List<Band>>();
        plotAreaSpace = PlotAreaSpace.getSingletonInstance();
        plotAreaSpace.addPlotAreaSpaceListener(this);
    }

    public static EVEDrawController getSingletonInstance() {
        if (instance == null) {
            instance = new EVEDrawController();
        }
        return instance;
    }

    private void addToMap(final Band band) {
        Interval<Date> interval = drawController.getSelectedInterval();
        Rectangle plotArea = drawController.getPlotArea();
        YAxisElement yAxisElement = drawController.getYAxisElementForUnit(band.getUnitLabel());
        if (yAxisElement == null && drawController.hasAxisAvailable()) {
            yAxisElement = new YAxisElement();
        }
        if (yAxisElement != null) {
            yAxisElementMap.put(band, yAxisElement);
            addToBandsPerYAxis(yAxisElement, band);
            EVEValues data = retrieveData(band, interval, plotArea);
            if (!dataMapPerUnitLabel.containsKey(yAxisElement)) {
                dataMapPerUnitLabel.put(yAxisElement, new HashMap<Band, EVEValues>());
            }
            if (data != null) {
                dataMapPerUnitLabel.get(yAxisElement).put(band, data);
            }
        } else {
            Log.debug("band could not be added. No Yaxis Available ");
        }
        fireRedrawRequest(true);
    }

    private void addToBandsPerYAxis(YAxisElement yAxisElement, Band band) {
        List<Band> bands = new ArrayList<Band>();
        if (bandsPerYAxis.containsKey(yAxisElement)) {
            bands = bandsPerYAxis.get(yAxisElement);
        }
        bands.add(band);
        bandsPerYAxis.put(yAxisElement, bands);
    }

    private void removeFromMap(final Band band) {
        YAxisElement yAxisElement = yAxisElementMap.get(band);
        if (dataMapPerUnitLabel.containsKey(yAxisElement)) {
            if (dataMapPerUnitLabel.get(yAxisElement).containsKey(band)) {
                dataMapPerUnitLabel.get(yAxisElement).remove(band);
                List<Band> bands = bandsPerYAxis.get(yAxisElement);
                bands.remove(band);
                if (bands.isEmpty()) {
                    EVEDrawableElement removed = eveDrawableElementMap.remove(yAxisElement);
                    availableRangeMap.remove(yAxisElement);
                    selectedRangeMap.remove(yAxisElement);
                    yAxisElementMap.remove(band);
                    bandsPerYAxis.remove(yAxisElement);
                    drawController.removeDrawableElement(removed);
                }
                resetAvailableRange();
                fireRedrawRequest(true);
            }
        }
    }

    private void updateBand(final Band band, boolean keepFullValueRange) {
        Interval<Date> interval = drawController.getSelectedInterval();
        Rectangle plotArea = drawController.getPlotArea();
        EVEValues data = retrieveData(band, interval, plotArea);
        boolean isLog = band.getBandType().isLogScale();
        YAxisElement yAxisElement = yAxisElementMap.get(band);
        if (!availableRangeMap.containsKey(yAxisElement)) {
            availableRangeMap.put(yAxisElement, new Range());
            selectedRangeMap.put(yAxisElement, new Range());
            dataMapPerUnitLabel.put(yAxisElement, new HashMap<Band, EVEValues>());
        }
        Range oldAvailableRange = new Range(availableRangeMap.get(yAxisElement));
        for (EVEValues v : dataMapPerUnitLabel.get(yAxisElement).values()) {
            if (v != null) {
                availableRangeMap.get(yAxisElement).setMin(v.getMinimumValue());
                availableRangeMap.get(yAxisElement).setMax(v.getMaximumValue());
            }
        }
        availableRangeMap.get(yAxisElement).setMin(data.getMinimumValue());
        availableRangeMap.get(yAxisElement).setMax(data.getMaximumValue());
        double avMin = availableRangeMap.get(yAxisElement).min;
        double avMax = availableRangeMap.get(yAxisElement).max;
        if (avMin == avMax) {
            if (avMin == 0) {
                availableRangeMap.get(yAxisElement).setMin(-1.0);
                availableRangeMap.get(yAxisElement).setMax(1.0);
            } else {
                availableRangeMap.get(yAxisElement).setMin(avMin - avMin / 10);
                availableRangeMap.get(yAxisElement).setMax(avMax + avMax / 10);
            }
        }
        if (oldAvailableRange.min != availableRangeMap.get(yAxisElement).min || oldAvailableRange.max != availableRangeMap.get(yAxisElement).max) {
            // Log.trace("update band available range changed so we change the plotareaSpace");
            checkSelectedRange(availableRangeMap.get(yAxisElement), selectedRangeMap.get(yAxisElement));
            updatePlotAreaSpace(availableRangeMap.get(yAxisElement), selectedRangeMap.get(yAxisElement), keepFullValueRange, isLog);
        } else {
            // Log.trace("Same available range");
        }
        dataMapPerUnitLabel.get(yAxisElement).put(band, data);
    }

    private void updateBands(boolean keepFullValueRange) {
        for (YAxisElement yAxisElement : dataMapPerUnitLabel.keySet()) {
            for (final Band band : dataMapPerUnitLabel.get(yAxisElement).keySet()) {
                updateBand(band, keepFullValueRange);
            }
        }
    }

    public void setSelectedRange(final Range newSelectedRange, YAxisElement yAxisElement) {
        selectedRangeMap.put(yAxisElement, new Range(newSelectedRange));
        drawController.setSelectedRange(newSelectedRange);
        fireRedrawRequest(false);
    }

    public void setSelectedRangeMaximal() {
        fireRedrawRequest(true);
    }

    private void fireRedrawRequest(final boolean maxRange) {
        Interval<Date> interval = drawController.getSelectedInterval();
        for (YAxisElement yAxisElement : dataMapPerUnitLabel.keySet()) {
            final Band[] bands = dataMapPerUnitLabel.get(yAxisElement).keySet().toArray(new Band[0]);
            final LinkedList<EVEValues> values = new LinkedList<EVEValues>();

            String unitLabel = "";
            boolean isLog = false;
            if (bands.length > 0) {
                unitLabel = bands[0].getUnitLabel();
                isLog = bands[0].getBandType().isLogScale();
            }

            if (!availableRangeMap.containsKey(yAxisElement)) {
                availableRangeMap.put(yAxisElement, new Range());
                selectedRangeMap.put(yAxisElement, new Range());
                eveDrawableElementMap.put(yAxisElement, new EVEDrawableElement());
            }

            Range oldAvailableRange = new Range(availableRangeMap.get(yAxisElement));

            for (EVEValues v : dataMapPerUnitLabel.get(yAxisElement).values()) {
                if (v != null) {
                    availableRangeMap.get(yAxisElement).setMin(v.getMinimumValue());
                    availableRangeMap.get(yAxisElement).setMax(v.getMaximumValue());
                    values.add(v);
                }
            }

            if (maxRange) {
                selectedRangeMap.put(yAxisElement, new Range());
            }
            checkSelectedRange(availableRangeMap.get(yAxisElement), selectedRangeMap.get(yAxisElement));
            if (oldAvailableRange.min != availableRangeMap.get(yAxisElement).min || oldAvailableRange.max != availableRangeMap.get(yAxisElement).max) {
                Log.error("Available range changed in redraw request. So update plotAreaSpace");
                Log.error("old range : " + oldAvailableRange.toString());
                Log.error("new available range : " + availableRangeMap.get(yAxisElement).toString());
                updatePlotAreaSpace(availableRangeMap.get(yAxisElement), selectedRangeMap.get(yAxisElement), false, isLog);

            }

            yAxisElement.set(selectedRangeMap.get(yAxisElement), availableRangeMap.get(yAxisElement), unitLabel, selectedRangeMap.get(yAxisElement).min, selectedRangeMap.get(yAxisElement).max, Color.PINK, isLog);
            eveDrawableElementMap.get(yAxisElement).set(interval, bands, yAxisElement);
            if (bands.length > 0) {
                drawController.updateDrawableElement(eveDrawableElementMap.get(yAxisElement));
            } else {
                drawController.removeDrawableElement(eveDrawableElementMap.get(yAxisElement));
            }

        }
    }

    private void updatePlotAreaSpace(Range availableRange, Range selectedRange, boolean keepFullValueSpace, boolean isLog) {
        if (!keepFullValueSpace) {
            if (isLog) {
                double diffAvailable = Math.log10(availableRange.max) - Math.log10(availableRange.min);
                double diffStart = Math.log10(selectedRange.min) - Math.log10(availableRange.min);
                double diffEnd = Math.log10(selectedRange.max) - Math.log10(availableRange.min);
                double startValue = plotAreaSpace.getScaledMinValue() + diffStart / diffAvailable;
                double endValue = plotAreaSpace.getScaledMinValue() + diffEnd / diffAvailable;
                plotAreaSpace.setScaledSelectedValue(startValue, endValue, true);
            } else {
                double diffAvailable = availableRange.max - availableRange.min;
                double diffStart = selectedRange.min - availableRange.min;
                double diffEnd = selectedRange.max - availableRange.min;
                double startValue = plotAreaSpace.getScaledMinValue() + diffStart / diffAvailable;
                double endValue = plotAreaSpace.getScaledMinValue() + diffEnd / diffAvailable;
                plotAreaSpace.setScaledSelectedValue(startValue, endValue, true);
            }
        } else {
            plotAreaSpace.setScaledSelectedValue(plotAreaSpace.getScaledMinValue(), plotAreaSpace.getScaledMaxValue(), true);
        }
    }

    private void checkSelectedRange(final Range availableRange, final Range selectedRange) {
        if (selectedRange.min > availableRange.max || selectedRange.max < availableRange.min) {
            selectedRange.min = availableRange.min;
            selectedRange.max = availableRange.max;

            return;
        }

        if (selectedRange.min < availableRange.min) {
            selectedRange.min = availableRange.min;
        }

        if (selectedRange.max > availableRange.max) {
            selectedRange.max = availableRange.max;
        }
    }

    private final EVEValues retrieveData(final Band band, final Interval<Date> interval, Rectangle plotArea) {
        return EVECacheController.getSingletonInstance().downloadData(band, interval, plotArea);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Zoom Controller Listener
    // //////////////////////////////////////////////////////////////////////////////
    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged() {

        updateBands(drawController.keepfullValueRange());
        fireRedrawRequest(false);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void bandAdded(final Band band) {
        addToMap(band);
    }

    @Override
    public void bandRemoved(final Band band) {
        removeFromMap(band);
    }

    @Override
    public void bandUpdated(final Band band) {
        if (band.isVisible()) {
            addToMap(band);
        } else {
            removeFromMap(band);
        }
    }

    @Override
    public void bandGroupChanged() {
        Interval<Date> interval = drawController.getSelectedInterval();
        Rectangle plotArea = drawController.getPlotArea();
        dataMapPerUnitLabel.clear();

        final Band[] activeBands = BandController.getSingletonInstance().getBands();

        for (final Band band : activeBands) {
            YAxisElement yAxisElement = yAxisElementMap.get(band);
            if (!dataMapPerUnitLabel.containsKey(band.getUnitLabel())) {
                dataMapPerUnitLabel.put(yAxisElement, new HashMap<Band, EVEValues>());
            }
            dataMapPerUnitLabel.get(yAxisElement).put(band, retrieveData(band, interval, plotArea));
        }

        fireRedrawRequest(true);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // EVE Cache Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void dataAdded(final Band band) {
        if (yAxisElementMap.containsKey(band)) {
            if (dataMapPerUnitLabel.get(yAxisElementMap.get(band)).containsKey(band)) {
                updateBand(band, false);
                fireRedrawRequest(true);
            }
        }
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        for (YAxisElement yAxisElement : bandsPerYAxis.keySet()) {
            double diffScaledAvailable = scaledMaxValue - scaledMinValue;
            double diffAvaliable = Math.log10(availableRangeMap.get(yAxisElement).max) - Math.log10(availableRangeMap.get(yAxisElement).min);
            double diffSelectedStart = scaledSelectedMinValue - scaledMinValue;
            double diffSelectedEnd = scaledSelectedMaxValue - scaledMinValue;
            double selectedStart = Math.pow(10, Math.log10(availableRangeMap.get(yAxisElement).min) + diffSelectedStart / diffScaledAvailable * diffAvaliable);
            double selectedEnd = Math.pow(10, Math.log10(availableRangeMap.get(yAxisElement).min) + diffSelectedEnd / diffScaledAvailable * diffAvaliable);
            if (selectedStart != selectedRangeMap.get(yAxisElement).min || selectedEnd != selectedRangeMap.get(yAxisElement).max) {
                setSelectedRange(new Range(selectedStart, selectedEnd), yAxisElement);
            } else {
                fireRedrawRequest(false);
            }
        }
    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinValue, double oldMaxValue, double oldMinTime, double oldMaxTime, double newMinValue, double newMaxValue, double newMinTime, double newMaxTime) {
        // TODO Auto-generated method stub
    }

    public EVEValues getValues(Band band, Interval<Date> interval, Rectangle graphArea) {
        return EVECacheController.getSingletonInstance().downloadData(band, interval, graphArea);
    }

    public void bandColorChanged(Band band) {
        fireRedrawRequest(false);
    }

    public boolean hasDataInSelectedInterval(Band band) {
        return EVECacheController.getSingletonInstance().hasDataInSelectedInterval(band, DrawController.getSingletonInstance().getSelectedInterval());
    }

    public void changeAxis(Band band) {
        YAxisElement currentYAxisElement = yAxisElementMap.get(band);
        if (((bandsPerYAxis.size() == 1 && bandsPerYAxis.get(currentYAxisElement).size() > 1) || bandsPerYAxis.size() == 2) && drawController.canChangeAxis(band.getUnitLabel())) {
            YAxisElement otherYAxisElement = getOtherAxisElement(currentYAxisElement);
            if (otherYAxisElement != null) {
                yAxisElementMap.put(band, otherYAxisElement);
                List<Band> bandsPerList = new ArrayList<Band>();
                if (bandsPerYAxis.containsKey(otherYAxisElement)) {
                    bandsPerList = bandsPerYAxis.get(otherYAxisElement);
                }
                bandsPerList.add(band);
                bandsPerYAxis.put(otherYAxisElement, bandsPerList);
                bandsPerYAxis.get(currentYAxisElement).remove(band);
                Map<Band, EVEValues> valuesPerBand = new HashMap<Band, EVEValues>();
                if (!dataMapPerUnitLabel.containsKey(otherYAxisElement)) {
                    dataMapPerUnitLabel.put(otherYAxisElement, valuesPerBand);
                }
                dataMapPerUnitLabel.get(otherYAxisElement).put(band, dataMapPerUnitLabel.get(currentYAxisElement).get(band));
                dataMapPerUnitLabel.get(currentYAxisElement).remove(band);
                if (!eveDrawableElementMap.containsKey(otherYAxisElement)) {
                    eveDrawableElementMap.put(otherYAxisElement, new EVEDrawableElement());
                }
                resetAvailableRange();
                updateBand(band, true);
                fireRedrawRequest(true);
            }
        }
    }

    private void resetAvailableRange() {
        for (YAxisElement yAxisElement : availableRangeMap.keySet()) {
            availableRangeMap.put(yAxisElement, new Range());
        }
    }

    private YAxisElement getOtherAxisElement(YAxisElement currentYAxisElement) {
        if (drawController.canChangeAxis(currentYAxisElement.getOriginalLabel())) {
            Set<YAxisElement> allYAxisElements = bandsPerYAxis.keySet();
            if (allYAxisElements.size() == 2) {
                for (YAxisElement el : allYAxisElements) {
                    if (!el.equals(currentYAxisElement)) {
                        return el;
                    }
                }
            }
            return new YAxisElement();
        }
        return null;
    }

    public boolean canChangeAxis(Band band) {
        return DrawController.getSingletonInstance().canChangeAxis(band.getUnitLabel());
    }

    public int getAxisLocation(Band band) {
        return drawController.getYAxisLocation(yAxisElementMap.get(band)) == YAxisElement.YAxisLocation.LEFT ? 0 : 1;
    }

}
