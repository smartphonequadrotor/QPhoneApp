package com.ventus.smartphonequadrotor.qphoneapp.test.util.control;

import android.util.Log;

import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.CmacInputParam;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.ControlLoop;

import junit.framework.TestCase;

/**
 * @author abhin
 *
 */
public class ControlLoopTest extends TestCase {
	private static final String TAG = ControlLoopTest.class.getCanonicalName();
	private ControlLoop loop;
	/**
	 * @param name
	 */
	public ControlLoopTest(String name) {
		super(name);
		loop = new ControlLoop(null);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test method for {@link ControlLoop#triggerCmacUpdate(SimpleMatrix)}.
	 */
	public void testTriggerCmacUpdate() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link ControlLoop#cmacOutput2MotorSpeeds(SimpleMatrix)}.
	 */
	public void testCmacOutput2MotorSpeeds() {
		fail("Not yet implemented");
	}
	
	/**
	 * This method attempts to check the motor directions that the cmac will output.
	 * In addition, this method also attempts to check the learning capabilities of cmac
	 * by asking the cmac logic to compute the motor speeds again and again.
	 */
	public void testHeightCorrection() {
		/*
		 * Lets pretend that the quadrotor is stable and has been stable since
		 * infinity. This means that the yaw, pitch and roll related items are
		 * all 0.
		 * Lets assume that the desired height is 0.5m lesser than the desired 
		 * height. This means that the height error is -0.5m.
		 * Lets assume that the time interval between updates is 0.02seconds.
		 * This would mean that the first derivative of the height error is 
		 * (-0.5-0)/0.02 = -25
		 * Lets assume that the motors are all running at the same speed (1000rpm) in the 
		 * previous time interval. Thus the omega-r is 4*1000*2*PI rad/sec.
		 */
		SimpleMatrix input = new SimpleMatrix(
			1, 
			CmacInputParam.count,
			true,
			new double[]{
				-0.5, 0, 0, 0, -25, 0, 0, 0, 0, 0, 0, 0, (4*1000*2*Math.PI)
			}
		);
		SimpleMatrix motorSpeeds, cmacOutput;
		Log.d(TAG, "Sending input to control loop: " + input);
		for (int i = 0; i < 5; i++) {
			cmacOutput = loop.triggerCmacUpdate(input);
			motorSpeeds = loop.cmacOutput2MotorSpeeds(cmacOutput);
			assertFalse("The motor speeds are NaN", 
					(Double.isNaN(motorSpeeds.get(0))));	//TODO add the rest of the conditions
			Log.d(TAG, "Motor speeds suggested by cmac: " + motorSpeeds);
		}
	}

	/**
	 * This method attempts to check the pitch corrections that the cmac will output. 
	 * In addition, this method also attempts to check the learning capabilities of cmac
	 * by asking the cmac logic to compute the motor speeds again and again.
	 */
	public void testPitchCorrection() {
		/*
		 * Lets pretend that the quadrotor is stable and has been stable since
		 * infinity. 
		 * Lets assume that the desired pitch is +5 degrees. This means that
		 * the pitch error is 0 - 5 deg = -5deg. 
		 * Lets assume that the time interval between updates is 0.02seconds.
		 * This would mean that the first derivative of the pitch error is 
		 * (-5-0)/0.02 = -250 deg/sec
		 * This would mean that the second derivative of the pitch error is
		 * (-250-0)/0.02 = -62500 deg/sec^2
		 * Lets assume that the motors are all running at the same speed (1000rpm) in the 
		 * previous time interval. Thus the omega-r is 4*1000*2*PI rad/sec.
		 */
		SimpleMatrix input = new SimpleMatrix(
			1, 
			CmacInputParam.count,
			true,
			new double[]{
				0, 0, (-5/180)*Math.PI, 0, 0, 0, (-250/180)*Math.PI, 0, 0, 0, 0, (-62500/180)*Math.PI, (4*1000*2*Math.PI)
			}
		);
		Log.d(TAG, "Sending input to control loop: " + input);
		SimpleMatrix motorSpeeds = loop.cmacOutput2MotorSpeeds(loop.triggerCmacUpdate(input));
		Log.d(TAG, "Motor speeds suggested by cmac: " + motorSpeeds);
		motorSpeeds = loop.cmacOutput2MotorSpeeds(loop.triggerCmacUpdate(input));
		Log.d(TAG, "Motor speeds suggested by cmac: " + motorSpeeds);
		motorSpeeds = loop.cmacOutput2MotorSpeeds(loop.triggerCmacUpdate(input));
		Log.d(TAG, "Motor speeds suggested by cmac: " + motorSpeeds);
	}
}
