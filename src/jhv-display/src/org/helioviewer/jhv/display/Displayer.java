package org.helioviewer.jhv.display;

import java.util.ArrayList;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;

public class Displayer implements JHVEventHighlightListener {
    private final static Displayer instance = new Displayer();

    public static Displayer getSingletonInstance() {
        return instance;
    }

    private ArrayList<DisplayListener> listeners = new ArrayList<DisplayListener>();
    private final ArrayList<RenderListener> renderListeners = new ArrayList<RenderListener>();

    private boolean displaying = false;


    public void addListener(final DisplayListener listener) {
        listeners.add(listener);
    }

    public void removeListener(final DisplayListener renderListener) {
        synchronized (renderListener) {
            listeners.remove(renderListener);
        }
    }

    public void addRenderListener(final RenderListener renderListener) {
        synchronized (renderListener) {
            renderListeners.add(renderListener);
        }
    }

    public void removeRenderListener(final RenderListener listener) {
        synchronized (renderListeners) {
            renderListeners.remove(listener);
        }
    }

    public void render() {
        if (!displaying) {
            displaying = true;
            synchronized (renderListeners) {
                for (final RenderListener renderListener : renderListeners) {
                    renderListener.render();
                }
            }
            displaying = false;
        }
    }

    public void display() {
        for (final DisplayListener listener : listeners) {
            listener.display();
        }
    }

    public void removeListeners() {
        this.listeners = new ArrayList<DisplayListener>();
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        this.display();
    }

}
