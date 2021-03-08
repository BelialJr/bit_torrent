package Server;


import java.util.LinkedList;
import java.util.List;

public class UserINFO
{
    private String Token;
    private int openedPort ;
    private int downloadPort;
    private String userAddress;
    private LinkedList<String> fileNameList;
    private LinkedList<String> fileSizeList;
    private LinkedList<String> checkSums;
    private LinkedList<Integer> seedsAvailable ;
    private LinkedList<Integer> peersAvailable;




    public UserINFO(String token, int openedPort, String userAddress) {
        this.Token = token;
        this.openedPort = openedPort;
        this.userAddress = userAddress;
        this.fileNameList = new LinkedList<>();
        this.fileSizeList = new LinkedList<>();
        this.checkSums    = new LinkedList<>();
        this.seedsAvailable =  new LinkedList<>();
        this.peersAvailable = new LinkedList<>();
    }

    public String getHost(){
        return  userAddress.split(":")[0];
    }

    public String getDownloadPort() {
        return String.valueOf(downloadPort);
    }

    public void setDownloadPort(int downloadPort) {
        this.downloadPort = downloadPort;
    }

    public String getToken() {
        return Token;
    }

    public void setToken(String token) {
        Token = token;
    }

    public String getOpenedPort() {
        return String.valueOf(openedPort);
    }
    public Integer getOpenedPortInt() {
        return openedPort;
    }

    public void setOpenedPort(int openedPort) {
        this.openedPort = openedPort;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }


    public LinkedList<Integer> getSeedsAvailable() {
        return seedsAvailable;
    }


    public LinkedList<Integer> getPeersAvailable() {
        return peersAvailable;
    }



    public List<String> getFileNames() {
        return fileNameList;
    }

    public List<String> getFileSizes() {
        return fileSizeList;
    }

    public List<String> getCheckSums() {
        return checkSums;
    }

    public void addNewFIle(String fileName, String fileSize, String checkSum) {
        this.fileNameList.add(fileName);
        this.fileSizeList.add(fileSize);
        this.checkSums.   add(checkSum);
        this.peersAvailable.add(0);
        this.seedsAvailable.add(0);
    }
}
