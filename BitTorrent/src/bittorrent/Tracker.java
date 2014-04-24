package bittorrent;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Map;
import java.util.Random;

import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.Bencoder2;
import edu.rutgers.cs.cs352.bt.util.TorrentInfo;

public class Tracker {

  TorrentInfo torrent;
  HttpURLConnection connection;
  InputStream response;
  final int port;
  byte[] peer_id;

  // byte[] response_bytes;

  public Tracker(TorrentInfo file, int port,byte[] peer_id) {
    this.torrent = file;
    this.port = port;
    this.peer_id = peer_id;
  }

  /*
   * Creates connection to tracker
   */
  public Peer[] connect() throws MalformedURLException, IOException,
      BencodingException {


    int left = this.torrent.file_length;

    String url = this.torrent.announce_url + "?info_hash="
        + toHex(this.torrent.info_hash.array()) + "&peer_id=" + toHex(peer_id)
        + "&port=" + this.port + "&downloaded=0" + "&left=" + left;
    System.out.println("Tracker announce: " + url);
    this.connection = (HttpURLConnection) new URL(url).openConnection();

    this.response = this.connection.getInputStream();
    DataInputStream reader = new DataInputStream(this.response);

    byte[] response_bytes = new byte[reader.available()];
    reader.readFully(response_bytes);
    reader.close();
    this.response.close();
    this.connection.disconnect();

    return this.getPeers(response_bytes);
  }


  /*
   * Close connection to tracker
   */
  public void disconnect() throws MalformedURLException, IOException {


    int left = this.torrent.file_length;

    String url = this.torrent.announce_url + "?info_hash="
        + toHex(this.torrent.info_hash.array()) + "&peer_id=" + toHex(peer_id)
        + "&port=" + this.port + "&downloaded=0" + "&left=" + left
        + "&event=stopped";
    System.out.println("Tracker announce: " + url);
    this.connection = (HttpURLConnection) new URL(url).openConnection();
    this.connection.connect();

    this.response.close();
    this.connection.disconnect();
  }

  /*
   * Takes the response from the tracker and accesses the lists of peers and stores them in a local array
   */
  @SuppressWarnings("unchecked")
  private Peer[] getPeers(byte[] responseBytes) throws BencodingException {
    Map<ByteBuffer, Object> map = (Map<ByteBuffer, Object>) Bencoder2
        .decode(responseBytes);
    ArrayList<Map<ByteBuffer, Object>> list = (ArrayList<Map<ByteBuffer, Object>>) map
        .get(str_to_bb("peers"));
    Peer[] peers = new Peer[list.size()];
    for (int i = 0; i < list.size(); i++) {

      Map<ByteBuffer, Object> peer = (Map<ByteBuffer, Object>) list.get(i);

      ByteBuffer buff = (ByteBuffer) peer.get(str_to_bb("peer id"));
      byte[] curr_id = buff.array();

      buff = (ByteBuffer) peer.get(str_to_bb("ip"));
      String curr_ip = new String(buff.array());

      int curr_port = (Integer) peer.get(str_to_bb("port"));

      if (curr_port == this.port) {
        continue;
      }

      peers[i] = new Peer(curr_id, curr_ip, curr_port);
    }
    return peers;
  }

  /*
   * Helper methods for the above functions
   */
  public static String toHex(byte[] bytes) {
    Formatter formatter = new Formatter();
    for (byte b : bytes) {
      formatter.format("%%%02x", b);
    }
    String output = formatter.toString();
    formatter.close();
    return output;
  }

  // public static String encode(String s) {
  // String result = "";
  // for (int i = 0; i < s.length(); i += 2) {
  // result += "%" + s.charAt(i) + s.charAt(i + 1);
  // }
  // return result;
  // }

  public static Charset charset = Charset.forName("UTF-8");
  public static CharsetEncoder encoder = charset.newEncoder();

  public static ByteBuffer str_to_bb(String msg) {
    try {
      return encoder.encode(CharBuffer.wrap(msg));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
