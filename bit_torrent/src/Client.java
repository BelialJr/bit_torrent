import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

public class Client {
    private final String downloadFrom = "Files/usersFiles/";
    private final String downloadTo = "Files/receivedFiles/";
    private final String torrentFilePath = "Files/receivedFiles/";
    private String host;
    private int port;
    public Socket socket;
    private InputStream is;
    private OutputStream os;

    public Client(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.socket = new Socket(host,port);
        this.is = socket.getInputStream();
        this.os = socket.getOutputStream();
    }

    public void share_FIle(String filename,boolean createTorrentFile){
        String fileSize = getFileSize(filename);
        String checkSum = getMd5(filename);
        send("/share " + filename +" " + fileSize+ " " + checkSum);
        if(createTorrentFile)
            createTorrentFile();
    }

    private void createTorrentFile(){

    }

    private void send(String string){
        try {
            if(!socket.isClosed()) {
                os.write((string + "\n").getBytes());
                os.flush();
            }
        } catch (IOException ignore) {

        }
    }

    public String getFileSize(String filename){
        File file = new File(downloadFrom+filename);
        return getFileSizeKiloBytes(file);
    }

    public  String getMd5(String filename) {
        try {
            InputStream is = Files.newInputStream(Paths.get(downloadFrom+filename));
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            DigestInputStream dis = new DigestInputStream(is, md);
            dis.readAllBytes();
            dis.close();
            byte[] raw = md.digest();

            BigInteger bigInt = new BigInteger(1, raw);
            StringBuilder hash = new StringBuilder(bigInt.toString(16));

            while (hash.length() < 40 ) {
                hash.insert(0, '0');
            }
            return hash.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private  String getFileSizeKiloBytes(File file) {
        return (int) file.length() / 1024 + "kb";
    }

    public void disconnect(){
        send("/quit");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void download(String checksum,boolean Debug, int StopAfterSegments) {
        send("/download "  +checksum);
    }
}
