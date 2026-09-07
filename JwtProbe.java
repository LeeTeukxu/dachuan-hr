import com.auth0.jwt.JWT;
import java.nio.file.*;

public class JwtProbe {
    public static void main(String[] a) throws Exception {
        String token = new String(Files.readAllBytes(Paths.get("/tmp/tok_employee.json"))).trim();
        var result = JWT.decode(token);
        String x = result.getClaim("Content").asString();
        System.out.println("asString len=" + (x == null ? "NULL" : x.length()));
        System.out.println("prefix=" + (x == null ? "" : x.substring(0, Math.min(60, x.length()))));
        var verified = JWT.require(com.auth0.jwt.algorithms.Algorithm.HMAC256("!TIANYE_HR_20240306$$")).build().verify(token);
        System.out.println("verify ok, jti=" + verified.getClaim("jti").asString());
    }
}
