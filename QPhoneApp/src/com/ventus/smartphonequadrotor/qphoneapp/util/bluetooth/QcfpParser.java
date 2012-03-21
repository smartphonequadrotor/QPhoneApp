package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

public class QcfpParser {

	private enum cobsState {
		COBS_DECODE, 
		COBS_COPY, 
		COBS_SYNC,
	}

	public static final int QCFP_MAX_PACKET_SIZE = 32;
	public static byte COBS_TERM_BYTE = 0;

	private int maxPacketSize;
	private cobsState decodeState;
	private byte incomingPacket[]; // Extra space in case of overflow
	private int packetSize; // Counts packet size
	private int byteCount; // Counts number of encoded bytes
	private QcfpHandlers packetHandlers;

	/**
	 * Creates a parser object that will not allow a decoded packet greater than
	 * size maxPacketSize.
	 * 
	 * @param maxPacketSize
	 *            Maximum allowable packet size.
	 * @param packetHandlers
	 *            Object that will handle packets.
	 */
	public QcfpParser(int maxPacketSize, QcfpHandlers packetHandlers) {
		this.maxPacketSize = maxPacketSize;
		this.byteCount = 0;
		this.packetSize = 0;
		this.decodeState = cobsState.COBS_SYNC;
		this.incomingPacket = new byte[this.maxPacketSize + 2];
		this.packetHandlers = packetHandlers;
	}

	/**
	 * Adds the data in buffer of length to any previously processed data. This
	 * method is equivalent in function to the function qcfp_data_received in
	 * the firmware.
	 * 
	 * @param buffer
	 *            Data to be processed.
	 * @param length
	 *            Number of bytes to process.
	 */
	public void addData(byte[] buffer, int length) {

		// Decode data from buffer
		for (int i = 0; i < length; i++) {
			if (this.packetSize > this.maxPacketSize) {
				this.decodeState = cobsState.COBS_SYNC;
			}

			switch (this.decodeState) {
			case COBS_DECODE:
				if (buffer[i] == COBS_TERM_BYTE) {
					if ((this.packetSize > 0) && (this.byteCount == 0)) {
						// Handle packet
						if (this.incomingPacket[this.packetSize] == COBS_TERM_BYTE) {
							this.packetSize--;
						}
						this.packetHandlers.dispatch(this.incomingPacket, this.packetSize);
					}
					this.packetSize = 0;
					this.byteCount = 0;
				} else {
					this.byteCount = buffer[i];
					this.decodeState = cobsState.COBS_COPY;
				}
				break;
			case COBS_COPY:
				if (this.byteCount == 1) {
					this.incomingPacket[this.packetSize++] = 0;
					i--; 	// i points to the next data chunk byte currently,
							// which is what the decode state needs
					this.byteCount--;
					this.decodeState = cobsState.COBS_DECODE;
				} else {
					if (buffer[i] == COBS_TERM_BYTE) {
						// Got a zero when expecting data, re-sync
						this.byteCount = 0;
						this.packetSize = 0;
						this.decodeState = cobsState.COBS_DECODE;
					} else {
						if (byteCount > 1) {
							this.incomingPacket[this.packetSize++] = buffer[i];
							this.byteCount--;
						}
					}
				}
				break;
			case COBS_SYNC:
			default:
				this.packetSize = 0;
				this.byteCount = 0;
				if (buffer[i] == COBS_TERM_BYTE) {
					this.decodeState = cobsState.COBS_DECODE;
				}
				break;
			}
		}
	}
}
