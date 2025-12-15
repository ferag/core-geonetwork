package org.fao.geonet.handle.client;

import org.apache.commons.lang.StringUtils;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.utils.Log;

import static org.fao.geonet.handle.client.HandleSettings.LOGGER_NAME;

public class HandleManager {
    private final SettingManager settingManager;
    private IHandleClient client;
    private String prefix;
    private boolean initialised;

    public HandleManager() {
        this.settingManager = ApplicationContextHolder.get().getBean(SettingManager.class);
        loadConfig();
    }

    HandleManager(SettingManager settingManager, IHandleClient client) {
        this.settingManager = settingManager;
        this.client = client;
        loadConfig();
    }

    public boolean isInitialised() {
        return initialised;
    }

    public void loadConfig() {
        initialised = false;
        if (settingManager == null) {
            if (client != null) {
                initialised = true;
            }
            return;
        }

        boolean handleEnabled = Boolean.parseBoolean(
            settingManager.getValue(HandleSettings.SETTING_PUBLICATION_HANDLE_ENABLED)
        );
        if (!handleEnabled) {
            Log.warning(LOGGER_NAME, "Handle configuration is disabled. Enable Handle PIDs in System Configuration.");
            return;
        }

        String apiUrl = settingManager.getValue(HandleSettings.SETTING_PUBLICATION_HANDLE_URL);
        prefix = settingManager.getValue(HandleSettings.SETTING_PUBLICATION_HANDLE_PREFIX);
        String username = settingManager.getValue(HandleSettings.SETTING_PUBLICATION_HANDLE_USERNAME);
        String password = settingManager.getValue(HandleSettings.SETTING_PUBLICATION_HANDLE_PASSWORD);
        String adminPermissions = settingManager.getValue(HandleSettings.SETTING_PUBLICATION_HANDLE_ADMIN_PERMISSIONS);

        if (StringUtils.isBlank(apiUrl) || StringUtils.isBlank(prefix) || StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            Log.warning(LOGGER_NAME, "Handle configuration is not complete. Check System Configuration.");
            return;
        }
        this.client = new HandleRestClient(apiUrl, username, password, adminPermissions);
        initialised = true;
    }

    public void createOrUpdate(String identifier, String url, boolean includeAdmin) throws HandleClientException {
        if (!initialised) {
            throw new HandleClientException("Handle manager is not configured");
        }
        String targetHandle = prefix.endsWith("/") ? prefix + identifier : prefix + "/" + identifier;
        client.createOrUpdate(targetHandle, url, includeAdmin);
    }
}
