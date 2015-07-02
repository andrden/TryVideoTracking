package com.example.tryvideotracking;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import boofcv.android.BoofAndroidFiles;
import boofcv.android.gui.VideoDisplayActivity;

/**
 * Activity for displaying video results.
 *
 * @author Peter Abeles
 */
public class DemoVideoDisplayActivity extends VideoDisplayActivity {

    // contains information on all the cameras.  less error prone and easier to deal with
    public static List<CameraSpecs> specs = new ArrayList<CameraSpecs>();
    public static DemoPreference preference = new DemoPreference();

    public DemoVideoDisplayActivity() {
    }

    public DemoVideoDisplayActivity(boolean hidePreview) {
        super(hidePreview);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadCameraSpecs();
        //preference = DemoMain.preference;
        setDefaultPreferences();
        setShowFPS(preference.showFps);
    }

    @Override
    protected Camera openConfigureCamera( Camera.CameraInfo info ) {
        Camera mCamera = Camera.open(preference.cameraId);
        Camera.getCameraInfo(preference.cameraId,info);

        Camera.Parameters param = mCamera.getParameters();
        Camera.Size sizePreview = param.getSupportedPreviewSizes().get(preference.preview);
        param.setPreviewSize(sizePreview.width, sizePreview.height);
        Camera.Size sizePicture = param.getSupportedPictureSizes().get(preference.picture);
        param.setPictureSize(sizePicture.width, sizePicture.height);
        mCamera.setParameters(param);

        return mCamera;
    }

    private void loadCameraSpecs() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraSpecs c = new CameraSpecs();
            specs.add(c);

            Camera.getCameraInfo(i, c.info);
            Camera camera = Camera.open(i);
            Camera.Parameters params = camera.getParameters();
            c.sizePreview.addAll(params.getSupportedPreviewSizes());
            c.sizePicture.addAll(params.getSupportedPictureSizes());
            camera.release();
        }
    }


    private void setDefaultPreferences() {
        preference.showFps = true;

        // There are no cameras.  This is possible due to the hardware camera setting being set to false
        // which was a work around a bad design decision where front facing cameras wouldn't be accepted as hardware
        // which is an issue on tablets with only front facing cameras
        if( specs.size() == 0 ) {
            dialogNoCamera();
        }
        // select a front facing camera as the default
        for (int i = 0; i < specs.size(); i++) {
            CameraSpecs c = specs.get(i);

            if( c.info.facing == Camera.CameraInfo.CAMERA_FACING_BACK ) {
                preference.cameraId = i;
                break;
            } else {
                // default to a front facing camera if a back facing one can't be found
                preference.cameraId = i;
            }
        }

        CameraSpecs camera = specs.get(preference.cameraId);
        preference.preview = UtilVarious.closest(camera.sizePreview,320,240);
        preference.picture = UtilVarious.closest(camera.sizePicture,640,480);

        // see if there are any intrinsic parameters to load
        loadIntrinsic();
    }

    private void dialogNoCamera() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your device has no cameras!")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        System.exit(0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void loadIntrinsic() {
        preference.intrinsic = null;
        try {
            FileInputStream fos = openFileInput("cam"+preference.cameraId+".txt");
            Reader reader = new InputStreamReader(fos);
            preference.intrinsic = BoofAndroidFiles.readIntrinsic(reader);
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            Toast.makeText(this, "Failed to load intrinsic parameters", Toast.LENGTH_SHORT).show();
        }
    }
}