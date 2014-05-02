package bittorrent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileWriter {

  RandomAccessFile destination = null;

  public FileWriter(String outputFile) {
    initialize(outputFile);
  }

  /*
   * Creates an empty file with the name of the given input
   */
  private void initialize(String outputFile) {
    try {
      destination = new RandomAccessFile(outputFile, "rwd");
    } catch (FileNotFoundException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  }

  /*
   * Writes a piece of data into the empty file starting at the position given by the piece's index
   */
  public void writeFile(Piece piece) {
    long trueOffset = (piece.getLength() * piece.getIndex());
    synchronized (this.destination) {
      try {
        destination.seek(trueOffset);
        destination.write(piece.getData());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  /*
   * Reads a piece of data from the file that is requested by the peer.
  */
  public byte[] readFile(Piece piece) {
    byte[] request = new byte[0];
    synchronized (this.destination) {
      try {
        destination.read(request, piece.getIndex(), piece.getLength());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return request;
  }
}
