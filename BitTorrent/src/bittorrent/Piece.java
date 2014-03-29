package bittorrent;

public class Piece {
	private byte[] data;
	private int index;
	private int offset;
	private int length;
	
	public Piece(byte[] payload, int index, int offset, int piece_length, int data_length) {
		//data = new byte[data_length];
		data = payload;
		this.index = index;
		this.offset = offset;
		length = piece_length;
	}

	public byte[] getData() {
		return data;
	}

	public int getIndex() {
		return index;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}
}
