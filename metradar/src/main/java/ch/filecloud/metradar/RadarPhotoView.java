package ch.filecloud.metradar;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.OverScroller;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by domi on 2/22/14.
 */
public class RadarPhotoView extends PhotoView implements PhotoViewAttacher.OnMatrixChangedListener {

    private Marker marker;

    private float left;
    private float top;

    private OverScroller overScroller;

    public RadarPhotoView(Context context) {
        super(context);
        init();
    }

    public RadarPhotoView(Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    public RadarPhotoView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
        init();
    }

    private void init() {
       setOnMatrixChangeListener(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (marker != null) {
            float scale = getScale();
            double ratio = getRatio(canvas);
            double xPosition = ((marker.getX() * ratio * scale) + left) - (marker.getBitMap().getWidth() / 2);
            double yPosition = ((marker.getY() * ratio * scale) + top) - (marker.getBitMap().getHeight());
            canvas.drawBitmap(marker.getBitMap(), (int)xPosition, (int)yPosition, null);
        }
    }

    public void setMarker(int resourceId, float left, float top) {
        Bitmap markerBitMap = BitmapFactory.decodeResource(getResources(), resourceId);
        marker = new Marker(markerBitMap, left, top);
    }

    @Override
    public void onMatrixChanged(RectF rect) {
        this.left = rect.left;
        this.top = rect.top;
    }

    public void removeMarker() {
        this.marker = null;
    }

    private double getRatio(Canvas canvas) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return (double) canvas.getWidth() / getDrawable().getIntrinsicWidth();
        }

        return (double) canvas.getHeight() / getDrawable().getIntrinsicHeight();
    }

    private class Marker {

        private final Bitmap bitMap;
        private final float x;
        private final float y;

        public Marker(Bitmap bitMap, float x, float y) {
            this.bitMap = bitMap;
            this.x = x;
            this.y = y;
        }

        public Bitmap getBitMap() {
            return bitMap;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

    }

}
