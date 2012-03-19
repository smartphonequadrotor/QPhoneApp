package com.ventus.smartphonequadrotor.qphoneapp.test.util.control;

import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;

import junit.framework.TestCase;

/**
 * The {@link SimpleMatrix} class contains a lot of method that had to be 
 * implemented ourselves. These are the only methods we will be testing.
 * @author abhin
 *
 */
public class SimpleMatrixTest extends TestCase {
	public void testEquals() {
		SimpleMatrix lhs = new SimpleMatrix(2, 2);
		SimpleMatrix rhs = new SimpleMatrix(2, 2);
		for (int row = 0; row < lhs.numRows(); row++) {
			for (int col = 0; col < lhs.numCols(); col++) {
				lhs.set(row, col, row*lhs.numRows() + col);
				rhs.set(row, col, row*lhs.numRows() + col);
			}
		}
		
		assertTrue("SimpleMatrix equals method doesn't work", lhs.equals(rhs));
	}
	
	/**
	 * Test method for the contstructor that accepts a double array to initialize the matrix.
	 */
	public void testArrayConstructor() {
		SimpleMatrix matRowMajor = new SimpleMatrix(2, 3, true, new double[] {1, 2, 3, 4, 5, 6});
		SimpleMatrix matColMajor = new SimpleMatrix(2, 3, false, new double[] {1, 2, 3, 4, 5, 6});
		//sample random points in the matrices
		assertEquals(2.0, matRowMajor.get(0, 1));
		assertEquals(5.0, matRowMajor.get(1, 1));
		assertEquals(3.0, matColMajor.get(0, 1));
		assertEquals(4.0, matColMajor.get(1, 1));
		
		try {
			SimpleMatrix mat = new SimpleMatrix(2, 3, true, new double[] {});
			fail("IllegalArgumentException not throws when array passed to constructor was of wrong size");
		} catch (IllegalArgumentException iae) {
			//this was expected. This is test passed
			assertTrue("IllegalArgumentException throws when expected", true);
		}
		
		try {
			SimpleMatrix mat = new SimpleMatrix(2, 3, true, null);
			fail("IllegalArgumentException not throws when array passed to constructor was of wrong size");
		} catch (IllegalArgumentException iae) {
			//this was expected. This is test passed
			assertTrue("IllegalArgumentException throws when expected", true);
		}
	}

	/**
	 * Test method for {@link SimpleMatrix#ones(int)}.
	 */
	public void testOnes() {
		SimpleMatrix refMat = new SimpleMatrix(3, 2, true, new double[] {1, 1, 1, 1, 1, 1});
		assertEquals(refMat, SimpleMatrix.ones(3, 2));
	}
	
	/**
	 * Test method for {@link SimpleMatrix#elementwiseDivision(SimpleMatrix)}.
	 */
	public void testElementwiseDivision() {
		SimpleMatrix divident = new SimpleMatrix(2, 2, true, new double[] {1, 2, 3, 4});
		assertEquals(SimpleMatrix.ones(2, 2), divident.elementwiseDivision(divident));
		
		SimpleMatrix divisor = new SimpleMatrix(2, 2, true, new double[] {4, 3, 2, 1});
		assertEquals(
			new SimpleMatrix(2, 2, true, new double[]{1.0/4.0, 2.0/3.0, 3.0/2.0, 4.0}), 
			divident.elementwiseDivision(divisor)
		);
	}

	/**
	 * Test method for {@link SimpleMatrix#mult(double)}.
	 */
	public void testMultDouble() {
		SimpleMatrix mat = new SimpleMatrix(2, 3, true, new double[] {1, 2, 3, 4, 5, 6});
		SimpleMatrix refMat = new SimpleMatrix(2, 3, true, new double[] {2, 4, 6, 8, 10, 12});
		assertEquals(refMat, mat.mult(2));
	}

	/**
	 * Test method for {@link SimpleMatrix#repmat(int, int)}.
	 */
	public void testRepmat() {
		SimpleMatrix mat = new SimpleMatrix(1, 2, true, new double[] {2, 3});
		SimpleMatrix refMat = new SimpleMatrix(3, 4, true, new double[] {2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3});
		assertEquals(refMat, mat.repmat(3, 2));
	}
}
