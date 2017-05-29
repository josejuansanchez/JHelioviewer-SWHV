package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.viewmodel.view.View;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class RenderableMiniview extends AbstractRenderable implements LayersListener {

    private final RenderableMiniviewOptionsPanel optionsPanel;

    private Viewport miniViewport = new Viewport(0, 0, 0, 100, 100);

    private static final int MIN_SCALE = 5;
    private static final int MAX_SCALE = 15;
    private int scale = 10;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("scale", scale);
    }

    public RenderableMiniview(JSONObject jo) {
        scale = MathUtils.clip(jo.optInt("scale", scale), MIN_SCALE, MAX_SCALE);
        optionsPanel = new RenderableMiniviewOptionsPanel(this, scale, MIN_SCALE, MAX_SCALE);
        reshapeViewport();
    }

    public RenderableMiniview() {
        optionsPanel = new RenderableMiniviewOptionsPanel(this, scale, MIN_SCALE, MAX_SCALE);
        setVisible(true);
    }

    void setScale(int _scale) {
        scale = _scale;
    }

    public void reshapeViewport() {
        int vpw = Displayer.fullViewport.width;
        int offset = (int) (vpw * 0.01);
        int size = (int) (vpw * 0.01 * scale);

        miniViewport = new Viewport(0, offset, offset, size, size);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
    }

    public static void renderBackground(Camera camera, Viewport vp, GL2 gl) {
        Mat4 cameraMatrix = camera.getViewpoint().orientation.toMatrix();
        gl.glDepthRange(0, 0);
        gl.glPushMatrix();
        {
            gl.glMultMatrixd(cameraMatrix.transpose().m, 0);

            gl.glColor4f(0, 1, 0, 0.2f);
            GLHelper.drawRectangleFront(gl, -30, -30, 60, 60);

            gl.glColor4f(1, 0, 0, 0.2f);
            GLHelper.drawCircleFront(gl, 0, 0, 1, 100);
        }
        gl.glPopMatrix();
        gl.glDepthRange(0, 1);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Miniview";
    }

    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
        Layers.addLayersListener(this);
    }

    @Override
    public void dispose(GL2 gl) {
        Layers.removeLayersListener(this);
    }

    @Override
    public void activeLayerChanged(View view) {
        CameraHelper.zoomToFit(Displayer.getMiniCamera());
    }

    public Viewport getViewport() {
        return miniViewport;
    }

}
