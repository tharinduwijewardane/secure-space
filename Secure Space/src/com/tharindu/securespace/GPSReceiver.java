package com.tharindu.securespace;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Location receiving service
 * @author Tharindu Wijewardane
 */

public class GPSReceiver extends Service {
	
	private static final String TAG = "-MY-";
	private LocationManager mLocationManager = null;
	private static final int LOCATION_INTERVAL = 1*60*1000;	//min interval
	private static final float LOCATION_DISTANCE = 20f;		//min distance
	private PreferenceHelp prefHelp;

	private class LocationListener implements android.location.LocationListener {
		
		Location mLastLocation;

		public LocationListener(String provider) {
			Log.d(TAG, "LocationListener " + provider);
			mLastLocation = new Location(provider);
		}

		@Override
		public void onLocationChanged(Location location) {
			Log.d(TAG, "onLocationChanged: " + location);
			mLastLocation.set(location);
			
			takeDecision(mLastLocation);
		}

		@Override
		public void onProviderDisabled(String provider) {
			Log.d(TAG, "onProviderDisabled: " + provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.d(TAG, "onProviderEnabled: " + provider);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.d(TAG, "onStatusChanged: " + provider);
		}
	}

	//keep location listeners in a array
	LocationListener[] mLocationListeners = new LocationListener[] {
			new LocationListener(LocationManager.GPS_PROVIDER),
			new LocationListener(LocationManager.NETWORK_PROVIDER) };

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		
		prefHelp = new PreferenceHelp(getApplicationContext());
		
		initializeLocationManager();
		try {
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL,
					LOCATION_DISTANCE, mLocationListeners[1]);
		} catch (java.lang.SecurityException ex) {
			Log.i(TAG, "fail to request location update, ignore", ex);
		} catch (IllegalArgumentException ex) {
			Log.d(TAG, "network provider does not exist, " + ex.getMessage());
		}
		try {
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, LOCATION_INTERVAL,
					LOCATION_DISTANCE, mLocationListeners[0]);
		} catch (java.lang.SecurityException ex) {
			Log.i(TAG, "fail to request location update, ignore", ex);
		} catch (IllegalArgumentException ex) {
			Log.d(TAG, "gps provider does not exist " + ex.getMessage());
		}
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
		if (mLocationManager != null) {
			for (int i = 0; i < mLocationListeners.length; i++) {
				try {
					mLocationManager.removeUpdates(mLocationListeners[i]);
				} catch (Exception ex) {
					Log.i(TAG, "fail to remove location listners, ignore", ex);
				}
			}
		}
	}

	private void initializeLocationManager() {
		Log.d(TAG, "initializeLocationManager");
		if (mLocationManager == null) {
			mLocationManager = (LocationManager) getApplicationContext()
					.getSystemService(Context.LOCATION_SERVICE);
		}
	}
	
	
	private void takeDecision(Location lastLocation){
		
		double lat1 = lastLocation.getLatitude();
		double lng1 = lastLocation.getLongitude();
		double lat2 = prefHelp.getPrefDouble(ConstVals.PREF_KEY_LAT);
		double lng2 = prefHelp.getPrefDouble(ConstVals.PREF_KEY_LON);
		
		double distance = distance(lat1, lng1, lat2, lng2);
		Log.d("-MY-", "distance: "+distance);
		boolean filesEncrypted = prefHelp.getPrefBool(ConstVals.PREF_KEY_ARE_FILES_ENCRYPTED);
		Log.d("-MY-", "pref key is encrypted: "+filesEncrypted);
		
		if(filesEncrypted && distance < 20){ 
			//if files in encrypted state and user in allowed zone
			sendCommand(ConstVals.COMMAND_TYPE_DECRYPT); //decrypt them
			Log.d("-MY-", "Decrypt command sent");
		}else if(!filesEncrypted && distance > 100){ 
			//if files in decrypted state and use out of allowe zone
			sendCommand(ConstVals.COMMAND_TYPE_ENCRYPT); //encrypt them
			Log.d("-MY-", "Encrypt command sent");
		}
		
	}
	
	// calculates the distance between two locations in meters
	private double distance(double lat1, double lng1, double lat2, double lng2) {

	    double earthRadius = 6371*1000; // in meters

	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);

	    double sindLat = Math.sin(dLat / 2);
	    double sindLng = Math.sin(dLng / 2);

	    double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
	        * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

	    double dist = earthRadius * c;

	    return dist;
	}
	
	private void sendCommand(String type){
		
		Intent i = new Intent(getApplicationContext(), EncDecManagerServive.class);
		i.putExtra(ConstVals.REQUESTER_TYPE_KEY, ConstVals.REQUEST_FROM_GPS);
		i.putExtra(ConstVals.COMMAND_TYPE_KEY, type);
		startService(i);
		
	}
}
