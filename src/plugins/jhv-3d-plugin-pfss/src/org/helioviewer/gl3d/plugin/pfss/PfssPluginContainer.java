package org.helioviewer.gl3d.plugin.pfss;

import org.helioviewer.gl3d.plugin.pfss.data.PfssCache;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponentManager;

/**
 * Plugincontainer for Pfss
 *
 * @author Stefan Meier
 */
public class PfssPluginContainer extends OverlayContainer {

    private PfssCache pfssCache;
    private PfssPluginPanel pfssPluginPanel;
    private boolean builtin_mode = false;

    public PfssPluginContainer(boolean builtin_mode) {
        this.builtin_mode = builtin_mode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installOverlayImpl(OverlayView overlayView, OverlayControlComponentManager controlList) {
        pfssCache = new PfssCache();
        new PfssPlugin3dRenderable(pfssCache);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "PFSS Model " + (builtin_mode ? "Built-In Version" : "");
    }

    @Override
    public Class<? extends PhysicalRenderer> getOverlayClass() {
        return null;
    }

}
