package org.helioviewer.jhv.viewmodel.view;

import org.helioviewer.jhv.viewmodel.imagedata.ImageData;

public interface ViewDataHandler {

    public abstract void handleData(View view, ImageData imageData, ImageData miniviewData, ImageData prevData);

}