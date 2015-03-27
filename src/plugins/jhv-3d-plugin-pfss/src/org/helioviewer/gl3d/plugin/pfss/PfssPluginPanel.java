package org.helioviewer.gl3d.plugin.pfss;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.plugin.pfss.data.PfssCache;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodelplugin.overlay.OverlayPanel;

/**
 * Panel of Pfss-Plugin
 *
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPluginPanel extends OverlayPanel implements ActionListener, LayersListener {

    private static final long serialVersionUID = 1L;
    private static final String VOIDDATELABEL = "Model time: ****-**-**T**:**:**";
    private PfssCache pfssCache = null;
    private boolean showAgain = true;
    private boolean retry = false;
    // UI Components
    private final JButton visibleButton;
    private final JButton reloadButton;
    private JSpinner qualitySpinner;
    private JLabel dateLabel;
    public static PfssPluginPanel currentPluginPanel;

    /**
     * Default constructor
     *
     * */
    public PfssPluginPanel(PfssCache pfssCache) {
        currentPluginPanel = this;
        this.pfssCache = pfssCache;
        reloadButton = new JButton(new ImageIcon(PfssPlugin.getResourceUrl("/images/reload.png")));
        reloadButton.setToolTipText("Reload data");
        visibleButton = new JButton(new ImageIcon(PfssPlugin.getResourceUrl("/images/invisible_dm.png")));
        visibleButton.setToolTipText("Toggle visibility");
        // set up visual components
        initVisualComponents();
        // register as layers listener
        LayersModel.getSingletonInstance().addLayersListener(this);
        if (LayersModel.getSingletonInstance().getNumLayers() == 0) {
            enableComponents(this, false);
        } else {
            enableComponents(this, true);
        }
    }

    /**
     * Sets up the visual sub components and the visual part of the component
     * itself.
     * */
    private void initVisualComponents() {
        // set general appearance
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
        gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
        gridBagLayout.columnWeights = new double[] { 0.0, 1., 0.0, Double.MIN_VALUE };
        gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        setLayout(gridBagLayout);

        visibleButton.addActionListener(this);
        reloadButton.addActionListener(this);

        setEnabled(true);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.WEST;
        c0.insets = new Insets(0, 0, 5, 0);
        c0.gridx = 0;
        c0.gridy = 1;
        this.qualitySpinner = new JSpinner();
        this.qualitySpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), new Integer(8), new Integer(1)));

        this.qualitySpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                PfssSettings.qualityReduction = 8 - ((Integer) qualitySpinner.getValue()).intValue();
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(qualitySpinner);

        JPanel helpPanel = new JPanel();
        helpPanel.add(new JLabel("Level"));
        helpPanel.add(qualitySpinner);

        JCheckBox fixedColors = new JCheckBox("Fixed colors", false);
        fixedColors.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                PfssSettings.fixedColor = (e.getStateChange() == ItemEvent.SELECTED);
                Displayer.display();
            }
        });
        helpPanel.add(fixedColors);
        this.add(helpPanel, c0);

        this.dateLabel = new JLabel(VOIDDATELABEL);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.add(dateLabel, gbc);

        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.gridx = 2;
        gbc.gridy = 0;
        this.add(visibleButton, gbc);

        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.gridx = 3;
        gbc.gridy = 0;
        this.add(reloadButton, gbc);

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.setToolTipText("Click here to check the availability of PFSS data");
        availabilityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String url = Settings.getSingletonInstance().getProperty("availability.pfss.url");
                JHVGlobals.openURL(url);
            }
        });
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        this.add(availabilityButton, gbc);
    }

    @Override
    public void actionPerformed(ActionEvent act) {
        if (act.getSource().equals(visibleButton)) {
            if (pfssCache.isVisible()) {
                pfssCache.setVisible(false);
                visibleButton.setIcon(new ImageIcon(PfssPlugin.getResourceUrl("/images/invisible_dm.png")));
            } else {
                pfssCache.setVisible(true);
                visibleButton.setIcon(new ImageIcon(PfssPlugin.getResourceUrl("/images/visible_dm.png")));
            }
            Displayer.display();
        }

        if (act.getSource().equals(reloadButton)) {
            layerAdded(0);
        }
    }

    @Override
    public void setEnabled(boolean b) {
    }

    @Override
    public void activeLayerChanged(View view) {
    }

    public void enableComponents(Container container, boolean enable) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            component.setEnabled(enable);
            if (component instanceof Container) {
                enableComponents((Container) component, enable);
            }
        }
    }

    @Override
    public void layerAdded(int idx) {
        enableComponents(this, true);
        Date start = LayersModel.getSingletonInstance().getFirstDate();
        Date end = LayersModel.getSingletonInstance().getLastDate();
        if (start != null && end != null) {

            Calendar startCal = GregorianCalendar.getInstance();
            startCal.setTime(start);

            Calendar endCal = GregorianCalendar.getInstance();
            endCal.setTime(end);

            int startYear = startCal.get(Calendar.YEAR);
            int startMonth = startCal.get(Calendar.MONTH);

            int endYear = endCal.get(Calendar.YEAR);
            int endMonth = endCal.get(Calendar.MONTH);
            boolean run = true;

            while (run) {
                retry = false;
                URL data;
                try {
                    String m = (startMonth) < 9 ? "0" + (startMonth + 1) : (startMonth + 1) + "";
                    data = new URL(PfssSettings.baseUrl + startYear + "/" + m + "/list.txt");
                    BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));

                    String inputLine;
                    String[] splitted = null;
                    String url;
                    String[] date;
                    String[] time;
                    while ((inputLine = in.readLine()) != null) {
                        splitted = inputLine.split(" ");
                        url = splitted[1];
                        splitted = splitted[0].split("T");
                        date = splitted[0].split("-");
                        time = splitted[1].split(":");
                        pfssCache.addData(startYear, startMonth, Integer.parseInt(date[2]) * 1000000 + Integer.parseInt(time[0]) * 10000 + Integer.parseInt(time[1]) * 100 + Integer.parseInt(time[2]), url);
                    }
                    in.close();

                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    if (showAgain && pfssCache.isVisible()) {

                        Object[] options = { "Retry", "OK" };
                        String message = "Pfss-Data for " + startYear + "-" + (startMonth + 1) + " isn't available";
                        Log.error(message);
                        JCheckBox checkBox = new JCheckBox("Don't show this message again.");
                        checkBox.setEnabled(showAgain);
                        Object[] params = { message, checkBox };
                        int n = JOptionPane.showOptionDialog(this, params, "Pfss-Data", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                        showAgain = !checkBox.isSelected();
                        if (n == 0) {
                            retry = true;
                        }
                    }
                }

                if (!retry) {
                    pfssCache.preloadData(startYear, startMonth, startCal.get(Calendar.DAY_OF_MONTH) * 1000000 + startCal.get(Calendar.HOUR_OF_DAY) * 10000 + startCal.get(Calendar.MINUTE) * 100 + startCal.get(Calendar.SECOND));
                    if (startYear == endYear && startMonth == endMonth)
                        run = false;
                    else if (startYear == endYear && startMonth < endMonth) {
                        startMonth++;
                    } else if (startYear < endYear) {
                        if (startMonth == 11) {
                            startMonth = 1;
                            startYear++;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void layerChanged(int idx) {
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
        if (LayersModel.getSingletonInstance().getNumLayers() == 0) {
            enableComponents(this, false);
        }
    }

/*  to be fixed
    @Override
    public void timestampChanged(int idx) {

        // Not used anymore
        TimedMovieView masterView = LinkedMovieManager.getActiveInstance().getMasterMovie();
        if (masterView != null) {
            Date date = masterView.getCurrentFrameDateTime().getTime();
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(date);
            pfssCache.updateData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH) * 1000000 + cal.get(Calendar.HOUR_OF_DAY) * 10000 + cal.get(Calendar.MINUTE) * 100 + cal.get(Calendar.SECOND));
        }
    }
*/

    public void setDate(String date) {
        if (date == "" || date == null) {
            this.dateLabel.setText(VOIDDATELABEL);
        } else {
            this.dateLabel.setText("Model time: " + date);
        }
    }

}
