package com.elgi.creditsimulator.remote;

import com.elgi.creditsimulator.exception.RemoteServiceException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class JdkHttpFetcher implements HttpFetcher {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient client;

    public JdkHttpFetcher() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String get(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException cause) {
            throw new RemoteServiceException(
                    "Could not reach the calculation service at " + uri + ". "
                            + "Check your connection and try again.", cause);
        } catch (InterruptedException cause) {
            // Restore the flag we just swallowed. Code that catches InterruptedException and does
            // not re-interrupt breaks every cancellation mechanism above it.
            Thread.currentThread().interrupt();
            throw new RemoteServiceException("The request to " + uri + " was interrupted.", cause);
        }

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new RemoteServiceException(
                    "The calculation service at " + uri + " answered with HTTP " + status + ".");
        }
        return response.body();
    }



}
