package com.smartmess.android.data.remote;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smartmess.android.data.remote.dto.LoginRequest;
import com.smartmess.android.data.remote.dto.LoginResponse;
import com.smartmess.android.data.remote.dto.RegisterRequest;
import com.smartmess.android.data.remote.dto.RegisterResponse;
import com.smartmess.android.utils.SessionManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String TAG = "ApiClient";

    // Official ISRG Root X1 Root CA (Let's Encrypt / cPanel AutoSSL Trust Anchor)
    // Bundled programmatically as fail-safe guarantee for Android 6.0.1 (API 23) and below
    private static final String ISRG_ROOT_X1_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw\n" +
            "TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh\n" +
            "cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4\n" +
            "WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu\n" +
            "ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY\n" +
            "MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc\n" +
            "h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+\n" +
            "0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U\n" +
            "A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW\n" +
            "T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH\n" +
            "B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC\n" +
            "B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv\n" +
            "KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn\n" +
            "OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn\n" +
            "jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw\n" +
            "qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI\n" +
            "rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV\n" +
            "HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq\n" +
            "hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL\n" +
            "ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ\n" +
            "3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK\n" +
            "NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5\n" +
            "ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur\n" +
            "TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC\n" +
            "jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc\n" +
            "oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq\n" +
            "4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA\n" +
            "mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d\n" +
            "emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=\n" +
            "-----END CERTIFICATE-----\n";

    private static ApiService sApiService;
    private static Retrofit sRetrofit;
    private static OkHttpClient sOkHttpClient;

    public static synchronized ApiService getApiService(Context context) {
        if (sApiService == null) {
            Retrofit retrofit = getRetrofitInstance(context);
            sApiService = retrofit.create(ApiService.class);
        }
        return sApiService;
    }

    public static synchronized Retrofit getRetrofitInstance(Context context) {
        if (sRetrofit == null) {
            OkHttpClient client = getOkHttpClient(context);
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .setLenient()
                    .create();

            sRetrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return sRetrofit;
    }

    public static synchronized OkHttpClient getOkHttpClient(Context context) {
        if (sOkHttpClient == null) {
            final SessionManager sessionManager = new SessionManager(context.getApplicationContext());

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            // Interceptor for Bearer token & Tenant headers
            builder.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Accept", "application/json");

                    String token = sessionManager.getAuthToken();
                    if (token != null && !token.trim().isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                    }

                    String messUuid = sessionManager.getMessUuid();
                    if (messUuid != null && !messUuid.trim().isEmpty()) {
                        requestBuilder.header("X-Mess-UUID", messUuid);
                    }

                    return chain.proceed(requestBuilder.build());
                }
            });

            // Modern Root CA bundling + TLS 1.2 Socket Factory (Resolves Android 6.0.1 CertPathValidatorException)
            configureCustomTls(builder, context);

            sOkHttpClient = builder.build();
        }
        return sOkHttpClient;
    }

    /**
     * Configures custom TrustManager bundling ISRG Root X1 and modern TLS protocols
     * onto an OkHttpClient.Builder to guarantee seamless connectivity on Android 6.0.1 (API 23).
     */
    public static void configureCustomTls(OkHttpClient.Builder builder, Context context) {
        try {
            X509TrustManager trustManager = getCompositeTrustManager(context);
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());

            TLSSocketFactory tlsSocketFactory = new TLSSocketFactory(sslContext.getSocketFactory());
            builder.sslSocketFactory(tlsSocketFactory, trustManager);

            ConnectionSpec modernTls = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                    .build();

            ConnectionSpec compatTls = new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
                    .build();

            List<ConnectionSpec> specs = new ArrayList<>();
            specs.add(modernTls);
            specs.add(compatTls);
            specs.add(ConnectionSpec.CLEARTEXT);
            builder.connectionSpecs(specs);

            Log.i(TAG, "Custom TLSv1.2 and bundled ISRG Root X1 TrustManager configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure custom TLS: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a composite X509TrustManager that checks against both the system trust store
     * and the bundled ISRG Root X1 trust anchor.
     */
    private static X509TrustManager getCompositeTrustManager(Context context) {
        // 1. System Default TrustManager
        X509TrustManager systemTm = null;
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    systemTm = (X509TrustManager) tm;
                    break;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not initialize system default TrustManager: " + e.getMessage());
        }

        // 2. Bundled ISRG Root X1 TrustManager
        X509TrustManager isrgTm = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate isrgCert = null;

            // Attempt to load from res/raw/isrgrootx1
            if (context != null) {
                try {
                    int rawId = context.getResources().getIdentifier("isrgrootx1", "raw", context.getPackageName());
                    if (rawId != 0) {
                        try (InputStream is = context.getResources().openRawResource(rawId)) {
                            isrgCert = cf.generateCertificate(is);
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // Fallback to embedded PEM string
            if (isrgCert == null) {
                try (InputStream is = new ByteArrayInputStream(ISRG_ROOT_X1_PEM.getBytes(StandardCharsets.UTF_8))) {
                    isrgCert = cf.generateCertificate(is);
                }
            }

            if (isrgCert != null) {
                KeyStore isrgKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                isrgKeyStore.load(null, null);
                isrgKeyStore.setCertificateEntry("isrgrootx1", isrgCert);

                TrustManagerFactory isrgTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                isrgTmf.init(isrgKeyStore);
                for (TrustManager tm : isrgTmf.getTrustManagers()) {
                    if (tm instanceof X509TrustManager) {
                        isrgTm = (X509TrustManager) tm;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating bundled ISRG Root X1 TrustManager: " + e.getMessage(), e);
        }

        final X509TrustManager finalSystemTm = systemTm;
        final X509TrustManager finalIsrgTm = isrgTm;

        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                if (finalSystemTm != null) {
                    finalSystemTm.checkClientTrusted(chain, authType);
                }
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // First attempt default system trust validation
                try {
                    if (finalSystemTm != null) {
                        finalSystemTm.checkServerTrusted(chain, authType);
                        return;
                    }
                } catch (CertificateException defaultEx) {
                    // System rejected cert (expected on Android 6.0.1 lacking ISRG Root X1)
                    // Validate against bundled modern trust anchor
                    if (finalIsrgTm != null) {
                        finalIsrgTm.checkServerTrusted(chain, authType);
                        return;
                    }
                    throw defaultEx;
                }

                if (finalIsrgTm != null) {
                    finalIsrgTm.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                List<X509Certificate> issuers = new ArrayList<>();
                if (finalSystemTm != null) {
                    issuers.addAll(Arrays.asList(finalSystemTm.getAcceptedIssuers()));
                }
                if (finalIsrgTm != null) {
                    issuers.addAll(Arrays.asList(finalIsrgTm.getAcceptedIssuers()));
                }
                return issuers.toArray(new X509Certificate[0]);
            }
        };
    }

    /**
     * Custom TLSSocketFactory that forces TLS 1.2 on older Android devices (API 23 and below).
     */
    public static class TLSSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;

        public TLSSocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket() throws IOException {
            return enableTLSOnSocket(delegate.createSocket());
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return enableTLSOnSocket(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return enableTLSOnSocket(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return enableTLSOnSocket(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return enableTLSOnSocket(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return enableTLSOnSocket(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket enableTLSOnSocket(Socket socket) {
            if (socket instanceof SSLSocket) {
                SSLSocket sslSocket = (SSLSocket) socket;
                String[] supported = sslSocket.getSupportedProtocols();
                List<String> enabled = new ArrayList<>();
                for (String proto : new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"}) {
                    for (String sup : supported) {
                        if (sup.equals(proto)) {
                            enabled.add(proto);
                            break;
                        }
                    }
                }
                if (!enabled.isEmpty()) {
                    sslSocket.setEnabledProtocols(enabled.toArray(new String[0]));
                }
            }
            return socket;
        }
    }

    // Direct synchronous helper methods for Auth (used during initial login/register before session exists)
    private final OkHttpClient standaloneClient;
    private final Gson gson;

    public ApiClient(Context context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // Attach custom TLS & bundled Root CAs to standalone client as well
        configureCustomTls(builder, context);

        this.standaloneClient = builder.build();
        this.gson = new Gson();
    }

    public ApiClient() {
        this(null);
    }

    public LoginResponse login(String phone, String password) throws IOException {
        String jsonBody = gson.toJson(new LoginRequest(phone, password));
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(ApiConfig.ROUTE_AUTH_LOGIN)
                .post(body)
                .build();

        try (Response response = standaloneClient.newCall(request).execute()) {
            if (response.body() != null) {
                return gson.fromJson(response.body().string(), LoginResponse.class);
            }
        }
        return null;
    }

    public RegisterResponse register(RegisterRequest registerRequest) throws IOException {
        String jsonBody = gson.toJson(registerRequest);
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(ApiConfig.ROUTE_AUTH_REGISTER)
                .post(body)
                .build();

        try (Response response = standaloneClient.newCall(request).execute()) {
            if (response.body() != null) {
                return gson.fromJson(response.body().string(), RegisterResponse.class);
            }
        }
        return null;
    }

    public RegisterResponse register(String name, String phone, String password, String messName, String inviteCode) throws IOException {
        return register(new RegisterRequest(name, phone, password, messName, inviteCode));
    }
}
