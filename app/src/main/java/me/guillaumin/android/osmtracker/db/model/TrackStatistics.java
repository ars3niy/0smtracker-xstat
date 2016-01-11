package me.guillaumin.android.osmtracker.db.model;

import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.content.ContentResolver;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

/**
 * Represents statistics, such as  total length and maximum speed, for a Track
 *
 * @author Arseniy Lartsev
 *
 */
public class TrackStatistics {
	private long trackId;
	private long pointCount;
	private float length;
	private float maxSpeed;
	private double lastLatitude;
	private double lastLongitude;
	private long lastTime;
	private float timeMoving;
	
	private static double square(double x) {
		return x*x;
	}

	/**
	 * Compute the approximate distance in meters between two locations, with 0.5% precision
	 */
	public static float getDistance(double lat1, double long1, double lat2, double long2) {
		final double R = 6370e3; // Earth radius
		final double rad_per_deg = Math.PI/180; // radians per degree
		
		lat1 *= rad_per_deg;
		long1 *= rad_per_deg;
		lat2 *= rad_per_deg;
		long2 *= rad_per_deg;
		
		double dLat = lat2-lat1;
		double dLon = long2-long1;

		double A = square(Math.sin(0.5*dLat)) + Math.cos(lat1) * Math.cos(lat2) * square(Math.sin(0.5*dLon));
		
		return (float)(2 * R * Math.asin(Math.sqrt(A)));
	}

	public TrackStatistics () {
		trackId = -1;
		length = 0;
		maxSpeed = 0;
		pointCount = 0;
		timeMoving = 0;
	}

	/**
	 * build a track statistics object with the given cursor
	 *
	 * @param trackId id of the track that will be built
	 * @param cr the content resolver to use
	 */
	public TrackStatistics (final long trackId, ContentResolver cr) {
		this.trackId = trackId;
		length = 0;
		maxSpeed = 0;
		pointCount = 0;
		timeMoving = 0;

		Cursor cursor = cr.query(TrackContentProvider.trackPointsUri(trackId), null, null, null, null);
		if(! cursor.moveToFirst())
			return;

		while (! cursor.isAfterLast()) {
			double latitude = cursor.getDouble(cursor.getColumnIndex(Schema.COL_LATITUDE));
			double longitude = cursor.getDouble(cursor.getColumnIndex(Schema.COL_LONGITUDE));
			float speed = cursor.getFloat(cursor.getColumnIndex(Schema.COL_SPEED));
			float accuracy = cursor.getFloat(cursor.getColumnIndex(Schema.COL_ACCURACY));
			long time = cursor.getLong(cursor.getColumnIndex(Schema.COL_TIMESTAMP));

			update(latitude, longitude, accuracy, speed, time);
			cursor.moveToNext();
		}

		cursor.close();
	}

	/**
	 * Update the statistics upon adding a new point to the track
	 */
	public void update(Location trackPoint){
		update(trackPoint.getLatitude(), trackPoint.getLongitude(), trackPoint.getAccuracy(),
				trackPoint.getSpeed(), trackPoint.getTime());
	}

	private void update(double latitude, double longitude, float accuracy, float speed, long time) {
		if (pointCount > 0) {
			// The "distance and time only counts when the speed is non-zero" principle has been borrowed from osmand
			if ((speed > 0) && (time != 0) && (lastTime != 0)) {
				length += getDistance(lastLatitude, lastLongitude, latitude, longitude);
				timeMoving += time - lastTime;
			}
		}
		pointCount += 1;

		if(speed > maxSpeed)
			maxSpeed = speed;
		lastLatitude = latitude;
		lastLongitude = longitude;
		lastTime = time;
	}

	/**
	 * Get track length, in meters
	 */
	public float totalLength() {
		return length;
	}

	/**
	 * Get maximum speed, in meters per second
	 */
	public float maximumSpeed() {
		return maxSpeed;
	}

	/**
	 * Get aveare speed, in meters per second
	 */
	public float averageSpeed() {
		return 1000*length/timeMoving;
	}

}
