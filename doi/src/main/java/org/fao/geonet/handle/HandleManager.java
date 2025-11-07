package org.fao.geonet.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import jeeves.server.context.ServiceContext;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPut;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.api.exception.ResourceAlreadyExistException;
import org.fao.geonet.api.exception.ResourceNotFoundException;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.domain.DoiServer;
import org.fao.geonet.domain.DoiServerType;
import org.fao.geonet.kernel.AccessManager;
import org.fao.geonet.kernel.datamanager.base.BaseMetadataManager;
import org.fao.geonet.kernel.datamanager.base.BaseMetadataSchemaUtils;
import org.fao.geonet.kernel.datamanager.base.BaseMetadataUtils;
import org.fao.geonet.kernel.search.IndexingMode;
import org.fao.geonet.utils.GeonetHttpRequestFactory;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager responsible for registering Handle persistent identifiers.
 */
public class HandleManager {
    private static final String HANDLE_ADD_XSL_PROCESS = "process/handle-add.xsl";
    private static final int HANDLE_URL_INDEX = 1;
    private static final int HANDLE_ADMIN_INDEX = 100;
    private static final int HANDLE_ADMIN_VALUE_INDEX = 200;
    private static final String HANDLE_DEFAULT_PERMISSIONS = "011111110011";
    private static final String HANDLE_DEFAULT_PROTOCOL = "HANDLE";

    private final BaseMetadataSchemaUtils schemaUtils;
    private final BaseMetadataManager metadataManager;
    private final BaseMetadataUtils metadataUtils;
    private final org.fao.geonet.doi.client.DoiBuilder identifierBuilder;
    private final GeonetHttpRequestFactory requestFactory;
    private final AccessManager accessManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HandleManager(BaseMetadataSchemaUtils schemaUtils,
                         BaseMetadataManager metadataManager,
                         BaseMetadataUtils metadataUtils,
                         org.fao.geonet.doi.client.DoiBuilder identifierBuilder,
                         GeonetHttpRequestFactory requestFactory,
                         AccessManager accessManager) {
        this.schemaUtils = schemaUtils;
        this.metadataManager = metadataManager;
        this.metadataUtils = metadataUtils;
        this.identifierBuilder = identifierBuilder;
        this.requestFactory = requestFactory;
        this.accessManager = accessManager;
    }

    public Map<String, Boolean> check(ServiceContext context, DoiServer handleServer, AbstractMetadata metadata)
        throws Exception {
        checkServerType(handleServer);
        checkInitialised(handleServer);
        checkCanHandleMetadata(handleServer, metadata);
        checkRecordVisibility(metadata);
        ensureNoExistingHandle(handleServer, metadata);

        Map<String, Boolean> status = new HashMap<>();
        status.put("HANDLE_READY", true);
        return status;
    }

    public Map<String, String> register(ServiceContext context, DoiServer handleServer, AbstractMetadata metadata)
        throws Exception {
        Map<String, String> info = new HashMap<>();
        check(context, handleServer, metadata);

        String handle = identifierBuilder.create(handleServer.getPattern(), handleServer.getPrefix(), metadata);
        String handleUrl = buildHandleUrl(handleServer, handle);
        String landingPage = buildLandingPage(handleServer, metadata.getUuid());

        sendHandleRequest(handleServer, handle, handleUrl, landingPage);

        Element recordWithHandle = setHandleValue(handleServer, handleUrl, metadata.getDataInfo().getSchemaId(),
            metadata.getXmlData(false));

        metadataManager.updateMetadata(context, metadata.getId() + "", recordWithHandle, false, true,
            context.getLanguage(), new org.fao.geonet.domain.ISODate().toString(), true, IndexingMode.full);

        info.put("handle", handle);
        info.put("handleUrl", handleUrl);
        info.put("landingPage", landingPage);
        return info;
    }

    private void sendHandleRequest(DoiServer server, String handle, String handleUrl, String landingPage)
        throws HandleClientException {
        String apiUrl = server.getUrl();
        if (!apiUrl.endsWith("/")) {
            apiUrl += "/";
        }
        String requestUrl = apiUrl + server.getPrefix() + "/" + handle.substring(server.getPrefix().length() + 1);

        HttpPut put = new HttpPut(requestUrl);
        put.addHeader("Content-Type", "application/json");

        Map<String, Object> payload = buildHandlePayload(server, handleUrl, landingPage);
        try {
            String body = objectMapper.writeValueAsString(payload);
            put.setEntity(new org.apache.http.entity.StringEntity(body, StandardCharsets.UTF_8));

            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(server.getUsername(),
                server.getPassword());
            ClientHttpResponse response = null;
            try {
                response = requestFactory.execute(put, credentials, AuthScope.ANY);
                int status = response.getRawStatusCode();
                if (status != HttpStatus.CREATED.value() && status != HttpStatus.OK.value()) {
                    String error = response.getBody() != null
                        ? new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)
                        : "";
                    throw new HandleClientException(String.format(
                        "Error registering handle '%s': %s", handleUrl, StringUtils.defaultIfEmpty(error,
                            response.getStatusText())));
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } catch (IOException e) {
            Log.error(Geonet.DATA_MANAGER, "Error registering handle", e);
            throw new HandleClientException(String.format("Error registering handle '%s': %s", handleUrl, e.getMessage()), e);
        }
    }

    private Map<String, Object> buildHandlePayload(DoiServer server, String handleUrl, String landingPage) {
        Map<String, Object> urlData = new HashMap<>();
        urlData.put("format", "string");
        urlData.put("value", landingPage);

        Map<String, Object> urlEntry = new HashMap<>();
        urlEntry.put("index", HANDLE_URL_INDEX);
        urlEntry.put("type", "URL");
        urlEntry.put("data", urlData);

        String adminHandle = decodeAdminHandle(server.getUsername());
        Map<String, Object> adminValue = new HashMap<>();
        adminValue.put("handle", adminHandle);
        adminValue.put("index", HANDLE_ADMIN_VALUE_INDEX);
        adminValue.put("permissions", HANDLE_DEFAULT_PERMISSIONS);

        Map<String, Object> adminData = new HashMap<>();
        adminData.put("format", "admin");
        adminData.put("value", adminValue);

        Map<String, Object> adminEntry = new HashMap<>();
        adminEntry.put("index", HANDLE_ADMIN_INDEX);
        adminEntry.put("type", "HS_ADMIN");
        adminEntry.put("data", adminData);

        Map<String, Object> payload = new HashMap<>();
        payload.put("values", List.of(urlEntry, adminEntry));
        return payload;
    }

    private String decodeAdminHandle(String username) {
        if (StringUtils.isEmpty(username)) {
            return "";
        }
        String decoded = URLDecoder.decode(username, StandardCharsets.UTF_8);
        int colonIndex = decoded.indexOf(':');
        if (colonIndex >= 0 && colonIndex < decoded.length() - 1) {
            return decoded.substring(colonIndex + 1);
        }
        return decoded;
    }

    private void ensureNoExistingHandle(DoiServer server, AbstractMetadata metadata)
        throws ResourceAlreadyExistException, HandleClientException, IOException, JDOMException, ResourceNotFoundException {
        String currentHandle = metadataUtils.getHandle(metadata.getUuid());
        if (StringUtils.isNotEmpty(currentHandle)) {
            String expectedHandle = buildHandleUrl(server,
                identifierBuilder.create(server.getPattern(), server.getPrefix(), metadata));
            if (!currentHandle.equals(expectedHandle)) {
                throw new HandleClientException(String.format(
                    "Record '%s' already contains a different handle '%s'.", metadata.getUuid(), currentHandle));
            }
            throw new ResourceAlreadyExistException(String.format(
                "Record '%s' already contains a handle '%s'.", metadata.getUuid(), currentHandle));
        }
    }

    private void checkInitialised(DoiServer server) throws HandleClientException {
        boolean emptyUrl = StringUtils.isEmpty(server.getUrl());
        boolean emptyUsername = StringUtils.isEmpty(server.getUsername());
        boolean emptyPassword = StringUtils.isEmpty(server.getPassword());
        boolean emptyPrefix = StringUtils.isEmpty(server.getPrefix());
        boolean emptyPattern = StringUtils.isEmpty(server.getPattern());
        boolean emptyLandingPage = StringUtils.isEmpty(server.getLandingPageTemplate());
        boolean emptyPublicUrl = StringUtils.isEmpty(server.getPublicUrl());

        if (emptyUrl || emptyUsername || emptyPassword || emptyPrefix || emptyPattern || emptyLandingPage || emptyPublicUrl) {
            throw new HandleClientException("Handle server configuration is not complete. Check the handle server configuration.");
        }
    }

    private void checkRecordVisibility(AbstractMetadata metadata) throws HandleClientException {
        try {
            boolean visibleToAll = accessManager.isVisibleToAll(metadata.getId() + "");
            if (!visibleToAll) {
                throw new HandleClientException(String.format(
                    "Record '%s' is not public and a handle cannot be created.", metadata.getUuid()));
            }
        } catch (HandleClientException e) {
            throw e;
        } catch (Exception e) {
            throw new HandleClientException(String.format(
                "Failed to check if record '%s' is visible to all: %s", metadata.getUuid(), e.getMessage()), e);
        }
    }

    private void checkCanHandleMetadata(DoiServer server, AbstractMetadata metadata) throws HandleClientException {
        if (!server.getPublicationGroups().isEmpty()) {
            Integer groupOwner = metadata.getSourceInfo().getGroupOwner();
            if (server.getPublicationGroups().stream().noneMatch(g -> g.getId() == groupOwner)) {
                throw new HandleClientException(String.format(
                    "Handle server '%s' cannot handle metadata with UUID '%s'.", server.getName(), metadata.getUuid()));
            }
        }
    }

    private void checkServerType(DoiServer server) throws HandleClientException {
        if (server.getType() != DoiServerType.HANDLE) {
            throw new HandleClientException(String.format("Server '%s' is not configured as a handle server.", server.getName()));
        }
    }

    private String buildHandleUrl(DoiServer server, String handle) {
        String publicUrl = server.getPublicUrl();
        if (!publicUrl.endsWith("/")) {
            publicUrl += "/";
        }
        return publicUrl + handle.substring(server.getPrefix().length() + 1);
    }

    private String buildLandingPage(DoiServer server, String uuid) {
        String template = server.getLandingPageTemplate();
        if (StringUtils.isEmpty(template)) {
            return ApplicationContextHolder.get().getBean(org.fao.geonet.kernel.setting.SettingManager.class)
                .getNodeURL() + "api/records/" + uuid;
        }
        return template.replace(org.fao.geonet.doi.client.DoiManager.DOI_DEFAULT_PATTERN, uuid);
    }

    private Element setHandleValue(DoiServer server, String handleUrl, String schema, Element md) throws Exception {
        Path styleSheet = schemaUtils.getSchemaDir(schema).resolve(HANDLE_ADD_XSL_PROCESS);
        if (!Files.exists(styleSheet)) {
            throw new HandleClientException(String.format(
                "Handle insertion stylesheet '%s' not found for schema '%s'.", HANDLE_ADD_XSL_PROCESS, schema));
        }
        Map<String, Object> params = new HashMap<>();
        params.put("handle", handleUrl);
        params.put("handleProxy", "");
        params.put("handleProtocol", HANDLE_DEFAULT_PROTOCOL);
        params.put("handleName", server.getName());
        return Xml.transform(md, styleSheet, params);
    }
}
