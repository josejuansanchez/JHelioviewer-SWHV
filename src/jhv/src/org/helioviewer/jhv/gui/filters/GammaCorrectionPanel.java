package org.helioviewer.jhv.gui.filters;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.viewmodel.view.AbstractView;

/**
 * Panel containing a slider for changing the gamma value of the image.
 *
 * <p>
 * To be able to reset the gamma value to 1.0, the slider snaps to 1.0 if it
 * close to it.
 *
 * @author Markus Langenberg
 */
public class GammaCorrectionPanel extends AbstractFilterPanel implements ChangeListener, MouseListener, FilterAlignmentDetails {

    private static double factor = 0.01 * Math.log(10);

    private final JSlider gammaSlider;
    private final JLabel title;
    private final JLabel gammaLabel;

    public GammaCorrectionPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        title = new JLabel("Gamma");
        title.setHorizontalAlignment(JLabel.RIGHT);
        add(title);

        gammaSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
        gammaSlider.setMajorTickSpacing(25 * 2); // twice wider
        gammaSlider.setPaintTicks(true);

        gammaSlider.addChangeListener(this);
        gammaSlider.addMouseListener(this);
        WheelSupport.installMouseWheelSupport(gammaSlider);
        add(gammaSlider);

        gammaLabel = new JLabel("1.0");
        add(gammaLabel);
    }

    /**
     * Sets the gamma value of the image.
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        int sliderValue = gammaSlider.getValue();

        double gamma = Math.exp(sliderValue * factor);
        jp2view.setGamma((float) gamma);

        String text = Double.toString(Math.round(gamma * 10.0) * 0.1);
        if (sliderValue == 100) {
            text = text.substring(0, 4);
        } else {
            text = text.substring(0, 3);
        }
        gammaLabel.setText(text);
        Displayer.display();
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    @Override
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    @Override
    public void mousePressed(MouseEvent e) {
    }

    /**
     * {@inheritDoc} In this case, snaps the slider to 1.0 if it is close to it.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        int sliderValue = gammaSlider.getValue();

        if (sliderValue <= 5 && sliderValue >= -5 && sliderValue != 0) {
            gammaSlider.setValue(0);
        }
    }

    @Override
    public int getDetails() {
        return FilterAlignmentDetails.POSITION_GAMMA;
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        gammaSlider.setEnabled(enabled);
        gammaLabel.setEnabled(enabled);
        title.setEnabled(enabled);
    }

    /**
     * Sets the panel values.
     *
     * This may be useful, if the values are changed from another source than
     * the panel itself.
     *
     * @param gamma
     *            New gamma value, must be within [0.1, 10]
     */
    void setValue(float gamma) {
        gammaSlider.setValue((int) (Math.log(gamma) / factor));
    }

    @Override
    public void setJP2View(AbstractView jp2view) {
        super.setJP2View(jp2view);
        setValue(jp2view.getGamma());
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Component getSlider() {
        return gammaSlider;
    }

    @Override
    public Component getValue() {
        return gammaLabel;
    }

}
