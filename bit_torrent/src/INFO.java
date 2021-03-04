import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class INFO
{
    private String Token;
    private int openedPort ;
    private String userAddress;
    private LinkedList<String> fileNameList;
    private LinkedList<String> fileSizeList;
    private LinkedList<String> checkSums;
    private String seedsAvailable;
    private String peersAvailable;




    public INFO(String token, int openedPort, String userAddress) {
        this.Token = token;
        this.openedPort = openedPort;
        this.userAddress = userAddress;
        this.fileNameList = new LinkedList<>();
        this.fileSizeList = new LinkedList<>();
        this.checkSums    = new LinkedList<>();
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


    public String getSeedsAvailable() {
        return seedsAvailable;
    }

    public void setSeedsAvailable(String seedsAvailable) {
        this.seedsAvailable = seedsAvailable;
    }

    public String getPeersAvailable() {
        return peersAvailable;
    }

    public void setPeersAvailable(String peersAvailable) {
        this.peersAvailable = peersAvailable;
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
    }
}
