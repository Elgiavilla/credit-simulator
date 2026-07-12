package com.elgi.creditsimulator.remote;

import com.elgi.creditsimulator.exception.RemoteServiceException;

import java.net.URI;

public interface  HttpFetcher {

    String get(URI uri);

}
