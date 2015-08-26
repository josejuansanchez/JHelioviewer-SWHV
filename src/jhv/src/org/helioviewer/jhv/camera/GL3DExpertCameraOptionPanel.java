package org.helioviewer.jhv.camera;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.TimeTextField;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.layers.Layers;

@SuppressWarnings("serial")
public class GL3DExpertCameraOptionPanel extends GL3DCameraOptionPanel {

    private final JLabel loadedLabel;

    private JPanel addBeginDatePanel;
    private JHVCalendarDatePicker beginDatePicker;
    private TimeTextField beginTimePicker;

    private JPanel addEndDatePanel;
    private JHVCalendarDatePicker endDatePicker;
    private TimeTextField endTimePicker;

    private JSeparatorComboBox objectCombobox;

    private final GL3DExpertCamera camera;
    private JButton synchronizeWithLayersButton;
    private JButton synchronizeWithNowButton;
    private JButton synchronizeWithCurrentButton;

    private JPanel buttonPanel;

    private final JCheckBox exactDateCheckBox;

    private boolean firstComboChanged = false;

    public GL3DExpertCameraOptionPanel(final GL3DExpertCamera camera) {
        super();
        this.camera = camera;
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(new JSeparator(SwingConstants.HORIZONTAL), c);

        JPanel loadedLabelPanel = new JPanel();
        loadedLabelPanel.setLayout(new BoxLayout(loadedLabelPanel, BoxLayout.LINE_AXIS));

        loadedLabel = new JLabel("Status: Not loaded");
        loadedLabelPanel.add(loadedLabel);
        c.gridy = 1;
        add(loadedLabelPanel, c);
        c.gridy = 2;
        add(new JSeparator(SwingConstants.HORIZONTAL), c);
        c.gridy = 3;

        addObjectCombobox(c);
        exactDateCheckBox = new JCheckBox("Use active layer timestamps", true);
        c.gridy = 4;
        add(exactDateCheckBox, c);
        c.gridy = 5;
        addBeginDatePanel(c);
        c.gridy = 6;
        addEndDatePanel(c);
        addBeginDatePanel.setVisible(false);
        addEndDatePanel.setVisible(false);
        c.gridy = 7;

        addSyncButtons(c);
        buttonPanel.setVisible(false);
        exactDateCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                boolean selected = !exactDateCheckBox.isSelected();
                addBeginDatePanel.setVisible(selected);
                addEndDatePanel.setVisible(selected);
                buttonPanel.setVisible(selected);
                if (selected) {
                    setBeginTime(false);
                    setEndTime(true);
                } else
                    camera.setInterpolation(selected);

            }
        });
    }

    public void addSyncButtons(GridBagConstraints c) {
        this.synchronizeWithLayersButton = new JButton("Sync");
        this.synchronizeWithLayersButton.setToolTipText("Fill selected layer dates");
        this.synchronizeWithLayersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayer();
            }
        });
        this.synchronizeWithNowButton = new JButton("Now");
        this.synchronizeWithNowButton.setToolTipText("Fill twice current time");
        this.synchronizeWithNowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncBothLayerNow();
            }
        });
        this.synchronizeWithCurrentButton = new JButton("Current");
        this.synchronizeWithCurrentButton.setToolTipText("Fill twice selected layer time");
        this.synchronizeWithCurrentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayerCurrentTime();
            }
        });
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 3));
        this.synchronizeWithLayersButton.getMaximumSize().width = 15;
        buttonPanel.add(this.synchronizeWithLayersButton);

        this.synchronizeWithCurrentButton.getMaximumSize().width = 15;
        buttonPanel.add(this.synchronizeWithCurrentButton);

        this.synchronizeWithNowButton.getMaximumSize().width = 15;
        buttonPanel.add(this.synchronizeWithNowButton);

        add(buttonPanel, c);
    }

    @Override
    public void deactivate() {
    }

    private void addObjectCombobox(GridBagConstraints c) {
        objectCombobox = new JSeparatorComboBox();
        GL3DSpaceObject[] objectList = GL3DSpaceObject.getObjectList();
        for (int i = 0; i < objectList.length; i++) {
            objectCombobox.addItem(objectList[i]);
            if (i == GL3DSpaceObject.LINESEPSATS || i == GL3DSpaceObject.LINESEPPLANETS) {
                objectCombobox.addItem(new JSeparator());
            }
        }
        objectCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED && firstComboChanged) {
                    GL3DSpaceObject object = (GL3DSpaceObject) event.getItem();
                    if (object != null) {
                        camera.setObservingObject(object.getUrlName(), true);
                        revalidate();
                    }
                }
                if (event.getStateChange() == ItemEvent.SELECTED && !firstComboChanged) {
                    firstComboChanged = true;
                }
            }
        });
        add(objectCombobox, c);
        objectCombobox.setSelectedItem(GL3DSpaceObject.earth);
    }

    private void addBeginDatePanel(GridBagConstraints c) {
        beginDatePicker = new JHVCalendarDatePicker();
        beginDatePicker.getTextField().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                beginDatePicker.checkDateStringInTextField();
                setBeginTime(true);
            }
        });
        beginTimePicker = new TimeTextField();
        addBeginDatePanel = new JPanel();
        addBeginDatePanel.setLayout(new BoxLayout(addBeginDatePanel, BoxLayout.LINE_AXIS));

        JLabel beginDateLabel = new JLabel("Begin", JLabel.RIGHT);
        beginDateLabel.setPreferredSize(new Dimension(40, 0));

        addBeginDatePanel.add(beginDateLabel);

        JPanel beginDatetimePanel = new JPanel();
        beginDatetimePanel.setLayout(new GridLayout(0, 2));
        beginDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                setBeginTime(true);
            }
        });
        beginTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setBeginTime(true);
            }
        });
        addBeginDatePanel.add(beginDatePicker);
        addBeginDatePanel.add(beginTimePicker);
        addBeginDatePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(addBeginDatePanel, c);
    }

    private void setEndTime(boolean applyChanges) {
        Date dt = endTimePicker.getValue();
        Date end_date = new Date(endDatePicker.getDate().getTime() + dt.getTime());
        camera.setEndDate(end_date, applyChanges);
    }

    private void setBeginTime(boolean applyChanges) {
        Date dt = beginTimePicker.getValue();
        Date begin_date = new Date(beginDatePicker.getDate().getTime() + dt.getTime());
        camera.setBeginDate(begin_date, applyChanges);
    }

    private void syncWithLayer() {
        syncWithLayerBeginTime(false);
        syncWithLayerEndTime(true);
    }

    void syncWithLayerBeginTime(boolean applyChanges) {
        Date startDate = null;
        startDate = Layers.getFirstDate();
        if (startDate == null) {
            startDate = new Date(System.currentTimeMillis());
        }

        beginDatePicker.setDate(new Date(startDate.getTime() - startDate.getTime() % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeUtils.timeDateFormat.format(startDate));
        setBeginTime(applyChanges);
    }

    private void syncBothLayerNow() {
        Date nowDate = new Date(System.currentTimeMillis());
        Date syncDate = new Date(nowDate.getTime() - nowDate.getTime() % (60 * 60 * 24 * 1000));

        beginDatePicker.setDate(syncDate);
        beginTimePicker.setText(TimeUtils.timeDateFormat.format(nowDate));

        endDatePicker.setDate(syncDate);
        endTimePicker.setText(TimeUtils.timeDateFormat.format(nowDate));

        setBeginTime(false);
        setEndTime(true);
    }

    private void syncWithLayerCurrentTime() {
        Date currentDate = Displayer.getLastUpdatedTimestamp();
        Date syncDate = new Date(currentDate.getTime() - currentDate.getTime() % (60 * 60 * 24 * 1000));

        endDatePicker.setDate(syncDate);
        endTimePicker.setText(TimeUtils.timeDateFormat.format(currentDate));

        beginDatePicker.setDate(syncDate);
        beginTimePicker.setText(TimeUtils.timeDateFormat.format(currentDate));

        setBeginTime(false);
        setEndTime(true);
    }

    void syncWithLayerEndTime(boolean applyChanges) {
        Date endDate = null;
        endDate = Layers.getLastDate();
        if (endDate == null) {
            endDate = new Date(System.currentTimeMillis());
        }
        endDatePicker.setDate(new Date(endDate.getTime() - endDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeUtils.timeDateFormat.format(endDate));
        setEndTime(applyChanges);
    }

    private void addEndDatePanel(GridBagConstraints c) {
        endDatePicker = new JHVCalendarDatePicker();
        endDatePicker.getTextField().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                beginDatePicker.checkDateStringInTextField();
                setEndTime(true);
            }
        });
        endTimePicker = new TimeTextField();
        addEndDatePanel = new JPanel();
        addEndDatePanel.setLayout(new BoxLayout(addEndDatePanel, BoxLayout.LINE_AXIS));

        JLabel endDateLabel = new JLabel("End", JLabel.RIGHT);
        endDateLabel.setPreferredSize(new Dimension(40, 0));
        addEndDatePanel.add(endDateLabel);

        JPanel endDatetimePanel = new JPanel();
        endDatetimePanel.setLayout(new GridLayout(0, 2));
        endDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                setEndTime(true);
            }
        });
        endTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEndTime(true);
            }
        });
        addEndDatePanel.add(endDatePicker);
        addEndDatePanel.add(endTimePicker);
        addEndDatePanel.add(Box.createRigidArea(new Dimension(40, 0)));

        add(addEndDatePanel, c);
    }

    public void fireLoaded(String state) {
        String htmlstart = "<html><body style='width: 200px'>";
        String htmlend = "</body></html>";
        loadedLabel.setText(htmlstart + "Status: " + state + htmlend);
    }

}
