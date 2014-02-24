import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
		
		System.out.println("Que");
		System.out.println(torrent.file_name);
		System.out.println("Quand");
		
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
