package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import org.ejml.simple.SimpleBase;

/**
 * This method adds additional features to the ones offered by the 
 * {@link org.ejml.simple.SimpleMatrix} class.
 * @author abhin
 *
 */
public class SimpleMatrix extends SimpleBase<SimpleMatrix> {
	public SimpleMatrix(int rows, int cols) {
		super(rows, cols);
	}
	
	/**
	 * This method creates a {@link SimpleMatrix} from a given one-dimensional double array
	 * and the desired dimensions.
	 * @param rows The number of rows in the desired matrix
	 * @param cols The number of columns in the desired matrix
	 * @param rowMajor Whether the one-dimensional array has to be read row major or not
	 * @param mat The one-dimensional array that has to have rows*cols elements
	 */
	public SimpleMatrix (int rows, int cols, boolean rowMajor, double[] mat) {
		super(rows, cols);
		if (rows*cols != mat.length)
			throw new IllegalArgumentException(String.format("mat has to have %d elements", rows*cols));
		
		if (rowMajor) {
			for (int row = 0; row < rows; row++)
				for (int col = 0; col < cols; col++)
					this.set(row, col, mat[row*cols + col]);
		} else {
			for (int col = 0; col < cols; col++)
				for (int row = 0; row < rows; row++)
					this.set(row, col, mat[col*rows + row]);
		}
	}
	
	/**
	 * Don't let anyone used the default constructor.
	 */
	protected SimpleMatrix() {}
	
	@Override
	protected SimpleMatrix createMatrix(int numRows, int numCols) {
		return new SimpleMatrix(numRows, numCols);
	}
	
	/**
	 * This method divides the current matrix by the provided matrix
	 * element-by-element and returns the result.
	 * @param mat
	 * @return
	 */
	public SimpleMatrix elementwiseDivision (SimpleMatrix mat) {
		if (this.numCols() != mat.numCols() || this.numRows() != mat.numRows())
			throw new IllegalArgumentException("Both matrices have to have the same dimensions");
		
		SimpleMatrix quotient = new SimpleMatrix(this.numRows(), this.numCols());
		for (int i = 0; i < this.getNumElements(); i++)
			quotient.set(i, this.get(i) / mat.get(i));
		
		return quotient;
	}
	
	/**
	 * Multiplies all element of the matrix with a constant double.
	 * @param arg
	 * @return
	 */
	public SimpleMatrix mult(double arg) {
		SimpleMatrix product = new SimpleMatrix(this.numRows(), this.numCols());
		for (int i = 0; i < product.getNumElements(); i++) {
			product.set(i, this.get(i) * arg);
		}
		return product;
	}
	
	/**
	 * This method repeats the matrix by the given number of times in the x and y axes.
	 * @param rowRep The number of times to repeat the matrix along the y-axis
	 * @param colRep The number of times to repeat the matrix along the x-axis
	 * @return
	 */
	public SimpleMatrix repmat (int rowRep, int colRep) {
		SimpleMatrix returnVal = new SimpleMatrix (numRows()*rowRep, numCols()*colRep);
		for (int x = 0; x < colRep; x++) {
			for (int y = 0; y < rowRep; y++) {
				returnVal.insertIntoThis(y*this.numRows(), x*this.numCols(), this);
			}
		}
		return returnVal;
	}
	
	/**
	 * Creates a row matrix of ones of the given length.
	 * @param number
	 * @return
	 */
	public static SimpleMatrix ones (int length) {
		SimpleMatrix ones = new SimpleMatrix(1, length);
		ones.set(1);
		return ones;
	}
}
