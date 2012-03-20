package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

public class QcfpCommunication {
	
	/**
	 * Max encoded packet size includes an extra byte at the start and end and
	 * up to 1 additional byte of overhead
	 * An encoded packet will never exceed 255 bytes
	 */
	private static final int QCFP_MAX_ENCODED_PACKET_SIZE = QcfpParser.QCFP_MAX_PACKET_SIZE + 3;
	
	/**
	 * This method is equivalent in function to the function qcfp_send_data
	 * in the firmware.
	 * @param buffer The buffer to send.
	 * @param length The length of the buffer.
	 * @return Returns a COBS encoded byte array that can be sent to the QCB.
	 * @throws Exception 
	 */
	public static byte[] encodeData(byte[] buffer, int length) throws Exception
	{
		if(length > QcfpParser.QCFP_MAX_PACKET_SIZE)
		{
			throw new Exception("Length of buffer is too large to be encoded.");
		}
		
		byte[] encoded_data = new byte[QCFP_MAX_ENCODED_PACKET_SIZE];
		byte byte_count = 1;
		int encoded_data_index = 1, chunk_index = 1;

		// First byte is always COBS_TERM_BYTE
		encoded_data[0] = QcfpParser.COBS_TERM_BYTE;

		for(int i = 0; i < length; i++, byte_count++)
		{
			if(buffer[i] == QcfpParser.COBS_TERM_BYTE)
			{
				encoded_data[chunk_index] = byte_count;
				chunk_index = ++encoded_data_index;
				byte_count = 0;
			}
			else
			{
				encoded_data[++encoded_data_index] = buffer[i];
			}
		}
		
		if(byte_count > 1)
		{
			encoded_data[chunk_index] = byte_count;
			encoded_data_index++;
		}
		
		if(buffer[length-1] == QcfpParser.COBS_TERM_BYTE)
		{
			encoded_data[encoded_data_index++] = 1;
		}
		
		encoded_data[++encoded_data_index] = QcfpParser.COBS_TERM_BYTE;
		
		byte[] return_array = new byte[encoded_data_index];
		System.arraycopy(encoded_data, 0, return_array, 0, encoded_data_index);
		return return_array;
	}
}
