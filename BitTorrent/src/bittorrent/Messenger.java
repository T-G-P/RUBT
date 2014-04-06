package bittorrent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import edu.rutgers.cs.cs352.bt.util.TorrentInfo;

public class Messenger {
	
	String id;
	String ip;
	int port;
	Socket socket;
	DataInputStream socketIn;
	DataOutputStream socketOut;
	

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
	
	/*
	 * Reads messages from the peer
	 */
	public Piece checkResponse() throws IOException {
		
		length = socketIn.readInt();
		if (length > 0) {
			message_id = socketIn.readByte();
                        //Uploading
                        if (message_id == 6) {
                            byte[] uploadByte = new byte[0];
                            
                            index = socketIn.readInt();
                            offset = socketIn.readInt();
                            piece_length = socketIn.readInt();
                            //TODO : Error checking for large block requests
                            //payload is null since we aren't getting anything from the person requesting the file
                            //just setting up the piece for now.
                            Piece piece = new Piece(null, index, offset, piece_length, (length - 9));
                            return piece;
                        }
			if (message_id == 7) {

				index = socketIn.readInt();
				offset = socketIn.readInt();
				payload = new byte[length - 9];
				socketIn.readFully(payload);
				// FIXME: Verify piece data first
				Piece piece = new Piece(payload, index, offset, piece_length, (length - 9));
				++count;
				return piece;
				

			} else if (message_id == 1) {
				unchoked = true;
			} else {
				// TODO: Handle other message types
				socketIn.readFully(new byte[length - 1]);
			}
		}
		return null;
	}
	/*
	 * Sends the peer a piece request message
	 */
	public int requestPiece(TorrentInfo torrent) throws IOException{
		if (unchoked) {
			if (count >= torrent.piece_hashes.length) {
				return -1;
			}
			socketOut.writeInt(13);
			socketOut.writeByte(6);
			socketOut.writeInt(count);
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
	public static boolean compareSHA(byte[] sent, byte[] response) {
		for (int x = 28; x < 48; x++) {
			if (sent[x-28] != response[x]) {
				return false;
			}
		}
		return true;
	}
}
