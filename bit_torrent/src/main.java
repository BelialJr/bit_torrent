import java.io.IOException;
import java.net.Socket;

public class main {
    public static int availablePort;

    public static void main(String[] args)
    {
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

             client1.share_FIle("libssh2.dll");
                Thread.sleep(1000);
             client1.share_FIle("memreduct.exe");
                Thread.sleep(1000);
             client3.share_FIle("libssh22.dll");


            } catch (InterruptedException | NullPointerException | IOException e) {
                e.printStackTrace();
            }
     }
}
