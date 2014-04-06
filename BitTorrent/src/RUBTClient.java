import java.io.IOException;

import bittorrent.FileWriter;
import bittorrent.Messenger;
import bittorrent.Peer;
import bittorrent.Piece;
import bittorrent.Torrent;
import bittorrent.Tracker;
import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.TorrentInfo;

public class RUBTClient {

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Que 1");
			return;
		}
		String torrentFile = args[0];
		String outputFile = args[1];
		
		FileWriter file = new FileWriter(outputFile);
		
		TorrentInfo torrent = null;
		try {
			torrent = Torrent.getTorrent(torrentFile);
		} catch (BencodingException | IOException e1) {
			// TODO Auto-generated catch block
			System.out.println("Error opening torrent file.");
			return;
		}
		
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
		
		try {
			
			String curr_ip = peers[0].getIp();
			int curr_port = peers[0].getPort();
			String peer_id = peers[0].getId();
			
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
				
				Piece current_piece = peer.checkResponse();
                                //We need to make it either do a read or write file.
				if (current_piece != null) {
					System.out.println("Downloading piece #" + (current_piece.getIndex() + 1));
					file.writeFile(current_piece);
				}
                                /*
                                This is the code for when you're sending a file, idk how you want to change how the responses are handled atm
                                
                                if (current_piece != null) {
                                    byte[] data = new byte[0];
                                    try {
                                     data = file.readFile(current_piece);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    socketOut.writeByte(data);
                                    socketOut.flush();
                                
                                }
                                */
				if (peer.requestPiece(torrent) == -1) {
					break;
				}
			}

			peer.destroy();
			tracker.disconnect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}