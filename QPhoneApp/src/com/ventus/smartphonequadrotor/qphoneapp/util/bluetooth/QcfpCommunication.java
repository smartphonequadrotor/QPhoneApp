package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

public class QcfpCommunication {
	
	/**
	 * Max encoded packet size includes an extra byte at the start and end and
	 * up to 1 additional byte of overhead
	 * An encoded packet will never exceed 255 bytes
	 */
	private static final int QCFP_MAX_ENCODED_PACKET_SIZE = QcfpParser.QCFP_MAX_PACKET_SIZE + 3;
	
	/**
	 * This method is equivalent in function to the function qcfp_send_data in
	 * the firmware.
	 * 
	 * @param buffer
	 *            The buffer to send.
	 * @param length
	 *            The length of the buffer.
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

		encodedData[++encodedDataIndex] = QcfpParser.COBS_TERM_BYTE;

		byte[] returnArray = new byte[encodedDataIndex];
		System.arraycopy(encodedData, 0, returnArray, 0, encodedDataIndex);
		return returnArray;
	}
}
