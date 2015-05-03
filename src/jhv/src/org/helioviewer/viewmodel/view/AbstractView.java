package org.helioviewer.viewmodel.view;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.base.Region;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.renderable.RenderableImageLayer;
import org.helioviewer.viewmodel.imagedata.ColorMask;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imageformat.ImageFormat;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Int32ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Short16ImageTransport;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.GLInfo;
import org.helioviewer.viewmodel.view.opengl.GLSLShader;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;

import com.jogamp.opengl.GL2;

public abstract class AbstractView implements View {

    private RenderableImageLayer imageLayer;

    protected ImageData imageData;
    public GLTextureHelper.GLTexture tex = new GLTextureHelper.GLTexture();

    private float contrast = 0f;
    private float gamma = 1f;
    private float opacity = 1f;
    private float sharpenWeighting = 0f;
    protected static final LUT gray = LUT.getStandardList().get("Gray");

    protected LUT lut = gray;
    private LUT lastLut;

    private boolean invertLUT = false;
    private boolean lastInverted = false;
    private IntBuffer lutBuffer;

    private boolean lutChanged = true;

    private ColorMask colorMask = new ColorMask(true, true, true);
    private final GLTextureHelper.GLTexture lutTex = new GLTextureHelper.GLTexture();
    private final GLTextureHelper.GLTexture diffTex = new GLTextureHelper.GLTexture();

    private boolean differenceMode = false;
    private boolean baseDifferenceMode = false;
    private boolean baseDifferenceNoRot = false;
    private boolean runningDifferenceNoRot = false;
    private float truncation = 1f - 0.8f;

    public void setContrast(float contrast) {
        this.contrast = contrast;
    }

    public void setGamma(float gamma) {
        this.gamma = gamma;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public void setWeighting(float sharpenWeighting) {
        this.sharpenWeighting = sharpenWeighting;
    }

    public void setLUT(LUT newLUT, boolean invert) {
        if (newLUT == null || (lut == newLUT && invertLUT == invert)) {
            return;
        }
        lut = newLUT;
        invertLUT = invert;
        this.lutChanged = true;
    }

    public void applyFilters(GL2 gl) {
        copyScreenToTexture(gl);
        applyRunningDifferenceGL(gl);

        GLSLShader.colorMask = colorMask;
        GLSLShader.setContrast(contrast);
        GLSLShader.setGamma(gamma);
        GLSLShader.setAlpha(opacity);

        float pixelWidth, pixelHeight;
        if (imageData != null) {
            pixelWidth = 1.0f / imageData.getWidth();
            pixelHeight = 1.0f / imageData.getHeight();
        } else {
            pixelWidth = 1.0f / 512;
            pixelHeight = 1.0f / 512;
        }
        GLSLShader.setFactors(sharpenWeighting, pixelWidth, pixelHeight, 1f);
        applyGLLUT(gl);

        if (imageData != null) {
            int width = imageData.getWidth();
            int height = imageData.getHeight();
            if (width <= GLInfo.maxTextureSize && height <= GLInfo.maxTextureSize) {
                int bitsPerPixel = imageData.getImageTransport().getNumBitsPerPixel();
                Buffer buffer;

                switch (bitsPerPixel) {
                case 8:
                    buffer = ByteBuffer.wrap(((Byte8ImageTransport) imageData.getImageTransport()).getByte8PixelData());
                    break;
                case 16:
                    buffer = ShortBuffer.wrap(((Short16ImageTransport) imageData.getImageTransport()).getShort16PixelData());
                    break;
                case 32:
                    buffer = IntBuffer.wrap(((Int32ImageTransport) imageData.getImageTransport()).getInt32PixelData());
                    break;
                default:
                    buffer = null;
                }

                gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
                gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
                gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, width);
                gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, bitsPerPixel >> 3);

                ImageFormat imageFormat = imageData.getImageFormat();
                int inputGLFormat = GLTextureHelper.mapImageFormatToInputGLFormat(imageFormat);
                int bppGLType = GLTextureHelper.mapBitsPerPixelToGLType(bitsPerPixel);

                gl.glBindTexture(GL2.GL_TEXTURE_2D, tex.get(gl));

                if (width != previousWidth || height != previousHeight) {
                    int internalGLFormat = GLTextureHelper.mapImageFormatToInternalGLFormat(imageFormat);
                    gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, internalGLFormat, width, height, 0, inputGLFormat, bppGLType, null);
                    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
                    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
                    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
                    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
                    previousWidth = width;
                    previousHeight = height;
                }
                gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, width, height, inputGLFormat, bppGLType, buffer);
            }
        }
    }

    private int previousWidth = -1;
    private int previousHeight = -1;

    public void setColorMask(boolean redColormask, boolean greenColormask, boolean blueColormask) {
        colorMask = new ColorMask(redColormask, greenColormask, blueColormask);
    }

    public void setStartLUT() {
        lut = gray;
    }

    private void applyGLLUT(GL2 gl) {
        gl.glActiveTexture(GL2.GL_TEXTURE1);

        LUT currlut;

        if ((this instanceof JHVJPXView) && ((JHVJPXView) this).getDifferenceMode()) {
            currlut = gray;
        } else {
            currlut = lut;
        }

        gl.glBindTexture(GL2.GL_TEXTURE_1D, lutTex.get(gl));

        if (this.lutChanged || lastLut != currlut || invertLUT != lastInverted) {
            int[] intLUT;

            if (invertLUT) {
                int[] sourceLUT = currlut.getLut8();
                intLUT = new int[sourceLUT.length];

                int offset = sourceLUT.length - 1;
                for (int i = 0; i < sourceLUT.length / 2; i++) {
                    intLUT[i] = sourceLUT[offset - i];
                    intLUT[offset - i] = sourceLUT[i];
                }
            } else {
                intLUT = currlut.getLut8();
            }

            lutBuffer = IntBuffer.wrap(intLUT);
            lastLut = currlut;
            lastInverted = invertLUT;

            gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
            gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
            gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
            gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 4);

            gl.glTexImage1D(GL2.GL_TEXTURE_1D, 0, GL2.GL_RGBA, lutBuffer.limit(), 0, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, lutBuffer);
            gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
            gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
            gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        }
        this.lutChanged = false;

        gl.glActiveTexture(GL2.GL_TEXTURE0);
    }

    private void applyRunningDifferenceGL(GL2 gl) {
        if (this.baseDifferenceMode || this.differenceMode) {
            if (this.baseDifferenceMode) {
                if (this.baseDifferenceNoRot) {
                    GLSLShader.setIsDifference(GLSLShader.BASEDIFFERENCE_NO_ROT);
                } else {
                    GLSLShader.setIsDifference(GLSLShader.BASEDIFFERENCE_ROT);
                }
            } else {
                if (this.runningDifferenceNoRot) {
                    GLSLShader.setIsDifference(GLSLShader.RUNNINGDIFFERENCE_NO_ROT);
                } else {
                    GLSLShader.setIsDifference(GLSLShader.RUNNINGDIFFERENCE_ROT);
                }
            }

            ImageData previousFrame;
            if (!this.baseDifferenceMode) {
                previousFrame = this.getPreviousImageData();
            } else {
                previousFrame = this.getBaseDifferenceImageData();
            }
            if (this.differenceMode && this.imageData != previousFrame && previousFrame != null) {
                GLSLShader.setTruncationValue(this.truncation);
                gl.glActiveTexture(GL2.GL_TEXTURE2);
                GLTextureHelper.moveImageDataToGLTexture(gl, previousFrame, 0, 0, previousFrame.getWidth(), previousFrame.getHeight(), diffTex);
                gl.glActiveTexture(GL2.GL_TEXTURE0);
            }
        } else {
            GLSLShader.setIsDifference(GLSLShader.NODIFFERENCE);
        }
    }

    public void setDifferenceMode(boolean differenceMode) {
        this.differenceMode = differenceMode;
    }

    @Override
    public boolean getDifferenceMode() {
        return this.differenceMode;
    }

    public void setBaseDifferenceMode(boolean selected) {
        this.baseDifferenceMode = selected;
    }

    @Override
    public boolean getBaseDifferenceMode() {
        return baseDifferenceMode;
    }

    public void setBaseDifferenceNoRot(boolean baseDifferenceNoRot) {
        this.baseDifferenceNoRot = baseDifferenceNoRot;
    }

    public void setRunDiffNoRot(boolean runningDifferenceNoRot) {
        this.runningDifferenceNoRot = runningDifferenceNoRot;
    }

    public void setTruncation(float truncation) {
        this.truncation = truncation;
    }

    public float getTruncation() {
        return this.truncation;
    }

    public float getOpacity() {
        return opacity;
    }

    public float getContrast() {
        return contrast;
    }

    public float getGamma() {
        return gamma;
    }

    public ColorMask getColorMask() {
        return colorMask;
    }

    public LUT getLUT() {
        return lut;
    }

    public boolean getInvertLUT() {
        return invertLUT;
    }

    private void copyScreenToTexture(GL2 gl) {
        ImageData image = this.getSubimageData();
        Region region = image.getRegion();

        double xOffset = region.getLowerLeftCorner().x;
        double yOffset = region.getLowerLeftCorner().y;
        double xScale = 1. / region.getWidth();
        double yScale = 1. / region.getHeight();

        MetaData metadata = image.getMETADATA();

        GLSLShader.changeRect(xOffset, yOffset, xScale, yScale);

        boolean diffMode = false;
        Region diffRegion = null;

        if (!this.getBaseDifferenceMode() && this.getPreviousImageData() != null) {
            diffMode = true;
            diffRegion = this.getPreviousImageData().getRegion();
        } else if (this.getBaseDifferenceMode() && this.getBaseDifferenceImageData() != null) {
            diffMode = true;
            diffRegion = this.getBaseDifferenceImageData().getRegion();
        }

        if (diffMode) {
            double diffXOffset = diffRegion.getLowerLeftCorner().x;
            double diffYOffset = diffRegion.getLowerLeftCorner().y;
            double diffXScale = 1. / diffRegion.getWidth();
            double diffYScale = 1. / diffRegion.getHeight();

            GLSLShader.setDifferenceRect(diffXOffset, diffYOffset, diffXScale, diffYScale);
        }

        double innerCutOff = 0;
        double outerCutOff = 40;
        if (metadata instanceof HelioviewerMetaData) {
            HelioviewerMetaData md = (HelioviewerMetaData) metadata;
            innerCutOff = md.getInnerPhysicalOcculterRadius();
            outerCutOff = md.getOuterPhysicalOcculterRadius();
        }

        GLSLShader.setCutOffRadius(innerCutOff, outerCutOff);
    }

    public void setImageLayer(RenderableImageLayer imageLayer) {
        this.imageLayer = imageLayer;
    }

    public RenderableImageLayer getImageLayer() {
        return imageLayer;
    }

}
