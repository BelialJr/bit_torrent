import java.io.IOException;
import java.net.Socket;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class main {
    public static int availablePort;

    public static void main(String[] args)
    {
        //6681

        new Thread(()->{
            BitTracker bitTracker =  BitTracker.getInstance();
        }).start();
            try
            {
                Thread.sleep(1500);
             Client client = new Client(BitTracker.HOST, availablePort);
                Thread.sleep(1000);
             Client client1 = new Client(BitTracker.HOST, availablePort);
                Thread.sleep(1000);
             Client client2 = new Client(BitTracker.HOST, availablePort);
                Thread.sleep(1000);
             Client client3 = new Client(BitTracker.HOST, availablePort);
                Thread.sleep(1000);


             client1.share_FIle("libssh2.dll",true);
                Thread.sleep(1000);
             client1.share_FIle("memreduct.exe",true);
                Thread.sleep(1000);
             client3.share_FIle("libssh22.dll",true);
                Thread.sleep(1000);

         //  client.socket.close();
             client2.download("81b2c5b02970b50e9c3621735c3d525e",true,2);
                Thread.sleep(1000);
            // client2.disconnect();
                Thread.sleep(1000);client2 = new Client(BitTracker.HOST, availablePort);
             client.download("81b2c5b02970b50e9c3621735c3d525e",true,0);

} catch (InterruptedException | NullPointerException | IOException e) {
        e.printStackTrace();
        }
        }
}
