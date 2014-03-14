import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Map;

import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.Bencoder2;
import edu.rutgers.cs.cs352.bt.util.TorrentInfo;

public class RUBTClient {

	// Variable for output file
	static RandomAccessFile destination;

	// Encodes strings into bytebuffers
	// used to search keys in the map<bytebuffer, object>
	public static Charset charset = Charset.forName("UTF-8");
	public static CharsetEncoder encoder = charset.newEncoder();

	public static ByteBuffer str_to_bb(String msg) {
		try {
			return encoder.encode(CharBuffer.wrap(msg));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Que 1");
			return;
		}
		String torrentFile = args[0];
		String outputFile = args[1];

		// Set output file and set writing to disc
		try {
			destination = new RandomAccessFile(outputFile, "rwd");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// turn torrent file into byte array
		byte[] torrentInBytes;
		try {
			torrentInBytes = bytesFromFile(torrentFile);
		} catch (Exception e) {
			System.out.println("Que 2");
			return;
		}

		// encode torrent byte array and store it in TorrentInfo object
		TorrentInfo torrent = null;
		try {
			torrent = new TorrentInfo(torrentInBytes);
			torrentInBytes = null;
		} catch (BencodingException e) {
			System.out.println("errors");
			return;
		}

		// Connection variables
		HttpURLConnection connection;
		InputStream response;
		InputStreamReader response_reader;
		try {
			// Contructing connection url
			String peer_id = "ABCDEFGHIJKLMNOPQRST";
			int port = 6881;
			int left = torrent.file_length;

			String url = torrent.announce_url
					+ "?info_hash="
					+ encode(toHex(torrent.info_hash.array())) // change byte
																// array to hex
																// string, then
																// encode
																// hexstring
					+ "&peer_id=" + peer_id + "&port=" + port + "&downloaded=0"
					+ "&left=" + left;

			connection = (HttpURLConnection) new URL(url).openConnection();

			response = connection.getInputStream();
			DataInputStream reader = new DataInputStream(response);

			byte[] response_bytes = new byte[reader.available()];
			reader.readFully(response_bytes);

			// casting generic objects to proper types to access the data within
			// them
			Map<ByteBuffer, Object> map = (Map<ByteBuffer, Object>) Bencoder2
					.decode(response_bytes);
			;
			ArrayList<Map<ByteBuffer, Object>> list = (ArrayList<Map<ByteBuffer, Object>>) map
					.get(str_to_bb("peers"));
			Map<ByteBuffer, Object> peers = (Map<ByteBuffer, Object>) list.get(0);

			// Storing the information from the peer (Needs to be changed to
			// find the peer by its peer id)
			ByteBuffer buff = (ByteBuffer) peers.get(str_to_bb("peer id"));
			String curr_id = new String(buff.array());

			buff = (ByteBuffer) peers.get(str_to_bb("ip"));
			String curr_ip = new String(buff.array());

			int curr_port = (Integer) peers.get(str_to_bb("port"));

			Socket socket = new Socket(curr_ip, curr_port);

			// Handshake with tracker
			byte[] header = { 0x13, 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e',
					'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o', 'l',
					0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
			byte[] SHA = torrent.info_hash.array();
			byte[] peerID = peer_id.getBytes();

			byte[] message = makeMessage(header, SHA, peerID);
			byte[] serverResponse = new byte[message.length];
			DataInputStream socketIn = new DataInputStream(
					socket.getInputStream());

			DataOutputStream socketOut = new DataOutputStream(
					socket.getOutputStream());

			// Shake hands
			socketOut.write(19);
			socketOut.write("BitTorrent protocol".getBytes());
			byte[] reserved = new byte[8];
			socketOut.write(reserved);
			socketOut.write(torrent.info_hash.array());
			socketOut.write(peer_id.getBytes());
			socketOut.flush();
			socketIn.readFully(serverResponse);

			// Show interest
			socketOut.writeInt(1);
			socketOut.writeByte(2);
			socketOut.flush();

			// Verify matching hash
			boolean SHA_match = compareSHA(message, serverResponse);
			boolean unchoked = false;

			// Variables for storing messages
			int length = 0;
			byte id = 0x00;
			byte[] payload = null;
			int index = 0;
			int offset = 0;
			// FIXME: Request blocks of 16384 at a time
			int piece_length = torrent.piece_length;

			// testing stuff
			int count = 0;
			boolean half = false;
			int remaining = 0;

			if (!SHA_match) {
				// Close all the connection variables
				socketOut.close();
				socketIn.close();
				socket.close();
				response.close();
				return;
			}
			while (true) {

				/*********************************************************/
				/* The message passing portion */
				/* Everything enclosed in this flag is subject to change */
				/*********************************************************/

				length = socketIn.readInt();

				if (length > 0) {
					id = socketIn.readByte();
					System.out.println(count);
					if (id == 7) {

						index = socketIn.readInt();
						offset = socketIn.readInt();
						payload = new byte[length - 9];
						socketIn.readFully(payload);
						remaining = socketIn.available();
						// FIXME: Verify piece data first
						writeFile(payload, index, offset, torrent.piece_length);
						++count;

					} else if (id == 1) {
						// Unchoke message
						unchoked = true;
					} else {
						// TODO: Handle other message types
						socketIn.readFully(new byte[length - 1]);
						continue;
					}

					System.out.println("Remaining bytes = " + remaining);

					/*
					 * socketOut.write(1); socketOut.writeByte(2);
					 * socketOut.flush();
					 */
					if (unchoked) {
						if (count >= torrent.piece_hashes.length) {
							break;
						}
						socketOut.writeInt(13);
						socketOut.writeByte(6);
						socketOut.writeInt(count);
						socketOut.writeInt(0);
						// FIXME: This will be 0 if file_length is divisible by
						// piece_length
						socketOut.writeInt(count == (torrent.piece_hashes.length -1)? torrent.file_length
								% piece_length : piece_length);
						socketOut.flush();

					}

					/*********************************************************/
					/* The message passing portion */
					/* Everything enclosed in this flag is subject to change */
					/*********************************************************/
				}
			}

			// Close all the connection variables
			socketOut.close();
			socketIn.close();
			socket.close();
			response.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Compares the hashes to ensure they are the same
	public static boolean compareSHA(byte[] sent, byte[] response) {
		for (int x = 28; x < 48; x++) {
			if (sent[x] != response[x]) {
				return false;
			}
		}
		return true;
	}

	// prints byte arrays, for debugging purposes
	public static void printBytes(byte[] b) {
		for (int x = 0; x < b.length; x++) {
			System.out.print(String.format("%02x", b[x]));
		}
		System.out.println();
	}

	// makes the hand shake message (will eventually be removed)
	public static byte[] makeMessage(byte[] header, byte[] sha, byte[] peer_id) {
		int length = header.length + sha.length + peer_id.length;
		byte[] message = new byte[length];
		int i = 0;
		for (int x = 0; x < header.length; x++) {
			message[i] = header[x];
			i++;
		}
		for (int x = 0; x < sha.length; x++) {
			message[i] = sha[x];
			i++;
		}
		for (int x = 0; x < peer_id.length; x++) {
			message[i] = peer_id[x];
			i++;
		}

		return message;
	}

	// writes the data pieces to a new file
	public static void writeFile(byte[] data, int index, int offset,
			int piecesize) {
		long trueOffset = (piecesize * index);
		try {
			destination.seek(trueOffset);
			destination.write(data);
			// destination.seek(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// translates a file into a byte array
	public static byte[] bytesFromFile(String filename) throws IOException {
		File file = new File(filename);

		byte[] bytes = new byte[(int) file.length()];

		FileInputStream byteWriter = null;
		try {
			byteWriter = new FileInputStream(file);

			if (byteWriter.read(bytes) == -1) {
				throw new IOException("error son");
			}
		} finally {
			try {
				if (byteWriter != null) {
					byteWriter.close();
				}
			} catch (Exception e) {
			}
		}

		return bytes;

	}

	// translate string to hex
	public static String toHex(byte[] bytes) {
		Formatter formatter = new Formatter();
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	// URL encodes the hex string
	public static String encode(String s) {
		String result = "";
		for (int i = 0; i < s.length(); i += 2) {
			result += "%" + s.charAt(i) + s.charAt(i + 1);
		}
		return result;
	}
}
