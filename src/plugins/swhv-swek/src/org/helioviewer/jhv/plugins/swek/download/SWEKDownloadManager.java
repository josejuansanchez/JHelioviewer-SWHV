package org.helioviewer.jhv.plugins.swek.download;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.plugins.swek.SWEKPluginLocks;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModelListener;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.plugins.swek.request.IncomingRequestManager;
import org.helioviewer.jhv.plugins.swek.request.IncomingRequestManagerListener;
import org.helioviewer.jhv.plugins.swek.settings.SWEKProperties;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKDownloadManager implements DownloadWorkerListener, IncomingRequestManagerListener, EventTypePanelModelListener,
        FilterManagerListener {

    /** Singleton instance of the SWE */
    private static SWEKDownloadManager instance;

    /** Threadpool for downloading events */
    private final ExecutorService downloadEventPool;

    /** The properties specific to the swek plugin */
    private final Properties swekProperties;

    /** Map holding the download workers order by event type and date */
    private final Map<Long, Map<SWEKEventType, Map<Date, DownloadWorker>>> dwMap;

    /** Map with all the finished and busy downloads */
    private final Map<SWEKEventType, Map<SWEKSupplier, Set<Date>>> busyAndFinishedJobs;

    /** Map with all the finished and busy interval downloads */
    private final Map<SWEKEventType, Map<SWEKSupplier, Map<Date, Set<Date>>>> busyAndFinishedIntervalJobs;

    /** Map holding the active event types and its sources */
    private final Map<SWEKEventType, Map<SWEKSource, Set<SWEKSupplier>>> activeEventTypes;

    /** Local instance of the request manager */
    private final IncomingRequestManager requestManager;

    /** Local instance of the event container */
    private final JHVEventContainer eventContainer;

    /** Local instance of filter manager */
    private final FilterManager filterManager;

    /** The instance of the SWEKTreeModel */
    private final SWEKTreeModel treeModel;

    /** The configuration manager instance */
    private final SWEKConfigurationManager configInstance = SWEKConfigurationManager.getSingletonInstance();

    /**
     * private constructor of the SWEKDownloadManager
     */
    private SWEKDownloadManager() {
        swekProperties = SWEKProperties.getSingletonInstance().getSWEKProperties();
        downloadEventPool = Executors.newFixedThreadPool(Integer.parseInt(swekProperties.getProperty("plugin.swek.numberofthreads")));
        dwMap = new HashMap<Long, Map<SWEKEventType, Map<Date, DownloadWorker>>>();
        activeEventTypes = new HashMap<SWEKEventType, Map<SWEKSource, Set<SWEKSupplier>>>();
        requestManager = IncomingRequestManager.getSingletonInstance();
        busyAndFinishedJobs = new HashMap<SWEKEventType, Map<SWEKSupplier, Set<Date>>>();
        busyAndFinishedIntervalJobs = new HashMap<SWEKEventType, Map<SWEKSupplier, Map<Date, Set<Date>>>>();
        requestManager.addRequestManagerListener(this);
        eventContainer = JHVEventContainer.getSingletonInstance();
        filterManager = FilterManager.getSingletonInstance();
        filterManager.addFilterManagerListener(this);
        treeModel = SWEKTreeModel.getSingletonInstance();
    }

    /**
     * Gets the singleton instance of the SWEKDownloadManager
     * 
     * @return The singleton instance
     */
    public static SWEKDownloadManager getSingletonInstance() {
        if (instance == null) {
            instance = new SWEKDownloadManager();
        }
        return instance;
    }

    /**
     * Stops downloading the event type for every source of the event type.
     * 
     * @param eventType
     *            the event type for which to stop downloads
     */
    public void stopDownloadingEventType(SWEKEventType eventType) {
        synchronized (SWEKPluginLocks.downloadLock) {
            for (Long requestID : dwMap.keySet()) {
                Map<SWEKEventType, Map<Date, DownloadWorker>> eventTypeMap = dwMap.get(requestID);
                if (eventTypeMap.containsKey(eventType)) {
                    Map<Date, DownloadWorker> dwMapOnDate = eventTypeMap.get(eventType);
                    for (DownloadWorker dw : dwMapOnDate.values()) {
                        dw.stopWorker();
                    }
                }
            }
        }
        removeFromBusyAndFinishedJobs(eventType);
        removeFromBusyAndFinishedIntervalJobs(eventType);
        for (SWEKSupplier supplier : eventType.getSuppliers()) {
            eventContainer.removeEvents(new JHVSWEKEventType(eventType.getEventName(), supplier.getSource().getSourceName(), supplier
                    .getSupplierName()));
        }
    }

    /**
     * Stops downloading the event type for the given source.
     * 
     * @param eventType
     *            the event type for which to stop the downloads
     * @param source
     *            the source for which to stop the downloads
     */
    public void stopDownloadingEventType(SWEKEventType eventType, SWEKSource source, SWEKSupplier supplier) {
        synchronized (SWEKPluginLocks.downloadLock) {
            for (Long requestID : dwMap.keySet()) {
                Map<SWEKEventType, Map<Date, DownloadWorker>> eventTypeMap = dwMap.get(requestID);
                if (eventTypeMap.containsKey(eventType)) {
                    Map<Date, DownloadWorker> dwMapOnDate = eventTypeMap.get(eventType);
                    for (DownloadWorker dw : dwMapOnDate.values()) {
                        if (dw.getSupplier().equals(supplier)) {
                            dw.stopWorker();
                        }
                    }
                }
            }
            removeFromBusyAndFinishedJobs(eventType, supplier);
            removeFromBusyAndFinishedIntervalJobs(eventType, supplier);
            eventContainer.removeEvents(new JHVSWEKEventType(eventType.getEventName(), source.getSourceName(), supplier.getSupplierName()));
        }
    }

    @Override
    public void workerStarted(DownloadWorker worker) {
        treeModel.setStartLoading(worker.getEventType());
    }

    @Override
    public void workerForcedToStop(DownloadWorker worker) {
        synchronized (SWEKPluginLocks.downloadLock) {
            treeModel.setStopLoading(worker.getEventType());
            removeWorkerFromMap(worker);
            removeFromBusyAndFinishedJobs(worker.getEventType(), worker.getSupplier(), worker.getDownloadStartDate());
        }
    }

    @Override
    public void workerFinished(DownloadWorker worker) {
        synchronized (SWEKPluginLocks.downloadLock) {
            treeModel.setStopLoading(worker.getEventType());
            removeWorkerFromMap(worker);
        }
    }

    @Override
    public void newEventTypeAndSourceActive(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier) {
        synchronized (SWEKPluginLocks.downloadLock) {
            addEventTypeToActiveEventTypeMap(eventType, swekSource, supplier);
            downloadForAllDates(eventType, swekSource, supplier);
        }
    }

    @Override
    public void newEventTypeAndSourceInActive(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier) {
        synchronized (SWEKPluginLocks.treeSelectionLock) {
            removeEventTypeFromActiveEventTypeMap(eventType, swekSource, supplier);
        }
        synchronized (SWEKPluginLocks.downloadLock) {
            stopDownloadingEventType(eventType, swekSource, supplier);
        }

    }

    @Override
    public void newRequestForDate(Date date, Long requestID) {
        synchronized (SWEKPluginLocks.downloadLock) {
            downloadAllSelectedEventTypes(date, requestID);
        }
    }

    @Override
    public void newRequestForInterval(Interval<Date> interval, Long requestID) {
        synchronized (SWEKPluginLocks.downloadLock) {
            downloadAllSelectedEventTypes(interval, requestID);
        }
    }

    @Override
    public void newRequestForDateList(List<Date> dates, Long requestID) {
        synchronized (SWEKPluginLocks.downloadLock) {
            downloadAllSelectedEventTypes(dates, requestID);
        }
    }

    @Override
    public void filtersAdded(SWEKEventType swekEventType) {
        synchronized (SWEKPluginLocks.downloadLock) {
            stopDownloadingEventType(swekEventType);
            downloadSelectedSuppliers(swekEventType);
        }
    }

    @Override
    public void filtersRemoved(SWEKEventType swekEventType, SWEKParameter parameter) {
        synchronized (SWEKPluginLocks.downloadLock) {
            stopDownloadingEventType(swekEventType);
            downloadSelectedSuppliers(swekEventType);
        }
    }

    /**
     * Removes the event type from the busy and finished interval jobs.
     * 
     * @param eventType
     *            the event type to remove
     */
    private void removeFromBusyAndFinishedIntervalJobs(SWEKEventType eventType) {
        busyAndFinishedIntervalJobs.remove(eventType);
    }

    /**
     * Removes the event type from the busy and finished jobs.
     * 
     * @param eventType
     *            the event type to remove
     */
    private void removeFromBusyAndFinishedJobs(SWEKEventType eventType) {
        busyAndFinishedJobs.remove(eventType);

    }

    /**
     * Removes the source for a given event type from the busy and finished
     * interval jobs.
     * 
     * @param eventType
     *            the event type to remove
     * @param supplier
     *            the supplier to remove the interval type for
     */
    private void removeFromBusyAndFinishedIntervalJobs(SWEKEventType eventType, SWEKSupplier supplier) {
        if (busyAndFinishedIntervalJobs.containsKey(eventType)) {
            Map<SWEKSupplier, Map<Date, Set<Date>>> datesPerSource = busyAndFinishedIntervalJobs.get(eventType);
            datesPerSource.remove(supplier);
            busyAndFinishedIntervalJobs.put(eventType, datesPerSource);
        }
    }

    /**
     * Removes the source for a given event type from the busy and finished
     * jobs.
     * 
     * @param eventType
     *            the event type to remove
     * @param supplier
     *            the supplier to remove the interval type for
     */
    private void removeFromBusyAndFinishedJobs(SWEKEventType eventType, SWEKSupplier supplier) {
        if (busyAndFinishedJobs.containsKey(eventType)) {
            Map<SWEKSupplier, Set<Date>> datesPerSource = busyAndFinishedJobs.get(eventType);
            datesPerSource.remove(supplier);
            busyAndFinishedJobs.put(eventType, datesPerSource);
        }
    }

    /**
     * Removes the worker from the map with workers.
     * 
     * @param worker
     *            The worker to remove
     */
    private void removeWorkerFromMap(DownloadWorker worker) {
        synchronized (SWEKPluginLocks.downloadLock) {
            for (Long requestId : dwMap.keySet()) {
                Map<SWEKEventType, Map<Date, DownloadWorker>> eventTypeMap = dwMap.get(requestId);
                Map<Date, DownloadWorker> dwMapOnDate = eventTypeMap.get(worker.getEventType());
                if (dwMapOnDate != null) {
                    dwMapOnDate.remove(worker.getDownloadStartDate());
                }
            }
        }
    }

    /**
     * Adds the downloader worker to the downloader map.
     * 
     * @param eventType
     *            The event type to add
     * @param date
     *            The date for which the event type was downloaded
     * @param dw
     *            The download worker used to download the event type
     * @param requestID
     */
    private void addToDownloaderMap(SWEKEventType eventType, Date date, DownloadWorker dw, Long requestID) {
        Map<SWEKEventType, Map<Date, DownloadWorker>> eventTypeMap = new HashMap<SWEKEventType, Map<Date, DownloadWorker>>();
        Map<Date, DownloadWorker> dwMapOnDate = new HashMap<Date, DownloadWorker>();
        if (dwMap.containsKey(requestID)) {
            eventTypeMap = dwMap.get(requestID);
            if (eventTypeMap.containsKey(eventType)) {
                dwMapOnDate = eventTypeMap.get(eventType);
            }
        }
        dwMapOnDate.put(date, dw);
        eventTypeMap.put(eventType, dwMapOnDate);
        dwMap.put(requestID, eventTypeMap);
    }

    /**
     * Add the combination of an event type and a swek source to the list of
     * active event types.
     * 
     * @param eventType
     *            the event type to add
     * @param swekSupplier
     *            the swek source to add
     */
    private void addEventTypeToActiveEventTypeMap(SWEKEventType eventType, SWEKSource source, SWEKSupplier swekSupplier) {
        Map<SWEKSource, Set<SWEKSupplier>> sourcesPerEventType = new HashMap<SWEKSource, Set<SWEKSupplier>>();
        Set<SWEKSupplier> supplierPerSource = new HashSet<SWEKSupplier>();
        if (activeEventTypes.containsKey(eventType)) {
            sourcesPerEventType = activeEventTypes.get(eventType);
            if (sourcesPerEventType.containsKey(source)) {
                supplierPerSource = sourcesPerEventType.get(source);
            }
        }
        supplierPerSource.add(swekSupplier);
        sourcesPerEventType.put(source, supplierPerSource);
        activeEventTypes.put(eventType, sourcesPerEventType);

    }

    /**
     * Removes the combination of an event type, a swek source and a swek
     * supplier from the list of active event types.
     * 
     * @param eventType
     *            the event type to remove
     * @param swekSource
     *            the swek source to remove
     * @param supplier
     *            the supplier to remove
     */
    private void removeEventTypeFromActiveEventTypeMap(SWEKEventType eventType, SWEKSource source, SWEKSupplier swekSupplier) {
        Map<SWEKSource, Set<SWEKSupplier>> sourcesPerEventtype = activeEventTypes.get(eventType);
        if (sourcesPerEventtype != null) {
            Set<SWEKSupplier> supplierPerSource = sourcesPerEventtype.get(source);
            if (supplierPerSource != null) {
                supplierPerSource.remove(swekSupplier);
            }
            sourcesPerEventtype.put(source, supplierPerSource);
            activeEventTypes.put(eventType, sourcesPerEventtype);
        }
    }

    /**
     * Downloads the given combination of event type and swek source for all the
     * already requested dates.
     * 
     * @param eventType
     *            the type to download
     * @param swekSource
     *            the source providing the event type
     * @param supplier
     *            the supplier to producing the event
     */
    private void downloadForAllDates(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier) {
        Map<Long, List<Date>> allDates = requestManager.getAllRequestedDates();
        for (Long requestID : allDates.keySet()) {
            for (Date date : allDates.get(requestID)) {
                startDownloadEventType(eventType, swekSource, date, supplier, requestID);
            }
        }
        Map<Long, Interval<Date>> allIntervals = requestManager.getAllRequestedIntervals();
        for (Long requestID : allIntervals.keySet()) {
            startDownloadEventType(eventType, swekSource, allIntervals.get(requestID), supplier, requestID);

        }
    }

    /**
     * Downloads for the given date the events of the currently active
     * combinations of event type and swek sources.
     * 
     * @param date
     *            The date for which the event should be downloaded from the
     *            sources
     * @param requestID
     */
    private void downloadAllSelectedEventTypes(Date date, Long requestID) {
        synchronized (SWEKPluginLocks.treeSelectionLock) {
            for (SWEKEventType eventType : activeEventTypes.keySet()) {
                for (SWEKSource source : activeEventTypes.get(eventType).keySet()) {
                    for (SWEKSupplier supplier : activeEventTypes.get(eventType).get(source)) {
                        startDownloadEventType(eventType, source, date, supplier, requestID);
                    }
                }
            }
        }
    }

    /**
     * Downloads for the given date the events of the currently active
     * combinations of event type and swek sources.
     * 
     * @param interval
     *            The interval for which the event should be downloaded from the
     *            sources
     */
    private void downloadAllSelectedEventTypes(Interval<Date> interval, Long requestID) {
        synchronized (SWEKPluginLocks.treeSelectionLock) {
            for (SWEKEventType eventType : activeEventTypes.keySet()) {
                for (SWEKSource source : activeEventTypes.get(eventType).keySet()) {
                    for (SWEKSupplier supplier : activeEventTypes.get(eventType).get(source)) {
                        startDownloadEventType(eventType, source, interval, supplier, requestID);
                    }
                }
            }
        }
    }

    /**
     * Checks if a job is already busy or finished.
     * 
     * @param eventType
     *            the type that should be checked
     * @param supplier
     *            the source that provides the event type
     * @param date
     *            the date that should be checked
     * @return true if the combination was found, false if not
     */
    private boolean inBusyAndFinishedJobs(SWEKEventType eventType, SWEKSupplier supplier, Date date) {
        Map<SWEKSupplier, Set<Date>> suppliersAndDatesForEvent = busyAndFinishedJobs.get(eventType);
        if (suppliersAndDatesForEvent != null) {
            Set<Date> datesForEventAndSource = suppliersAndDatesForEvent.get(supplier);
            if (datesForEventAndSource != null) {
                return datesForEventAndSource.contains(date);
            }
        }
        return false;
    }

    /**
     * Checks if a job is already busy or finished.
     * 
     * @param eventType
     *            the type that should be checked
     * @param swekSupplier
     *            the supplier that provides the event type
     * @param interval
     *            the interval that should be checked
     * @return true if the combination was found, false if not.
     */
    private boolean inBusyAndFinishedIntervalJobs(SWEKEventType eventType, SWEKSupplier swekSupplier, Interval<Date> interval) {
        Map<SWEKSupplier, Map<Date, Set<Date>>> suppliersAndDatesForEvent = busyAndFinishedIntervalJobs.get(eventType);
        if (suppliersAndDatesForEvent != null) {
            Map<Date, Set<Date>> datesForEventTypeAndSource = suppliersAndDatesForEvent.get(swekSupplier);
            if (datesForEventTypeAndSource != null) {
                Set<Date> endDatesForStartDate = datesForEventTypeAndSource.get(interval.getStart());
                if (endDatesForStartDate != null) {
                    return endDatesForStartDate.contains(interval.getEnd());
                }
            }
        }
        return false;
    }

    /**
     * Removes the combination of event type, source and date from the busy and
     * finished jobs.
     * 
     * @param eventType
     *            the event type to remove
     * @param supplier
     *            the supplier to remove
     * @param date
     *            the date to remove
     */
    private void removeFromBusyAndFinishedJobs(SWEKEventType eventType, SWEKSupplier supplier, Date date) {
        Map<SWEKSupplier, Set<Date>> sourcesAndDatesForEvent = busyAndFinishedJobs.get(eventType);
        if (sourcesAndDatesForEvent != null) {
            Set<Date> datesForEventAndSource = sourcesAndDatesForEvent.get(supplier);
            if (datesForEventAndSource != null) {
                datesForEventAndSource.remove(date);
            }
        }
    }

    /**
     * Starts downloading for every source the requested event type. This will
     * start a thread to download the events.
     * 
     * @param eventType
     *            The event type to download
     * @param swekSource
     *            the source from where the event is downloaded
     * @param date
     *            The date to download the event type for
     */
    private void startDownloadEventType(SWEKEventType eventType, SWEKSource swekSource, Date date, Long requestID) {
        for (SWEKSupplier s : eventType.getSuppliers()) {
            startDownloadEventType(eventType, s.getSource(), date, s, requestID);
        }
    }

    /**
     * Starts downloading for one particular source the given event type.
     * 
     * @param eventType
     *            The event type to download
     * @param source
     *            The source from which to download the event type
     * @param date
     *            the date for which to start downloading
     * @param supplier
     *            the supplier providing the events
     * @param requestID
     */
    private void startDownloadEventType(SWEKEventType eventType, SWEKSource source, Date date, SWEKSupplier supplier, Long requestID) {
        synchronized (SWEKPluginLocks.downloadLock) {
            List<SWEKParam> params = defineParameters(eventType, source, supplier);
            DownloadWorker dw = new DownloadWorker(eventType, source, supplier, date, params, configInstance.getSWEKRelatedEvents());
            if (!inBusyAndFinishedJobs(eventType, supplier, date)) {
                dw.addDownloadWorkerListener(this);
                addToDownloaderMap(eventType, dw.getDownloadStartDate(), dw, requestID);
                addToBusyAndFinishedJobs(eventType, supplier, date);
                downloadEventPool.execute(dw);
            }
        }
    }

    /**
     * Defines the parameters based on filters and provider.
     * 
     * @param eventType
     *            the event type for which the parameters are defined
     * @param source
     *            the source from where the events are coming
     * @return the parameters
     */
    private List<SWEKParam> defineParameters(SWEKEventType eventType, SWEKSource source, SWEKSupplier supplier) {
        List<SWEKParam> params = new ArrayList<SWEKParam>();
        params.add(new SWEKParam("provider", supplier.getSupplierName(), SWEKOperand.EQUALS));
        // TODO bram: add filter here coming from event type ==> done
        Map<SWEKParameter, List<SWEKParam>> paramsPerEventParameter = filterManager.getFilterForEventType(eventType);
        for (List<SWEKParam> paramPerParameter : paramsPerEventParameter.values()) {
            params.addAll(paramPerParameter);
        }
        return params;
    }

    /**
     * Starts downloading the event from the source of an interval.
     * 
     * @param eventType
     *            the event type to download
     * @param swekSource
     *            the source from which to download
     * @param interval
     *            the interval over which to download
     * @param supplier
     *            the supplier providing the event
     */
    private void startDownloadEventType(SWEKEventType eventType, SWEKSource swekSource, Interval<Date> interval, SWEKSupplier supplier,
            Long requestID) {
        synchronized (SWEKPluginLocks.downloadLock) {
            List<SWEKParam> params = defineParameters(eventType, swekSource, supplier);
            DownloadWorker dw = new DownloadWorker(eventType, swekSource, supplier, interval, params, configInstance.getSWEKRelatedEvents());
            if (!inBusyAndFinishedIntervalJobs(eventType, supplier, interval)) {
                dw.addDownloadWorkerListener(this);
                addToDownloaderMap(eventType, dw.getDownloadStartDate(), dw, requestID);
                addToBusyAndFinishedIntervalJobs(eventType, supplier, interval);
                downloadEventPool.execute(dw);
            }
        }
    }

    /**
     * Add event type, source, date to busy and finished jobs.
     * 
     * @param eventType
     *            the event type to add
     * @param supplier
     *            the supplier to add
     * @param date
     *            the date to add
     */
    private void addToBusyAndFinishedJobs(SWEKEventType eventType, SWEKSupplier supplier, Date date) {
        Map<SWEKSupplier, Set<Date>> sourcesForEventType = new HashMap<SWEKSupplier, Set<Date>>();
        Set<Date> dates = new HashSet<Date>();
        if (busyAndFinishedJobs.containsKey(eventType)) {
            sourcesForEventType = busyAndFinishedJobs.get(eventType);
            if (sourcesForEventType.containsKey(supplier)) {
                dates = sourcesForEventType.get(supplier);
            }
        }
        dates.add(date);
        sourcesForEventType.put(supplier, dates);
        busyAndFinishedJobs.put(eventType, sourcesForEventType);
    }

    /**
     * Adds event type, source, interval to busy and finished jobs.
     * 
     * @param eventType
     *            the event type to add
     * @param swekSource
     *            the source to add
     * @param interval
     *            the interval to add
     */
    private void addToBusyAndFinishedIntervalJobs(SWEKEventType eventType, SWEKSupplier supplier, Interval<Date> interval) {
        Map<SWEKSupplier, Map<Date, Set<Date>>> sourcesForEventType = new HashMap<SWEKSupplier, Map<Date, Set<Date>>>();
        Map<Date, Set<Date>> datesPerSource = new HashMap<Date, Set<Date>>();
        Set<Date> endDate = new HashSet<Date>();
        if (busyAndFinishedIntervalJobs.containsKey(eventType)) {
            sourcesForEventType = busyAndFinishedIntervalJobs.get(eventType);
            if (sourcesForEventType.containsKey(supplier)) {
                datesPerSource = sourcesForEventType.get(supplier);
                if (datesPerSource.containsKey(interval.getStart())) {
                    endDate = datesPerSource.get(interval.getStart());
                }
            }
        }
        endDate.add(interval.getEnd());
        datesPerSource.put(interval.getStart(), endDate);
        sourcesForEventType.put(supplier, datesPerSource);
        busyAndFinishedIntervalJobs.put(eventType, sourcesForEventType);
    }

    /**
     * Starts downloading all the selected events for a list of dates.
     * 
     * @param dates
     *            the list of dates for which to download all the selected
     *            events
     */
    private void downloadAllSelectedEventTypes(List<Date> dates, Long requestID) {
        synchronized (SWEKPluginLocks.downloadLock) {
            for (Date date : dates) {
                downloadAllSelectedEventTypes(date, requestID);
            }
        }
    }

    /**
     * Downloads the event type for all the dates.
     * 
     * @param swekEventType
     */
    private void downloadSelectedSuppliers(SWEKEventType swekEventType) {
        synchronized (SWEKPluginLocks.treeSelectionLock) {
            if (activeEventTypes.get(swekEventType) != null) {
                for (SWEKSource source : activeEventTypes.get(swekEventType).keySet()) {
                    for (SWEKSupplier supplier : activeEventTypes.get(swekEventType).get(source)) {
                        downloadForAllDates(swekEventType, source, supplier);
                    }
                }
            }
        }
    }

    @Override
    public void stopRequest(Long requestID) {
        synchronized (SWEKPluginLocks.downloadLock) {
            if (dwMap.containsKey(requestID)) {
                Map<SWEKEventType, Map<Date, DownloadWorker>> eventTypeMap = dwMap.get(requestID);
                for (Map<Date, DownloadWorker> eventTypeOnDate : eventTypeMap.values()) {
                    for (DownloadWorker dw : eventTypeOnDate.values()) {
                        dw.stopWorker();
                    }
                }
            }
        }
    }
}
