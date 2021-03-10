import Client.Client;
import Server.BitTracker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class main {
    //szyfrowanie + port changing;
    public static void main(String[] args) {

        handleFiles();

        new Thread(()->{
            BitTracker bitTracker =  BitTracker.getInstance();
        }).start();
            try
            {
                Thread.sleep(1500);
             Client client = new Client();
                client.connect(BitTracker.HOST,BitTracker.PORT);
                Thread.sleep(1000);
             Client client1 = new Client();
                client1.connect(BitTracker.HOST,BitTracker.PORT);
                Thread.sleep(1000);
             Client client2 = new Client();
                client2.connect(BitTracker.HOST,BitTracker.PORT);
                Thread.sleep(1000);
             Client client3 = new Client();
                client3.connect(BitTracker.HOST,BitTracker.PORT);
                Thread.sleep(1000);
             Client client4 = new Client();
                client4.connect(BitTracker.HOST,BitTracker.PORT);


             client1.share_FIle("libssh2.dll",true);
                Thread.sleep(1000);
             client1.share_FIle("memreduct.exe",true);
                Thread.sleep(1000);
             client3.share_FIle("libssh22.dll",true);
                Thread.sleep(1000);
             client.share_FIle("sql.jar",true);
                Thread.sleep(1000);
             client4.share_FIle("sql.jar",false);


            Client donwloadClient1 = new Client();
                donwloadClient1.download("sql.torrent.txt",true,2);
                Thread.sleep(10000);

            Client donwloadClient2 = new Client();
                donwloadClient2.download("sql.torrent.txt",true,0);
                Thread.sleep(10000);
               // donwloadClient2.showBytes();

            Client donwloadClient3 = new Client();
                donwloadClient3.download("memreduct.torrent.txt",true,0);
                Thread.sleep(100000);

} catch (InterruptedException | NullPointerException | IOException e) {
        e.printStackTrace();
        }
        }

    private static void handleFiles() {
        try {
            Files.walk(Paths.get("Files/torrentFiles")).filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.walk(Paths.get("Files/receivedFiles")).filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(File::delete);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
