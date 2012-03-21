package com.ventus.smartphonequadrotor.qphoneapp.util;

import java.text.DecimalFormat;

import org.ejml.simple.SimpleBase;

/**
 * This method adds additional features to the ones offered by the 
 * {@link org.ejml.simple.SimpleMatrix} class.
 * @author abhin
 *
 */
public class SimpleMatrix extends SimpleBase<SimpleMatrix> {
	private static final DecimalFormat df = new DecimalFormat("#.######");
	
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
		
		if (mat == null)
			throw new IllegalArgumentException("Mat is null");
		
		if (rows*cols != mat.length)
			throw new IllegalArgumentException(String.format("Mat has to have %d elements", rows*cols));
		
		if (rowMajor) {
			for (int row = 0; row < rows; row++)
				for (int col = 0; col < cols; col++)
					this.set(row, col, round(mat[row*cols + col]));
		} else {
			for (int col = 0; col < cols; col++)
				for (int row = 0; row < rows; row++)
					this.set(row, col, round(mat[col*rows + row]));
		}
	}
	
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
	 * Creates a matrix of the given size that is all ones.
	 * @param rows
	 * @param cols
	 * @return
	 */
	public static SimpleMatrix ones (int rows, int cols) {
		SimpleMatrix ones = new SimpleMatrix(rows, cols);
		ones.set(1);
		return ones;
	}
	
	/**
	 * Creates a matrix of the given size that is all zeros.
	 * @param rows
	 * @param cols
	 * @return
	 */
	public static SimpleMatrix zeros (int rows, int cols) {
		SimpleMatrix zeros = new SimpleMatrix(rows, cols);
		zeros.set(0);
		return zeros;
	}
	
	/**
	 * Because of rounding concerns, it has been decided that all the values
	 * will be rounded off at 6 decimal places.
	 * @param arg
	 * @return
	 */
	public static double round(double arg) {
		return Double.valueOf(df.format(arg));
	}

	/**
	 * Because of rounding concerns, it has been decided that all the values
	 * will be rounded off at 6 decimal places.
	 * @param arg
	 * @return
	 */
	public SimpleMatrix round() {
		SimpleMatrix rounded = new SimpleMatrix(this.numRows(), this.numCols());
		for (int i = 0; i < this.getNumElements(); i++) {
			rounded.set(i, round(this.get(i)));
		}
		return rounded;
	}
	
	/**
	 * This method takes the absolute value of all the elements of the given
	 * matrix.
	 * @param arg
	 * @return
	 */
	public SimpleMatrix elementWiseAbs () {
		SimpleMatrix abs = new SimpleMatrix(this.numRows(), this.numCols());
		for (int i = 0; i < this.getNumElements(); i++) {
			abs.set(i, Math.abs(this.get(i)));
		}
		return abs;
	}
	
	@Override
	public boolean equals(Object o) {
		//Optimisation
		if (this == o) {
			return true;
		}
		
		//wrong type
		if (!(o instanceof SimpleMatrix)) {
			return false;
		}
		
		SimpleMatrix lhs = (SimpleMatrix) o;
		
		//check dimensions
		if (lhs.numCols() != this.numCols() || lhs.numRows() != this.numRows()) {
			return false;
		}
		
		//check data
		for (int row = 0; row < this.numRows(); row++) {
			for (int col = 0; col < this.numCols(); col++) {
				if (round(this.get(row, col)) != round(lhs.get(row, col))) {
					return false;
				}
			}
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		final int SEED = 23;
		final int PRIME = 37;
		int hash = SEED;
		for (int i = 0; i < this.getNumElements(); i++) {
			long longBits = Double.doubleToRawLongBits(round(this.get(i)));
			hash = ((hash*PRIME) + (int)(longBits ^ (longBits >>> 32)));
		}
		return hash;
	}
}
