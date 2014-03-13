package ch.filecloud.metradar;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;

import ch.filecloud.metradar.util.ApproxSwissProj;

public class RadarActivity extends Activity implements LocationListener {

    private LocationManager locManager;
    private String provider;

    private LinearLayout legend;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        legend = (LinearLayout) findViewById(R.id.legend);
        legend.setVisibility(LinearLayout.INVISIBLE);
        updateRadar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                updateRadar();
                return true;
            case R.id.menu_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locManager != null) {
            locManager.requestLocationUpdates(provider, 60000, 1000, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locManager != null) {
            locManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        setLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO - implement method
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO - implement method
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO - implement method
    }

    private void updateRadar() {
        legend.setVisibility(LinearLayout.GONE);
        final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setTitle("Loading radar data...");
        //dlg.setIndeterminate(false);
        dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dlg.show();

        final RadarPhotoView radarImageView = (RadarPhotoView) findViewById(R.id.imageView);
        final LinearLayout legend = (LinearLayout) findViewById(R.id.legend);

        Ion.with(RadarActivity.this).load("http://radar.netdata.ch/newest.gif").noCache().progressDialog(dlg).intoImageView(radarImageView).setCallback(new FutureCallback<ImageView>() {
            @Override
            public void onCompleted(Exception e, ImageView result) {
                dlg.cancel();

                // no error
                if (e == null) {
                    legend.setVisibility(LinearLayout.VISIBLE);

                    if (locManager == null) {
                        setLocationProvider();
                    }
                }

                // error handling
                else {
                    Toast.makeText(getApplicationContext(), "Unable to retrieve radar data", Toast.LENGTH_LONG).show();
                }

            }
        });

        radarImageView.setScaleType(ScaleType.CENTER_CROP);

    }

    private void setLocationProvider() {
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        provider = locManager.getBestProvider(criteria, true);
        Location loc = locManager.getLastKnownLocation(provider);
        locManager.requestLocationUpdates(provider, 5000, 5, this);
    }

    private void setLocation(Location loc) {
        final RadarPhotoView radarImageView = (RadarPhotoView) findViewById(R.id.imageView);

        if (loc != null) {

            double latitude = loc.getLatitude();
            double longitude = loc.getLongitude();

            int x = ((int) ApproxSwissProj.WGStoCHx(latitude, longitude) / 1000);
            int y = ((int) ApproxSwissProj.WGStoCHy(latitude, longitude) / 1000);

            radarImageView.setMarker(R.drawable.red_pin, y - 250, 480 - x);

        } else {
            radarImageView.removeMarker();
        }

    }

}
