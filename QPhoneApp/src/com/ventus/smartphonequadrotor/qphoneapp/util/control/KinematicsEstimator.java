package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;

/**
 * This class receives the raw outputs from the accelerometer, magnetometer and
 * gyroscope onboard the quadrotor. It then uses these raw values to get better
 * estimates of the tilt and displacement of the quadrotor.
 * 
 * For estimating the tilt from the three sensors, it uses a simple filter described
 * by equations 17-19 of the main-paper.
 * 
 * For estimating the translational motion (displacement) of the quadrotor, it uses simple
 * double-integration of the accelerometer values projected onto the earth-frame using the 
 * angles computed by the tilt filter.
 * @author abhin
 * @deprecated The kinematics estimator is not required because the phone receives pre-filtered values
 */
public class KinematicsEstimator {
	/**
	 * This enum contains the indexes for the various matrices that will contain
	 * euler angles.
	 * @author abhin
	 *
	 */
	public enum AngleIndex {
		ROLL(0), PITCH(1), YAW(2);
		public int index;
		AngleIndex(int index) {
			this.index = index;
		}
	}
	
	private static final float DRIFT_CORRECTION_GAIN = 0.5f;
	
	private SimpleMatrix driftCorrectionAngles;
	private SimpleMatrix previousEstimate;
	private SimpleMatrix currentEstimate;
	
	private long previousGyroTimestamp;

	public KinematicsEstimator() {
		driftCorrectionAngles = SimpleMatrix.zeros(3, 1);
		previousEstimate = SimpleMatrix.zeros(3, 1);
		currentEstimate = SimpleMatrix.zeros(3, 1);
		previousGyroTimestamp = 0L;
	}
	
	public void registerAccelValues(int x, int y, int z, long timestamp) {
		//figure out the roll and pitch
		driftCorrectionAngles.set(AngleIndex.ROLL.index, Math.atan2(z, y));
		driftCorrectionAngles.set(AngleIndex.PITCH.index, Math.atan2(x, z));
		
		//TODO : translation motion
	}
	
	public void registerGyroValues(int x, int y, int z, long timestamp) {
		
	}
	
	public void registerMagValues(int x, int y, int z, long timestamp) {
		//figure out the yaw
		driftCorrectionAngles.set(AngleIndex.YAW.index, Math.atan2(y, x));
	}
}
