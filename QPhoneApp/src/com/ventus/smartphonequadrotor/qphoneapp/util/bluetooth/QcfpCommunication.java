package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCommands;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.MotorModel;

public class QcfpCommunication {
	
	private static final String TAG = QcfpCommunication.class.getName();
	
	/**
	 * Max encoded packet size includes an extra byte at the start and end and
	 * up to 1 additional byte of overhead
	 * An encoded packet will never exceed 255 bytes
	 */
	private static final int QCFP_MAX_ENCODED_PACKET_SIZE = QcfpParser.QCFP_MAX_PACKET_SIZE + 3;
	
	private BluetoothManager bluetoothManager;
	
	public QcfpCommunication(BluetoothManager bluetoothManager) {
		this.bluetoothManager = bluetoothManager;
	}
	
	public static float decodeFloat(byte[] buffer, int index)
	{
		int temp = buffer[index] & 0x00FF;
		for(int i = index + 1; i < index + 4; i++)
		{
			int toOr = (buffer[i] << (8*(i-index))) & (0x00FF << (8*(i-index)));
			temp |= toOr;
		}
		return Float.intBitsToFloat(temp);
	}
	
	/**
	 * Sends raw motor speeds to the QCB. Motor values will only take effect if
	 * the QCB is in flight mode.
	 * @param motorSpeeds Motor speeds to set motors to. 0 will turn the motor off. 1 is the lowest
	 * setting and the current maximum defined is 80. Sending a higher value will result in the
	 * motor being set to 80. These values will result in a PWM duty cycle on the motors of
	 * (PWM_BASE + value)/PWM_PERIOD*100% where PWM_BASE is currently 110 and PWM_PERIOD is 200.
	 * @throws Exception Throws an exception if the command cannot be sent (Bluetooth manager couldn't write).
	 */
	public void sendRawMotorSpeeds(byte[] motorSpeeds) throws Exception
	{
		if (motorSpeeds == null || motorSpeeds.length != MotorModel.NUMBER_OF_MOTORS)
			throw new IllegalArgumentException("Motor speeds array is incorrect");
		
		byte[] buffer = new byte[5];
		buffer[0] = QcfpCommands.QCFP_RAW_MOTOR_CONTROL;
		System.arraycopy(motorSpeeds, 0, buffer, 1, motorSpeeds.length);
		sendBluetoothMessage(QcfpCommunication.encodeData(buffer, buffer.length));
	}
	
	/**
	 * Sends the command to enable flight mode in the firmware. This mode must be enabled for
	 * the sendRawMotorSpeeds method to have an effect. Flight mode will not take effect if
	 * the quadrotor is currently being calibrated.
	 * Flight mode takes several seconds to take effect. As such, this command must continue to
	 * be sent until the QCB responds with the byte QcfpCommands.QCFP_FLIGHT_MODE_ENABLE in the
	 * response payload. If the motors/ESCs are still being initialized, the response payload
	 * will be QcfpCommands.QCFP_FLIGHT_MODE_PENDING.
	 * @param enabled true to enable flight mode, false to disable flight mode
	 * @throws Exception Throws an exception if the command cannot be sent (Bluetooth manager couldn't write).
	 */
	public void sendFlightMode(Boolean enabled) throws Exception
	{
		byte[] buffer = new byte[2];
		buffer[0] = QcfpCommands.QCFP_FLIGHT_MODE;
		if(enabled == true)
		{
			buffer[1] = QcfpCommands.QCFP_FLIGHT_MODE_ENABLE;
		}
		else
		{
			buffer[1] = QcfpCommands.QCFP_FLIGHT_MODE_DISABLE;
		}
		sendBluetoothMessage(QcfpCommunication.encodeData(buffer, buffer.length));
	}

	public void setDesiredThrottle(int throttle) throws Exception
	{
		byte[] buffer = new byte[2];
		buffer[0] = QcfpCommands.QCFP_SET_THROTTLE;
		buffer[1] = (byte) (throttle & 0x00FF);
		buffer[2] = (byte) ((throttle & 0xFF00) >> 8);
		sendBluetoothMessage(QcfpCommunication.encodeData(buffer, buffer.length));
	}
	
	public void sendDesiredTHrpy(int throttle, short height, float[] rpy) throws Exception {
		if (rpy == null || rpy.length != 3) 
			throw new IllegalArgumentException("Roll, pitch, yaw array is illegal");
		
		ByteBuffer rpyBuffer = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer heightBuffer = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN);
		
		rpyBuffer.put(QcfpCommands.QCFP_SET_DESIRED_ANGLES);
		heightBuffer.put(QcfpCommands.QCFP_SET_DESIRED_HEIGHT);
		
		for (float angle : rpy) {
			rpyBuffer.putFloat(angle);
		}
		heightBuffer.putShort(height);
		
		setDesiredThrottle(throttle);
		sendBluetoothMessage(QcfpCommunication.encodeData(rpyBuffer.array(), rpyBuffer.array().length));
		sendBluetoothMessage(QcfpCommunication.encodeData(heightBuffer.array(), heightBuffer.array().length));
	}
	
	public void setAltitudeEnable(Boolean enabled) throws Exception
	{
		byte[] buffer = new byte[2];
		buffer[0] = QcfpCommands.QCFP_ALTITUDE_HOLD_EN;
		if(enabled == true)
		{
			buffer[1] = 1;
		}
		else
		{
			buffer[1] = 0;
		}
		sendBluetoothMessage(QcfpCommunication.encodeData(buffer, buffer.length));
	}
	
	/**
	 * Queries the current flight mode.
	 * @throws Exception
	 */
	public void queryFlightMode() throws Exception
	{
		byte[] buffer = new byte[1];
		buffer[0] = QcfpCommands.QCFP_FLIGHT_MODE;
		sendBluetoothMessage(QcfpCommunication.encodeData(buffer, buffer.length));
	}
	
	/**
	 * 
	 * @param start Starts a calibration if true, stop a calibration if false.
	 * @throws Exception Throws an exception if the command cannot be sent (Bluetooth manager couldn't write).
	 */
	public void sendStartStopCalibration(Boolean start) throws Exception
	{
		byte[] buffer = new byte[2];
		buffer[0] = QcfpCommands.QCFP_CALIBRATE_QUADROTOR;
		if(start == true)
		{
			buffer[1] = QcfpCommands.QCFP_CALIBRATE_QUADROTOR_START;
		}
		else
		{
			buffer[1] = QcfpCommands.QCFP_CALIBRATE_QUADROTOR_STOP;
		}
		sendBluetoothMessage(QcfpCommunication.encodeData(buffer, buffer.length));
	}
	
	public void sendDebugInformation(String debugStr) throws Exception {
		byte[] buffer = hexString2Byte(debugStr);
		sendBluetoothMessage(QcfpCommunication.encodeData(buffer, buffer.length));
	}
	
	/**
	 * Queries the calibration state.
	 * @throws Exception
	 */
	public void queryCalibration() throws Exception
	{
		byte[] buffer = new byte[1];
		buffer[0] = QcfpCommands.QCFP_CALIBRATE_QUADROTOR;
		sendBluetoothMessage(QcfpCommunication.encodeData(buffer, buffer.length));
	}
	
	/**
	 * This uses the {@link BluetoothManager} to send a message to the QCB
	 * over bluetooth.
	 * @param message
	 */
	public void sendBluetoothMessage(byte[] message) {
		try {
			this.bluetoothManager.write(message);
		} catch (IOException ioEx) {
			Log.e(TAG, "Could not send message", ioEx);
		}
	}

	/**
	 * This method is equivalent in function to the function qcfp_send_data in
	 * the firmware.
	 * 
	 * @param buffer The buffer to send.
	 * @param length The length of the buffer.
	 * @return Returns a COBS encoded byte array that can be sent to the QCB.
	 * @throws IllegalArgumentException
	 */
	public static byte[] encodeData(byte[] buffer, int length) throws Exception {
		if (length > QcfpParser.QCFP_MAX_PACKET_SIZE) {
			throw new IllegalArgumentException(
					"Length of buffer is too large to be encoded.");
		}

		byte[] encodedData = new byte[QCFP_MAX_ENCODED_PACKET_SIZE];
		byte byteCount = 1;
		int encodedDataIndex = 1, chunkIndex = 1;

		// First byte is always COBS_TERM_BYTE
		encodedData[0] = QcfpParser.COBS_TERM_BYTE;

		for (int i = 0; i < length; i++, byteCount++) {
			if (buffer[i] == QcfpParser.COBS_TERM_BYTE) {
				encodedData[chunkIndex] = byteCount;
				chunkIndex = ++encodedDataIndex;
				byteCount = 0;
			} else {
				encodedData[++encodedDataIndex] = buffer[i];
			}
		}

		if (byteCount > 1) {
			encodedData[chunkIndex] = byteCount;
			encodedDataIndex++;
		}

		if (buffer[length - 1] == QcfpParser.COBS_TERM_BYTE) {
			encodedData[encodedDataIndex++] = 1;
		}

		encodedData[encodedDataIndex++] = QcfpParser.COBS_TERM_BYTE;

		byte[] returnArray = new byte[encodedDataIndex];
		System.arraycopy(encodedData, 0, returnArray, 0, encodedDataIndex);
		return returnArray;
	}
	
	public static byte[] hexString2Byte(String hex) {
		int bufferLength = hex.length()/2;
		ByteBuffer buffer = ByteBuffer.allocate(bufferLength);
		
		for (int i = 0; i < bufferLength; i++) {
			String byteStr = hex.substring(2*i, 2*(i+1));
			buffer.put((byte)(Short.parseShort(byteStr, 16) & 0x00ff));
		}
		return buffer.array();
	}
}
