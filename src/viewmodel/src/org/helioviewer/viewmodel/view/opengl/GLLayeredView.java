package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.AbstractLayeredView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.opengl.shader.ShaderFactory;

/**
 * Implementation of LayeredView for rendering in OpenGL mode.
 *
 * <p>
 * This class manages different layers in OpenGL by branching the renderGL calls
 * as well as the calls for building shaders.
 *
 * <p>
 * For further information about the role of the LayeredView within the view
 * chain, see {@link org.helioviewer.viewmodel.view.LayeredView}
 *
 * @author Markus Langenberg
 *
 */
public class GLLayeredView extends AbstractLayeredView implements GLView {
    private final int shaderID = -1;

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderGL(GL2 gl, boolean nextView) {
        layerLock.lock();
        {
            for (View v : layers) {
                Layer layer = viewLookup.get(v);
                if (!layer.visibility) {
                    continue;
                }
                // if layer is GLView, go on, otherwise render now
                if (v instanceof GLView) {
                    ShaderFactory.filter(gl);
                    ((GLView) v).renderGL(gl, true);
                } else {
                    GLTextureHelper.renderImageDataToScreen(gl, layer.regionView.getRegion(), layer.subimageDataView.getSubimageData(), v.getAdapter(JHVJP2View.class).tex);
                }
            }
        }
        layerLock.unlock();
    }

    /**
     * {@inheritDoc}
     *
     * In this case, it does nothing, since for OpenGL views, the rendering
     * takes place in {@link #renderGL(GL2)}.
     */

    @Override
    protected void redrawBufferImpl() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean recalculateRegionsAndViewports(ChangeEvent event) {
        boolean changed = false;
        if (region == null && metaData != null) {
            region = StaticRegion.createAdaptedRegion(metaData.getPhysicalRectangle());
        }

        if (viewport != null && region != null) {
            viewportImageSize = ViewHelper.calculateViewportImageSize(viewport, region);

            layerLock.lock();
            for (Layer layer : viewLookup.values()) {
                Region layerRegion = ViewHelper.cropInnerRegionToOuterRegion(layer.metaDataView.getMetaData().getPhysicalRegion(), region);
                changed |= layer.regionView.setRegion(layerRegion, event);
                changed |= layer.viewportView.setViewport(ViewHelper.calculateInnerViewport(layerRegion, region, viewportImageSize), event);
            }
            layerLock.unlock();
        }

        return changed;
    }

}
