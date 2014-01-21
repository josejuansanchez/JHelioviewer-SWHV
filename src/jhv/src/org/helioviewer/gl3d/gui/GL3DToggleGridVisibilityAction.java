package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DSolarRotationTrackingTrackballCamera;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.gui.ImageViewerGui;

/**
 * Action that enables the Solar Rotation Tracking, which ultimately changes the
 * current {@link GL3DCamera} to the
 * {@link GL3DSolarRotationTrackingTrackballCamera}
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DToggleGridVisibilityAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public GL3DToggleGridVisibilityAction() {
        super("Grid");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        GL3DSceneGraphView sceneGraph = ImageViewerGui.getSingletonInstance().getMainView().getAdapter(GL3DSceneGraphView.class);
        if (sceneGraph != null) {
            sceneGraph.toggleGridVisibility();
        }
    }

}
