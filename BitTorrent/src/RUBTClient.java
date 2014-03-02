
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Map;

import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.Bencoder2;
import edu.rutgers.cs.cs352.bt.util.ToolKit;
import edu.rutgers.cs.cs352.bt.util.TorrentInfo;


public class RUBTClient {
	
	public static Charset charset = Charset.forName("UTF-8");
	public static CharsetEncoder encoder = charset.newEncoder();

	public static ByteBuffer str_to_bb(String msg){
	  try{
	    return encoder.encode(CharBuffer.wrap(msg));
	  }catch(Exception e){e.printStackTrace();}
	  return null;
	}
	
	public static void main(String[] args) {
		if (args.length != 2 ) {
			System.out.println("Que 1");
			return;
		}
		
		String torrentFile = args[0];
		String outputFile = args[1];
		
		
		byte[] torrentInBytes;
		try {
			torrentInBytes = bytesFromFile(torrentFile);
		}catch (Exception e) { System.out.println("Que 2"); return; }
		
		TorrentInfo torrent = null;
		try {
			torrent = new TorrentInfo(torrentInBytes);
		} catch (BencodingException e) {
			System.out.println("errors");
		}
				
		HttpURLConnection connection;
		InputStream response;
		InputStreamReader response_reader;
		
		try {
			byte[] info_hash = torrent.info_hash.array();
			
			String hex = toHex(info_hash);
			String encodedHex = encode(hex);
						
			
			String peer_id = "ABCDEFGHIJKLMNOPQRST";
			int port = 6881;
			int left = torrent.file_length;
			
			String url = torrent.announce_url + "?info_hash=" + encodedHex
						+ "&peer_id=" + peer_id
						+ "&port=" + port 
						+ "&downloaded=0"
						+ "&left=" + left;
			
			connection = (HttpURLConnection) new URL(url).openConnection();
			int responseCode = connection.getResponseCode();
			response = connection.getInputStream();
			
			DataInputStream reader = new DataInputStream(response);
			
			byte[] response_bytes = new byte[reader.available()];
			reader.readFully(response_bytes);			
			
			Map<ByteBuffer, Object> map = (Map<ByteBuffer, Object>)Bencoder2.decode(response_bytes);;						
			ArrayList<Map<ByteBuffer, Object>> list = (ArrayList<Map<ByteBuffer, Object>>) map.get(str_to_bb("peers"));
			Map<ByteBuffer, Object> peers = (Map<ByteBuffer, Object>) list.get(0);
			
			ByteBuffer buff1 = (ByteBuffer)  peers.get(str_to_bb("peer id"));
			String curr_id = new String(buff1.array());
			
			ByteBuffer buff2 = (ByteBuffer)  peers.get(str_to_bb("ip"));
			String curr_ip = new String(buff2.array());
			
			int curr_port = (Integer) peers.get(str_to_bb("port"));
			
			//Socket socket = new Socket(curr_ip, port);
			
			response.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
		
	
	public static byte[] bytesFromFile(String filename) throws IOException {
		File file = new File(filename);
		
		byte[] bytes = new byte[(int) file.length()];
		
		FileInputStream byteWriter = null;
		try {
			byteWriter = new FileInputStream(file);
			
			if (byteWriter.read(bytes) == -1) {
				throw new IOException("error son"); 
			}
		}finally {
			try {
				if (byteWriter != null) {
					byteWriter.close();
				}
			}catch (Exception e) {}
		}
		
		return bytes;
		
	}
	
	public static String toHex(byte[] bytes) {
		Formatter formatter = new Formatter();
		for (byte b: bytes) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}
	
	public static String encode(String s) {
		String result = "";
		for (int i = 0; i < s.length(); i += 2){
			result += "%" + s.charAt(i) + s.charAt(i +1); 
		}
		return result;
	}
}
