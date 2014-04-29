package bittorrent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import edu.rutgers.cs.cs352.bt.util.TorrentInfo;

public class Messenger {
	
	String id;
	String ip;
	int port;
	Socket socket;
	DataInputStream socketIn;
	DataOutputStream socketOut;
	ArrayList<Integer> pieces;

	// Variables for storing messages
	int length = 0;
	byte message_id = 0x00;
	byte[] payload = null;
	int index = 0;
	int offset = 0;
	// FIXME: Request blocks of 16384 at a time
	int piece_length;
	
	// testing stuff
	boolean unchoked = false;
	int count = 0;
	boolean half = false;
	
	public Messenger(String ip, String id, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
		pieces = new ArrayList<Integer>();
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
		piece_length = torrent.piece_length;
		
		
		socketOut.write(19);
		socketOut.write("BitTorrent protocol".getBytes());
		socketOut.write(reserved);
		socketOut.write(torrent.info_hash.array());
		socketOut.write(id.getBytes());
		socketOut.flush();
		socketIn.readFully(serverResponse);
		
		return compareSHA(torrent.info_hash.array(), serverResponse);
	}
	
	public String[] bitfieldBits(byte[] message) {
		String[] result = new String[message.length];
		for (int i = 0; i < message.length; i++) {
			String bits = "00000000" + Integer.toBinaryString(message[i]);
			result[i] = bits.substring(bits.length() - 8);
		}
		return result;
	}
	
	/*
	 * Reads messages from the peer
	 */
	public Object checkResponse() throws IOException {
		
		try {
			length = socketIn.readInt();
		} catch (EOFException e) {
			return "disconnected";
		}
		if (length > 0) {
			message_id = socketIn.readByte();
			if (message_id == 7) {
				index = socketIn.readInt();
				offset = socketIn.readInt();
				payload = new byte[length - 9];
				socketIn.readFully(payload);
				// FIXME: Verify piece data first
				Piece piece = new Piece(payload, index, offset, piece_length, (length - 9));
				return piece;
			} else if (message_id == 1) {
				unchoked = true;
			} else if (message_id == 2) {
				System.out.println(message_id + " - Interested");
				//interested
			} else if (message_id == 3) {
				System.out.println(message_id + " - Not Interested");
				//not interested
			} else if (message_id == 4) {
				index = socketIn.readInt();
				pieces.add(new Integer(index));
				//have
			} else if (message_id == 5) {
				payload = new byte[length - 1];
				//return bitfieldBits(payload);
				return payload;
			} else if (message_id == 6) {
				System.out.println(message_id + " -  Request");
				//request
			} else if (message_id == 8) {
				System.out.println(message_id + " - Cancel");
				//cancel
			} else if (message_id == 0){
				unchoked = false;
			} else {
				// TODO: Handle other message types
				System.out.println("in the else");
				int left = socketIn.available();
				socketIn.readFully(new byte[left]);
			}
		}
		return null;
	}
	
	/*
	 * Sends the peer a piece request message
	 */
	public int requestPiece(TorrentInfo torrent) throws IOException{
		if (unchoked) {
			
			int piece_index = findViablePiece();
			if (piece_index == -1) {
				return -1;
			} //else if (piece_index == -2) {
			//	return 0;
			//}
			
			socketOut.writeInt(13);
			socketOut.writeByte(6);
			socketOut.writeInt(piece_index);
			socketOut.writeInt(0);
			// FIXME: This will be 0 if file_length is divisible by
			// piece_length
			socketOut.writeInt(count == (torrent.piece_hashes.length -1)? torrent.file_length % piece_length : piece_length);
			socketOut.flush();
		}
		return 1;
	}
	
	/*
	 * Sends the peer an interested message
	 */
	public void showInterest() throws IOException{
		socketOut.writeInt(1);
		socketOut.writeByte(2);
		socketOut.flush();
	}
	
	/*
	 * Verifies that the peer has a matching SHA hash with the client
	 */
	public boolean compareSHA(byte[] sent, byte[] response) {
		for (int x = 28; x < 48; x++) {
			if (sent[x-28] != response[x]) {
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
			}//else if (PieceManager.checkPieceNecessity(pieces.get(i)) == -1) {
				//return -2;
			//}
		}
		System.out.println(-1);
		return -1;
	}
}
