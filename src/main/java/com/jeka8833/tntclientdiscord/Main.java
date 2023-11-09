package com.jeka8833.tntclientdiscord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class Main {

    @Value("${bot.token}")
    private String botToken;

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Bean
    public DataSource getDataSource() {
        return DataSourceBuilder.create()
                .url(dbUrl)
                .username(dbUsername)
                .password(dbPassword)
                .build();
    }

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {
        return DiscordClientBuilder.create(botToken).build()
                .gateway()
                .setInitialPresence(ignore -> ClientPresence.online(ClientActivity.playing("TNTClient")))
                .login()
                .block();
    }

    @Bean
    public RestClient discordRestClient(GatewayDiscordClient client) {
        return client.getRestClient();
    }

    public static OkHttpClient okHttpClient = createSocket();
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(Main.class, args);
        Thread.sleep(10000000);
    }

    @SuppressWarnings("KotlinInternalInJava")
    private static OkHttpClient createSocket() {
        OkHttpClient.Builder newBuilder = new OkHttpClient.Builder();
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            newBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
            newBuilder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            logger.error("Fail create noSSL socket", e);
        }
        newBuilder.retryOnConnectionFailure(false);

        Dispatcher dispatcher = new Dispatcher();


        // Setting connection pool
        dispatcher.setMaxRequests(5);
        dispatcher.setMaxRequestsPerHost(1);
        newBuilder.dispatcher(dispatcher);
        newBuilder.connectionPool(new ConnectionPool(4, 30, TimeUnit.SECONDS));
        return newBuilder.build();
    }
}
