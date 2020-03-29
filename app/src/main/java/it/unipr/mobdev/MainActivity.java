package it.unipr.mobdev;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by Marco Picone (picone.m@gmail.com) 20/03/2020
 * Simple Activity and application to show how to use Location Provider on Android to build
 * Location Based Service (LBS) and Applications
 */
public class MainActivity extends AppCompatActivity implements LocationListener {

	private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 54;

	public static String TAG = "LocationExample";

	private LocationManager locationManager = null;

	private Location currentLocation = null;

	private TextView locationTextView = null;

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Log.d(TAG, "onCreate()");

		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);

		ActionBar actionBar = getSupportActionBar();

		if(actionBar != null){
			actionBar.setHomeButtonEnabled(false);
			actionBar.setDisplayHomeAsUpEnabled(false);
		}

		locationTextView = (TextView) findViewById(R.id.locationLabel);

		//Check for Location Permissions
		checkForLocationPermissions();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy()");
		super.onDestroy();
		locationManager.removeUpdates(this);
	}

	@Override
	public void onLocationChanged(Location location) {

		Log.d(TAG,
				"onLocationChanged(): Provider: " + location.getProvider()
						+ " Lat: " + location.getLatitude() + " Lng: "
						+ location.getLongitude() + " Accuracy: "
						+ location.getAccuracy());

		if (currentLocation == null)
			currentLocation = location;
		else if (isBetterLocation(location, currentLocation)) {
			Log.d(TAG, "onLocationChanged(): Updating Location ... ");
			currentLocation = location;
		}

		updateLocationTextView(currentLocation);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d(TAG, "onStatusChanged()");
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "onProviderEnabled()");
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "onProviderDisabled()");
	}

	private void localizationInfoInit(){
		try {

			if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

				Log.d(TAG,"Location Permission Granted ! Starting Localization Info Init ....");

				// Acquire a reference to the system Location Manager
				locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

				if(locationManager == null){
					Toast.makeText(this, "Location Manager not Available !", Toast.LENGTH_LONG).show();
					return;
				}

				if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					Log.d(TAG, "GPS_PROVIDER is enabled !");

					Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

					Log.d(TAG, "Last Known Location: " + lastKnownLocation);

					if (lastKnownLocation != null) {
						currentLocation = lastKnownLocation;
						updateLocationTextView(currentLocation);
					}

					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

				} else
					Toast.makeText(this, "GPS is not enabled !", Toast.LENGTH_LONG)
							.show();

				if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					Log.d(TAG, "NETWORK_PROVIDER is enabled !");

					Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

					Log.d(TAG, "Last Known Location: " + lastKnownLocation);

					if (lastKnownLocation != null
							&& isBetterLocation(lastKnownLocation, currentLocation)) {
						currentLocation = lastKnownLocation;
						updateLocationTextView(currentLocation);
					}

					// Register the listener with the Location Manager to receive
					// location updates
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
				} else
					Toast.makeText(this, "LOCATION NETWORK PROVIDER is not enabled !",
							Toast.LENGTH_LONG).show();
			}else {
				Log.e(TAG,"Location Permission Not Granted !");
				showLocationPermissionDeniedMessage();
			}

		}catch (Exception e){
			Log.e(TAG,"Exception: " + e.getLocalizedMessage());
		}
	}

	private void showLocationPermissionDeniedMessage() {
		Toast.makeText(this,"Location Permission Not Granted !",Toast.LENGTH_LONG).show();
	}

	private void updateLocationTextView(Location estimatedLocation){
		if(locationTextView != null)
			locationTextView.setText(String.format("Provider: %s Lat: %s Lng: %s Accuracy: %s",
					estimatedLocation.getProvider(),
					estimatedLocation.getLatitude(),
					estimatedLocation.getLongitude(),
					estimatedLocation.getAccuracy()));
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	/**
	 * Evaluate required and granted permission to show the right message and request to the user
	 */
	private void checkForLocationPermissions(){

		String myPermission = Manifest.permission.ACCESS_FINE_LOCATION;

		int permissionCheck = ContextCompat.checkSelfPermission(this,myPermission);

		if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

			Log.w(TAG,"checkForLocationPermissions() -> ACCESS_FINE_LOCATION Not Granted !");

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,myPermission)) {

				Log.d(TAG,"checkForLocationPermissions() -> shouldShowRequestPermissionRationale(): true");

				// Show an expanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.

				Toast.makeText(this,"The Application needs the access to your location to properly work ! Check System Setting to grant access !",Toast.LENGTH_LONG).show();
				ActivityCompat.requestPermissions(this, new String[]{myPermission}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

			} else {

				// No explanation needed, we can request the permission.

				Log.d(TAG,"checkForLocationPermissions() -> shouldShowRequestPermissionRationale(): false");

				ActivityCompat.requestPermissions(this,new String[]{myPermission},MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

				// MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION is an
				// app-defined int constant. The callback method gets the
				// result of the request.
			}
		}
		else{
			Log.d(TAG,"checkForLocationPermissions() -> ACCESS_FINE_LOCATION GRANTED !");
			localizationInfoInit();
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

		Log.d(TAG,"onRequestPermissionsResult() -> requestCode:"+requestCode);

		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0	&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.d(TAG,"onRequestPermissionsResult() -> Permission GRANTED !");
					// permission was granted, yay! Do the
					localizationInfoInit();
				} else {
					// permission denied, boo! Disable the functionality that depends on this permission.
					Log.e(TAG,"onRequestPermissionsResult() -> Permission DENIED !");
					showLocationPermissionDeniedMessage();
				}
				return;
			}
		}
	}
}