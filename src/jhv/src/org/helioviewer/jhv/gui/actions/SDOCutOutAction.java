package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class SDOCutOutAction extends AbstractAction
{

    private static final String URL = "http://www.lmsal.com/get_aia_data/?";

    private static final double AIA_CDELT = 0.6;

    public SDOCutOutAction(boolean small, boolean useIcon) {
        super("SDO Cut-out", useIcon ? (small ? IconBank.getIcon(JHVIcon.SDO_CUTOUT) : IconBank.getIcon(JHVIcon.SDO_CUTOUT)) : null);
        putValue(SHORT_DESCRIPTION, "SDO cut-out service");
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        StringBuilder url = new StringBuilder(URL);
        JHVDate startdate = Layers.getStartDate();
        if (startdate != null) {
            String start = startdate.toString();
            String startTime = start.substring(11, 16);
            String startDate = start.substring(0, 10);
            url.append("startDate=" + startDate);
            url.append("&startTime=" + startTime);
        }

        JHVDate enddate = Layers.getEndDate();
        if (enddate != null) {
            String end = enddate.toString();
            String endDate = end.substring(0, 10);
            String endTime = end.substring(11, 16);
            url.append("&stopDate=" + endDate);
            url.append("&stopTime=" + endTime);
        }

        url.append("&wavelengths=");
        url.append(Layers.getSDOCutoutString());

        url.append("&cadence=" + ObservationDialog.getInstance().getObservationImagePane().getCadence());
        url.append("&cadenceUnits=s");

        ImageData imd = Layers.getActiveView().getImageLayer().getImageData();
        if (imd != null) {
            Region region = imd.getRegion();
            MetaData md = imd.getMetaData();
            Region fullregion = md.getPhysicalRegion();
            double arcsec_in_image = AIA_CDELT * 4096;
            double centr_x = region.llx + region.width / 2.;
            double centr_y = region.lly + region.height / 2.;
            double arc_w = arcsec_in_image / fullregion.width;
            double arc_h = arcsec_in_image / fullregion.height;

            url.append("&width=" + format_double(region.width * arc_w));
            url.append("&height=" + format_double(region.height * arc_h));
            url.append("&xCen=" + format_double(centr_x * arc_w - AIA_CDELT / 2.));
            url.append("&yCen=" + format_double(-centr_y * arc_h + AIA_CDELT / 2.));
        }

        JHVGlobals.openURL(url.toString());
    }

    private String format_double(double x) {
        return String.format("%.1f", x);
    }

}