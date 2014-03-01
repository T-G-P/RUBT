
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.TorrentInfo;


public class RUBTClient {
	
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
			System.out.println(encodedHex);
			System.out.println(responseCode);
			response = connection.getInputStream();
			
			for (Entry<String, List<String>> header: connection.getHeaderFields().entrySet()) {
				System.out.println("key = " + header.getKey() + ", value = " + header.getValue());
			}
			Map<String, List<String>> headers = connection.getHeaderFields();
			
			List<String> list = headers.get("Content-Type");
			BufferedReader reader = new BufferedReader(new InputStreamReader(response));
			
			for (String line : list){
				System.out.println(line);
			}
			
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
