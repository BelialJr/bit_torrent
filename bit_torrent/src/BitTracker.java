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

    private List<UserINFO> listINFO;  // Current Users Info
    private List<FileINFO> filesINFO; // Segments Checksum

    private List<ConnectionHandler>  activeConnections;
    private ConnectionHandler currentConnection;

    private boolean isAlive;
    private int token = 1;
    private final int portRange =  65535;
    public  int availablePort;


    private BitTracker()
    {
        listINFO = new ArrayList<>();
        filesINFO = new ArrayList<>();
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
            String line =  br.readLine();
            String[] str =line.split(" ");
            if (str[0].equals("/list")) {
                System.out.println(list_command());
            }
            if (str[0].equals("/file")) {
              //  System.out.println(list_command());
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
                UserINFO INFOtoRemove = listINFO.stream().filter(e1->e1.getToken().equals(currentConnection.getUserToken())).findFirst().get();
                listINFO.remove(INFOtoRemove);
                activeConnections.remove(currentConnection);
                System.out.println("[SERVER] : TOKEN["+ currentConnection.getUserToken()+"]: "+" : DISCONNECTED" );
                updateListSeeds();
                e.printStackTrace();

            }
        }).start();

    }
    public void disconnectUser(String userToken){
        UserINFO INFOtoRemove = listINFO.stream().filter(e1->e1.getToken().equals(userToken)).findFirst().get();
        listINFO.remove(INFOtoRemove);
        activeConnections.remove(currentConnection);
        System.out.println("[SERVER] : TOKEN["+ userToken+"]: "+" : DISCONNECTED" );
        updateListSeeds();
        System.out.println(list_command());
    }


    private void updateListSeeds(){//recount seeds
        ArrayList<String> combined = new ArrayList<>();
        for(UserINFO var:listINFO) {
            combined.addAll(var.getCheckSums());
        }
        for(UserINFO var:listINFO) {
            int index = 0;
            for (String checkSum : var.getCheckSums()){
                int seeds = Collections.frequency(combined,checkSum);
                var.getSeedsAvailable().add(index,seeds);
                index++;
            }
        }
    }

    private void addNewUser(int port, Socket clientSocket,String userToken) {
        listINFO.add(new UserINFO(userToken,port,clientSocket.getLocalAddress().toString().replace("/","")+":"+clientSocket.getPort()));
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
        try {
            UserINFO obj = listINFO.stream().filter(e -> e.getToken().equals(userToken)).findFirst().get();
            obj.addNewFIle(fileName, fileSize, checkSum);
            updateListSeeds();
            System.out.println(list_command());
        }catch (java.util.NoSuchElementException e){}
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

    public  String list_command() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n"+getSymbols(152,'_')+"\n");
        sb.append(String.format("%-9s",  "| TOKEN "));
        sb.append(String.format("%-14s", "| SOCKET_PORT " ));
        sb.append(String.format("%-22s", "| USER_ADDRESS " ));
        sb.append(String.format("%-20s", "| FILE_NAME "));
        sb.append(String.format("%-15s", "| FILE_SIZE "));
        sb.append(String.format("%-42s", "| CHECK_SUM "));
        sb.append(String.format("%-20s", "| SEEDS_AVAILABLE"));
        sb.append(String.format("%-20s", "| PEERS_AVAILABLE|"));
        sb.append( "\n"+ getSymbols(152,'‾')+"\n");
        LinkedList<UserINFO> temp = new LinkedList<>();
        temp.addAll(listINFO);
        for(UserINFO var : temp){
            if(!(var.getCheckSums().size() > 0)){
                sb.append(getLineInfo(var.getToken(),var.getOpenedPort(),var.getUserAddress(),"","","",0,0));
            }
            else
            {
                for (int i = 0; i <var.getCheckSums().size() ; i++) {
                    if(i == 0)
                        sb.append(getLineInfo(var.getToken(),var.getOpenedPort(),var.getUserAddress(),var.getFileNames().get(i),var.getFileSizes().get(i),var.getCheckSums().get(i),var.getSeedsAvailable().get(i),var.getPeersAvailable().get(i)));
                    else
                        sb.append(getLineInfo("","","",var.getFileNames().get(i),var.getFileSizes().get(i),var.getCheckSums().get(i),var.getSeedsAvailable().get(i),var.getPeersAvailable().get(i) ));
                }
            }
        }

        sb.append(getSymbols(152,'-')+"\n");
        return sb.toString();
    }
    private String getLineInfo(String token,String port,String address,String fileName,String fileSize,String checkSum,int seeds,int peers){
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-9s",  "| " + token));
        sb.append(String.format("%-14s", "| " + port));
        sb.append(String.format("%-22s", "| " + address));
        sb.append(String.format("%-20s", "| " + fileName));
        sb.append(String.format("%-15s", "| " + fileSize));
        sb.append(String.format("%-42s", "| " + checkSum));
        sb.append(String.format("%-20s", "| " +  seeds));
        sb.append(String.format("%-20s", "| " +  peers) + "\n");

        return sb.toString();
    }


}
