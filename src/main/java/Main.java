import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        switch (args[0]) {
            case "multithread": {
                System.out.println("Calculating MD5 hash for " + args[1] + "...");
                try {
                    System.out.println(new BigInteger(
                            1, MD5Calculator.md5ForkJoin(args[1])).toString());
                } catch (IOException e) {
                    System.out.println("File I/O error.");
                } catch (NoSuchAlgorithmException e) {
                    System.out.println("MD5 algorithm not found.");
                }
                break;
            }
            case "compare": {

                break;
            }
            default: {
                System.out.println("Calculating MD5 hash for " + args[0] + "...");
                try {
                    System.out.println(new BigInteger(
                            1, MD5Calculator.md5WithSingleThread(args[0])).toString());
                } catch (IOException e) {
                    System.out.println("File I/O error.");
                } catch (NoSuchAlgorithmException e) {
                    System.out.println("MD5 algorithm not found.");
                }
            }
        }
    }
}
