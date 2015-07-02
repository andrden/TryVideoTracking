package com.example.tryvideotracking;

import boofcv.struct.calib.IntrinsicParameters;

/**
 * @author Peter Abeles
 */
public class DemoPreference {
    int cameraId;
    int preview;
    int picture;
    boolean showFps=true;
    IntrinsicParameters intrinsic;
}