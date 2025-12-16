package org.fao.geonet.api.records;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jeeves.services.ReadWriteController;
import jeeves.server.context.ServiceContext;
import org.fao.geonet.api.ApiUtils;
import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.handle.client.HandleClientException;
import org.fao.geonet.handle.client.HandleManager;
import org.fao.geonet.handle.client.HandleResult;
import org.fao.geonet.handle.client.HandleSettings;
import org.fao.geonet.utils.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_OPS;
import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_TAG;

/**
 * REST endpoint for creating/updating Handle identifiers.
 */
@RequestMapping(value = {
    "/{portal}/api/records"
})
@Tag(name = API_CLASS_RECORD_TAG,
    description = API_CLASS_RECORD_OPS)
@Controller("handleApi")
@PreAuthorize("hasAuthority('Editor')")
@ReadWriteController
public class HandleApi {

    @Autowired
    private HandleManager handleManager;

    @io.swagger.v3.oas.annotations.Operation(
        summary = "Create or update a Handle PID for a record.")
    @RequestMapping(value = "/{metadataUuid}/handle",
        method = {RequestMethod.POST, RequestMethod.PUT},
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Handle created/updated")
    })
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignHandle(
        @PathVariable(value = "metadataUuid") String uuid,
        @RequestBody Map<String, Object> body,
        javax.servlet.http.HttpServletRequest request) throws Exception {

        AbstractMetadata metadata = ApiUtils.canEditRecord(uuid, request);
        ServiceContext context = ApiUtils.createServiceContext(request);
        String targetUrl = body.getOrDefault("url", "").toString();
        boolean includeAdmin = Boolean.parseBoolean(body.getOrDefault("includeAdmin", Boolean.FALSE).toString());
        Log.info(HandleSettings.LOGGER_NAME,
            "Handle PID request received for record " + uuid + " (includeAdmin=" + includeAdmin + ")");
        HandleResult result = handleManager.createOrUpdate(context, metadata, targetUrl, includeAdmin);
        Log.info(HandleSettings.LOGGER_NAME,
            "Handle PID " + result.getHandle() + " stored for record " + uuid + " targeting " + result.getTargetUrl());
        return new ResponseEntity<>(result.asMap(), HttpStatus.CREATED);
    }
}
