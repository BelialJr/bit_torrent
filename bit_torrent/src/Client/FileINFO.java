package Client;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileINFO
{
    private String URL;
    private String fileCheckSum;
    private String fileName;
    private String fileNameFull;
    private String fileSize;
    private int fileSegments;
    private String fileSegmentSize;
    private HashMap<Integer,String> segments; // #Number //Segemnt CheckSum



    public FileINFO() {
        this.segments = new HashMap<>();
    }

    public static FileINFO coppyHeaders(FileINFO coppyFile){
        FileINFO result = new FileINFO();
        result.setURL(coppyFile.getURL());
        result.setFileCheckSum(coppyFile.getFileCheckSum());
        result.setFileName(coppyFile.getFileName());
        result.setFileNameFull(coppyFile.getFileNameFull());
        result.setFileSize(coppyFile.getFileSize());
        result.setFileSegments(coppyFile.getFileSegments());
        result.setFileSegmentSize(coppyFile.getFileSegmentSize());
        return result;
    }

    public  static boolean canCreateAFile(FileINFO fileINFO) {
        return fileINFO.getFileSegments() == fileINFO.getSegments().keySet().size();
    }


    public void setURL(InetAddress inetAddress, int port) {
        String host = inetAddress.toString().replace("/","");
        this.URL = host+":"+String.valueOf(port);
    }
    public void setFileCheckSum(byte[] fileContent) {
        this.fileCheckSum = Crypto.getSHA1(new ByteArrayInputStream(fileContent));
        int firstNumber = Character.getNumericValue(String.valueOf(fileContent.length).charAt(0));
        int digitsCount = String.valueOf(fileContent.length).length();
        int multiplier = digitsCount > 1 ? firstNumber + 1:firstNumber;
        int segmentSize = (int)Math.pow(10,digitsCount-1);                // result size =  multiplier * segmentSize
        this.fileSegmentSize = String.valueOf(segmentSize);
        this.fileSegments = multiplier;

        int bytesLeft = fileContent.length;
        int coppyIndex = 0;

        for (int i = 1; i <this.fileSegments+1 ; i++)
        {
            int howMuchToCoppy =  bytesLeft > segmentSize ? segmentSize: bytesLeft;
            byte[] byteSegment  = new byte[segmentSize];
            System.arraycopy(fileContent,coppyIndex,byteSegment,0,howMuchToCoppy);
            InputStream is = new ByteArrayInputStream(byteSegment);
            String hashSum = Crypto.getSHA1(is);
            this.segments.put(i,hashSum);
            bytesLeft -= segmentSize;
            coppyIndex += segmentSize;
        }
    }

    public void generateFile(String torrentFilePath){
        File file = new File(torrentFilePath+fileName+".torrent.txt");
        BufferedWriter bw = null;
        try {
            if(file.createNewFile()){
                FileWriter fw = new FileWriter(file);
                bw = new BufferedWriter(fw);
                bw.write("URL = " +URL +"\n");
                bw.write("FILE_CHECK_SUM = " +fileCheckSum +"\n");
                bw.write("FILE_NAME = " +fileNameFull +"\n");
                bw.write("FILE_SIZE = " +fileSize +"\n");
                bw.write("FILE_SEGMENTS = " +fileSegments +"\n");
                bw.write("SEGMENT_SIZE = " +fileSegmentSize +"bytes" +"\n");
                for(Map.Entry<Integer,String> line: segments.entrySet())
                {
                    bw.write("# " + line.getKey() + " : " + line.getValue()+"\n");
                }
                bw.flush();
                bw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally
        {
            try{
                if(bw!=null)
                    bw.close();
            }catch(Exception ex){
               ex.printStackTrace();
            }
        }

    }
    public static FileINFO getInstance(String torrentFilePAth) throws IOException {
        FileINFO fileINFO = new FileINFO();
        List<String> lines = Files.readAllLines(Paths.get(torrentFilePAth));
        for(String line : lines)
        {
            String str[] = line.split(" ");
            if(str[0].equals("URL")){
                fileINFO.setURL(str[2]);
            }else if(str[0].equals("FILE_CHECK_SUM")){
                fileINFO.setFileCheckSum(str[2]);
            }else if(str[0].equals("FILE_NAME")){
                fileINFO.setFileName(str[2]);
            }else if(str[0].equals("FILE_SIZE")){
                fileINFO.setFileSize(str[2]);
            }else if(str[0].equals("FILE_SEGMENTS")){
                fileINFO.setFileSegments(str[2]);
            }else if(str[0].equals("SEGMENT_SIZE")) {
                fileINFO.setFileSegmentSize(str[2]);
            }else if(str[0].equals("#")){
                fileINFO.getSegments().put(Integer.valueOf(str[1]),str[3]);
            }
        }
        return fileINFO;
    }

    @Override
    public String toString() {
        return "FileINFO : \n" +
                "-URL='" + URL + '\'' + '\n'+
                "-fileCheckSum='" + fileCheckSum + '\'' + '\n'+
                "-fileName='" + fileName + '\'' + '\n'+
                "-fileNameFull='" + fileNameFull + '\'' + '\n'+
                "-fileSize='" + fileSize + '\'' + '\n'+
                "-fileSegments=" + fileSegments + '\n'+
                "-fileSegmentSize=" + fileSegmentSize + '\n'+
                "-segments=\n" + segments.entrySet()
                .stream()
                .map(entry -> entry.getKey() + " - " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }





    public int getFileSegments() {
        return fileSegments;
    }

    public void setFileSegments(int fileSegments) {
        this.fileSegments = fileSegments;
    }
    public void setFileSegments(String fileSegments) {
        this.fileSegments = Integer.valueOf(fileSegments);
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getFileCheckSum() {
        return fileCheckSum;
    }

    public void setFileCheckSum(String fileCheckSum) {
        this.fileCheckSum = fileCheckSum;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public HashMap<Integer, String> getSegments() {
        return segments;
    }

    public void setSegments(HashMap<Integer, String> segments) {
        this.segments = segments;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileNameFull = fileName;
        this.fileName = fileName.split("\\.")[0];
    }

    public String getFileSegmentSize() {
        return fileSegmentSize;
    }

    public void setFileSegmentSize(String fileSegmentSize) {
        this.fileSegmentSize = fileSegmentSize;
    }

    public String getFileNameFull() {
        return fileNameFull;
    }

    public void setFileNameFull(String fileNameFull) {
        this.fileNameFull = fileNameFull;
    }
}


