package org.helioviewer.jhv.plugins.pfss;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

@SuppressWarnings("serial")
class PfssOptionsPanel extends JPanel {

    private int detail;
    private boolean fixedColor;

    PfssOptionsPanel(int _detail, boolean _fixedColor) {
        detail = _detail;
        fixedColor = _fixedColor;
        setLayout(new GridBagLayout());

        JSpinner levelSpinner = new JSpinner(new SpinnerNumberModel(detail, 0, PfssSettings.MAX_DETAIL, 1));
        levelSpinner.addChangeListener(e -> {
            detail = (Integer) levelSpinner.getValue();
            Displayer.display();
        });
        WheelSupport.installMouseWheelSupport(levelSpinner);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.EAST;
        add(new JLabel("Detail", JLabel.RIGHT), c0);

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.WEST;
        add(levelSpinner, c0);

        JCheckBox fixedColors = new JCheckBox("Fixed colors", fixedColor);
        fixedColors.addActionListener(e -> {
            fixedColor = fixedColors.isSelected();
            Displayer.display();
        });

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.WEST;
        add(fixedColors, c0);

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.setToolTipText("Click here to check the PFSS data availability");
        availabilityButton.addActionListener(e -> JHVGlobals.openURL(PfssSettings.availabilityURL));

        c0.anchor = GridBagConstraints.EAST;
        c0.gridx = 3;
        add(availabilityButton, c0);

        ComponentUtils.smallVariant(this);
    }

    int getDetail() {
        return detail;
    }

    boolean getFixedColor() {
        return fixedColor;
    }

}
