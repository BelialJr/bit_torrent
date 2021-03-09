package Client;

import javafx.util.Pair;

import java.io.*;
import java.net.Socket;
import java.util.*;



public class ClientP2P {


    public  Client clientReference;
    private String checkSum;
    private String addresses;
    private int stopAfterSegments;
    
    public ClientP2P(Client client)
    {
        this.clientReference = client;

    }
    public void start(String checkSum, String addresses, int stopAfterSegments) throws IOException, InterruptedException {
        System.out.println(clientReference.getSymbols(60,'_')+"\n" + "[TOKEN:"+this.clientReference.token+"] P2P STARTED "+"\n"+ clientReference.getSymbols(60,'‾'));
        this.checkSum = checkSum;
        this.addresses = addresses.replace("[","").replace("]","");
        this.stopAfterSegments = stopAfterSegments > 0 ? stopAfterSegments : Integer.MAX_VALUE;
        HashMap<Socket,List<Integer>> connectedSocketsAndSegments = generateSocketsPool();
        HashMap<Socket, Pair<BufferedReader,OutputStream>> socketsStreams = generateSocketsStreams(connectedSocketsAndSegments) ;
        startListeningP2PClientThread(socketsStreams);
        askForSegments(connectedSocketsAndSegments,socketsStreams);

    }

    private void askForSegments(HashMap<Socket, List<Integer>> connectedSocketsAndSegments, HashMap<Socket, Pair<BufferedReader, OutputStream>> socketsStreams) throws InterruptedException {
        int downloadedSegmentsCount = 0;
        int maxDownloadedSegmentsCount =  this.clientReference.downloadingFile.getFileSegments();
        int willDownloadSegemntsCount =    this.stopAfterSegments > maxDownloadedSegmentsCount ? maxDownloadedSegmentsCount : this.stopAfterSegments;
        Thread.sleep(50);
        System.out.println(clientReference.getSymbols(60,'_')+"\n" + "[P2P DOWNlOADING STARTED "+"\n"+ clientReference.getSymbols(60,'‾'));
        System.out.println("MAX possible segment count = " + maxDownloadedSegmentsCount + " : WIll be downloaded = " + willDownloadSegemntsCount );

        while(downloadedSegmentsCount <  willDownloadSegemntsCount) {
            for (Map.Entry<Socket, List<Integer>> socketSegments : connectedSocketsAndSegments.entrySet()) {
                if (downloadedSegmentsCount < willDownloadSegemntsCount) {

                        try
                            {
                                Socket targetSocket = socketSegments.getKey();
                                int segmentToDownloaded = socketSegments.getValue().remove(0);
                                System.out.println("P2P Socket[" + targetSocket.getLocalPort() + "] SENDS [/get " + this.checkSum + " " + segmentToDownloaded + "] TO P2P Server[" + targetSocket.getPort() + "]" + "\n-----\n");
                                sendP2P(socketsStreams.get(targetSocket).getValue(), "/get " + this.checkSum + " " + segmentToDownloaded);
                                downloadedSegmentsCount++;
                            } catch(
                            IndexOutOfBoundsException ignore)

                            {
                            }

                    Thread.sleep(150);
                } else {
                    break;
                }
            }

        }
        System.out.println(clientReference.getSymbols(60,'-'));
    }


    private void startListeningP2PClient(Socket socket, BufferedReader bufferedReader, OutputStream os) {
        try {
            while (!socket.isClosed()) {
                String line = bufferedReader.readLine();
                String[] str = line.split(" ");
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                if (str[0].equals("/get_answer")){
                   System.out.println("P2P Socket[" + socket.getLocalPort() + "] Downloading segment " + str[2] + " : ##########");
                   byte[] bytes = new byte[Integer.parseInt(str[3])];
                   in.readFully(bytes);
                   if(this.clientReference.downloadingFile.getSegments().get(Integer.parseInt(str[2])).equals(Crypto.getSHA1((new ByteArrayInputStream(bytes)))))
                   {
                       System.out.println("P2P Socket[" + socket.getLocalPort() + "] Downloaded segment " + str[2] + " : Successfully");
                       clientReference.checkIfFileInfoExistInPossesed(this.clientReference.downloadingFile.getFileCheckSum());
                       clientReference.addToPossesedFilesInfo(this.clientReference.downloadingFile.getFileCheckSum() ,Integer.valueOf(str[2]),Crypto.getSHA1((new ByteArrayInputStream(bytes))) );
                       clientReference.addDownloadedBytes(Integer.valueOf(str[2]),bytes,this.clientReference.downloadingFile.getFileCheckSum());

                       if(clientReference.canCreateAFile(this.clientReference.downloadingFile.getFileCheckSum()))
                       {
                           System.out.println("P2P Socket[" + socket.getLocalPort() + "] : Ready to create file ");
                           clientReference.createFile(this.clientReference.downloadingFile.getFileCheckSum());
                        //   clientReference.printPossessedFiles();
                           clientReference.share_FIle(clientReference.getFileFullName(this.clientReference.downloadingFile.getFileCheckSum()),false);
                       }else{
                           System.out.println("P2P Socket[" + socket.getLocalPort() + "] : Not ready to create file [Not enough segments ]");
                           System.out.println("[TOKEN:"+this.clientReference.token+"] TO Server [/segments_update]");
                           clientReference.send("/segments_update " + this.clientReference.downloadingFile.getFileCheckSum() +  " " + this.clientReference.getFileSegments(this.clientReference.downloadingFile.getFileCheckSum()));
                         }
                   }
                }

            }
        } catch (IOException e ) {
            e.printStackTrace();
        }
    }

    private void startListeningP2PServer() {
        try {
            System.out.println("[TOKEN:" + this.clientReference.token + "] :  READY TO P2P CONNECTION ");
            Socket uploadSocket = clientReference.serverSocket.accept();
            Thread.sleep(30);
            System.out.println("[TOKEN:" + this.clientReference.token + "] :  RECEIVED P2P CONNECTION : From " + uploadSocket.getPort());
            InputStream inputStream = uploadSocket.getInputStream();
            OutputStream outputStream = uploadSocket.getOutputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

            while (!uploadSocket.isClosed()) {
                String line = br.readLine();
                String[] str = line.split(" ");

                if (str[0].equals("/get")){
                    System.out.println("P2P Server[" +uploadSocket.getLocalPort()+"] Received FROM P2P CLient["+uploadSocket.getPort()+"] : "+line + "\n-----\n");
                    String checksum = str[1];
                    Integer segment = Integer.valueOf(str[2]);

                    int segmentSize = MyFile.getFileSegementSize(clientReference.possessedFiles,checksum);
                    System.out.println("P2P Server[" +uploadSocket.getLocalPort()+"] Sends [ "+"/get_answer " + checksum + " " +str[2] + " "+ segmentSize +"] "+ "\n-----\n" );
                    sendP2P(outputStream,"/get_answer " + checksum + " " +str[2] + " "+ segmentSize );
                    DataOutputStream out = new DataOutputStream(uploadSocket.getOutputStream());
                    out.write(MyFile.getFilePartBytes(clientReference.possessedFiles,checksum,segment));
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startListeningP2PClientThread(HashMap<Socket, Pair<BufferedReader, OutputStream>> socketsStreams) {
        for(Map.Entry<Socket, Pair<BufferedReader, OutputStream>> map : socketsStreams.entrySet()){
            new Thread(()->{
                startListeningP2PClient(map.getKey(),map.getValue().getKey(),map.getValue().getValue());
            }).start();
        }

    }

    public  void startListeningP2PServerThread(){
        new Thread(()->{
            startListeningP2PServer();
        }).start();
    }

    public void sendP2P(OutputStream outputStream ,String string){
        try {
            outputStream.write((string + "\n").getBytes());
            outputStream.flush();

        } catch (IOException ignore) { }
    }
    private HashMap<Socket, Pair<BufferedReader, OutputStream>> generateSocketsStreams(HashMap<Socket, List<Integer>> connectedSocketsAndSegments) throws IOException {
        HashMap<Socket, Pair<BufferedReader, OutputStream>> result = new HashMap<>();
        for(Socket socket:connectedSocketsAndSegments.keySet())
        {
            InputStream inputStream = socket.getInputStream();
            BufferedReader br =  new BufferedReader(new InputStreamReader(inputStream));
            OutputStream outputStream = socket.getOutputStream();
            result.put(socket,new Pair<>(br,outputStream));
        }
        return result;
    }

    private HashMap<Socket,List<Integer>> generateSocketsPool() { // [1-127.0.0.1:50036,2-127.0.0.1:50028,3-127.0.0.1:50036,4-127.0.0.1:50028] ADDRESSES
        HashMap<Socket,List<Integer>> connectedSocketsAndSegments = new HashMap<>();
        HashMap<String,List<Integer>> adressesAndSegments = new HashMap<>();
        List<String> addresses = new ArrayList<>();
        Set<String> adressesSet = new LinkedHashSet<>();
        Collections.addAll(addresses, this.addresses.split(","));


        for(String str : addresses){
            adressesSet.add(str.split("-")[1]);
        }
        for( String uniqueAddress : adressesSet){
            for(String fullADdress : addresses){

                Integer segment = Integer.valueOf(fullADdress.split("-")[0]);
                String lineAddress = fullADdress.split("-")[1];

                if(uniqueAddress.equals(lineAddress)){
                    if(adressesAndSegments.get(uniqueAddress) == null){
                        adressesAndSegments.put(uniqueAddress,new ArrayList<Integer>(List.of(segment)));
                    }else{
                        adressesAndSegments.get(uniqueAddress).add(segment);
                    }

                }
            }
        }
        System.out.println("Addresses and Segments : \n" + adressesAndSegments + "\n");


        for(Map.Entry<String,List<Integer>> map : adressesAndSegments.entrySet())
        {
            try {
                String host = map.getKey().split(":")[0];
                Integer port = Integer.valueOf(map.getKey().split(":")[1]);
                Socket socket = new Socket(host, port);
                System.out.println("SOCKET CREATED ["+socket.getLocalPort()+"] TO " + host + ":" + port + " = "+ map.getValue());
                connectedSocketsAndSegments.put( socket , map.getValue())  ;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return connectedSocketsAndSegments;
    }


}
