package bittorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.TorrentInfo;

public class Torrent {
	
	byte[] torrentInBytes;
	
	public static TorrentInfo getTorrent(String torrentFile) throws BencodingException, IOException  {
		return new TorrentInfo(bytesFromFile(torrentFile));
	}
	
	/*
	 * Takes the torrent file and converts it into an array of bytes for the bencoder to use
	 */
	static byte[] bytesFromFile(String filename) throws IOException {
		File file = new File(filename);
		byte[] bytes = new byte[(int) file.length()];
		FileInputStream byteWriter = null;
		byteWriter = new FileInputStream(file);
		if (byteWriter.read(bytes) == -1) {
			byteWriter.close();
			throw new IOException("error son");
		}
		byteWriter.close();

		return bytes;
	}
}
