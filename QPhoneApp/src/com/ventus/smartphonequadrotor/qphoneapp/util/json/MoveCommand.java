package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class MoveCommand implements Cloneable{
	private float XVector;
	private float YVector;
	private float ZVector;
	private int Speed;
	private int Duration;
	public MoveCommand(float xVector, float yVector, float zVector, int speed, int duration) {
		XVector = xVector;
		YVector = yVector;
		ZVector = zVector;
		Speed = speed;
		Duration = duration;
	}
	public float getXVector() {
		return XVector;
	}
	public float getYVector() {
		return YVector;
	}
	public float getZVector() {
		return ZVector;
	}
	public int getSpeed() {
		return Speed;
	}
	public int getDuration() {
		return Duration;
	}
	@Override
	public String toString() {
		return String.format(
			"Vector: {%1$f, %2$f, %3$f}\nSpeed: %4$d\nDuration: %5$d", 
			XVector, 
			YVector, 
			ZVector, 
			Speed, 
			Duration
		);
	}	
	
	@Override
	protected MoveCommand clone() {
		return new MoveCommand(XVector, YVector, ZVector, Speed, Duration);
	}
	
	/**
	 * This method normalizes the direction of the move command's direction vector. This
	 * is because we don't actually use the magnitude of this vector in the computation.
	 * @return A move command that is similar to the current command except for the fact
	 * that the direction vector is normalized.
	 */
	public MoveCommand normalizeDirection() {
		double magnitude = Math.sqrt(Math.pow(this.XVector, 2) + Math.pow(this.YVector, 2) + Math.pow(this.ZVector, 2));
		MoveCommand normMvCmd = this.clone();
		normMvCmd.XVector = (float) (this.XVector/magnitude);
		normMvCmd.YVector = (float) (this.YVector/magnitude);
		normMvCmd.ZVector = (float) (this.ZVector/magnitude);
		return normMvCmd;
	}
}
