package Client;


import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client {
    private final String downloadFrom = "Files/usersFiles/";
    private final String downloadTo = "Files/receivedFiles/";
    private final String torrentFilePath = "Files/torrentFiles/";
    private int stopAfterSegments;
    public List<FileINFO> possessedFiles;
    public FileINFO downloadingFile;
    public ServerSocket serverSocket;
    public ClientP2P clientP2P;
    private String host;
    private int port;
    public Socket socket;
    private InputStream is;
    private OutputStream os;
    public String token;

    public Client() throws IOException {
        this.possessedFiles = new ArrayList<FileINFO>();
        this.serverSocket = new ServerSocket(0);
        this.clientP2P = new ClientP2P(this);
    }
    public void connect(String host, int port) throws IOException{
        this.host = host;
        this.port = port;
        this.socket = new Socket(host,port);
        this.is = socket.getInputStream();
        this.os = socket.getOutputStream();
        startListening();
        send("/available_socket " + serverSocket.getLocalPort());
    }

    public void share_FIle(String filename,boolean createTorrentFile){
        String fileSize = getFileSize(filename);
        String checkSum = Crypto.getSHA1(downloadFrom+filename);
        FileINFO fileINFO = generateFileINFo(filename);
        possessedFiles.add(fileINFO);
        send("/share " + filename +" " + fileSize+ " " + checkSum);
        if(createTorrentFile && fileINFO != null)
            fileINFO.generateFile(torrentFilePath);
    }

    public void startListening() throws IOException {
        new Thread(()->{
        BufferedReader br = new BufferedReader(new InputStreamReader(this.is));
        while (!socket.isClosed())
        {
            try {
                String line = br.readLine();
                String[] str = line.split(" ");

                if (str[0].equals("/token")) {
                    this.token = str[1];
                }
                if (str[0].equals("/announce")) {
                    int[] arr = getAvailableSegments(str[1]);
                    System.out.println("[TOKEN:"+this.token+"] Sending to SERVER [ANNOUNCE] RESPONSE : " + Arrays.toString(arr));
                    send("/announce_resp : " + Arrays.toString(arr));
                    this.clientP2P.startListeningP2PServerThread();
                }
                if(str[0].equals("/clientsAddresses")) {
                    System.out.println("[TOKEN:"+this.token+"] RECEIVED [GET] RESPONSE : " + line);
                    this.clientP2P.start(str[1],str[2],stopAfterSegments);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        }).start();
    }



    public void download(String fileName,boolean Debug, int StopAfterSegments) {
        try {
            this.stopAfterSegments = StopAfterSegments;
            FileINFO fileINFO = FileINFO.getInstance(torrentFilePath +fileName);
            this.downloadingFile = fileINFO;
            System.out.println("[TOKEN:"+this.token+"] \n"+getSymbols(60,'_')+"\nUnpacking Torrent FIle :\n"+getSymbols(60,'‾')+"\n"+ fileINFO + '\n'+getSymbols(60,'-') );
            if(socket == null){
                System.out.println("Connecting to the SERVER \n" + getSymbols(60,'-'));
                connect(fileINFO.getURL().split(":")[0], Integer.parseInt(fileINFO.getURL().split(":")[1]));
            }
            if(this.token == null)
                Thread.sleep(1000);
            System.out.println("[TOKEN:"+this.token+"] Sending to SERVER [/GET] command : \n");
            send("/GET "  + fileINFO.getFileCheckSum());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private FileINFO generateFileINFo(String filename) {
        File file = new File(downloadFrom+filename);
        FileINFO fileINFO = new FileINFO();
        fileINFO.setURL(socket.getInetAddress(), socket.getPort());
        fileINFO.setFileName(filename);
        fileINFO.setFileSize(getFileSize(filename));
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            fileINFO.setFileCheckSum(fileContent);
            return fileINFO;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }


    private int[] getAvailableSegments(String checkSum) {
        FileINFO fileINFO = possessedFiles.stream().filter(e->e.getFileCheckSum().trim().equals(checkSum.trim())).findFirst().get();
        int[] arr = fileINFO.getSegments().keySet().stream().mapToInt(Number::intValue).toArray();
        return arr;
    }

    public String getSymbols(int howMuch,char symbol) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < howMuch; i++) {
            stringBuilder.append(symbol);
        }
        return stringBuilder.toString();
    }

    public synchronized void checkIfFileInfoExistInPossesed(String fileCheckSum)
    {
        if( possessedFiles.stream().anyMatch(e->e.getFileCheckSum().equals(fileCheckSum))  == false)
        {
            possessedFiles.add(FileINFO.coppyHeaders(this.downloadingFile));
        }
    }

    public synchronized void addToPossesedFilesInfo(String fileCheckSum, Integer segment, String segmentCheckSum)
    {
         possessedFiles.stream().filter(e->e.getFileCheckSum().equals(fileCheckSum)).findFirst().get().getSegments().put(segment,segmentCheckSum);
    }

    public synchronized boolean canCreateAFile(String fileCheckSum) {
        return FileINFO.canCreateAFile(possessedFiles.stream().filter(e->e.getFileCheckSum().equals(fileCheckSum)).findFirst().get());
    }

    public synchronized String getFileFullName(String fileCheckSum) {
        return this.possessedFiles.stream().filter(e->e.getFileCheckSum().equals(fileCheckSum)).findFirst().get().getFileNameFull();
    }
    public synchronized String getFileSegments(String fileCheckSum) {
       return  possessedFiles.stream().filter(e->e.getFileCheckSum().equals(fileCheckSum)).findFirst().get().getSegments().keySet().toString().replace(" ","");
    }



    public void printPossessedFiles()
    {
        List<FileINFO> possessedFilesCoppies = new ArrayList<FileINFO>(possessedFiles);
        for(FileINFO f : possessedFilesCoppies)
            System.out.println(f);
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
    public synchronized void send(String string){
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



}
