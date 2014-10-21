package org.helioviewer.gl3d.scenegraph.visuals;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;

public class GL3DArrow extends GL3DGroup {

    public GL3DArrow(double radius, double offset, double length, int detail, GL3DVec4f color) {
        this("Arrow", radius, offset, length, detail, color);
    }

    public GL3DArrow(String name, double radius, double offset, double length, int detail, GL3DVec4f color) {
        super(name);
        GL3DShape cylinder = new GL3DCylinder(radius / 2, length / 2, detail, color);
        cylinder.modelView().setTranslation(0.0, 0, offset + length / 4);
        addNode(cylinder);

        GL3DShape cone = new GL3DCone(radius, length / 2, detail, color);
        cone.modelView().setTranslation(0, 0, offset + length / 2);
        addNode(cone);
    }

    @Override
    public void shapeDraw(GL3DState state) {
        GL2 gl = state.get().gl;
        gl.glDisable(GL2.GL_LIGHTING);
        super.shapeDraw(state);
        gl.glEnable(GL2.GL_LIGHTING);
    }
}
