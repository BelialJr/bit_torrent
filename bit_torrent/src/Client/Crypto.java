package Client;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class Crypto {


    public  static String getSHA1(String filePath) {
        try {
            InputStream is = Files.newInputStream(Paths.get(filePath));
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            DigestInputStream dis = new DigestInputStream(is, md);
            dis.readAllBytes();
            dis.close();
            byte[] raw = md.digest();

            BigInteger bigInt = new BigInteger(1, raw);
            StringBuilder hash = new StringBuilder(bigInt.toString(16));

            while (hash.length() < 40 ) {
                hash.insert(0, '0');
            }
            return hash.toString();
        } catch (Throwable t) {
            return null;
        }
    }
    public  static String getSHA1(InputStream is) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            DigestInputStream dis = new DigestInputStream(is, md);
            dis.readAllBytes();
            dis.close();
            byte[] raw = md.digest();

            BigInteger bigInt = new BigInteger(1, raw);
            StringBuilder hash = new StringBuilder(bigInt.toString(16));

            while (hash.length() < 40 ) {
                hash.insert(0, '0');
            }
            return hash.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
