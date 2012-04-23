package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class Commands {
	private MoveCommand[] Move;
	private THrpyCommand[] THRPY;
	private String SystemState;
	private String[] Debug;

	public Commands(MoveCommand[] move, THrpyCommand[] thrpy, String SystemState, String[] debug) {
		Move = move;
		THRPY = thrpy;
		this.SystemState = SystemState;
		this.Debug = debug;
	}

	public MoveCommand[] getMoveCommandArray() {
		return Move;
	}
	
	public THrpyCommand[] getHrpyCommandArray() {
		return THRPY;
	}
	
	public String getSystemState() {
		return SystemState;
	}
	
	public String[] getDebug() {
		return Debug;
	}

	@Override
	public String toString() {
		return String.format("Move: %1$s\nHrpy: %2$s\nSystemState: %3$s\nDebug: %4$s\n", Move, THRPY, SystemState, Debug);
	}
}