package org.fao.geonet.handle.client;

public interface IHandleClient {
    void createOrUpdate(String handleIdentifier, String targetUrl, boolean includeAdmin) throws HandleClientException;
}
