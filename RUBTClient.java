import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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
		
		URL url; 
		
		
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
		
		
		System.out.println("Que");
		System.out.println(torrent.announce_url);
		System.out.println("Quand");
		
		URLConnection connection;
		try {
			connection = torrent.announce_url.openConnection();
			InputStream response = connection.getInputStream();
			
			System.out.println(response.toString());
		} catch (IOException e) {
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
}
