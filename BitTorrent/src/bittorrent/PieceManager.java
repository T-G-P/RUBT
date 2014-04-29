package bittorrent;

public class PieceManager {

	public static int pieces[];
	
	public static void makeArray(int length){
		pieces = new int[length];
	}
	
	public static int checkPieceNecessity(int index) {
		return pieces[index];
	}
	
	public static void markPiece(int index) {
		pieces[index] = 1;
	}
	
	public static int numTrue() {
		int count = 0;
		for (int i = 0; i < pieces.length; i++) {
			if (pieces[i] == 1) {
				count += 1;
			}
		}
		return count;
	}
	
	public static boolean clearIncomplete(){
		boolean complete = true;
		for (int i = 0; i < pieces.length; i++) {
			if (pieces[i] == -1) {
				pieces[i] = 0;
				complete = false;
			}
		}
		return complete;
	}
}
