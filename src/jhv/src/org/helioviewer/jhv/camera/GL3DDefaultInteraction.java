package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.display.Displayer;

/**
 * Default {@link GL3DInteraction} class . Default behavior includes camera
 * reset on double click.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DDefaultInteraction extends GL3DInteraction {

    protected GL3DDefaultInteraction(GL3DCamera camera) {
        super(camera);
    }

    @Override
    public void reset(GL3DCamera camera) {
    }

    @Override
    public void mouseClicked(MouseEvent e, GL3DCamera camera) {
        if (e.getClickCount() == 2) {
            camera.reset();
            Displayer.render();
        }
    }

    public void reset() {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e, GL3DCamera camera) {
        camera.zoom(e.getWheelRotation());
        Displayer.render();
    }

}
