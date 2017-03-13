package com.ets.log320.lab3;

import java.io.*;
import java.net.Socket;

class ChooseMove {
	long best = Long.MIN_VALUE;
	long bestmove = 0;

	void call(long current, long opponent, long move) {
		long newBest = Heuristique.evaluate(current ^ move, opponent & ~Long.reverse(move));

		if (best < newBest) {
			best = newBest;
			bestmove = move;
		}
	}
}

class Moves {
	//							  v       v       v       v       v       v       v       v
	final static long up =		0b0000000000000000000000000000000000000000000000000000000100000001L;
	final static long right =	0b0000000000000000000000000000000000000000000000000000000010000001L;
	final static long left =	0b0000000000000000000000000000000000000000000000000000001000000001L;

	static void generate(int level, long current, long opponent, ChooseMove m) {
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

					ChooseMove cm = new ChooseMove();
					Moves.generate(1, boardUs, boardOpp, cm);

					boardUs ^= cm.bestmove;
					boardOpp &= ~Long.reverse(cm.bestmove);

					String move = Moves.fromLong(cm.bestmove, isNoir);
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

					ChooseMove cm = new ChooseMove();

					Moves.generate(1, boardUs, boardOpp, cm);

					String move = Moves.fromLong(cm.bestmove, isNoir);;

					boardUs ^= cm.bestmove;
					boardOpp &= ~Long.reverse(cm.bestmove);
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

	public static long evaluate(long current, long opponent){
		return current - opponent;

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
