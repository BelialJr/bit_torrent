import java.io.*;
import java.net.Socket;

public class ConnectionHandler
{
    private BitTracker bitTrakcer;
    private Socket clientSocket;
    private String userToken;



    private InputStream inputStream;
    private OutputStream outputStream;

    public ConnectionHandler(String userToken, int port, Socket clientSocket, BitTracker bitTrakcer) throws IOException {
        this.bitTrakcer = bitTrakcer;
        this.clientSocket = clientSocket;
        this.userToken = userToken;
        this.inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();
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
                bitTrakcer.share_command(userToken,fileName,fileSize,checkSum);
            }
            if (str[0].equals("/download")) {

            }
            if(str[0].equals("/quit")){
                bitTrakcer.disconnectUser(userToken);
                break;

        }
        }
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

}
