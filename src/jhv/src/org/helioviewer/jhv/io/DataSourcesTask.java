package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONException;
import org.json.JSONObject;

public class DataSourcesTask extends JHVWorker<Void, Void> {

    private final DataSourcesParser parser;
    private URL url;

    public DataSourcesTask(String server) {
        parser = new DataSourcesParser(server);
        try {
            url = new URL(DataSources.getServerSetting(server, "API.dataSources.path"));
        } catch (MalformedURLException e) {
            Log.error("Invalid data sources URL", e);
        }
        setThreadName("MAIN--DataSources");
    }

    @Override
    protected Void backgroundWork() {
        while (true) {
            try {
                JSONObject json = JSONUtils.getJSONStream(new DownloadStream(url).getInput());
                parser.parse(json);
                return null;
            } catch (JSONException e) {
                Log.error("Invalid response while retrieving the available data sources", e);
                break;
            } catch (ParseException e) {
                Log.error("Invalid response while retrieving the available data sources", e);
                break;
            } catch (IOException e) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    Log.error(e1);
                    break;
                }
            }
        }
        return null;
    }

    @Override
    protected void done() {
        ObservationDialog.getInstance().getObservationImagePane().setupSources(parser);
    }

}