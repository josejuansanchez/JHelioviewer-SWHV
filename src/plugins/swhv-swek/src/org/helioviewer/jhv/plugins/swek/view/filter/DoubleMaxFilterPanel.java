package org.helioviewer.jhv.plugins.swek.view.filter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.basegui.components.WheelSupport;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.download.SWEKOperand;
import org.helioviewer.jhv.plugins.swek.download.SWEKParam;

public class DoubleMaxFilterPanel extends AbstractFilterPanel {

    /** Maximum value spinner */
    private JSpinner maximumValueSpinner;

    public DoubleMaxFilterPanel(SWEKEventType eventType, SWEKParameter parameter) {
        super(eventType, parameter);
        // TODO Auto-generated constructor stub
    }

    /**
     * The UID.
     */
    private static final long serialVersionUID = -6593939617306920649L;

    @Override
    public void filter(boolean active) {
        if (active) {
            SWEKParam paramMax = new SWEKParam(parameter.getParameterName(), "" + maximumValueSpinner.getValue(),
                    SWEKOperand.SMALLER_OR_EQUAL);
            ArrayList<SWEKParam> params = new ArrayList<SWEKParam>();
            params.add(paramMax);
            filterManager.addFilter(eventType, parameter, params);

        } else {
            filterManager.removedFilter(eventType, parameter);
        }
    }

    @Override
    public JComponent initFilterComponents() {
        SpinnerModel maximumSpinnerModel = new SpinnerNumberModel(middleValue, min, max, stepSize);

        maximumValueSpinner = new JSpinner(maximumSpinnerModel);
        maximumValueSpinner.setEditor(new JSpinner.NumberEditor(maximumValueSpinner, "0.0000000"));
        WheelSupport.installMouseWheelSupport(maximumValueSpinner);
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Maximum Value:"), c);
        c.gridx = 1;
        p.add(maximumValueSpinner, c);
        return p;

    }

}
