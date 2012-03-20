package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

public class QcfpParser {
	
	private enum cobsState
	{
		COBS_DECODE,
		COBS_COPY,
		COBS_SYNC,
	}
	
	public static final int QCFP_MAX_PACKET_SIZE = 32;
	public static byte COBS_TERM_BYTE = 0;
	
	private int maxPacketSize;
	private cobsState decode_state;
	private byte incoming_packet[]; // Extra space in case of overflow
	private int packet_size; // Counts packet size
	private int byte_count; // Counts number of encoded bytes
	private QcfpHandlers packetHandlers;
	
	/**
	 * Creates a parser object that will not allow a decoded packet greater
	 * than size maxPacketSize.
	 * @param maxPacketSize Maximum allowable packet size.
	 * @param packetHandlers Object that will handle packets.
	 */
	public QcfpParser(int maxPacketSize, QcfpHandlers packetHandlers) {
		this.maxPacketSize = maxPacketSize;
		this.byte_count = 0;
		this.packet_size = 0;
		this.decode_state = cobsState.COBS_SYNC;
		this.incoming_packet = new byte[this.maxPacketSize+2];
		this.packetHandlers = packetHandlers;
	}
	
	/**
	 * Adds the data in buffer of length to any previously processed data.
	 * This method is equivalent in function to the function qcfp_data_received
	 * in the firmware.
	 * @param buffer Data to be processed.
	 * @param length Number of bytes to process.
	 */
	public void addData(byte[] buffer, int length) {

		// Decode data from buffer
		for(int i = 0; i < length; i++)
		{
			if(this.packet_size > this.maxPacketSize)
			{
				this.decode_state = cobsState.COBS_SYNC;
			}

			switch(this.decode_state)
			{
			case COBS_DECODE:
				if(buffer[i] == COBS_TERM_BYTE)
				{
					if((this.packet_size > 0) && (this.byte_count == 0))
					{
						// Handle packet
						if(this.incoming_packet[this.packet_size] == COBS_TERM_BYTE)
						{
							this.packet_size--;
						}
						this.packetHandlers.dispatch(this.incoming_packet, this.packet_size);
					}
					this.packet_size = 0;
					this.byte_count = 0;
				}
				else
				{
					this.byte_count = buffer[i];
					this.decode_state = cobsState.COBS_COPY;
				}
				break;
			case COBS_COPY:
				if(this.byte_count == 1)
				{
					this.incoming_packet[this.packet_size++] = 0;
					i--; // i points to the next data chunk byte currently, which is what the decode state needs
					this.byte_count--;
					this.decode_state = cobsState.COBS_DECODE;
				}
				else
				{
					if(buffer[i] == COBS_TERM_BYTE)
					{
						// Got a zero when expecting data, re-sync
						this.byte_count = 0;
						this.packet_size = 0;
						this.decode_state = cobsState.COBS_DECODE;
					}
					else
					{
						if(byte_count > 1)
						{
							this.incoming_packet[this.packet_size++] = buffer[i];
							this.byte_count--;
						}
					}
				}
				break;
			case COBS_SYNC:
			default:
				this.packet_size = 0;
				this.byte_count = 0;
				if(buffer[i] == COBS_TERM_BYTE)
				{
					this.decode_state = cobsState.COBS_DECODE;
				}
				break;
			}
		}
	}
}
