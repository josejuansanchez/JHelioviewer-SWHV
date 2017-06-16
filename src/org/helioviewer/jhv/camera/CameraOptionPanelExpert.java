package org.helioviewer.jhv.camera;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;

import org.helioviewer.jhv.camera.object.SpaceObjectContainer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.DateTimePanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.view.View;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class CameraOptionPanelExpert extends CameraOptionPanel implements LayersListener {

    private final JCheckBox exactDateCheckBox = new JCheckBox("Use master layer time interval", true);
    private final DateTimePanel startDateTimePanel = new DateTimePanel("Start");
    private final DateTimePanel endDateTimePanel = new DateTimePanel("End");

    private final SpaceObjectContainer container;

    CameraOptionPanelExpert(JSONObject jo, UpdateViewpoint uv, String frame, boolean exclusive) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;

        c.gridy = 0;
        JSONArray ja = jo == null ? null : jo.optJSONArray("objects");
        container = new SpaceObjectContainer(ja, uv, frame, exclusive);
        add(container, c);

        c.gridy = 1;
        add(exactDateCheckBox, c);
        c.gridy = 2;
        startDateTimePanel.addListener(e -> request());
        startDateTimePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(startDateTimePanel, c);
        c.gridy = 3;
        endDateTimePanel.addListener(e -> request());
        endDateTimePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(endDateTimePanel, c);

        startDateTimePanel.setVisible(false);
        endDateTimePanel.setVisible(false);
        exactDateCheckBox.addActionListener(e -> {
            boolean selected = !exactDateCheckBox.isSelected();
            startDateTimePanel.setVisible(selected);
            endDateTimePanel.setVisible(selected);
            syncWithLayer();
        });

        ComponentUtils.smallVariant(this);
    }

    @Override
    void activate() {
        Layers.addLayersListener(this);
        syncWithLayer();
    }

    @Override
    void deactivate() {
        Layers.removeLayersListener(this);
    }

    @Override
    public void activeLayerChanged(View view) {
        syncWithLayer();
    }

    JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("objects", container.toJson());
        return jo;
    }

    private void syncWithLayer() {
        if (!exactDateCheckBox.isSelected())
            return;

        View view = Layers.getActiveView();
        if (view == null)
            return;

        startDateTimePanel.setTime(view.getFirstTime().milli);
        endDateTimePanel.setTime(view.getLastTime().milli);
        request();
    }

    private void request() {
        container.loadSelected(startDateTimePanel.getTime(), endDateTimePanel.getTime());
    }

}
