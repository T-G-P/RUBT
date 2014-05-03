import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.util.Random;

import bittorrent.FileWriter;
import bittorrent.Messenger;
import bittorrent.Peer;
import bittorrent.PeerConnector;
import bittorrent.Piece;
import bittorrent.PieceManager;
import bittorrent.Torrent;
import bittorrent.Tracker;
import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.TorrentInfo;

public class RUBTClient extends Thread{
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
		Messenger.setPID(myPeerId);

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

		int i = 7;
		System.out.println("Length of pieces" + torrent.piece_length);
		System.out.println("Chunks per piece" + (torrent.piece_length / 16384));
		System.out.println("Remainder" + (torrent.piece_length % 16384));
		while (PieceManager.numTrue() != torrent.piece_hashes.length) {
			PeerConnector peerTest = new PeerConnector(peers, i, torrent, file);
			new Thread(peerTest).start();
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
