package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class Commands {
	private MoveCommand[] Move;
	private HrpyCommand[] HRPY;
	private String SystemState;
	private String[] Debug;

	public Commands(MoveCommand[] move, HrpyCommand[] hrpy, String SystemState, String[] debug) {
		Move = move;
		HRPY = hrpy;
		this.SystemState = SystemState;
		this.Debug = debug;
	}

	public MoveCommand[] getMoveCommandArray() {
		return Move;
	}
	
	public HrpyCommand[] getHrpyCommandArray() {
		return HRPY;
	}
	
	public String getSystemState() {
		return SystemState;
	}
	
	public String[] getDebug() {
		return Debug;
	}

	@Override
	public String toString() {
		return String.format("Move: %1$s\nHrpy: %2$s\nSystemState: %3$s\nDebug: %4$s\n", Move, HRPY, SystemState, Debug);
	}
}