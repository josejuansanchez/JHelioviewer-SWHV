package org.helioviewer.jhv.renderable.components.ImageLayer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

public class OpacityPanel extends AbstractFilterPanel implements ChangeListener, FilterDetails {

    private final JSlider opacitySlider;
    private final JLabel opacityLabel;
    private final JLabel title;

    public OpacityPanel() {
        title = new JLabel("Opacity", JLabel.RIGHT);

        opacitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        opacitySlider.setMinorTickSpacing(25);
        // opacitySlider.setPaintTicks(true);

        opacitySlider.addChangeListener(this);
        WheelSupport.installMouseWheelSupport(opacitySlider);

        opacityLabel = new JLabel("0%");
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        image.setOpacity(opacitySlider.getValue() / 100.f);
        opacityLabel.setText(opacitySlider.getValue() + "%");
        Displayer.display();
    }

    /**
     * @param opacity
     *            New opacity value. Must be within [0, 1]
     */
    private void setValue(float opacity) {
        opacitySlider.setValue((int) (opacity * 100.f));
    }

    @Override
    public void setGLImage(GLImage image) {
        super.setGLImage(image);
        if (image != null) {
            setValue(image.getOpacity());
        }
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Component getSlider() {
        return opacitySlider;
    }

    @Override
    public Component getValue() {
        return opacityLabel;
    }

}