package bittorrent;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

public class Tracker {
	
	TorrentInfo torrent;
	HttpURLConnection connection;
	InputStream response;
	byte[] response_bytes;
	
	public Tracker(TorrentInfo file) {
		torrent = file;
	}
	
	/*
	 * Creates connection to tracker
	 */
	public void connect() throws MalformedURLException, IOException {
		
		String peer_id = "ABCDEFGHIJKLMNOPQRST";
		int port = 6881;
		int left = torrent.file_length;
	
		String url = torrent.announce_url + "?info_hash=" + encode(toHex(torrent.info_hash.array())) + "&peer_id=" + peer_id + "&port=" + port + "&downloaded=0"+ "&left=" + left;
	
		connection = (HttpURLConnection) new URL(url).openConnection();
	
		response = connection.getInputStream();
		DataInputStream reader = new DataInputStream(response);
	
		response_bytes = new byte[reader.available()];
		reader.readFully(response_bytes);
		reader.close();
	}
	
	/*
	 * Close connection to tracker
	 */
	public void disconnect() throws MalformedURLException, IOException {
		response.close();	
		connection.disconnect();
	}
	
	public boolean validateIP(String ip) {
		if (ip.equals("128.6.171.130") || ip.equals("128.6.171.131")) {
			return true;
		}
		return false;
	}
	
	/*
	 * Takes the response from the tracker and accesses the lists of peers and stores them in a local array
	 */
	@SuppressWarnings("unchecked")
	public Peer[] getPeers() throws BencodingException{
		Map<ByteBuffer, Object> map = (Map<ByteBuffer, Object>) Bencoder2.decode(response_bytes);
		ArrayList<Map<ByteBuffer, Object>> list = (ArrayList<Map<ByteBuffer, Object>>) map.get(str_to_bb("peers"));
		ArrayList<Peer> peers = new ArrayList<Peer>();
		for (int i = 0;  i < list.size(); i++){
			Map<ByteBuffer, Object> peer = (Map<ByteBuffer, Object>) list.get(i);
			
			ByteBuffer buff = (ByteBuffer) peer.get(str_to_bb("peer id"));
			String curr_id = new String(buff.array());

			buff = (ByteBuffer) peer.get(str_to_bb("ip"));
			String curr_ip = new String(buff.array());

			int curr_port = (Integer) peer.get(str_to_bb("port"));
			if (validateIP(curr_ip)) {
				peers.add(new Peer(curr_id, curr_ip, curr_port));
			}
		}
		
		return peers.toArray(new Peer[peers.size()]);
	}
	
	
	/*
	 * Helper methods for the above functions
	 */
	public static String toHex(byte[] bytes) {
		Formatter formatter = new Formatter();
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		String output =  formatter.toString();
		formatter.close();
		return output;
	}
	
	public static String encode(String s) {
		String result = "";
		for (int i = 0; i < s.length(); i += 2) {
			result += "%" + s.charAt(i) + s.charAt(i + 1);
		}
		return result;
	}
	
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
}
