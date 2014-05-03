package bittorrent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import edu.rutgers.cs.cs352.bt.util.TorrentInfo;

public class Messenger {

	static byte[] sourceID;
	
	byte[] id;
	String ip;
	int port;
	Socket socket;
	DataInputStream socketIn;
	DataOutputStream socketOut;
	ArrayList<Integer> pieces;

	// FIXME: Request blocks of 16384 at a time
	int requestedPieceLength;

	// testing stuff
	boolean unchoked = false;
	int count = 0;
	boolean half = false;

	public Messenger(String ip, byte[] id, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
		pieces = new ArrayList<Integer>();
	}
	
	public static void setPID(byte[] source_id) {
		sourceID = source_id;
	}

	/*
	 * Opens a connection to the peer the messenger was created with
	 */
	public void initialize() throws UnknownHostException, IOException {
		socket = new Socket(ip, port);
		socketIn = new DataInputStream(socket.getInputStream());
		socketOut = new DataOutputStream(socket.getOutputStream());
	}

	/*
	 * Closes all connections the current messenger has open
	 */
	public void destroy() throws UnknownHostException, IOException {
		socketOut.close();
		socketIn.close();
		socket.close();
	}

	/*
	 * Handshakes with the peer
	 */
	public boolean handshake(TorrentInfo torrent) throws IOException {

		byte[] serverResponse = new byte[68];
		byte[] reserved = new byte[8];
		requestedPieceLength = torrent.piece_length;
		socketOut.writeByte(19);
		socketOut.write("BitTorrent protocol".getBytes());
		socketOut.write(reserved);
		socketOut.write(torrent.info_hash.array());
		socketOut.write(sourceID);
		socketOut.flush();
		socketIn.readFully(serverResponse);

		return compareSHA(torrent.info_hash.array(), serverResponse);
	}

	public String[] bitfieldBits(byte[] message) {
		String[] result = new String[message.length];
		for (int i = 0; i < message.length; i++) {
			String bits = String.format("%8s", Integer.toBinaryString(message[i] & 0xFF)).replace(' ', '0');
			result[i] = bits;
		}
		return result;
	}

	/*
	 * Reads messages from the peer
	 */
	public Object checkResponse() throws IOException {
		System.out.println("Reading length");
		int length = socketIn.readInt();
		System.out.println("She said I wanna go home");
		
		if (length > 0) {
			byte message_id = socketIn.readByte();
			if (message_id == 6) {
				//This is for requests
				System.out.println(message_id);

				int index = socketIn.readInt();
				int offset = socketIn.readInt();
				int piece_length = socketIn.readInt();
				// TODO : Error checking for large block requests
				// payload is null since we aren't getting anything from the
				// person
				// requesting the file
				// just setting up the piece for now.
				Piece piece = new Piece(null, index, offset, piece_length, (length - 9));
				return piece;
			} else if (message_id == 7) {

				int index = socketIn.readInt();
				int offset = socketIn.readInt();
				byte[] payload = new byte[length - 9];
				socketIn.readFully(payload);
				// FIXME: Verify piece data first
				Piece piece = new Piece(payload, index, offset,
						this.requestedPieceLength, (length - 9));
				++count;
				System.out.println(message_id);
				return piece;

			} else if (message_id == 1) {
				System.out.println(message_id);
				unchoked = true;
			} else if (message_id == 2) {
				System.out.println(message_id);
				// interested
			} else if (message_id == 3) {
				System.out.println(message_id);
				// not interested
			} else if (message_id == 4) {
				System.out.println(message_id);
				int index = socketIn.readInt();
				pieces.add(new Integer(index));
				// have
			} else if (message_id == 5) {
				System.out.println(message_id);
				byte[] payload = new byte[length - 1];
				return bitfieldBits(payload);
			} else if (message_id == 6) {
				System.out.println(message_id);
				// request
			} else if (message_id == 8) {
				System.out.println(message_id);
				// cancel
			} else if (message_id == 0) {
				System.out.println(message_id);
				unchoked = false;
			} else {
				// TODO: Handle other message types

				System.out.println(message_id);
				int left = socketIn.available();
				socketIn.readFully(new byte[left]);
			}
		} else {
			System.out.println("keep alive");
		}
		return null;
	}

	/*
	 * Sends the peer a piece request message
	 */
	public int requestPiece(TorrentInfo torrent) throws IOException {
		if (unchoked) {
			int piece_index = findViablePiece();
			if (piece_index == -1) {
				return -1;
			}
			System.out.println("Requesting piece #" + piece_index);
			socketOut.writeInt(13);
			socketOut.writeByte(6);
			socketOut.writeInt(piece_index);
			socketOut.writeInt(0);
			// FIXME: This will be 0 if file_length is divisible by
			// piece_length
			/*socketOut
					.writeInt(count == (torrent.piece_hashes.length - 1) ? torrent.file_length
							% requestedPieceLength
							: requestedPieceLength);*/
			socketOut.writeInt(16384);
			socketOut.flush();
		}
		return 1;
	}

	/*
	 * Sends the peer an interested message
	 */
	public void showInterest() throws IOException {
		socketOut.writeInt(1);
		socketOut.writeByte(2);
		socketOut.flush();
	}

	/*
	 * Verifies that the peer has a matching SHA hash with the client
	 */
	public boolean compareSHA(byte[] sent, byte[] response) {
		for (int x = 28; x < 48; x++) {
			if (sent[x - 28] != response[x]) {
				return false;
			}
		}
		return true;
	}

	public int findViablePiece() {
		for (int i = 0; i < pieces.size(); i++) {
			if (PieceManager.checkPieceNecessity(pieces.get(i)) == 0) {
				PieceManager.pieces[pieces.get(i)] = -1;
				return pieces.get(i).intValue();
			}
		}
		System.out.println(-1);
		return -1;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder("Messenger ");
		sb.append(this.ip).append(":").append(this.port);
		if(this.id != null){
			sb.append(" (");
			for(int i = 0; i < this.id.length; ++i){
				if(this.id[i] < ' ' || this.id[i] > 126){
					sb.append(String.format("_%02X",this.id[i]));
				}else {
					sb.append((char)this.id[i]);
				}
			}
		}
		sb.append(')');
		return sb.toString();
	}
}
