package bittorrent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileWriter {
	
	RandomAccessFile destination;
	
	public FileWriter(String outputFile) {
		initialize(outputFile);
	}
	
	private void initialize(String outputFile) {
		try {
			destination = new RandomAccessFile(outputFile, "rwd");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void writeFile(Piece piece) {
		long trueOffset = (piece.getLength() * piece.getIndex());
		try {
			destination.seek(trueOffset);
			destination.write(piece.getData());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
