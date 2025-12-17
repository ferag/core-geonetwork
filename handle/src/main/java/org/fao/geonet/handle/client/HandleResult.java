package org.fao.geonet.handle.client;

import java.util.HashMap;
import java.util.Map;

public class HandleResult {
    private final String handle;
    private final String targetUrl;
    private final boolean adminIncluded;

    public HandleResult(String handle, String targetUrl, boolean adminIncluded) {
        this.handle = handle;
        this.targetUrl = targetUrl;
        this.adminIncluded = adminIncluded;
    }

    public String getHandle() {
        return handle;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public boolean isAdminIncluded() {
        return adminIncluded;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>(3);
        map.put("handle", handle);
        map.put("targetUrl", targetUrl);
        map.put("adminIncluded", adminIncluded);
        return map;
    }
}
