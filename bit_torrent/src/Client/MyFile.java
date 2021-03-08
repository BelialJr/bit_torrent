package Client;

import java.io.*;

import java.util.List;

public class MyFile {



    public static int getFileSegementSize(List<FileINFO> possessedFiles, String checksum){
        FileINFO fileData = possessedFiles.stream().filter(e->e.getFileCheckSum().equals(checksum)).findFirst().get();
        String fileName = fileData.getFileNameFull();
        int segmentSize  = Integer.parseInt(fileData.getFileSegmentSize().replace("bytes","").trim());
        return  segmentSize;
    }

    public static byte[] getFilePartBytes(List<FileINFO> possessedFiles, String checksum, Integer segment){
        FileINFO fileData = possessedFiles.stream().filter(e->e.getFileCheckSum().equals(checksum)).findFirst().get();
        String fileName = fileData.getFileNameFull();
        int segmentSize  = Integer.parseInt(fileData.getFileSegmentSize().replace("bytes","").trim());

        try {
            byte[] bytes = new byte[segmentSize];
            RandomAccessFile file = new RandomAccessFile("Files/usersFiles/"+fileName, "r");
            file.seek((segment-1)*segmentSize);
            file.read(bytes);
            return bytes;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }
}
