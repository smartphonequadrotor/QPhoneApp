package com.ventus.smartphonequadrotor.qphoneapp.test.util.control;

import android.util.Log;

import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.CmacInputParam;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.CmacOutput;
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
//	public void testTriggerCmacUpdate() {
//		fail("Not yet implemented");
//	}

	/**
	 * Test method for {@link ControlLoop#cmacOutput2MotorSpeeds(SimpleMatrix)}.
	 */
//	public void testCmacOutput2MotorSpeeds() {
//		fail("Not yet implemented");
//	}
	
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
		final int HEIGHT_CORRECTION_TEST_ITERATIONS = 10;
		SimpleMatrix input = new SimpleMatrix(
			1, 
			CmacInputParam.count,
			true,
			new double[]{
				-0.5, 0, 0, 0, -25, 0, 0, 0, 0, 0, 0, 0, (4*1000*2*Math.PI)
			}
		);
		SimpleMatrix currentMotorSpeeds, cmacOutput;
		SimpleMatrix previousMotorSpeeds = SimpleMatrix.zeros(1, CmacOutput.NUMBER_OF_WEIGHTS);
		Log.d(TAG, "***Starting height change input to control loop: " + input);
		for (int i = 0; i < HEIGHT_CORRECTION_TEST_ITERATIONS; i++) {
			cmacOutput = loop.triggerCmacUpdate(input);
			currentMotorSpeeds = loop.cmacOutput2MotorSpeeds(cmacOutput);
			for (int j = 0; j < CmacOutput.NUMBER_OF_WEIGHTS; j++) {
				assertFalse("The motor speeds are NaN", Double.isNaN(currentMotorSpeeds.get(j)));
				assertTrue("Motor speeds are not supposed to decrease", currentMotorSpeeds.get(j) >= previousMotorSpeeds.get(j));
			}
			previousMotorSpeeds = currentMotorSpeeds;
			Log.d(TAG, "Motor speeds suggested by cmac: " + currentMotorSpeeds);
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
		final int PITCH_CORRECTION_TEST_ITERATIONS = 10;
		SimpleMatrix input = new SimpleMatrix(
			1, 
			CmacInputParam.count,
			true,
			new double[]{
				0, 0, (-5/180)*Math.PI, 0, 0, 0, (-250/180)*Math.PI, 0, 0, 0, 0, (-62500/180)*Math.PI, (4*1000*2*Math.PI)
			}
		);
		SimpleMatrix cmacOutput, currentMotorSpeeds;
		SimpleMatrix previousMotorSpeeds = SimpleMatrix.zeros(1, CmacOutput.NUMBER_OF_WEIGHTS);
		Log.d(TAG, "***Starting pitch change input to control loop: " + input);
		for (int i = 0; i < PITCH_CORRECTION_TEST_ITERATIONS; i++) {
			cmacOutput = loop.triggerCmacUpdate(input);
			currentMotorSpeeds = loop.cmacOutput2MotorSpeeds(cmacOutput);
			for (int j = 0; j < CmacOutput.NUMBER_OF_WEIGHTS; j++) {
				assertFalse("The motor speeds are NaN", Double.isNaN(currentMotorSpeeds.get(j)));
			}
			assertTrue(
				"Motor speeds are supposed to change in different directions diagonally",
				currentMotorSpeeds.get(0) == -currentMotorSpeeds.get(2)
			);
			assertTrue(
				"Motor speeds are supposed to change in different directions diagonally",
				currentMotorSpeeds.get(1) == -currentMotorSpeeds.get(3)
			);
			previousMotorSpeeds = currentMotorSpeeds;
			Log.d(TAG, "Motor speeds suggested by cmac: " + currentMotorSpeeds);
		}
	}
}
