package Server;

import Client.FileINFO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class BitTracker
{
    private static BitTracker _instance;

    public static final String HOST = "127.0.0.1";
    public static final int PORT = 45550;
    private static ServerSocket openedSocket;
    private boolean isWaitinForSegments = false;

    private List<UserINFO> listINFO;  // Current Users Info
    private List<ConnectionHandler>  activeConnections;
    private HashMap<String,HashMap<String,List<Integer>>> usersSegments; //user token : MAP<FILE CHECKSUM, available segments>
    private HashMap<String,HashMap<String,List<Integer>>> peersSegments = new HashMap<>();
    private ConnectionHandler currentConnection;

    private boolean isAlive;
    private int token = 1;



    private BitTracker()
    {
        listINFO = new ArrayList<>();
        activeConnections = new ArrayList<>();
        usersSegments = new HashMap<>();
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

        }
    }

    private void startListening()
    {
        try {
            openedSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("[SERVER] : STARTED");
        System.out.println("[SERVER] : Socket[PORT :" + openedSocket.getLocalPort() + "] : Has been opened");
        while (isAlive)
        {
            try {
                int port =  openedSocket.getLocalPort() ;
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

    private void updateListPeers(){//recount peers
        ArrayList<String> combined = new ArrayList<>();
        for(Map.Entry<String,HashMap<String,List<Integer>>> peer: peersSegments.entrySet())
        {
            for(Map.Entry<String,List<Integer>> entry2 : peer.getValue().entrySet())
            {
                combined.add(entry2.getKey());
            }
        }
        for(UserINFO var:listINFO) {
            int index = 0;
            for (String checkSum : var.getCheckSums()){
                int peers = Collections.frequency(combined,checkSum);
                var.getPeersAvailable().add(index,peers);
                index++;
            }
        }

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

    public void removeFromPeers(String userToken, String checkSum) {
         if(peersSegments.get(userToken) != null)
             if(peersSegments.get(userToken).get(checkSum) != null)
                 peersSegments.get(userToken).remove(checkSum);
            updateListPeers();
    }

    private void addNewUser(int port, Socket clientSocket,String userToken) {
        listINFO.add(new UserINFO(userToken,port,clientSocket.getLocalAddress().toString().replace("/","")+":"+clientSocket.getPort()));
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




    public void peersSegmnetsADd(String userToken, String fileCheckSum, String segments) {
        if(peersSegments.get(userToken) != null)
            peersSegments.remove(userToken);
        List<Integer> segmentsList = new ArrayList<>();
        for(char c : segments.toCharArray())
        {
            if(Character.isDigit(c))
                segmentsList.add(Character.getNumericValue(c));
        }
        HashMap<String,List<Integer>> values = new HashMap<>();
        values.put(fileCheckSum,segmentsList);
        peersSegments.put(userToken,values);

        updateListPeers();

    }

    public void sendAnnounce(String checksumToDownload) {
        List<String> tokens = new ArrayList<>();
        for (UserINFO userINFO : listINFO)
        {
            if(userINFO.getCheckSums().contains(checksumToDownload))
                tokens.add(userINFO.getToken());
        }
        for(Map.Entry<String,HashMap<String,List<Integer>>> peer: peersSegments.entrySet())
        {
            for(Map.Entry<String,List<Integer>> entry2 : peer.getValue().entrySet())
            {
                if(entry2.getKey().equals(checksumToDownload))
                    tokens.add(peer.getKey());
            }
        }

        System.out.println("[SERVER] : Sending [/announce] to clients " + Arrays.toString(tokens.toArray()) + "\n");
        for(ConnectionHandler conn : activeConnections)
        {
            if(tokens.contains(conn.getUserToken())) {
                conn.currentFileChecksum = checksumToDownload;
                conn.send("/announce " + checksumToDownload);
            }
        }
    }

    public void addOpenedSocket(String userToken, String openedSocketPort) {
        UserINFO userINFO = listINFO.stream().filter(e->e.getToken().equals(userToken)).findFirst().get();
        userINFO.setDownloadPort(Integer.valueOf(openedSocketPort));
    }


    public synchronized void addUserSegments(String token, String fileCheckSum, List<Integer> segments){
        if(usersSegments.get(token) == null){
            HashMap<String,List<Integer>> temp = new HashMap<>();
            temp.put(fileCheckSum,segments);
            usersSegments.put(token,temp);
        }else{
            HashMap<String,List<Integer>> temp = usersSegments.get(token);

            if(temp.get(token) != null)
                temp.remove(fileCheckSum);
            temp.put(fileCheckSum,segments);
        }
    }

    public synchronized void sendDownloadList(String userToken, String checksumToDownload) {
        ConnectionHandler conn = activeConnections.stream().filter(e->e.getUserToken().equals(userToken)).findFirst().get();


        new java.util.Timer().schedule(new java.util.TimerTask() {
                                           @Override
                                           public void run() {
                                               String resultTokens = getSortedSegmentsAndAddresses(checksumToDownload);
                                               String resultAddresses  = castTokensToAdresses(resultTokens);
                                               conn.send("/clientsAddresses " + checksumToDownload + " " + resultAddresses);
                                           }

                                           private String castTokensToAdresses(String str){
                                               List<String> result = new ArrayList<>();
                                               str = str.replace("[","").replace("]","");
                                               String[] data = str.split(",");
                                               for(String s:data)
                                               {
                                                  UserINFO info = listINFO.stream().filter(e->e.getToken().equals(s.split(":")[1])).findFirst().get();
                                                  String adresses =  info.getUserAddress().split(":")[0];
                                                  String port = info.getDownloadPort();
                                                  result.add(s.split(":")[0]+"-" + adresses +":" + port);
                                               }
                                               return result.toString().replace(" ","");
                                           }
                                       },
                3000
        );
    }

    private String getSortedSegmentsAndAddresses(String checkSum){
        HashMap<String,List<Integer>> tokenSegments = new HashMap<>(); // TOKEn , segments

        for(Map.Entry<String,HashMap<String,List<Integer>>> entry1:usersSegments.entrySet())
        {
        for(Map.Entry<String,List<Integer>> entry2:entry1.getValue().entrySet()){
            if(entry2.getKey().equals(checkSum))
            {
                tokenSegments.put(entry1.getKey(),entry2.getValue());
            }
        }
    }
        List<Integer> allSegments = new ArrayList<>();
        for(Map.Entry<String,List<Integer>> map : tokenSegments.entrySet())
        {
            allSegments.addAll(map.getValue());
        }
        Set<Integer> segmentsSet = new HashSet<>(allSegments);
        Map<Integer,Integer> segmentFrequency = new HashMap<>(); // Segment , frequency
        for(Integer var: segmentsSet)
        {
            segmentFrequency.put(var,Collections.frequency(allSegments,var));
        }
        Map<Integer,Integer> segmentFrequencySorted = new LinkedHashMap<>();

        segmentFrequency.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e->segmentFrequencySorted.put(e.getKey(),e.getValue()));

        Map<Integer,List<String>> segmentUserTokens = new LinkedHashMap<>(); // Segment ,Tokens which contains this segment

        for(Map.Entry<Integer,Integer> map : segmentFrequencySorted.entrySet())
        {
            Integer segment = map.getKey();

            for(Map.Entry<String,List<Integer>> entry:tokenSegments.entrySet())
            {
                List<Integer> segments = entry.getValue();
                String token = entry.getKey();

                if(segments.contains(segment)) {
                    if(segmentUserTokens.get(segment) == null) {
                        ArrayList<String> list = new ArrayList<>();
                        list.add(token);
                        segmentUserTokens.put(segment, list);
                    }
                    else{
                        List<String> list =   segmentUserTokens.get(segment);
                        list.add(token);
                    }

                }
            }
        }


        Map<Integer,String> resultMap = new HashMap<>(); // Segment , Token
        List<String> alreadyOccurred = new ArrayList<>();
        List<String> willOccurre = new ArrayList<>();

        System.out.println(getSymbols(60,'_')+"\n"+"[SERVER] : SEGMENTS SORTING ALGORITHM STARTED "+"\n"+ getSymbols(60,'‾'));
        System.out.println("Sorted Segments by TOKENS COUNT foreach segment \n");
        segmentUserTokens.entrySet().forEach(e-> System.out.print(e.getKey() + " : " + e.getValue() + "\n"));
        System.out.println("-----");
        for(Map.Entry<Integer,List<String>> map : segmentUserTokens.entrySet()) {
            willOccurre.addAll(map.getValue());
        }


        for(Map.Entry<Integer,List<String>> map : segmentUserTokens.entrySet())
        {
            Integer segment = map.getKey();
            List<String> tokens = new ArrayList<>(map.getValue()) ;
            System.out.println("ALREADY OCCURED : " + alreadyOccurred);
            System.out.println("WILL OCCUR : " + willOccurre);
            if(tokens.size() > 1) {
                System.out.print(segment + " : ");
                HashMap<String,Integer> mapScores = new LinkedHashMap<>();
                int currentScore = 0;
                for (int i = 0; i < tokens.size(); i++) {
                    currentScore += Collections.frequency(willOccurre,tokens.get(i));
                    currentScore += 3*Collections.frequency(alreadyOccurred,tokens.get(i));
                    System.out.print(tokens.get(i) + "="+currentScore +" ; ");
                    mapScores.put(tokens.get(i),currentScore);
                    willOccurre.remove(0);
                    currentScore = 0;
                }
                System.out.println();
                String winner = mapScores.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).findFirst().map(Map.Entry::getKey).get();
                System.out.println("WINNER : " + winner + "\n-----\n");
                alreadyOccurred.add(winner);
                resultMap.put(segment,winner);
            }else {
                System.out.println(segment + "  : " + "\n" + tokens.get(0));
                resultMap.put(segment, tokens.get(0));
                alreadyOccurred.add(tokens.get(0));
                willOccurre.remove(0);

            }
        }
        List<String> resultStr = new ArrayList<>();;
        resultMap.entrySet().forEach(e->resultStr.add(e.getKey() + ":" + e.getValue()));
        System.out.println(resultStr.toString());
        System.out.println(getSymbols(60,'-'));
        return resultStr.toString().replace(" ","");
    }

    public synchronized void waitForAllSegements(int i) {
        if(!isWaitinForSegments){
            isWaitinForSegments = true;
            new java.util.Timer().schedule(new java.util.TimerTask() {
                                               @Override
                                               public void run() {
                                                   printUpdatedUsersSegments();
                                                   isWaitinForSegments = false;
                                               }
                                           },
                    2000
            );
        }
    }


    public synchronized void printUpdatedUsersSegments(){
        System.out.println(getSymbols(60,'_')+"\n"+"[SERVER] : UPDATED USERS SEGMENTS" +"\n"+ getSymbols(60,'‾'));
        for(Map.Entry<String,HashMap<String,List<Integer>>> entry1:usersSegments.entrySet())
        {
            System.out.println(entry1.getKey() + " : ");
            for(Map.Entry<String,List<Integer>> entry2:entry1.getValue().entrySet()){
                System.out.println("-"+entry2.getKey() + " : " + entry2.getValue());
            }
            System.out.println(getSymbols(60,'-'));
        }
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


    public synchronized  String list_command() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n"+getSymbols(179,'_')+"\n");
        sb.append(String.format("%-9s",  "| TOKEN "));
        sb.append(String.format("%-14s", "| SOCKET_PORT " ));
        sb.append(String.format("%-22s", "| USER_ADDRESS " ));
        sb.append(String.format("%-17s", "| DOWNLOAD_PORT " ));
        sb.append(String.format("%-20s", "| FILE_NAME "));
        sb.append(String.format("%-15s", "| FILE_SIZE "));
        sb.append(String.format("%-42s", "| CHECK_SUM "));
        sb.append(String.format("%-20s", "| SEEDS_AVAILABLE"));
        sb.append(String.format("%-20s", "| PEERS_AVAILABLE|"));
        sb.append( "\n"+ getSymbols(179,'‾')+"\n");
        LinkedList<UserINFO> temp = new LinkedList<>();
        temp.addAll(listINFO);
        for(UserINFO var : temp){
            if(!(var.getCheckSums().size() > 0)){
                sb.append(getLineInfo(var.getToken(),var.getOpenedPort(),var.getUserAddress(),var.getDownloadPort(),"","","",0,0));
            }
            else
            {
                for (int i = 0; i <var.getCheckSums().size() ; i++) {
                    if(i == 0)
                        sb.append(getLineInfo(var.getToken(),var.getOpenedPort(),var.getUserAddress(),var.getDownloadPort(),var.getFileNames().get(i),var.getFileSizes().get(i),var.getCheckSums().get(i),var.getSeedsAvailable().get(i),var.getPeersAvailable().get(i)));
                    else
                        sb.append(getLineInfo("","","","",var.getFileNames().get(i),var.getFileSizes().get(i),var.getCheckSums().get(i),var.getSeedsAvailable().get(i),var.getPeersAvailable().get(i) ));
                }
            }
        }

        sb.append(getSymbols(179,'-')+"\n");
        return sb.toString();
    }
    private String getLineInfo(String token,String port,String address,String downloadPort,String fileName,String fileSize,String checkSum,int seeds,int peers){
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-9s",  "| " + token));
        sb.append(String.format("%-14s", "| " + port));
        sb.append(String.format("%-22s", "| " + address));
        sb.append(String.format("%-17s", "| " + downloadPort));
        sb.append(String.format("%-20s", "| " + fileName));
        sb.append(String.format("%-15s", "| " + fileSize));
        sb.append(String.format("%-42s", "| " + checkSum));
        sb.append(String.format("%-20s", "| " +  seeds));
        sb.append(String.format("%-20s", "| " +  peers) + "\n");

        return sb.toString();
    }



}
