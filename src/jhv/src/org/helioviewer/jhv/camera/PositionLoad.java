package org.helioviewer.jhv.camera;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Iterator;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PositionLoad {

    private static final String LOADEDSTATE = "Loaded";
    private static final String FAILEDSTATE = "Failed";

    private static final String baseURL = "http://swhv.oma.be/position?";
    private static final String target = "SUN";
    private String observer = "Earth";

    private long beginTime = TimeUtils.EPOCH.milli;
    private long endTime = TimeUtils.EPOCH.milli;

    private boolean isLoaded = false;
    private Position.L[] position;
    private JHVWorker<Position.L[], Void> worker;

    private final PositionLoadFire receiver;

    public PositionLoad(PositionLoadFire _receiver) {
        receiver = _receiver;
    }

    private class LoadPositionWorker extends JHVWorker<Position.L[], Void> {

        private String report = null;
        private final long start;
        private final long end;
        private final String observer;

        public LoadPositionWorker(long start, long end, String observer) {
            this.start = start;
            this.end = end;
            this.observer = observer;
        }

        @Override
        protected Position.L[] backgroundWork() {
            JSONObject result;
            try {
                long deltat = 60, span = (end - start) / 1000;
                final long max = 100000;

                if (span / deltat > max)
                    deltat = span / max;

                URL url = new URL(baseURL + "abcorr=LT%2BS&utc=" + TimeUtils.utcFullDateFormat.format(start) + 
                                            "&utc_end=" + TimeUtils.utcFullDateFormat.format(end) + "&deltat=" + deltat +
                                            "&observer=" + observer + "&target=" + target + "&ref=HEEQ&kind=latitudinal");
                DownloadStream ds = new DownloadStream(url, true);

                result = JSONUtils.getJSONStream(ds.getInput());
                if (ds.isResponse400()) {
                    if (result.has("faultstring"))
                        report = result.getString("faultstring");
                    else
                        report = "Invalid network response";
                } else {
                    return parseData(result);
                }
            } catch (MalformedURLException e) {
                Log.error("Malformed URL", e);
            } catch (UnknownHostException e) {
                Log.debug("Unknown host, network down?", e);
            } catch (IOException e) {
                report = FAILEDSTATE + ": server error";
            } catch (JSONException | ParseException | NumberFormatException e) {
                report = FAILEDSTATE + ": JSON parse error";
            }

            return null;
        }

        private Position.L[] parseData(JSONObject jsonResult) throws JSONException, ParseException {
            JSONArray resArray = jsonResult.getJSONArray("result");
            int resLength = resArray.length();
            Position.L[] ret = new Position.L[resLength];

            for (int j = 0; j < resLength; j++) {
                JSONObject posObject = resArray.getJSONObject(j);
                Iterator<?> iterKeys = posObject.keys();
                if (!iterKeys.hasNext())
                    throw new JSONException("unexpected format");

                String dateString = (String) iterKeys.next();
                JSONArray posArray = posObject.getJSONArray(dateString);

                double rad, lon, lat, jlon;
                rad = posArray.getDouble(0) * (1000. / Sun.RadiusMeter);
                jlon = posArray.getDouble(1);
                lon = jlon + (jlon > 0 ? -Math.PI : Math.PI);
                lat = -posArray.getDouble(2);

                JHVDate time = new JHVDate(TimeUtils.utcFullDateFormat.parse(dateString).getTime());
                Position.L p = Sun.getEarth(time);

                ret[j] = new Position.L(time, rad, -lon + p.lon, lat);
            }
            return ret;
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                Position.L[] newPosition = null;
                try {
                    newPosition = get();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (report == null) {
                    if (newPosition == null) {
                        report = "response is void";
                    } else if (newPosition.length == 0) {
                        report = "response is zero length array";
                    } else {
                        position = newPosition;
                        setLoaded(true);
                    }
                }
                if (report != null) {
                    fireLoaded(report);
                    report = null;
                }
            }
        }
    }

    private void requestData() {
        if (worker != null) {
            worker.cancel(false);
        }
        fireLoaded("Loading...");

        worker = new LoadPositionWorker(beginTime, endTime, observer);
        worker.setThreadName("MAIN--PositionLoad");
        JHVGlobals.getExecutorService().execute(worker);
    }

    private void setLoaded(boolean _isLoaded) {
        isLoaded = _isLoaded;
        if (isLoaded) {
            fireLoaded(LOADEDSTATE);
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    private void applyChanges() {
        setLoaded(false);
        position = null;
        requestData();
    }

    public void setBeginTime(long beginTime, boolean applyChanges) {
        this.beginTime = beginTime;
        if (applyChanges) {
            applyChanges();
        }
    }

    public void setEndTime(long endTime, boolean applyChanges) {
        this.endTime = endTime;
        if (applyChanges) {
            applyChanges();
        }
    }

    public void fireLoaded(String state) {
        receiver.firePositionLoaded(state);
    }

    public long getStartTime() {
        if (position.length > 0) {
            return position[0].time.milli;
        }
        return -1L;
    }

    public long getEndTime() {
        if (position.length > 0) {
            return position[position.length - 1].time.milli;
        }
        return -1L;
    }

    public Position.Q getInterpolatedPosition(long currentCameraTime) {
        if (isLoaded && position.length > 0) {
            double dist, hgln, hglt;
            long milli;

            long tstart = getStartTime();
            long tend = getEndTime();
            if (tstart == tend) {
                milli = position[0].time.milli;
                dist = position[0].rad;
                hgln = position[0].lon;
                hglt = position[0].lat;
            } else {
                double interpolatedIndex = (currentCameraTime - tstart) / (double) (tend - tstart) * position.length;
                int i = (int) interpolatedIndex;
                i = Math.min(i, position.length - 1);
                if (i < 0) {
                    i = 0;
                }
                int inext = Math.min(i + 1, position.length - 1);

                tstart = position[i].time.milli;
                tend = position[inext].time.milli;

                double alpha;
                if (tend == tstart)
                    alpha = 1.;
                else
                    alpha = ((currentCameraTime - tstart) / (double) (tend - tstart)) % 1.;

                milli = (long) ((1. - alpha) * position[i].time.milli + alpha * position[inext].time.milli);
                dist = (1. - alpha) * position[i].rad + alpha * position[inext].rad;
                hgln = (1. - alpha) * position[i].lon + alpha * position[inext].lon;
                hglt = (1. - alpha) * position[i].lat + alpha * position[inext].lat;
            }
            return new Position.Q(new JHVDate(milli), dist, new Quat(hglt, hgln));
        } else {
            return null;
        }
    }

    public void setObserver(String object, boolean applyChanges) {
        observer = object;
        if (applyChanges) {
            applyChanges();
        }
    }

}
