/**
 * 
 */
package com.ventus.smartphonequadrotor.qphoneapp.test.util.bluetooth;

import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCommunication;

import junit.framework.TestCase;
import org.junit.Assert;

/**
 * @author Jordan
 *
 */
public class QcfpCommunicationTest extends TestCase {

	/**
	 * @param name
	 */
	public QcfpCommunicationTest(String name) {
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
	 * Test method for {@link com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCommunication#encodeData(byte[], int)}.
	 * @throws Exception 
	 */
	public void testEncodeData() throws Exception {
		byte[] data1 = {0};
		byte[] expected1 = {0, 1, 0};
		byte[] result1 = QcfpCommunication.encodeData(data1, data1.length);
		Assert.assertArrayEquals(expected1, result1);
		
		byte[] data2 = {1};
		byte[] expected2 = {0, 2, 1, 0};
		byte[] result2 = QcfpCommunication.encodeData(data2, data2.length);
		Assert.assertArrayEquals(expected2, result2);
		
		byte[] data3 = {0, 0};
		byte[] expected3 = {0, 1, 1, 0};
		byte[] result3 = QcfpCommunication.encodeData(data3, data3.length);
		Assert.assertArrayEquals(expected3, result3);
		
		byte[] data4 = {0, 0, 0};
		byte[] expected4 = {0, 1, 1, 1, 0};
		byte[] result4 = QcfpCommunication.encodeData(data4, data4.length);
		Assert.assertArrayEquals(expected4, result4);
		
		byte[] data5 = {0, 1, 0, 1, 0};
		byte[] expected5 = {0, 1, 2, 1, 2, 1, 0};
		byte[] result5 = QcfpCommunication.encodeData(data5, data5.length);
		Assert.assertArrayEquals(expected5, result5);
		
		byte[] data6 = {1, 1, 1, 1, 1};
		byte[] expected6 = {0, 6, 1, 1, 1, 1, 1, 0};
		byte[] result6 = QcfpCommunication.encodeData(data6, data6.length);
		Assert.assertArrayEquals(expected6, result6);
	}

}
