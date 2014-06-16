package org.helioviewer.gl3d.camera;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class GL3DFollowObjectCameraOptionPanel extends GL3DCameraOptionPanel implements GL3DFollowObjectCameraListener {
    private final JLabel loadedLabel;
    private final JLabel beginDateLabel;
    private JPanel beginDatetimePanel;
    JHVCalendarDatePicker beginDatePicker;
    TimeTextField beginTimePicker;

    private final JLabel endDateLabel;
    private JPanel endDatetimePanel;
    JHVCalendarDatePicker endDatePicker;
    TimeTextField endTimePicker;
    JComboBox objectCombobox;
    private final GL3DFollowObjectCamera camera;
    private final JLabel cameraTime;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final String DISABLED_TEXT = "----";
    private JPanel addBeginDatePanel;
    private JPanel addEndDatePanel;
    private JButton synchronizeWithLayersButton;
    private JButton synchronizeWithBeginButton;
    private JButton synchronizeWithEndButton;
    private JButton synchronizeWithNowButton;
    private JButton synchronizeWithCurrentButton;

    public GL3DFollowObjectCameraOptionPanel(GL3DFollowObjectCamera camera) {
        this.camera = camera;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        JPanel infoPanel = new JPanel(new GridLayout(2, 0));
        cameraTime = new JLabel("Camera date: " + DISABLED_TEXT);
        infoPanel.add(this.cameraTime);
        this.loadedLabel = new JLabel("Status: Not loaded");
        infoPanel.add(this.loadedLabel);
        add(infoPanel);
        add(new JSeparator(SwingConstants.HORIZONTAL));
        addObjectCombobox();
        beginDateLabel = new JLabel("Begin");
        beginDatePicker = new JHVCalendarDatePicker();
        beginTimePicker = new TimeTextField();
        addBeginDatePanel();
        endDateLabel = new JLabel("End");
        endDatePicker = new JHVCalendarDatePicker();
        endTimePicker = new TimeTextField();
        addEndDatePanel();
        addSyncButtons();
        this.syncWithLayerBeginTime();
        this.syncWithLayerEndTime();
        this.camera.addFollowObjectCameraListener(this);
    }

    public void addSyncButtons() {
        this.synchronizeWithLayersButton = new JButton("Sync");
        this.synchronizeWithLayersButton.setToolTipText("Fill the dates based on the current active layer.");
        this.synchronizeWithLayersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayer();
            }
        });
        this.synchronizeWithBeginButton = new JButton("Begin");
        this.synchronizeWithBeginButton.setToolTipText("Fill twice begin date of current active layer.");
        this.synchronizeWithBeginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncBothLayerBeginTime();
            }
        });
        this.synchronizeWithEndButton = new JButton("End");
        this.synchronizeWithEndButton.setToolTipText("Fill twice end date of current active layer.");
        this.synchronizeWithEndButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncBothLayerEndTime();
            }
        });
        this.synchronizeWithNowButton = new JButton("Now");
        this.synchronizeWithNowButton.setToolTipText("Fill twice now.");
        this.synchronizeWithNowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncBothLayerNow();
            }
        });
        this.synchronizeWithCurrentButton = new JButton("Current");
        this.synchronizeWithCurrentButton.setToolTipText("Fill twice current layer time.");
        this.synchronizeWithCurrentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayerCurrentTime();
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        this.synchronizeWithLayersButton.getMaximumSize().width = 15;
        //this.synchronizeWithLayersButton.setBorder(null);
        buttonPanel.add(this.synchronizeWithLayersButton);

        this.synchronizeWithBeginButton.getMaximumSize().width = 15;
        //this.synchronizeWithBeginButton.setBorder(null);
        //buttonPanel.add(this.synchronizeWithBeginButton);

        this.synchronizeWithCurrentButton.getMaximumSize().width = 15;
        //this.synchronizeWithCurrentButton.setBorder(null);
        buttonPanel.add(this.synchronizeWithCurrentButton);

        this.synchronizeWithEndButton.getMaximumSize().width = 15;
        this.synchronizeWithEndButton.setBorder(null);
        //buttonPanel.add(this.synchronizeWithEndButton);
        this.synchronizeWithNowButton.getMaximumSize().width = 15;
        buttonPanel.add(this.synchronizeWithNowButton);

        add(buttonPanel);
    }

    @Override
    public void deactivate() {
        this.camera.removeFollowObjectCameraListener(this);

        cameraTime.setText(DISABLED_TEXT);
    }

    private void addObjectCombobox() {
        objectCombobox = new JComboBox();
        GL3DSpaceObject[] objectList = GL3DSpaceObject.getObjectList();
        for (int i = 0; i < objectList.length; i++) {
            objectCombobox.addItem(objectList[i]);
        }
        objectCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    GL3DSpaceObject object = (GL3DSpaceObject) event.getItem();
                    if (object != null) {
                        camera.setObservingObject(object.getUrlName());
                        revalidate();
                    }
                }
            }
        });
        add(objectCombobox);
    }

    private void addBeginDatePanel() {
        addBeginDatePanel = new JPanel();
        addBeginDatePanel.setLayout(new BoxLayout(addBeginDatePanel, BoxLayout.LINE_AXIS));
        beginDateLabel.setPreferredSize(new Dimension(40, 0));

        addBeginDatePanel.add(beginDateLabel);
        beginDatetimePanel = new JPanel();
        beginDatetimePanel.setLayout(new GridLayout(0, 2));
        beginDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                setBeginTime();
                Displayer.getSingletonInstance().render();
            }
        });
        syncWithLayerBeginTime();
        beginTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setBeginTime();
                Displayer.getSingletonInstance().render();
            }
        });
        addBeginDatePanel.add(beginDatePicker);
        addBeginDatePanel.add(beginTimePicker);
        //addBeginDatePanel.add(beginDatetimePanel);
        addBeginDatePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(addBeginDatePanel);
    }

    private void setEndTime() {
        Date dt = endTimePicker.getValue();
        Date end_date = new Date(endDatePicker.getDate().getTime() + dt.getTime());
        camera.setEndDate(end_date);
    }

    private void setBeginTime() {
        Date dt = beginTimePicker.getValue();
        Date begin_date = new Date(beginDatePicker.getDate().getTime() + dt.getTime());
        camera.setBeginDate(begin_date);
    }

    private void syncWithLayer() {
        syncWithLayerBeginTime();
        syncWithLayerEndTime();
    }

    private void syncWithLayerBeginTime() {
        Date startDate = null;
        startDate = LayersModel.getSingletonInstance().getFirstDate();
        if (startDate == null) {
            startDate = new Date(System.currentTimeMillis());
        }
        beginDatePicker.setDate(new Date(startDate.getTime() - startDate.getTime() % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeTextField.formatter.format(startDate));
        setBeginTime();
    }

    private void syncBothLayerBeginTime() {
        Date startDate = null;
        startDate = LayersModel.getSingletonInstance().getFirstDate();
        if (startDate == null) {
            startDate = new Date(System.currentTimeMillis());
        }
        beginDatePicker.setDate(new Date(startDate.getTime() - startDate.getTime() % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeTextField.formatter.format(startDate));
        endDatePicker.setDate(new Date(startDate.getTime() - startDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(startDate));
        setBeginTime();
        setEndTime();
    }

    private void syncBothLayerEndTime() {
        Date endDate = null;
        endDate = LayersModel.getSingletonInstance().getLastDate();
        if (endDate == null) {
            endDate = new Date(System.currentTimeMillis());
        }
        beginDatePicker.setDate(new Date(endDate.getTime() - endDate.getTime() % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeTextField.formatter.format(endDate));
        endDatePicker.setDate(new Date(endDate.getTime() - endDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(endDate));
        setBeginTime();
        setEndTime();
    }

    private void syncBothLayerNow() {
        Date nowDate = new Date(System.currentTimeMillis());
        beginDatePicker.setDate(new Date(nowDate.getTime() - nowDate.getTime() % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeTextField.formatter.format(nowDate));
        endDatePicker.setDate(new Date(nowDate.getTime() - nowDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(nowDate));
        setBeginTime();
        setEndTime();
    }

    private void syncWithLayerCurrentTime() {
        ImmutableDateTime helpDate = null;
        helpDate = LayersModel.getSingletonInstance().getCurrentFrameTimestamp(LayersModel.getSingletonInstance().getActiveLayer());
        Date currentDate = helpDate.getTime();
        if (currentDate == null) {
            currentDate = new Date(System.currentTimeMillis());
        }
        endDatePicker.setDate(new Date(currentDate.getTime() - currentDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(currentDate));
        beginDatePicker.setDate(new Date(currentDate.getTime() - currentDate.getTime() % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeTextField.formatter.format(currentDate));
        setBeginTime();
        setEndTime();
    }

    private void syncWithLayerEndTime() {
        Date endDate = null;
        endDate = LayersModel.getSingletonInstance().getLastDate();
        if (endDate == null) {
            endDate = new Date(System.currentTimeMillis());
        }
        endDatePicker.setDate(new Date(endDate.getTime() - endDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(endDate));
        setEndTime();
    }

    private void addEndDatePanel() {
        addEndDatePanel = new JPanel();
        addEndDatePanel.setLayout(new BoxLayout(addEndDatePanel, BoxLayout.LINE_AXIS));
        endDateLabel.setPreferredSize(new Dimension(40, 0));
        addEndDatePanel.add(endDateLabel);
        endDatetimePanel = new JPanel();
        endDatetimePanel.setLayout(new GridLayout(0, 2));
        endDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                setEndTime();
                Displayer.getSingletonInstance().render();
            }
        });
        syncWithLayerEndTime();
        endTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEndTime();
                Displayer.getSingletonInstance().render();
            }
        });
        addEndDatePanel.add(endDatePicker);
        addEndDatePanel.add(endTimePicker);
        addEndDatePanel.add(Box.createRigidArea(new Dimension(40, 0)));

        add(addEndDatePanel);
    }

    @Override
    public void fireLoaded(String state) {
        this.loadedLabel.setText("Status: " + state);
    }

    @Override
    public void fireCameraTime(Date cameraDate) {
        this.cameraTime.setText("Camera date: " + format.format(cameraDate));
    }

    @Override
    public void fireNewDate(Date date) {
        this.cameraTime.setText(this.format.format(date));
    }
}
