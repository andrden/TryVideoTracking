package com.example.tryvideotracking;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoRenderProcessing;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;

/**
 * Created by denny on 7/2/15.
 */
class PointProcessing extends VideoRenderProcessing<ImageUInt8> {
    Paint paintLine = new Paint();
    Paint paintRed = new Paint();
    Paint paintBlue = new Paint();

    volatile long xshiftAvg;
    volatile long yshiftAvg;
    BlockingQueue<Point2D_F32> stepShifts = new LinkedBlockingQueue<>();

    PointTracker<ImageUInt8> tracker;

    long tick;

    Bitmap bitmap;
    byte[] storage;

    List<PointTrack> active = new ArrayList<PointTrack>();
    List<PointTrack> spawned = new ArrayList<PointTrack>();
    List<PointTrack> inactive = new ArrayList<PointTrack>();

    // storage for data structures that are displayed in the GUI
    FastQueue<Point2D_F64> trackSrc = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
    FastQueue<Point2D_F64> trackDst = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
    FastQueue<Point2D_F64> trackSpawn = new FastQueue<Point2D_F64>(Point2D_F64.class, true);


    public PointProcessing(PointTracker<ImageUInt8> tracker) {
        super(ImageType.single(ImageUInt8.class));

        paintLine.setColor(Color.RED);
        paintLine.setStrokeWidth(1.5f);
        paintRed.setColor(Color.MAGENTA);
        paintRed.setStyle(Paint.Style.FILL);
        paintBlue.setColor(Color.BLUE);
        paintBlue.setStyle(Paint.Style.FILL);

        this.tracker = tracker;
    }

    @Override
    protected void declareImages(int width, int height) {
        super.declareImages(width, height);
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        storage = ConvertBitmap.declareStorage(bitmap, storage);
    }

    void calcShift(){
        int count=0;
        long sumX=0;
        long sumY=0;

        for (int i = 0; i < active.size(); i++) {
            PointTrack t = active.get(i);
            TrackInfo info = t.getCookie();
            if( info.spawn!=null ) {
                sumX += t.getX() - info.spawn.getX();
                sumY += t.getY() - info.spawn.getY();
                count ++;
            }
        }
        if( count>5 ){
            xshiftAvg = sumX / count;
            yshiftAvg = sumY / count;
        }
    }

    void calcStepShift(){
        int count=0;
        double sumX=0;
        double sumY=0;

        for (int i = 0; i < active.size(); i++) {
            PointTrack t = active.get(i);
            TrackInfo info = t.getCookie();
            if( info.spawn!=null ) {
                if( info.prev==null ) {
                    info.prev = new Point2D_F64();
                }else{
                    sumX += t.getX() - info.prev.getX();
                    sumY += t.getY() - info.prev.getY();
                    count ++;
                }
                info.prev.set(t.getX(), t.getY());
            }
        }
        if( count>5 ){
            stepShifts.add(new Point2D_F32((float)sumX / count, (float)sumY / count));
        }
    }

    @Override
    protected void process(ImageUInt8 gray) {
        tracker.process(gray);

        // drop tracks which are no longer being used
        inactive.clear();
        tracker.getInactiveTracks(inactive);
        for (int i = 0; i < inactive.size(); i++) {
            PointTrack t = inactive.get(i);
            TrackInfo info = t.getCookie();
            if (tick - info.lastActive > 2) {
                tracker.dropTrack(t);
            }
        }

        active.clear();
        tracker.getActiveTracks(active);


        for (int i = 0; i < active.size(); i++) {
            PointTrack t = active.get(i);
            TrackInfo info = t.getCookie();
            info.lastActive = tick;
        }

        calcShift();
        calcStepShift();

        spawned.clear();
        if (active.size() < 50) {
            tracker.spawnTracks();

            // update the track's initial position
            for (int i = 0; i < active.size(); i++) {
                PointTrack t = active.get(i);
                TrackInfo info = t.getCookie();
                info.spawn.set(t);
            }

            tracker.getNewTracks(spawned);
            for (int i = 0; i < spawned.size(); i++) {
                PointTrack t = spawned.get(i);
                if (t.cookie == null) {
                    t.cookie = new TrackInfo();
                }
                TrackInfo info = t.getCookie();
                info.lastActive = tick;
                info.spawn.set(t);
            }
        }

        synchronized (lockGui) {
            ConvertBitmap.grayToBitmap(gray, bitmap, storage);

            trackSrc.reset();
            trackDst.reset();
            trackSpawn.reset();

            for (int i = 0; i < active.size(); i++) {
                PointTrack t = active.get(i);
                TrackInfo info = t.getCookie();
                Point2D_F64 s = info.spawn;
                Point2D_F64 p = active.get(i);

                trackSrc.grow().set(s);
                trackDst.grow().set(p);
            }

            for (int i = 0; i < spawned.size(); i++) {
                Point2D_F64 p = spawned.get(i);
                trackSpawn.grow().set(p);
            }
        }

        tick++;
    }

    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        canvas.drawBitmap(bitmap, 0, 0, null);

        for (int i = 0; i < trackSrc.size(); i++) {
            Point2D_F64 s = trackSrc.get(i);
            Point2D_F64 p = trackDst.get(i);
            canvas.drawLine((int) s.x, (int) s.y, (int) p.x, (int) p.y, paintLine);
            canvas.drawCircle((int) p.x, (int) p.y, 2f, paintRed);
        }

        for (int i = 0; i < trackSpawn.size(); i++) {
            Point2D_F64 p = trackSpawn.get(i);
            canvas.drawCircle((int) p.x, (int) p.y, 3, paintBlue);
        }
    }
}
