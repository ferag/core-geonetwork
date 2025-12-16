package org.fao.geonet.handle.client;

import jeeves.server.context.ServiceContext;
import org.apache.commons.lang.StringUtils;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.kernel.datamanager.base.BaseMetadataUtils;
import org.fao.geonet.kernel.search.IndexingMode;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.fao.geonet.handle.client.HandleSettings.LOGGER_NAME;

public class HandleManager {
    private static final String HANDLE_ADD_XSL_PROCESS = "process/handle-add.xsl";
    private static final String HANDLE_PROXY_DEFAULT = "https://hdl.handle.net/";

    private final SettingManager settingManager;
    private final DataManager dataManager;
    private final BaseMetadataUtils metadataUtils;
    private IHandleClient client;
    private String prefix;
    private boolean initialised;

    public HandleManager() {
        this(ApplicationContextHolder.get().getBean(SettingManager.class), null,
            ApplicationContextHolder.get().getBean(DataManager.class),
            ApplicationContextHolder.get().getBean(BaseMetadataUtils.class));
    }

    HandleManager(SettingManager settingManager, IHandleClient client) {
        this(settingManager, client,
            ApplicationContextHolder.get().getBean(DataManager.class),
            ApplicationContextHolder.get().getBean(BaseMetadataUtils.class));
    }

    HandleManager(SettingManager settingManager, IHandleClient client, DataManager dataManager, BaseMetadataUtils metadataUtils) {
        this.settingManager = settingManager;
        this.client = client;
        this.dataManager = dataManager;
        this.metadataUtils = metadataUtils;
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

    public HandleResult createOrUpdate(ServiceContext context, AbstractMetadata metadata, String url, boolean includeAdmin)
        throws HandleClientException {
        if (!initialised) {
            throw new HandleClientException("Handle manager is not configured");
        }
        String targetHandle = buildHandleIdentifier(metadata.getUuid());
        String targetUrl = resolveTargetUrl(context, metadata, url);
        Log.info(LOGGER_NAME, "Requesting Handle creation for record " + metadata.getUuid() + " -> " + targetUrl);
        client.createOrUpdate(targetHandle, targetUrl, includeAdmin);
        Log.info(LOGGER_NAME, "Handle service accepted identifier " + targetHandle + ", updating metadata record.");
        try {
            Element recordWithHandle = setHandleValue(targetHandle, metadata.getDataInfo().getSchemaId(), metadata.getXmlData(false));
            dataManager.updateMetadata(context, metadata.getId() + "", recordWithHandle, false, true,
                context.getLanguage(), new ISODate().toString(), true, IndexingMode.full);
        } catch (Exception e) {
            throw new HandleClientException("Handle created but failed to update metadata", e);
        }
        return new HandleResult(targetHandle, targetUrl, includeAdmin);
    }

    private String buildHandleIdentifier(String identifier) {
        return prefix.endsWith("/") ? prefix + identifier : prefix + "/" + identifier;
    }

    private String resolveTargetUrl(ServiceContext context, AbstractMetadata metadata, String url) {
        if (StringUtils.isNotBlank(url)) {
            return url;
        }
        String language = context != null ? context.getLanguage() : null;
        if (StringUtils.isBlank(language)) {
            language = "all";
        }
        try {
            return metadataUtils.getDefaultUrl(metadata.getUuid(), language);
        } catch (Exception e) {
            Log.warning(LOGGER_NAME, "Unable to resolve default landing page for " + metadata.getUuid() + ", using API URL.", e);
            return settingManager.getNodeURL() + "api/records/" + metadata.getUuid();
        }
    }

    private Element setHandleValue(String handle, String schema, Element md) throws Exception {
        Path styleSheet = dataManager.getSchemaDir(schema).resolve(HANDLE_ADD_XSL_PROCESS);
        boolean exists = Files.exists(styleSheet);
        if (!exists) {
            String message = String.format("To create a Handle, the schema must define how to insert it. Missing %s in schema %s.",
                HANDLE_ADD_XSL_PROCESS, schema);
            Log.error(LOGGER_NAME, message);
            throw new HandleClientException(message);
        }

        Map<String, Object> params = new HashMap<>(2);
        params.put("handle", handle);
        params.put("handleProxy", HANDLE_PROXY_DEFAULT);
        return Xml.transform(md, styleSheet, params);
    }
}
