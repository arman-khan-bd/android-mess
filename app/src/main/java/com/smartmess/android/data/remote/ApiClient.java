package com.smartmess.android.data.remote;

import android.content.Context;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smartmess.android.data.remote.dto.LoginRequest;
import com.smartmess.android.data.remote.dto.LoginResponse;
import com.smartmess.android.data.remote.dto.RegisterRequest;
import com.smartmess.android.data.remote.dto.RegisterResponse;
import com.smartmess.android.utils.SessionManager;

import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
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

            // TLS 1.2 Support for Android 4.4 / 5.0 (API 19/21+) compatibility
            enableTls12OnOlderDevices(builder);

            sOkHttpClient = builder.build();
        }
        return sOkHttpClient;
    }

    /**
     * Enables TLS 1.2 for Android 4.4/5.0 devices where TLS 1.2 is supported by the OS
     * but not enabled by default.
     */
    private static void enableTls12OnOlderDevices(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore) null);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                if (trustManagers.length < 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
                }
                X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, new TrustManager[]{trustManager}, null);
                client.sslSocketFactory(sslContext.getSocketFactory(), trustManager);

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);
                client.connectionSpecs(specs);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    // Direct synchronous helper methods for Auth (used during initial login/register before session exists)
    private final OkHttpClient standaloneClient;
    private final Gson gson;

    public ApiClient() {
        this.standaloneClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
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
