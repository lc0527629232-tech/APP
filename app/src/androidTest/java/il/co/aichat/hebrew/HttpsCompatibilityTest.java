package il.co.aichat.hebrew;

import android.test.InstrumentationTestCase;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class HttpsCompatibilityTest extends InstrumentationTestCase {
    private void checkHttps(String address) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection)new URL(address).openConnection();
        connection.setSSLSocketFactory(new MainActivity.Tls12SocketFactory((SSLSocketFactory)SSLSocketFactory.getDefault()));
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();
        assertTrue("HTTPS server did not return an HTTP response for " + address, code > 0);
        connection.disconnect();
    }
    public void testGoogleGeminiHttpsHandshake() throws Exception {
        checkHttps("https://generativelanguage.googleapis.com/");
    }
    public void testOpenAiHttpsHandshake() throws Exception {
        checkHttps("https://api.openai.com/");
    }
}
