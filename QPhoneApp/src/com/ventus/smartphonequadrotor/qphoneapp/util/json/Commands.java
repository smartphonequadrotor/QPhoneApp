package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class Commands {
	private MoveCommand[] Move;
	private String SystemState;
	private String Debug;

	public Commands(MoveCommand[] move, String SystemState, String debug) {
		Move = move;
		this.SystemState = SystemState;
		this.Debug = debug;
	}

	public MoveCommand[] getMoveCommandArray() {
		return Move;
	}
	
	public String getSystemState() {
		return SystemState;
	}
	
	public String getDebug() {
		return Debug;
	}

	@Override
	public String toString() {
		return String.format("Move: %1$s\nSystemState: %1$s\nDebug: %2$s\n", Move.toString(), SystemState, Debug);
	}
}