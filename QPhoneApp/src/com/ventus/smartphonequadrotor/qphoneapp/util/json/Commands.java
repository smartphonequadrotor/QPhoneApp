package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class Commands {
	private MoveCommand[] Move;
	private String SystemState;

	public Commands(MoveCommand[] move, String SystemState) {
		Move = move;
		this.SystemState = SystemState;
	}

	public MoveCommand[] getMoveCommandArray() {
		return Move;
	}
	
	public String getSystemState() {
		return SystemState;
	}

	@Override
	public String toString() {
		return String.format("Move: %1$s\nSystemState: %1$s\n", Move.toString(), SystemState);
	}
}