import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class BitTracker
{
    private static BitTracker _instance;

    public static final String HOST = "127.0.0.1";
    private static ServerSocket openedSocket;

    private List<INFO> listINFO;
    private List<ConnectionHandler>  activeConnections;
    private ConnectionHandler currentConnection;

    private boolean isAlive;
    private int token = 1;
    private final int portRange =  65535;
    public  int availablePort;


    private BitTracker()
    {
        listINFO = new ArrayList<>();
        activeConnections = new ArrayList<>();
        isAlive = true;
        startReadingInput();
        startListening();

    }


   private void startReadingInput() {
      new Thread(()->{
        try {
           startReadingInputThread();
            } catch (IOException ignore) {
        }
       }).start();
    }

    private void startReadingInputThread() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (isAlive){
            String str = br.readLine();
            switch (str){
                case "/list":
                    System.out.println(list_command());
                    break;
                default:
                    break;
            }
        }
    }

    private void startListening()
    {
        System.out.println("[SERVER] : STARTED");
        while (isAlive)
        {
            try {

                int port = generatePort();
                main.availablePort = port;
                availablePort = port;
                openedSocket = new ServerSocket(port);
                System.out.println("[SERVER] : Socket[PORT :" + openedSocket.getLocalPort() + "] : Has been opened");
                Socket clientSocket = openedSocket.accept();
                System.out.println("[SERVER] : Socket[PORT :" + openedSocket.getLocalPort() + "] : CONNECTED : CLIENT["+clientSocket.getLocalAddress().toString().replace("/","")+":"+clientSocket.getPort()+"]");
                String userToken = generateUserToken();
                addNewUser(port,clientSocket,userToken);
                System.out.println(list_command());
                handleNewUser(port,clientSocket,userToken);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void handleNewUser(int port, Socket clientSocket, String userToken)
    {
        new Thread( () ->{
            try
            {
                currentConnection = new ConnectionHandler(userToken,port,clientSocket,this);
                activeConnections.add(currentConnection);
                currentConnection.startListening();
            }
            catch (NullPointerException | IOException e)
            {
                INFO INFOtoRemove = listINFO.stream().filter(e1->e1.getToken().equals(currentConnection.getUserToken())).findFirst().get();
                listINFO.remove(INFOtoRemove);
                updateListInfo();
                activeConnections.remove(currentConnection);

            }
        }).start();

    }

    private void updateListInfo(){//recount seeds

    }

    private void addNewUser(int port, Socket clientSocket,String userToken) {
        listINFO.add(new INFO(userToken,port,clientSocket.getLocalAddress().toString().replace("/","")+":"+clientSocket.getPort()));
    }

    private Integer generatePort()
    {
        Random rand = new Random();
        while(true)
        {
            int result = rand.nextInt(portRange)+1;
            boolean flag = listINFO.stream().anyMatch(e->e.getOpenedPortInt() == result);
            if(!flag)
                return result;
        }

    }
    private String generateUserToken()
    {
        return "USER#" + token++;
    }

    public void share_command(String userToken, String fileName, String fileSize, String checkSum){//recount seeds
        INFO obj = listINFO.stream().filter(e->e.getToken().equals(userToken)).findFirst().get();
        obj.addNewFIle(fileName,fileSize,checkSum);
        updateListInfo();
        System.out.println(list_command());
    }


    public static BitTracker getInstance() {
        if(_instance == null)
            _instance = new BitTracker();
        return _instance;
    }


    private String getSymbols(int howMuch,char symbol) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < howMuch; i++) {
            stringBuilder.append(symbol);
        }
        return stringBuilder.toString();
    }

    public String list_command() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n"+getSymbols(152,'_')+"\n");
        sb.append(String.format("%-9s",  "| TOKEN "));
        sb.append(String.format("%-14s", "| SOCKET_PORT " ));
        sb.append(String.format("%-22s", "| USER_ADDRESS " ));
        sb.append(String.format("%-20s", "| FILE_NAME "));
        sb.append(String.format("%-15s", "| FILE_SIZE "));
        sb.append(String.format("%-34s", "| CHECK_SUM "));
        sb.append(String.format("%-20s", "| SEEDS_AVAILABLE"));
        sb.append(String.format("%-20s", "| PEERS_AVAILABLE|"));
        sb.append( "\n"+ getSymbols(152,'‾')+"\n");
        for(INFO var : listINFO){
//            sb.append(String.format("%-9s",  "| " + var.getToken()) );
//            sb.append(String.format("%-14s", "| " + var.getOpenedPort()));
//            sb.append(String.format("%-22s", "| " + var.getUserAddress()));
//            sb.append(String.format("%-15s", "| " + var.getFileNames()));
//            sb.append(String.format("%-15s", "| " + var.getFileSizes()));
//            sb.append(String.format("%-32s", "| " + var.getCheckSums()));
//            sb.append(String.format("%-20s", "| " + var.getSeedsAvailable()));
//            sb.append(String.format("%-20s", "| " + var.getPeersAvailable()) + "\n");

            if(!(var.getCheckSums().size() > 0)){
                sb.append(getLineInfo(var.getToken(),var.getOpenedPort(),var.getUserAddress(),"","","","",""));
            }
            else
            {
                for (int i = 0; i <var.getCheckSums().size() ; i++) {
                    if(i == 0)
                        sb.append(getLineInfo(var.getToken(),var.getOpenedPort(),var.getUserAddress(),var.getFileNames().get(i),var.getFileSizes().get(i),var.getCheckSums().get(i),"",""));
                    else
                        sb.append(getLineInfo("","","",var.getFileNames().get(i),var.getFileSizes().get(i),var.getCheckSums().get(i),"","" ));
                }
            }
        }

        sb.append(getSymbols(152,'-')+"\n");
        return sb.toString();
    }
    private String getLineInfo(String token,String port,String address,String fileName,String fileSize,String checkSum,String seeds,String peers){
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-9s",  "| " + token));
        sb.append(String.format("%-14s", "| " + port));
        sb.append(String.format("%-22s", "| " + address));
        sb.append(String.format("%-20s", "| " + fileName));
        sb.append(String.format("%-15s", "| " + fileSize));
        sb.append(String.format("%-34s", "| " + checkSum));
        sb.append(String.format("%-20s", "| " +  seeds));
        sb.append(String.format("%-20s", "| " +  peers) + "\n");

        return sb.toString();
    }


}
