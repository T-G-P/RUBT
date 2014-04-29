import java.io.IOException;

import bittorrent.FileWriter;
import bittorrent.Messenger;
import bittorrent.Peer;
import bittorrent.Piece;
import bittorrent.PieceManager;
import bittorrent.Torrent;
import bittorrent.Tracker;
import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.TorrentInfo;

public class RUBTClient {

	public static void main(String[] args) {
		if (args.length != 2) {
			return;
		}
		String torrentFile = args[0];
		String outputFile = args[1];
		
		TorrentInfo torrent = null;
		try {
			torrent = Torrent.getTorrent(torrentFile);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			System.out.println("Error opening torrent file.");
			return;
		}
		
		FileWriter file = new FileWriter(outputFile);
		PieceManager.makeArray(torrent.piece_hashes.length);
		
		Tracker tracker = new Tracker(torrent);
		try {
			tracker.connect();
		} catch (IOException e1) {
			System.out.println("Error connecting to tracker.");
			return;
		}
		
		Peer[] peers;
		try {
			peers = tracker.getPeers();
		} catch (BencodingException e1) {
			System.out.println("Error encoding tracker response.");
			return;
		}
		
		for(int x = 0; x < peers.length; x++){
			System.out.println("Peer #" + (x + 1) + "\n\tid:" + peers[x].getId() + "\n\tip:" + peers[x].getIp() + "\n\tport:" + peers[x].getPort());
		}
		
		int i = 0;
		try {
			while (PieceManager.numTrue() != torrent.piece_hashes.length) {
				if (i == peers.length) {
					i = 0;
				}
				String curr_ip = peers[i].getIp();
				int curr_port = peers[i].getPort();
				String peer_id = peers[i].getId();
				
				Messenger peer = new Messenger(curr_ip, peer_id, curr_port);
				peer.initialize();
				boolean SHA_match = peer.handshake(torrent);
				
				if (!SHA_match) {
					peer.destroy();
					tracker.disconnect();
					return;
				}
				
				peer.showInterest();
				while (true) {
					// FIXME: Verify piece data first
					Object message = peer.checkResponse();
					if (message != null) {
						if (message instanceof byte[]) {
							byte[] bytes = (byte[]) message;
							String byteString = new String(bytes);
							System.out.println(byteString);
						} else if (message instanceof Piece){
							Piece current_piece = (Piece) message;
							System.out.println("Downloading piece #" + (current_piece.getIndex()));
							file.writeFile(current_piece);
							PieceManager.markPiece(current_piece.getIndex());
						} else if (message instanceof String) {
							String response = (String) message;
							if (response.equals("disconnected")) {
								System.out.println("disconnected");
								System.out.println(PieceManager.numTrue());
								break;
							} 
						} 
					}
					
					if (peer.requestPiece(torrent) == -1) {
						System.out.println("Breaking");
						System.out.println(PieceManager.numTrue());
						if (PieceManager.clearIncomplete()) {
							break;
						}
					}
				}
				peer.destroy();
				i++;
			}
			tracker.disconnect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void printBytes(byte[] b) {
		for (int x = 0; x < b.length; x++) {
			System.out.print(String.format("%02x",b[x]));
		}
		System.out.println();
	}
}