package com.example.tryvideotracking;

import georegression.struct.point.Point2D_F64;

/**
 * Created by denny on 7/2/15.
 */
class TrackInfo {
    long lastActive;
    Point2D_F64 spawn = new Point2D_F64();
    Point2D_F64 prev;
}
