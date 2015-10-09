package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.container.cache.JHVEventCacheResult;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.viewmodel.view.View;

/**
 * This class intercepts changes of the layers and request data from the
 * JHVEventContainer.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWHVHEKData implements LayersListener, JHVEventHandler {

    private static SWHVHEKData instance;
    private Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> data;
    private ArrayList<JHVEvent> events;
    private Date beginDate = null;
    private Date endDate = null;

    private SWHVHEKData() {
    }

    public static SWHVHEKData getSingletonInstance() {
        if (instance == null) {
            instance = new SWHVHEKData();
        }
        return instance;
    }

    public void reset() {
        instance = null;
    }

    public void requestEvents() {
        boolean request = false;
        Date first = Layers.getFirstDate();
        Date last = Layers.getLastDate();
        if (beginDate == null || (first != null && first.getTime() < beginDate.getTime())) {
            beginDate = first;
            request = true;
        }
        if (endDate == null || (last != null && last.getTime() > endDate.getTime())) {
            endDate = last;
            request = true;
        }

        if (request) {
            JHVEventContainer.getSingletonInstance().requestForInterval(beginDate, endDate, SWHVHEKData.this);
        }
    }

    @Override
    public void layerAdded(View view) {
        requestEvents();
    }

    @Override
    public void activeLayerChanged(View view) {
    }

    @Override
    public void newEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventList) {
        requestEvents();
        displayEvents();
    }

    @Override
    public void cacheUpdated() {
        if (beginDate != null && endDate != null) {
            beginDate = null;
            endDate = null;
            requestEvents();
            displayEvents();
        }
    }

    private void displayEvents() {
        JHVEventCacheResult result = JHVEventCache.getSingletonInstance().get(beginDate, endDate, beginDate, endDate);
        data = result.getAvailableEvents();
        ArrayList<JHVEvent> events = new ArrayList<JHVEvent>();
        for (String eventType : data.keySet()) {
            for (Date sDate : data.get(eventType).keySet()) {
                for (Date eDate : data.get(eventType).get(sDate).keySet()) {
                    for (JHVEvent event : data.get(eventType).get(sDate).get(eDate)) {
                        events.add(event);
                    }
                }
            }
        }
        this.events = events;
        Displayer.display();
    }

    public Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> getData() {
        return data;
    }

    public ArrayList<JHVEvent> getActiveEvents(Date currentDate) {
        ArrayList<JHVEvent> activeEvents = new ArrayList<JHVEvent>();
        if (events != null) {
            for (JHVEvent event : events) {
                if (event != null && event.getStartDate() != null && event.getEndDate() != null) {
                    if (event.getStartDate().getTime() <= currentDate.getTime() && event.getEndDate().getTime() >= currentDate.getTime()) {
                        activeEvents.add(event);
                    }
                    event.addHighlightListener(Displayer.getSingletonInstance());
                } else {
                    Log.warn("Possibly something strange is going on with incoming events. Either the date or the event is null");
                }
            }
        }
        return activeEvents;
    }

}
