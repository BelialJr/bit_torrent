package Server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConnectionHandler
{
    private BitTracker bitTrakcer;
    private int port;
    private Socket clientSocket;
    private String userToken;
    private InputStream inputStream;
    private OutputStream outputStream;
    public String currentFileChecksum;

    public ConnectionHandler(String userToken, int port, Socket clientSocket, BitTracker bitTrakcer) throws IOException {
        this.port = port;
        this.bitTrakcer = bitTrakcer;
        this.clientSocket = clientSocket;
        this.userToken = userToken;
        this.inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();
        send("/token " + this.userToken);
    }

    public void startListening() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(this.inputStream));
        while (!clientSocket.isClosed())
        {

            String line = br.readLine();
            String[] str = line.split(" ");
            if (str[0].equals("/share")){
                System.out.println("[SERVER] : TOKEN["+ this.userToken+"]: Socket[PORT :" + clientSocket.getLocalPort() + "] : SHARE :["+line+"]" );
                String fileName = str[1];
                String fileSize = str[2];
                String checkSum = str[3];
                bitTrakcer.removeFromPeers(userToken,checkSum);
                bitTrakcer.share_command(userToken,fileName,fileSize,checkSum);
            }
            if (str[0].equals("/GET")) {
                String checksumToDownload = str[1];
                System.out.println("[SERVER] : TOKEN["+ this.userToken+"]: [/GET = "+checksumToDownload+"]");
                bitTrakcer.sendAnnounce(checksumToDownload);
                bitTrakcer.sendDownloadList(userToken,checksumToDownload);
            }
            if (str[0].equals("/available_socket")) {
                String openedSocketPort = str[1];
                this.bitTrakcer.addOpenedSocket(this.userToken,openedSocketPort);
            }
            if (str[0].equals("/announce_resp")) {
                this.saveAnnounceREsponse(line.split(":")[1]);
            }
            if (str[0].equals("/segments_update")) {
                this.bitTrakcer.peersSegmnetsADd(this.userToken,str[1],str[2]);
                System.out.println("[SERVER] : TOKEN["+ this.userToken+"]: RECEIVED [/segments_update] = " + line );
            }

            if(str[0].equals("/quit")){
                bitTrakcer.disconnectUser(userToken);
                break;

        }
        }
    }
    public void saveAnnounceREsponse(String segmentsArray){
        List<Integer> segments = new ArrayList<>();
        for(char c : segmentsArray.toCharArray()){
            if(Character.isDigit(c)){
                segments.add(Character.getNumericValue(c));
            }
        }
        bitTrakcer.addUserSegments(userToken,currentFileChecksum,segments);
        bitTrakcer.waitForAllSegements(2000);

    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void send(String string){
        try {
            if(!this.clientSocket.isClosed()) {
                outputStream.write((string + "\n").getBytes());
                outputStream.flush();
            }
        } catch (IOException ignore) {

        }
    }
}
