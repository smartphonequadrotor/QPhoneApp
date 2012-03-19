/**
 * 
 */
package com.ventus.smartphonequadrotor.qphoneapp.test.util.bluetooth;

import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCallback;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpHandlers;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpParser;

import junit.framework.TestCase;
import org.junit.Assert;

/**
 * @author Jordan
 *
 */
public class QcfpParserTest extends TestCase {

	Boolean handler0Run = false;
	Boolean handler1Run = false;
	Boolean handler2Run = false;
	Boolean handler3Run = false;
	Boolean handler4Run = false;
	Boolean handler5Run = false;
	Boolean handler6Run = false;
	
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
		byte[] buffer6 = {2, 3, 1}; // Valid 0x03 command, uses previous term byte, doesn't finish
		byte[] buffer7 = {0, 5, 4, 1, 2, 3, 0}; // Valid 0x04 command, terminates previous command
		byte[] buffer8 = {4, 5, 5, 5}; // Next three packets are a single, valid 0x05 command
		byte[] buffer9 = {4, 5, 5, 5};
		byte[] buffer10= {4, 5, 5, 5, 0};
		byte[] buffer11= {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}; // Too big
		byte[] buffer12= {0, 2, 6, 0}; // Valid 0x06 after overflow
		
		QcfpHandlers h = new QcfpHandlers();
		
		h.registerHandler(0, new QcfpCallback(){
			public void run(byte[] packet, int length)
			{
				assertEquals(4, length);
				byte[] actual = new byte[length];
				System.arraycopy(packet, 0, actual, 0, length);
				Assert.assertArrayEquals(new byte[]{0, 0, 5, 0}, actual);
				handler0Run = true;
			}
		});
		
		h.registerHandler(1, new QcfpCallback(){
			public void run(byte[] packet, int length)
			{
				assertEquals(4, length);
				byte[] actual = new byte[length];
				System.arraycopy(packet, 0, actual, 0, length);
				Assert.assertArrayEquals(new byte[]{1, 2, 3, 0}, actual);
				handler1Run = true;
			}
		});
		
		h.registerHandler(2, new QcfpCallback(){
			public void run(byte[] packet, int length)
			{
				assertEquals(3, length);
				byte[] actual = new byte[length];
				System.arraycopy(packet, 0, actual, 0, length);
				Assert.assertArrayEquals(new byte[]{2, 3, 0}, actual);
				handler2Run = true;
			}
		});
		
		h.registerHandler(3, new QcfpCallback(){
			public void run(byte[] packet, int length)
			{
				assertEquals(3, length);
				byte[] actual = new byte[length];
				System.arraycopy(packet, 0, actual, 0, length);
				Assert.assertArrayEquals(new byte[]{3, 0, 0}, actual);
				handler3Run = true;
			}
		});
		
		h.registerHandler(4, new QcfpCallback(){
			public void run(byte[] packet, int length)
			{
				assertEquals(5, length);
				byte[] actual = new byte[length];
				System.arraycopy(packet, 0, actual, 0, length);
				Assert.assertArrayEquals(new byte[]{4, 1, 2, 3, 0}, actual);
				handler4Run = true;
			}
		});
		
		h.registerHandler(5, new QcfpCallback(){
			public void run(byte[] packet, int length)
			{
				assertEquals(12, length);
				byte[] actual = new byte[length];
				System.arraycopy(packet, 0, actual, 0, length);
				Assert.assertArrayEquals(new byte[]{5, 5, 5, 0, 5, 5, 5, 0, 5, 5, 5, 0}, actual);
				handler5Run = true;
			}
		});
		
		h.registerHandler(6, new QcfpCallback(){
			public void run(byte[] packet, int length)
			{
				assertEquals(2, length);
				byte[] actual = new byte[length];
				System.arraycopy(packet, 0, actual, 0, length);
				Assert.assertArrayEquals(new byte[]{6, 0}, actual);
				handler6Run = true;
			}
		});
		
		QcfpParser p = new QcfpParser(QcfpParser.QCFP_MAX_PACKET_SIZE, h);
		
		assertFalse(handler0Run);
		p.addData(buffer1, buffer1.length);
		assertTrue(handler0Run);
		
		p.addData(buffer2, buffer2.length);
		
		assertFalse(handler1Run);
		p.addData(buffer3, buffer3.length);
		assertTrue(handler1Run);
		
		p.addData(buffer4, buffer4.length);
		
		assertFalse(handler2Run);
		p.addData(buffer5, buffer5.length);
		assertTrue(handler2Run);
		
		p.addData(buffer6, buffer6.length);
		
		assertFalse(handler3Run);
		assertFalse(handler4Run);
		p.addData(buffer7, buffer7.length);
		assertTrue(handler3Run);
		assertTrue(handler4Run);
		
		p.addData(buffer8, buffer8.length);
		p.addData(buffer9, buffer9.length);
		assertFalse(handler5Run);
		p.addData(buffer10, buffer10.length);
		assertTrue(handler5Run);
		
		p.addData(buffer11, buffer11.length);
		
		assertFalse(handler6Run);
		p.addData(buffer12, buffer12.length);
		assertTrue(handler6Run);
	}

}
