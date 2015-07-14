package com.example.tryvideotracking;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F32;


/**
 * Displays KLT tracks.
 *
 * @author Peter Abeles
 */
public class MainActivity/*KltDisplayActivity*/ extends DemoVideoDisplayActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ConfigGeneralDetector config = new ConfigGeneralDetector();
        config.maxFeatures = 150;
        config.threshold = 40;
        config.radius = 3;

        PointTracker<ImageUInt8> tracker =
                FactoryPointTracker.klt(new int[]{1, 2, 4}, config, 3, ImageUInt8.class, ImageSInt16.class);

        setProcessing(new MyPointProcessing(tracker));
    }

    private static class MyPointProcessing extends PointProcessing {
        long tstart = System.currentTimeMillis();

        //Point2D_F32 basePoint;
        //List<Point2D_F32> track = new ArrayList<>();

        Paint paintTrack = new Paint();

        public MyPointProcessing(PointTracker<ImageUInt8> tracker) {
            super(tracker);
            paintTrack.setColor(Color.GREEN);
            paintTrack.setStrokeWidth(2.5f);
        }

        @Override
        protected void process(ImageUInt8 gray) {
//            if( basePoint==null ){
//                basePoint = new Point2D_F32(outputWidth/2, outputHeight/2);
//                track.add(basePoint);
//            }
//            if( System.currentTimeMillis() - tstart > 5000 ){
//                tstart = System.currentTimeMillis();
//
//                basePoint.set(basePoint.getX() - xshiftAvg, basePoint.getY() - yshiftAvg);
//                xshiftAvg = 0;
//                yshiftAvg = 0;
//                tracker.reset(); // start anew
//            }
            super.process(gray);
        }

        @Override
        protected void render(Canvas canvas, double imageToOutput) {
            super.render(canvas, imageToOutput);

//        int cx = canvas.getWidth()/2;
//        int cy = canvas.getHeight()/2;
            int cx = outputWidth/2;
            int cy = outputHeight/2;
            canvas.drawLine(cx, cy - 50, cx, cy + 50, paintLine);
            canvas.drawLine(cx - 50, cy, cx + 50, cy, paintLine);

            canvas.drawText("Δx==" + xshiftAvg, 10, 30, paintRed);

//            if( basePoint!=null ) {
//                track.add(new Point2D_F32(basePoint.getX() - xshiftAvg, basePoint.getY() - yshiftAvg));
//            }
//            Point2D_F32 prev=null;
//            for( Point2D_F32 p : track ) {
//                if( prev!=null ) {
//                    canvas.drawLine(p.x, p.y, prev.x, prev.y, paintTrack);
//                }
//                prev = p;
//            }

            Point2D_F32 prev = new Point2D_F32(cx, cy);
            for( Point2D_F32 p : stepShifts ){
                canvas.drawLine(prev.x, prev.y, prev.x-p.getX(), prev.y-p.getY(), paintTrack);
                prev.set(prev.x-p.getX(), prev.y-p.getY());
            }

        }
    }
}