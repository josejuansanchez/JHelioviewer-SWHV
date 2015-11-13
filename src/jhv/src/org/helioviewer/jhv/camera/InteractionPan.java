package org.helioviewer.jhv.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.display.Displayer;

public class InteractionPan extends Interaction {

    private Point lastMousePoint;

    protected InteractionPan(Camera _camera) {
        super(_camera);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        lastMousePoint = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        int x = p.x - lastMousePoint.x;
        int y = p.y - lastMousePoint.y;
        double m = 2. * camera.getWidth() / Displayer.getViewport().getHeight();

        Vec2 pan = camera.getCurrentTranslation();
        pan.x += x * m;
        pan.y -= y * m;
        camera.setCurrentTranslation(pan);
        Displayer.render();

        lastMousePoint = p;
    }

}