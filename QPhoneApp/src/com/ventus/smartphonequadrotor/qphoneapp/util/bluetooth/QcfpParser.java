package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

public class QcfpParser {
	
	private enum cobsState
	{
		COBS_DECODE,
		COBS_COPY,
		COBS_SYNC,
	}
	
	private static int COBS_TERM_BYTE = 0;
	
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
	 * @param buffer Data to be processed.
	 * @param length Number of bytes to process.
	 */
	public void addData(byte[] buffer, int length) {

		int i;

		// Decode data from buffer
		for(i = 0; i < length; i++)
		{
			if(packet_size > this.maxPacketSize)
			{
				decode_state = cobsState.COBS_SYNC;
			}

			switch(decode_state)
			{
			case COBS_DECODE:
				if(buffer[i] == COBS_TERM_BYTE)
				{
					if(packet_size > 0)
					{
						// Handle packet
						this.packetHandlers.dispatch(this.incoming_packet, this.packet_size);
					}
					packet_size = 0;
					byte_count = 0;
				}
				else
				{
					byte_count = buffer[i];
					decode_state = cobsState.COBS_COPY;
				}
				break;
			case COBS_COPY:
				if(buffer[i] == COBS_TERM_BYTE)
				{
					// Got a zero when expecting data, re-sync
					byte_count = 0;
					packet_size = 0;
					decode_state = cobsState.COBS_DECODE;
				}
				else
				{
					if(byte_count > 1)
					{
						incoming_packet[packet_size++] = buffer[i];
						byte_count--;
					}
					else // byte_count == 1
					{
						incoming_packet[packet_size++] = buffer[i];
						byte_count--;
						// We add two bytes to the packet here which could result in the packet
						// being larger than the max size which is why the packet has a couple of
						// extra bytes in it
						incoming_packet[packet_size++] = 0;
						decode_state = cobsState.COBS_DECODE;
					}
				}
				break;
			case COBS_SYNC:
			default:
				packet_size = 0;
				byte_count = 0;
				if(buffer[i] == COBS_TERM_BYTE)
				{
					decode_state = cobsState.COBS_DECODE;
				}
				break;
			}
		}
	}
}
