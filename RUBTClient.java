
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
	public static CharsetDecoder decoder = charset.newDecoder();

	public static ByteBuffer str_to_bb(String msg){
	  try{
	    return encoder.encode(CharBuffer.wrap(msg));
	  }catch(Exception e){e.printStackTrace();}
	  return null;
	}

	public static String bb_to_str(ByteBuffer buffer){
	  String data = "";
	  try{
	    int old_position = buffer.position();
	    data = decoder.decode(buffer).toString();
	    // reset buffer's position to its original so it is not altered:
	    buffer.position(old_position);  
	  }catch (Exception e){
	    e.printStackTrace();
	    return "";
	  }
	  return data;
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
			
			/*
			for (Entry<String, List<String>> header: connection.getHeaderFields().entrySet()) {
					System.out.println("key = " + header.getKey() + ", value = " + header.getValue());
			}
			Map<String, List<String>> headers = connection.getHeaderFields();
			*/
			
			DataInputStream reader = new DataInputStream(response);
			
			byte[] response_bytes = new byte[reader.available()];
			reader.readFully(response_bytes);
			
			
			Map<ByteBuffer, String> map = (Map<ByteBuffer, String>)Bencoder2.decode(response_bytes);;
			
			
			ArrayList<Map<ByteBuffer, String>> list = (ArrayList<Map<ByteBuffer, String>>) Bencoder2.decode(map.get(encoder.encode(CharBuffer.wrap("peers"))).getBytes());
			//Map<ByteBuffer, String> map = Bencoder2.decode(list.get(0).get(encoder.encode(CharBuffer.wrap("peer id"))).);
			//ArrayList<Map<ByteBuffer, String>> test1 = (ArrayList<Map<ByteBuffer, String>>) Bencoder2.decode((byte[]) test);
			
			//ToolKit.print(test1.get(0).get(str_to_bb("peer id")));
			
			//String s = test1.get(0).get(str_to_bb("peer id")).toString();
			//System.out.println(s);
			
			
			ToolKit.print(map);
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
