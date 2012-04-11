package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class HeightSensorResponse extends ResponseAbstract {
	private Integer Height;
	
	public HeightSensorResponse(Integer height, long timestamp) {
		super(timestamp);
		this.Height = height;
	}

	public Integer getHeight() {
		return Height;
	}

	@Override
	public String toString() {
		return String.format("%1$s Height: %2$d\n", super.toString(), Height);
	}
}
