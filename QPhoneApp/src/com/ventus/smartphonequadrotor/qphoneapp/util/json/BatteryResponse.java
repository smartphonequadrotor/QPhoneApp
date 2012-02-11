package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class BatteryResponse extends ResponseAbstract {
	private float Percentage;
	private String Status;
	private float[] Cells;

	public BatteryResponse(long Timestamp, float percentage, String status, float[] cells) {
		super(Timestamp);
		Percentage = percentage;
		Status = status;
		Cells = cells;
	}

	public float getPercentage() {
		return Percentage;
	}

	public String getStatus() {
		return Status;
	}

	public float[] getCells() {
		return Cells;
	}

	public String toString() {
		return String.format(
			"%1$sPercentage: %2$f\nStatus: %3$s\nCell Voltages: %4$f\n", 
			super.toString(), 
			Percentage, 
			Status, 
			Cells
		);
	}
}
