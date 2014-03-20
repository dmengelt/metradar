package ch.filecloud.metradar;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import ch.filecloud.metradar.util.ApproxSwissProj;
import ch.filecloud.metradar.util.LocationUtils;

public class RadarActivity extends FragmentActivity implements LocationListener, GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {

    private final String TAG = RadarActivity.class.getName();

    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;

    private LinearLayout mLegend;
    private TextView mLastRefreshed;

    private SharedPreferences mPrefs;

    private SharedPreferences.Editor mEditor;

    // location updates enabled by default
    boolean mUpdatesRequested = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mLegend = (LinearLayout) findViewById(R.id.legend);
        mLegend.setVisibility(LinearLayout.INVISIBLE);

        mLastRefreshed = (TextView) findViewById(R.id.last_refreshed);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Note that location updates are off until the user turns them on
        mUpdatesRequested = true;

        // Open Shared Preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(RadarActivity.this);

        // Get an editor
        mEditor = mPrefs.edit();

        mLocationClient = new LocationClient(this, this, this);

        updateRadar();
    }

    @Override
    public void onPause() {
        mEditor.putBoolean("pref_location_updates", mUpdatesRequested);
        mEditor.commit();
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        mLocationClient.connect();

    }

    @Override
    public void onStop() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }

        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();

        super.onStop();
    }

    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // Log the result
                        Log.d(TAG, getString(R.string.resolved));

                        // Display the result
                        //mConnectionState.setText(R.string.connected);
                        //mConnectionStatus.setText(R.string.resolved);
                        break;

                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        Log.d(TAG, getString(R.string.no_resolution));

                        // Display the result
                        //mConnectionState.setText(R.string.disconnected);
                        //mConnectionStatus.setText(R.string.no_resolution);

                        break;
                }

                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                Log.d(TAG, getString(R.string.unknown_activity_request_code, requestCode));
                break;
        }
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
            case R.id.menu_settings:
                Intent intent = new Intent(this, UserSettingsActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPrefs.getBoolean("pref_legend", true)) {
            mLegend.setVisibility(LinearLayout.VISIBLE);
        } else {
            mLegend.setVisibility(LinearLayout.GONE);
        }

        // If the app already has a setting for getting location updates, get it
        if (mPrefs.contains("pref_location_updates")) {
            mUpdatesRequested = mPrefs.getBoolean("pref_location_updates", true);

            // Otherwise, turn off location updates until requested
        } else {
            mEditor.putBoolean("pref_location_updates", false);
            mEditor.commit();
        }

        // remove the marker if no location updates are requested
        if(!mUpdatesRequested) {
            setLocation(null);
        }
    }


    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        if (mUpdatesRequested) {
            startPeriodicUpdates();
        }
    }

    /*
    * Called by Location Services if the connection to the
    * location client drops because of an error.
    */
    @Override
    public void onDisconnected() {
        //setLocation(null);
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    /**
     * Report location updates to the UI.
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {
        if(mPrefs.getBoolean("pref_location_updates", true)){
            setLocation(location);
        }
    }

    private void startPeriodicUpdates() {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
    }

    private void updateRadar() {
        updateLastRefreshed();

        mLegend.setVisibility(LinearLayout.GONE);
        final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setTitle("Loading radar data...");
        dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dlg.show();

        final RadarPhotoView radarImageView = (RadarPhotoView) findViewById(R.id.imageView);

        Ion.with(RadarActivity.this).load("http://radar.netdata.ch/newest.gif").noCache().progressDialog(dlg).intoImageView(radarImageView).setCallback(new FutureCallback<ImageView>() {
            @Override
            public void onCompleted(Exception e, ImageView result) {
                dlg.cancel();

                // no error
                if (e == null) {
                    if (mPrefs.getBoolean("pref_legend", true)) {
                        mLegend.setVisibility(LinearLayout.VISIBLE);
                    }
                }

                // error handling
                else {
                    mLegend.setVisibility(LinearLayout.GONE);
                    mLastRefreshed.setVisibility(TextView.GONE);
                    Toast.makeText(getApplicationContext(), "Unable to retrieve radar data", Toast.LENGTH_LONG).show();
                }

            }
        });

        radarImageView.setScaleType(ScaleType.CENTER_CROP);

    }

    private void updateLastRefreshed() {
        Locale current = getResources().getConfiguration().locale;
        String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, current).format(new Date());
        mLastRefreshed.setText(date);
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

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(TAG, getString(R.string.play_services_available));

            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), TAG);
            }
            return false;
        }
    }

    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(), TAG);
        }
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

}
