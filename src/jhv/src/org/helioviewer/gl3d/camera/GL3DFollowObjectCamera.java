package org.helioviewer.gl3d.camera;

import java.util.ArrayList;
import java.util.Date;

import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.cache.DateTimeCache;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class GL3DFollowObjectCamera extends GL3DSolarRotationTrackingTrackballCamera implements ViewListener, GL3DPositionLoadingListener, LayersListener {

    private Date currentDate = null;
    private double currentRotation = 0.0;
    private long timediff;
    private final ArrayList<GL3DFollowObjectCameraListener> followObjectCameraListeners = new ArrayList<GL3DFollowObjectCameraListener>();
    GL3DCameraFOV cameraFOV;
    private final GL3DPositionLoading positionLoading;
    private double FOVangle;
    private double currentL = 0.;
    private double currentB = 0.;
    private double currentDistance = Constants.SunMeanDistanceToEarth / Constants.SunRadius;

    private long currentCameraTime;
    private double lratio;
    private boolean interpolation;

    public GL3DFollowObjectCamera(GL3DSceneGraphView sceneGraphView) {
        super(sceneGraphView);
        positionLoading = new GL3DPositionLoading();
        positionLoading.addListener(this);
        cameraFOV = new GL3DCameraFOV(1., 1.);
        sceneGraphView.getRoot().addNode(cameraFOV);
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        getSceneGraphView().addViewListener(this);
    }

    @Override
    public void activate() {
        super.activate();
        this.cameraFOV.getDrawBits().off(Bit.Hidden);
    };

    @Override
    public void deactivate() {
        super.deactivate();
        getSceneGraphView().removeViewListener(this);
        this.cameraFOV.getDrawBits().on(Bit.Hidden);
    };

    @Override
    public String getName() {
        return "Follow Object Camera";
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (this.positionLoading.isLoaded()) {
            TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
            if (timestampReason != null && LayersModel.getSingletonInstance().getActiveView() != null) {
                boolean isjp2 = LayersModel.getSingletonInstance().getActiveView().getAdapter(JHVJP2View.class).getClass() == JHVJP2View.class;
                if (isjp2 || ((timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView()))) {
                    currentDate = timestampReason.getNewDateTime().getTime();
                    //Layer times
                    long t1 = LayersModel.getSingletonInstance().getFirstDate().getTime();
                    long t2 = LayersModel.getSingletonInstance().getLastDate().getTime();
                    //Camera times
                    long t3 = this.positionLoading.getBeginDate().getTime();
                    long t4 = this.positionLoading.getEndDate().getTime();
                    if (interpolation) {
                        if (t4 != t3) {
                            currentCameraTime = (long) ((t3 + 1. * (t4 - t3) * (timestampReason.getNewDateTime().getMillis() - t1) / (t2 - t1)));
                        } else {
                            currentCameraTime = t4;
                        }
                    } else {
                        currentCameraTime = timestampReason.getNewDateTime().getMillis();
                    }
                    this.setTime(currentCameraTime);

                    this.fireCameratTime(new Date(currentCameraTime));

                    GL3DVec3d position = this.positionLoading.getInterpolatedPosition(currentCameraTime);
                    if (position != null) {
                        currentL = position.y;
                        currentB = -position.z;
                        currentDistance = position.x;
                        GL3DVec3d initPosition = this.positionLoading.getInterpolatedPosition(t3);
                        lratio = initPosition.x / position.x;

                        updateRotation();
                        setFOV(currentDistance * Math.tan(FOVangle));
                    }
                }
            }

        }
    }

    private void setFOV(double scale) {
        this.cameraFOV.scale(scale);
        this.cameraFOV.setAngles(this.currentB, this.currentRotation);
    }

    private void fireCameratTime(Date currentCameraTime) {
        synchronized (followObjectCameraListeners) {
            for (GL3DFollowObjectCameraListener listener : followObjectCameraListeners) {
                listener.fireCameraTime(currentCameraTime);
            }
        }
    }

    public void updateRotation() {
        if (this.positionLoading.isLoaded() && currentDate != null) {
            this.currentRotation = (-currentL + Astronomy.getL0Radians(new Date(currentCameraTime))) % (Math.PI * 2.0);
            GL3DQuatd newRotation = GL3DQuatd.createRotation(0., new GL3DVec3d(0, 1, 0));
            newRotation.rotate(GL3DQuatd.createRotation(-currentB, new GL3DVec3d(1, 0, 0)));
            newRotation.rotate(GL3DQuatd.createRotation(this.currentRotation, new GL3DVec3d(0, 1, 0)));
            this.setLocalRotation(newRotation);
            this.setZTranslation(-currentDistance);
            this.updateCameraTransformation();
        }
    }

    public void addFollowObjectCameraListener(GL3DFollowObjectCameraListener listener) {
        synchronized (followObjectCameraListeners) {
            this.followObjectCameraListeners.add(listener);
        }
    }

    public void removeFollowObjectCameraListener(GL3DFollowObjectCameraListener listener) {
        synchronized (followObjectCameraListeners) {
            this.followObjectCameraListeners.remove(listener);
        }
    }

    @Override
    public void fireNewLoaded(String state) {
        synchronized (followObjectCameraListeners) {
            for (GL3DFollowObjectCameraListener listener : followObjectCameraListeners) {
                listener.fireLoaded(state);
            }
        }
    }

    @Override
    public void fireNewDate() {
        synchronized (followObjectCameraListeners) {
            for (GL3DFollowObjectCameraListener listener : followObjectCameraListeners) {
                listener.fireNewDate(new Date(this.currentCameraTime));
            }
        }
    }

    public void setBeginDate(Date date, boolean applyChanges) {
        this.positionLoading.setBeginDate(date, applyChanges);
    }

    public void setEndDate(Date date, boolean applyChanges) {
        this.positionLoading.setEndDate(date, applyChanges);
    }

    public void setObservingObject(String object, boolean applyChanges) {
        this.positionLoading.setObserver(object, applyChanges);
    }

    public void setFOVangleDegrees(double fovAngle) {
        this.FOVangle = fovAngle * Math.PI / 180.0;

    }

    public void setInterpolation(boolean interpolation) {
        this.interpolation = interpolation;
    }

    @Override
    public void layerAdded(int idx) {
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
    }

    @Override
    public void layerChanged(int idx) {
    }

    @Override
    public void activeLayerChanged(int idx) {
        if (!this.interpolation) {
            View nextView = LayersModel.getSingletonInstance().getLayer(idx);
            JHVJPXView jpxView = nextView.getAdapter(JHVJPXView.class);
            if (jpxView != null) {
                DateTimeCache dtc = jpxView.getDateTimeCache();
                Date beginDate = null;
                Date endDate = null;
                for (int frame = 0; frame < jpxView.getMaximumFrameNumber(); frame++) {
                    ImmutableDateTime date = dtc.getDateTime(frame);
                    if (beginDate == null || date.getTime().getTime() < beginDate.getTime()) {
                        beginDate = date.getTime();
                    }
                    if (endDate == null || date.getTime().getTime() > endDate.getTime()) {
                        endDate = date.getTime();
                    }
                }
                this.positionLoading.setBeginDate(beginDate, false);
                this.positionLoading.setEndDate(endDate, true);
            }
        }
    }

    @Override
    public void viewportGeometryChanged() {
    }

    @Override
    public void timestampChanged(int idx) {
    }

    @Override
    public void subImageDataChanged() {
    }

    @Override
    public void layerDownloaded(int idx) {
    }

    public Date getBeginTime() {
        return this.positionLoading.getBeginDate();
    }

    public Date getEndTime() {
        return this.positionLoading.getEndDate();
    }
}
