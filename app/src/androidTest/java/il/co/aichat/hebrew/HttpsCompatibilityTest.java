package il.co.aichat.hebrew;

import android.test.ActivityInstrumentationTestCase2;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class HttpsCompatibilityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public HttpsCompatibilityTest() { super(MainActivity.class); }

    private void checkHttps(String address, boolean gemini) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection)new URL(address).openConnection();
        SSLSocketFactory factory = gemini
            ? getActivity().geminiSocketFactory()
            : (SSLSocketFactory)SSLSocketFactory.getDefault();
        if (android.os.Build.VERSION.SDK_INT >= 16 && android.os.Build.VERSION.SDK_INT <= 19)
            factory = new MainActivity.Tls12SocketFactory(factory);
        connection.setSSLSocketFactory(factory);
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();
        assertTrue("HTTPS server did not return an HTTP response for " + address, code > 0);
        connection.disconnect();
    }

    public void testGoogleGeminiHttpsHandshake() throws Exception {
        checkHttps("https://generativelanguage.googleapis.com/", true);
    }

    public void testOpenAiHttpsHandshake() throws Exception {
        checkHttps("https://api.openai.com/", false);
    }
}
