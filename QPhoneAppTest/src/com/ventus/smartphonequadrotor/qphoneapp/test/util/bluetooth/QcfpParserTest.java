/**
 * 
 */
package com.ventus.smartphonequadrotor.qphoneapp.test.util.bluetooth;

import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCallback;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpHandlers;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpParser;

import junit.framework.TestCase;

/**
 * @author Jordan
 *
 */
public class QcfpParserTest extends TestCase {

	/**
	 * @param name
	 */
	public QcfpParserTest(String name) {
		super(name);
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
	 * Test method for {@link com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpParser#addData(byte[], int)}.
	 */
	public void testAddData() {
		byte[] buffer1 = {0, 1, 1, 2, 5, 0}; // Valid packet with a 0 command
		byte[] buffer2 = {10, 5, 0, 0, 1, 7}; // Garbage data
		byte[] buffer3 = {0, 4, 1, 2, 3, 0}; // Valid 0x01 command
		byte[] buffer4 = {0, 0, 0, 0, 0, 5, 0, 2, 0}; // Garbage data
		byte[] buffer5 = {0, 3, 2, 3, 0}; // Valid 0x02 command
		
		QcfpHandlers h = new QcfpHandlers();
		
		h.registerHandler(0, new QcfpCallback(){
			public void run(byte[] packet, int length)
			{
				assertEquals(4, length);
				byte[] compareArray = new byte[length];
				System.arraycopy(packet, 0, compareArray, 0, length);
				byte[] a = new byte[]{0, 0, 5, 0};
				assertEquals(a, compareArray);
			}
		});
		
		h.registerHandler(1, new QcfpCallback(){
			public void run(byte[] packet, int length)
			{
				assertEquals(4, length);
				assertEquals(new byte[]{1, 2, 3, 0}, packet);
			}
		});
		
		h.registerHandler(2, new QcfpCallback(){
			public void run(byte[] packet, int length)
			{
				assertEquals(3, length);
				assertEquals(new byte[]{2, 3, 0}, packet);
			}
		});
		
		QcfpParser p = new QcfpParser(QcfpParser.MAX_QCFP_PACKET_SIZE, h);
		p.addData(buffer1, buffer1.length);
		p.addData(buffer2, buffer2.length);
		p.addData(buffer3, buffer3.length);
		p.addData(buffer4, buffer4.length);
		p.addData(buffer5, buffer5.length);
	}

}
