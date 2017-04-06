package com.ets.log320.lab3;

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Timer;

interface MoveChooser {
	void call(long current, long opponent, long move);
};

class ChooseMove {
	long best = Long.MIN_VALUE;
	long bestmove = 0;

	void call(long current, long opponent, long move) {
		long newBest = Heuristique.evaluate(current ^ move, opponent & ~Long.reverse(move), opponent & Long.reverse(move));

		if (best < newBest) {
			best = newBest;
			bestmove = move;
		}
	}
}

class BestMoves implements MoveChooser {
	@Override
	public void call(long current, long opponent, long move) {


	}
};

class Moves {
	//							  v       v       v       v       v       v       v       v
	final static long up =		0b0000000000000000000000000000000000000000000000000000000100000001L;
	final static long right =	0b0000000000000000000000000000000000000000000000000000000010000001L;
	final static long left =	0b0000000000000000000000000000000000000000000000000000001000000001L;

	static void iterateMoves(int level, long current, long opponent, MoveChooser m) {
		for (int i = 0 ; i < 56 ; i++) {
			if (((1L << i) & current) != 0) {
				if ((i + 1) % 8 > 0) {
					if (((1L << (i + 9)) & current) == 0) {
						m.call(current, opponent, left << i);
					}
				}

				if ((i % 8) > 0) {
					if (((1L << (i + 7)) & current) == 0) {
						m.call(current, opponent, right << i);
					}
				}

				if ((((1L << (i + 8)) & current) | ((1L << (63 - (i + 8))) & opponent)) == 0) {
					m.call(current, opponent, up << i);
				}
			}
		}
    }

    static long fromString(String move){
		long convMove = 0;
		//A1B2  F7E6

		convMove |= 1L << ('H' - move.charAt(0)) + ((move.charAt(1) - '1') * 8);
		convMove |= 1L << ('H' - move.charAt(2)) + ((move.charAt(3) - '1') * 8);
		return convMove;
	}

	static String fromLong(long move, boolean isNoir) {
		String convMove = "";

		if(!isNoir) {
			for (int i = 0; i < 8; i++) {
				byte b = (byte) (move >>> i * 8);
				switch (b) {
					case 1:
						convMove += "H" + (i+1);
						break;
					case 2:
						convMove += "G" + (i+1);
						break;
					case 4:
						convMove += "F" + (i+1);
						break;
					case 8:
						convMove += "E" + (i+1);
						break;
					case 16:
						convMove += "D" + (i+1);
						break;
					case 32:
						convMove += "C" + (i+1);
						break;
					case 64:
						convMove += "B" + (i+1);
						break;
					case -128:
						convMove += "A" + (i+1);
						break;
				}
			}
		}
		else
		{
			for (int i = 0; i < 8; i++) {
				byte b = (byte) (move >>> i * 8);
				switch (b) {
					case 1:
						convMove = "H" + (i+1) + convMove;
						break;
					case 2:
						convMove = "G" + (i+1) + convMove;
						break;
					case 4:
						convMove = "F" + (i+1) + convMove;
						break;
					case 8:
						convMove = "E" + (i+1) + convMove;
						break;
					case 16:
						convMove = "D" + (i+1) + convMove;
						break;
					case 32:
						convMove = "C" + (i+1) + convMove;
						break;
					case 64:
						convMove = "B" + (i+1) + convMove;
						break;
					case -128:
						convMove = "A" + (i+1) + convMove;
						break;
				}
			}
		}
    	return convMove;
	}

	static long minMax2(final int wantedLevel, long current, long opponent) {
    	final long[] allFirstMoves = new long[48];
    	final int[] firstMovesCount = new int[1];

    	class MinMaxMoveChooser implements MoveChooser {
			int currentLevel;
			int[] movesValue = new int[48];
			int movesCount = 0;


			public MinMaxMoveChooser(int level) {
				this.currentLevel = level;
			}

			@Override
			public void call(long current, long opponent, long move) {
				current = current ^ move;
				opponent = opponent & ~Long.reverse(move);

				if (currentLevel == 0){
					allFirstMoves[firstMovesCount[0]++] = move;
				}

				if (currentLevel < wantedLevel && currentLevel % 2 != 0) {
					MinMaxMoveChooser mmmc = new MinMaxMoveChooser(currentLevel + 1);
					Moves.iterateMoves(0, current, opponent, mmmc);
					int currentBest = mmmc.movesValue[0];
					for(int i = 1; i < mmmc.movesCount; i++) {
						if(currentBest > mmmc.movesValue[i]){
							currentBest = mmmc.movesValue[i];
						}
					}
					movesValue[movesCount++] = currentBest;
				}
				else if(currentLevel < wantedLevel){
					MinMaxMoveChooser mmmc = new MinMaxMoveChooser(currentLevel + 1);
					Moves.iterateMoves(0,opponent, current , mmmc);
					int currentBest = mmmc.movesValue[0];
					for(int i = 1; i < mmmc.movesCount; i++) {
						if(currentBest < mmmc.movesValue[i]){
							currentBest = mmmc.movesValue[i];
						}
					}
					movesValue[movesCount++] = currentBest;
				}

				if(currentLevel == wantedLevel) {
					movesValue[movesCount++] = Heuristique.evaluate(current, opponent, 0);
				}
			}
		}
		MinMaxMoveChooser mmmc = new MinMaxMoveChooser(0);
		Moves.iterateMoves(0, current, opponent, mmmc);
		int currentBest = mmmc.movesValue[0];
		int bestKey = 0;
		for(int i = 1; i < mmmc.movesCount; i++) {
			if(currentBest < mmmc.movesValue[i]){
				currentBest = mmmc.movesValue[i];
				bestKey = i;
			}
		}

		System.out.println("Les patates en poches: ");
		for (int i = 0 ; i < firstMovesCount[0] ; i++) {
			System.out.println("    " + i + " " + Moves.fromLong(allFirstMoves[i], false) + " : " + + mmmc.movesValue[i]);

		}
		System.out.println("We took potato number " + bestKey);

		return allFirstMoves[bestKey];
	}

	static long minMax(int level, long current, long opponent){
		final int size = Math.max(2, level >= 3 ? 8 - level : 6);
		final long[] heuristics = new long[size];
		final long[] moves = new long[size];

		final long minValue = Long.MIN_VALUE;
		for (int i = 0 ; i < size ; i++) {
			heuristics[i] = minValue;
		}

		Moves.iterateMoves(0, current, opponent, new MoveChooser() {
			@Override
			public void call(long current, long opponent, long move) {
				long newBest = Heuristique.evaluate(current ^ move, opponent & ~Long.reverse(move), opponent & Long.reverse(move));

				int minKey = 0;
				long min = Long.MAX_VALUE;
				for (int i = 0 ; i < size ; i++) {
					long heuristic = heuristics[i];
					if (heuristic < min) {
						minKey = i;
						min = heuristic;
					}
				}

				long heuristic = 0;
				if (heuristics[minKey] < newBest) {
					heuristics[minKey] = newBest;
					moves[minKey] = move;
				}
			}
		});

		if (level < 3) {
			long maxHeuristic = Long.MIN_VALUE;
			long maxMove = 0;
			for (int i = 0; i < size; i++) {
				long move = moves[i];
				long result = minMax(level + 1, opponent & ~Long.reverse(move), current ^ move);
				if (result > maxHeuristic) {
					maxMove = move;
					maxHeuristic = result;
				}
			}

			if (level == 0) {
				return maxMove;
			} else {
				return maxHeuristic;
			}
		} else {
			long maxHeuristic = 0;
			for (int i = 0 ; i < size ; i++) {
				if (heuristics[i] > maxHeuristic) {
					maxHeuristic = heuristics[i];
				}
			}

			return maxHeuristic;
		}
	}
}
/***************/
//Emprunter de Client.java
//Créé par Francis Cardinal
//Emprunté le 12 Mars 2017
/***************/
class Connexion {
	public void connectServer() {
		Socket MyClient;
		BufferedInputStream input;
		BufferedOutputStream output;

		try {
			MyClient = new Socket("localhost", 8888);
			input = new BufferedInputStream(MyClient.getInputStream());
			output = new BufferedOutputStream(MyClient.getOutputStream());
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			long boardUs = 0;
			long boardOpp = 0;
			boolean isNoir = false;
			while (1 == 1) {
				char cmd = 0;

				cmd = (char) input.read();

				// D�but de la partie en joueur blanc
				if (cmd == '1') {
					byte[] aBuffer = new byte[1024];

					int size = input.available();
					//System.out.println("size " + size);
					input.read(aBuffer, 0, size);
					String s = new String(aBuffer).trim().replaceAll(" ", "");
					long[] boards = Board.generate(s);
					boardUs = boards[1];
					boardOpp = boards[0];
					Board.print(boardUs, boardOpp);
					System.out.println(s);

					System.out.println("Nouvelle partie! Vous jouer blanc, entrez votre premier coup : ");

					//move = console.readLine();
					long t1 = System.currentTimeMillis();
					long bestMove = Moves.minMax2(3, boardUs, boardOpp);
					System.out.println(System.currentTimeMillis() - t1);


					boardUs ^= bestMove;
					boardOpp &= ~Long.reverse(bestMove);
					Board.print(boardUs, boardOpp);

					String move = Moves.fromLong(bestMove, isNoir);
					System.out.println(move);
					output.write(move.getBytes(), 0, move.length());
					output.flush();
				}
				// D�but de la partie en joueur Noir
				if (cmd == '2') {
					System.out.println("Nouvelle partie! Vous jouer noir, attendez le coup des blancs");
					byte[] aBuffer = new byte[1024];
					isNoir = true;
					int size = input.available();
					input.read(aBuffer, 0, size);
					String s = new String(aBuffer).trim().replaceAll(" ", "");
					long[] boards = Board.generate(s);
					boardUs = boards[0];
					boardOpp = boards[1];
					Board.print(boardOpp, boardUs);
					System.out.println(s);
				}


				// Le serveur demande le prochain coup
				// Le message contient aussi le dernier coup jou�.
				if (cmd == '3') {
					byte[] aBuffer = new byte[16];

					int size = input.available();
					input.read(aBuffer, 0, size);

					String s = new String(aBuffer).trim().replaceAll(" ", "").replaceAll("-", "");
					System.out.println("Dernier coup : " + s);

					long oppMove = !isNoir ? Long.reverse(Moves.fromString(s)) : Moves.fromString(s);

					boardOpp ^= oppMove;
					boardUs &= ~Long.reverse(oppMove);
					Board.print(boardUs, boardOpp);

					long t1 = System.currentTimeMillis();
					long bestMove = Moves.minMax2(3, boardUs, boardOpp);
					System.out.println(System.currentTimeMillis() - t1);


					boardUs ^= bestMove;
					boardOpp &= ~Long.reverse(bestMove);
					bestMove = isNoir ? Long.reverse(bestMove) : bestMove;
					String move = Moves.fromLong(bestMove, isNoir);
					System.out.println(move);
					output.write(move.getBytes(), 0, move.length());
					output.flush();

				}
				// Le dernier coup est invalide
				if (cmd == '4') {
					System.out.println("Coup invalide, entrez un nouveau coup : ");
					String move = null;
					move = console.readLine();
					output.write(move.getBytes(), 0, move.length());
					output.flush();

				}
			}
		} catch (IOException e) {
			System.out.println(e);
		}

	}
}

class Heuristique {

	public static int evaluate(long current, long opponent, long eaten) {
//
//		int GetValue(board, ColorMoving){
//			for (byte x= 0; x < 8; x++){
//				for (byte y = 0; y< 8; y++){
//					BoardSquare square = board. GetPosition(x,y);
//					if (NoPieceOnSquare) continue;
//					if (square.CurrentPiece.PieceColor ==White)
//					{
//						Value += GetPieceValue(square, x, y);
//						if(y ==7)board.WhiteWins = true;
//						if(y == 0)Value += HomeGroundValue;
//						if (column > 0) ThreatA = (board[GetPosition(y - 1, 7).NoPieceOnSquare);
//						if (column < 7) ThreatB = (board.GetPosition(y + 1, 7).NoPieceOnSquare);
//						if (ThreatA && ThreatB) // almost win
//							board.Value += PieceAlmostWinValue;
//					} else {
//						// Same for black, with inverted signs for NegaMax
//					}
//				}
//				if (WhitePiecesOnColumn == 0)Value -= PieceColumnHoleValue;
//				if (BlackPiecesOnColumn == 0)Value += PieceColumnHoleValue;
//			}
//			// if no more material available
//			if (RemainingWhitePieces == 0) board.BlackWins = true;
//			if (RemainingBlackPieces == 0) board.WhiteWins = true;
//
//			// winning positions
//			if (board.WhiteWins)Value += WinValue;
//			if (board.BlackWins)Value -= WinValue;
//		}
//
//		int GetPieceValue(square, Column, Row)
//		{
//			int Value = PieceValue;
//			var Piece = square.CurrentPiece;
//
//			// add connections value
//			if (Piece.ConnectedH) Value += PieceConnectionHValue;
//			if (Piece.ConnectedV) Value += PieceConnectionVValue;
//
//			// add to the value the protected value
//			Value += Piece.ProtectedValue;
//
//			// evaluate attack
//			if (Piece.AttackedValue > 0)
//			{
//				Value -= Piece.AttackedValue;
//				if (Piece.ProtectedValue == 0)
//					Value -= Piece.AttackedValue;
//			} else {
//				if (Piece.ProtectedValue != 0){
//					if (Piece.PieceColor == White){
//						if (Row == 5) Value += PieceDangerValue;
//						else if (Row == 6) Value += PieceHighDangerValue;
//					} else {
//						if (Row == 2) Value += PieceDangerValue;
//						else if (Row == 1) Value += PieceHighDangerValue;
//					}
//				}
//			}
//			// danger value
//			if (Piece.PieceColor ==White)
//				Value += Row * PieceDangerValue;
//			else
//				Value += (8-Row) * PieceDangerValue;
//
//			// mobility feature
//			Value += Piece.ValidMoves.Count;
//
//			return Value;
//		}

		//opponent = Long.reverse(opponent);
		int opponentCount = Long.bitCount(opponent);
		int usCount = Long.bitCount(current);

//		int usValue = 2;
//		int opponentValue = -1;

		int result = 0;

		final int win = 500000;
		final int almostWin = 200;
		final int dangerValue = 10;
		final int protectedValue = 65;
		final int pieceValue = 100;
		final int validMoves = 15;
		final int highDanger = 100;
		final int attackValue = 50;
		final int homeGround = 10;

		result += pieceValue * usCount;
		result -= pieceValue * opponentCount;

		byte last = (byte) (current);
		byte lastOpponent = (byte)(opponent);

//		if(last > 0) {
//			return win;
//		}
//
//		if (lastOpponent > 0) {
//			return -win;
//		}

		for (int i = 0 ; i < 64 ; i++) {
			int row = (i / 8);

			if(((1L << (63 - i)) & opponent) != 0) {
				int pieceAttackedValue = 0;
				int pieceProtectedValue = 0;

				if (row == 1|| row == 2) {
					result -= almostWin;
				}

				//check danger value
				if(((1L << ((63 - i) - 7)) & current) != 0){
					pieceAttackedValue += attackValue;
				}
				if(((1L << ((63 - i) - 9)) & current) != 0){
					pieceAttackedValue += attackValue;
				}
				//check protected value
				if (i >= 7 && ((1L << (63 - i) + 7) & opponent) != 0) {
					pieceProtectedValue += protectedValue;
				}
				if ( i >= 9 && ((1L << (63 - i) + 9) & opponent) != 0) {
					pieceProtectedValue += protectedValue;
				}
				result += pieceAttackedValue;
				result -= pieceProtectedValue;
			}

			if (((1L << 63 - i) & opponent) != 0) {
				result -= row * dangerValue;
			}

			if(((1L << i) & current) != 0){
				if (((1L << i) & eaten) != 0) {
					result += attackValue;
				}

				if (row == 0) {
					result += homeGround;
				}

				if(row == 7) {
					result += 2140000000;
				}
				result += row;

				if (row == 6) {
					result += almostWin;
				}

				int pieceAttackedValue = 0;
				int pieceProtectedValue = 0;

				//check danger value
				if( i % 8 != 0 && ((1L << (63 - i) + 7) & opponent) != 0){
					pieceAttackedValue += attackValue;
					if( (i-1) % 8 != 0 && ((1L << (63 - i) + 14) & opponent) != 0){
						pieceAttackedValue += attackValue;
					}
					if(  ((1L << (63 - i) + 16) & opponent) != 0){
						pieceAttackedValue += attackValue;
					}
				}
				if( i % 8 != 7 && ((1L << (63 - i) + 9) & opponent) != 0){
					pieceAttackedValue += attackValue;
					if( (i+1) % 8 != 7 && ((1L << (63 - i) + 18) & opponent) != 0){
						pieceAttackedValue += attackValue;
					}
					if(((1L << (63 - i) + 16) & opponent) != 0){
						pieceAttackedValue += attackValue;
					}
				}

				//check protected value
				if ( i % 8 != 7 && i >= 7 && ((1L << i - 7) & current) != 0) {
					pieceProtectedValue += protectedValue;
				}
				if ( i % 8 != 0 && i >= 9 && ((1L << i - 9) & current) != 0) {
					pieceProtectedValue += protectedValue;
				}

				result -= pieceAttackedValue;
				result += pieceProtectedValue;

				if (pieceAttackedValue > 0 && protectedValue == 0) {
					result -= pieceAttackedValue;
				}

				if (pieceAttackedValue == 0) {
					if (pieceProtectedValue != 0) {
						if (row == 5) result += dangerValue;
						if (row == 6) result += highDanger;
					}
				}

				//check valid moves
				if(((1L << i + 7) & current) == 0){
					result += validMoves;
				}
				if(i < 56 && ((1L << i + 9) & current) == 0){
					result += validMoves;
				}
				if(((1L << i + 8) & (current)) == 0 && ((1L << i + 8) & opponent) == 0) {
					result += validMoves;
				}
			}
		}

		return result;
	}
}



class Board {
	public static void print(long white, long black) {
		String out = "";
		black = Long.reverse(black);
		for (int i = 0 ; i < 64 ; i++) {
			int n = 63 - i;
			if (((1L << n) & white) != 0) {
				out += 'x';
			} else if (((1L << n) & black) != 0) {
				out += 'o';
			} else if (((1L << n) & (black | white)) == 0) {
				out += '.';
			} else {
				out += 'E';
			}
			out += ' ';
			out += (i%8 == 7) ? "\n" : "";
		}

		System.out.println(out);
	}

	public static long[] generate(String buffer){
		long[] boards = new long[2];
		for(int i=0; i<buffer.length();i++){
			char c = buffer.charAt(i);
			if(c == '2')
			{
				boards[0] += 1L << i;
			}
			if(c == '4')
			{
				boards[1] += 1L << (63-i);
			}
		}
		return boards;
	}
}

public class Main {
	public static void main(String[] args) {
		Connexion conn = new Connexion();
		conn.connectServer();
	}
}
