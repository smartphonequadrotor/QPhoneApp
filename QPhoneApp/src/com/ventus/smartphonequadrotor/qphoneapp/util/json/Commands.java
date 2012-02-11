package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class Commands {
	private MoveCommand[] Move;

	public Commands(MoveCommand[] move) {
		Move = move;
	}

	public MoveCommand[] getMove() {
		return Move;
	}

	@Override
	public String toString() {
		return String.format("Move: %1$s\n", Move.toString());
	}
}