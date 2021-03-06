package org.helioviewer.jhv.timelines.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandType;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;

@SuppressWarnings("serial")
public class TimelineDataPanel extends JPanel {

    private final JHVCalendarDatePicker calendarStartDate = new JHVCalendarDatePicker();
    private final JComboBox<String> comboBoxGroup = new JComboBox<>();
    private final JComboBox<BandType> comboBoxData = new JComboBox<>();

    private boolean userSet;

    public TimelineDataPanel() {
        setLayout(new GridBagLayout());

        comboBoxGroup.addActionListener(e -> updateGroupValues());
        calendarStartDate.addJHVCalendarListener(e -> userSet = true);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridy = 0;
        c.gridx = 0;
        add(new JLabel("Start date", JLabel.RIGHT), c);
        c.gridx = 1;
        add(calendarStartDate, c);

        c.gridy = 1;
        c.gridx = 0;
        add(new JLabel("Group", JLabel.RIGHT), c);
        c.gridx = 1;
        add(comboBoxGroup, c);

        c.gridy = 2;
        c.gridx = 0;
        add(new JLabel("Dataset", JLabel.RIGHT), c);
        c.gridx = 1;
        add(comboBoxData, c);
    }

    public void setupDatasets() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(BandType.getGroups());
        if (model.getSize() > 0) {
            comboBoxGroup.setModel(model);
            comboBoxGroup.setSelectedIndex(0);
        }
    }

    void updateGroupValues() {
        if (!userSet)
            calendarStartDate.setTime(Layers.getStartDate().milli);

        String selectedGroup = (String) comboBoxGroup.getSelectedItem();
        if (selectedGroup == null)
            return;

        DefaultComboBoxModel<BandType> model = (DefaultComboBoxModel<BandType>) comboBoxData.getModel();
        model.removeAllElements();

        HashSet<BandType> bandTypesInSelectorModel = new HashSet<>();
        for (TimelineRenderable el : Timelines.getModel().getAllLineDataSelectorElements()) {
            if (el instanceof Band)
                bandTypesInSelectorModel.add(((Band) el).getBandType());
        }

        for (BandType value : BandType.getBandTypes(selectedGroup)) {
            if (!bandTypesInSelectorModel.contains(value))
                model.addElement(value);
        }

        if (model.getSize() > 0)
            comboBoxData.setSelectedIndex(0);
    }

    void loadButtonPressed() {
        BandType bandType = (BandType) comboBoxData.getSelectedItem();
        if (bandType == null)
            return;

        Timelines.getModel().addLineData(new Band(bandType));

        long time = calendarStartDate.getTime();
        long movieStart = Layers.getStartDate().milli;
        long movieEnd = Layers.getEndDate().milli;
        if (time >= movieStart && time <= movieEnd) {
            DrawController.setSelectedInterval(movieStart, movieEnd);
        } else {
            long now = System.currentTimeMillis();
            DrawController.setSelectedInterval(Math.min(time, now), Math.min(time + 2 * TimeUtils.DAY_IN_MILLIS, now));
        }
    }

}
