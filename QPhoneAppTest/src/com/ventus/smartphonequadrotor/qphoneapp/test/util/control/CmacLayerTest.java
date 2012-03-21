package com.ventus.smartphonequadrotor.qphoneapp.test.util.control;

import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.CmacInputParam;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.CmacLayer;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.CmacOutput;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.ControlLoop;

import junit.framework.TestCase;

/**
 * This will test the {@link CmacLayer} class's ability to recall values 
 * stored in its lookup tables and to update the values when feedback is 
 * available. 
 * @author abhin
 *
 */
public class CmacLayerTest extends TestCase {

	private CmacLayer layer;
	private SimpleMatrix tenPercentOffset;
	private SimpleMatrix stateInterval;
	
	/**
	 * @param name
	 */
	public CmacLayerTest(String name) {
		super(name);
		stateInterval = CmacInputParam.getDefaultMaxBound()
						.minus(CmacInputParam.getDefaultMinBound())
						.divide(CmacLayer.DEFAULT_QUANTIZATION_NUMBER);
		stateInterval = stateInterval.round();
		tenPercentOffset = stateInterval.divide(10);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		layer = new CmacLayer(tenPercentOffset);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		layer = null;	//not really necessary, but why not.
	}

	/**
	 * Test method for {@link CmacLayer#roundAndOffsetInput(SimpleMatrix)}.
	 */
	public void testRoundAndOffsetInput() {
		//if 10% of the default state interval is added to the min-bound, then rounding and offsetting
		//it would give us back the min-bound
		SimpleMatrix input = CmacInputParam.getDefaultMinBound().plus(tenPercentOffset);
		SimpleMatrix output = layer.roundAndOffsetInput(input);
		SimpleMatrix expected = CmacInputParam.getDefaultMinBound();
		//since the equals method for SimpleMatrix class has been defined and tested, its OK to use it here
		assertEquals(expected, output);
		
		input = CmacInputParam.getDefaultMaxBound().minus(tenPercentOffset);
		output = layer.roundAndOffsetInput(input);
		expected = CmacInputParam.getDefaultMaxBound().minus(stateInterval).round();
		assertEquals(expected, output);
	}

	/**
	 * Test method for {@link CmacLayer#applyDeltas(SimpleMatrix, SimpleMatrix, SimpleMatrix)}.
	 */
	public void testApplyDeltas() {
		SimpleMatrix input = CmacInputParam.getDefaultMinBound().plus(tenPercentOffset);
		SimpleMatrix deltaControlWeights = SimpleMatrix.ones(1, CmacOutput.NUMBER_OF_WEIGHTS);
		SimpleMatrix deltaAlternateWeights = SimpleMatrix.ones(1, CmacOutput.NUMBER_OF_WEIGHTS).mult(-1);
		
		layer.applyDeltas(input, deltaControlWeights, deltaAlternateWeights, 1000);
		SimpleMatrix key = CmacInputParam.getDefaultMinBound();
		assertEquals(deltaControlWeights, layer.getRawControlWeights().get(key));
		assertEquals(deltaAlternateWeights, layer.getRawAlternateWeights().get(key));
		
		layer.applyDeltas(input, deltaControlWeights, deltaAlternateWeights, 1000);
		assertEquals(deltaControlWeights.mult(2), layer.getRawControlWeights().get(key));
		assertEquals(deltaAlternateWeights.mult(2), layer.getRawAlternateWeights().get(key));
		
		input = CmacInputParam.getDefaultMaxBound().minus(tenPercentOffset);
		layer.applyDeltas(input, deltaControlWeights, deltaAlternateWeights, 1000);
		assertEquals(deltaControlWeights, layer.getRawControlWeights().get(CmacInputParam.getDefaultMaxBound().minus(stateInterval)));
		assertEquals(deltaAlternateWeights, layer.getRawAlternateWeights().get(CmacInputParam.getDefaultMaxBound().minus(stateInterval)));
	}

	/**
	 * Test method for {@link CmacLayer#query(SimpleMatrix)}.
	 */
	public void testQuery() {
		SimpleMatrix input = CmacInputParam.getDefaultMinBound().plus(tenPercentOffset);
		SimpleMatrix deltaControlWeights = SimpleMatrix.ones(1, CmacOutput.NUMBER_OF_WEIGHTS);
		SimpleMatrix deltaAlternateWeights = SimpleMatrix.ones(1, CmacOutput.NUMBER_OF_WEIGHTS).mult(-1);
		
		layer.applyDeltas(input, deltaControlWeights, deltaAlternateWeights, 1000);
		assertEquals(deltaControlWeights, layer.query(input).getControlWeights());
		assertEquals(deltaControlWeights, layer.query(input.plus(tenPercentOffset)).getControlWeights());
		assertEquals(deltaControlWeights, layer.query(CmacInputParam.getDefaultMinBound()
				.plus(stateInterval).minus(tenPercentOffset)).getControlWeights());
		assertEquals(deltaAlternateWeights, layer.query(input).getAlternateWeights());
		assertEquals(deltaAlternateWeights, layer.query(input.plus(tenPercentOffset)).getAlternateWeights());
		assertEquals(deltaAlternateWeights, layer.query(CmacInputParam.getDefaultMinBound()
				.plus(stateInterval).minus(tenPercentOffset)).getAlternateWeights());
	}

}
