package bittorrent;

import edu.rutgers.cs.cs352.bt.util.TorrentInfo;

public class PeerConnector implements Runnable {
	private Peer[] peers;
	int i;
	TorrentInfo torrent;
	FileWriter file;
	
	public PeerConnector(Peer[] peers, int i, TorrentInfo torrent, FileWriter file) {
		this.peers = peers;
		this.i = i;
		this.torrent = torrent;
		this.file = file;
		
	}
	public void run() {
		Messenger peer = null;
		try {
			if (i == peers.length) {
				i = 0;
			}
			String curr_ip = peers[i].getIp();
			int curr_port = peers[i].getPort();
			byte[] peer_id = peers[i].getId();
			
			peer = new Messenger(curr_ip, peer_id, curr_port);
			System.out.println("Using " + peer);
			peer.initialize();
			boolean SHA_match = peer.handshake(torrent);

			if (!SHA_match) {
				peer.destroy();
				i++;
			}

			peer.showInterest();
			while (true) {
				// FIXME: Verify piece data first
				//System.out.println("Running");
				Object message = peer.checkResponse();
				
				if (message != null) {
					if (message instanceof byte[]) {
						byte[] bytes = (byte[]) message;
						String byteString = new String(bytes);
						System.out.println(byteString);
					} else if (message instanceof Piece) {
						Piece current_piece = (Piece) message;
						System.out.println("Downloading piece #"
								+ (current_piece.getIndex()));
						file.writeFile(current_piece);
						PieceManager.markPiece(current_piece.getIndex());
					} else if (message instanceof String[]) {
						String[] response = (String[]) message;
						for (int x = 0; x < response.length; x++) {
							System.out.println(response[x]);
						}
						i++;
						break;
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
			
		} catch (Exception e) {
			System.err.println("Exception from " + peer);
			e.printStackTrace();
			
		}finally {
			++i; 
		}
	}
}
