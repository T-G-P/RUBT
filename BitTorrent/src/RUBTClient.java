import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.util.Random;

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

		FileWriter file = new FileWriter(outputFile);

		TorrentInfo torrent = null;
		try {
			torrent = Torrent.getTorrent(torrentFile);
		} catch (BencodingException | IOException e1) {
			System.out.println("Error opening torrent file.");
			return;
		}

		PieceManager.makeArray(torrent.piece_hashes.length);
		ServerSocket listenSocket = null;
		try {
			listenSocket = new ServerSocket();
			listenSocket.bind(null);

		} catch (IOException ioe) {
			System.err.println("Couldn't bind a server socket!!!");
			ioe.printStackTrace();
		}

		if (listenSocket == null) {
			System.err.println("Oh noes, no server socket!!");
			return;
		}

		//int myPort = listenSocket.getLocalPort();
		int myPort = 6881;
		System.out.println(myPort);
		byte[] myPeerId = genPeerId();

		final Tracker tracker = new Tracker(torrent, myPort, myPeerId);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					tracker.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		Peer[] peers = null;
		try {
			peers = tracker.connect();
		} catch (Exception e1) {
			System.out.println("Error connecting to tracker.");
			return;
		}

		if (peers == null) {
			// FIXME: ???
		}

		for (int x = 0; x < peers.length; x++) {
			System.out.println("Peer #" + (x + 1) + "\n\tid:"
					+ peers[x].getId() + "\n\tip:" + peers[x].getIp()
					+ "\n\tport:" + peers[x].getPort());
		}

		int i = 1;
		Messenger peer = null;
		System.out.println(toString(myPeerId));
		while (PieceManager.numTrue() != torrent.piece_hashes.length) {
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
					continue;
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
				
			} catch (Exception e) {
				System.err.println("Exception from " + peer);
				e.printStackTrace();
				
			}finally {
				++i; 
			}
		}
		try {
			tracker.disconnect();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static final byte[] genPeerId() {
		byte[] peerId = new byte[20];
		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < 20; ++i) {
			peerId[i] = (byte) (r.nextInt(26) + 'A');
		}
		return peerId;
	}

	public static void printBytes(byte[] b) {
		for (int x = 0; x < b.length; x++) {
			System.out.print(String.format("%02x", b[x]));
		}
		System.out.println();
	}

	private static String toString(byte[] id){
		StringBuilder sb = new StringBuilder("Peer ID: ");
		if(id != null){
			sb.append(" (");
			for(int i = 0; i < id.length; ++i){
				if(id[i] < ' ' || id[i] > 126){
					sb.append(String.format("_%02X",id[i]));
		 		}else {
					sb.append((char)id[i]);
				}
			}
		}
		sb.append(')');
		return sb.toString();
	}
}
