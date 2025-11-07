package org.fao.geonet.api.records;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jeeves.server.context.ServiceContext;
import jeeves.services.ReadWriteController;
import org.fao.geonet.api.ApiParams;
import org.fao.geonet.api.ApiUtils;
import org.fao.geonet.api.exception.ResourceNotFoundException;
import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.domain.DoiServer;
import org.fao.geonet.domain.DoiServerType;
import org.fao.geonet.handle.HandleManager;
import org.fao.geonet.repository.DoiServerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Optional;

import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_OPS;
import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_TAG;
import static org.fao.geonet.api.ApiParams.API_PARAM_RECORD_UUID;

/**
 * API endpoints for Handle PID registration.
 */
@RequestMapping(value = {
    "/{portal}/api/records"
})
@Tag(name = API_CLASS_RECORD_TAG,
    description = API_CLASS_RECORD_OPS)
@RestController("handle")
@PreAuthorize("hasAuthority('Editor')")
@ReadWriteController
public class HandleApi {

    private final HandleManager handleManager;
    private final DoiServerRepository doiServerRepository;

    public HandleApi(HandleManager handleManager, DoiServerRepository doiServerRepository) {
        this.handleManager = handleManager;
        this.doiServerRepository = doiServerRepository;
    }

    @io.swagger.v3.oas.annotations.Operation(
        summary = "Check that a record can be submitted to a Handle server." )
    @GetMapping(value = "/{metadataUuid}/handle/{handleServerId}/checkPreConditions",
        produces = {
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Record can be submitted to the Handle server."),
        @ApiResponse(responseCode = "404", description = "Metadata not found."),
        @ApiResponse(responseCode = "400", description = "Record does not meet preconditions. Check error message."),
        @ApiResponse(responseCode = "500", description = "Service unavailable."),
        @ApiResponse(responseCode = "403", description = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT)
    })
    public ResponseEntity<Map<String, Boolean>> checkHandleStatus(
        @Parameter(description = API_PARAM_RECORD_UUID,
            required = true)
        @PathVariable
            String metadataUuid,
        @Parameter(description = "Handle server identifier",
            required = true)
        @PathVariable
            Integer handleServerId,
        @Parameter(hidden = true)
            HttpServletRequest request
    ) throws Exception {
        AbstractMetadata metadata = ApiUtils.canEditRecord(metadataUuid, request);
        ServiceContext serviceContext = ApiUtils.createServiceContext(request);

        DoiServer handleServer = retrieveHandleServer(handleServerId);
        Map<String, Boolean> reportStatus = handleManager.check(serviceContext, handleServer, metadata);
        return new ResponseEntity<>(reportStatus, HttpStatus.OK);
    }

    @io.swagger.v3.oas.annotations.Operation(
        summary = "Register a Handle PID for the record.")
    @PutMapping(value = "/{metadataUuid}/handle/{handleServerId}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Handle created."),
        @ApiResponse(responseCode = "404", description = "Metadata not found."),
        @ApiResponse(responseCode = "500", description = "Service unavailable."),
        @ApiResponse(responseCode = "403", description = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT)
    })
    public ResponseEntity<Map<String, String>> createHandle(
        @Parameter(description = API_PARAM_RECORD_UUID,
            required = true)
        @PathVariable
            String metadataUuid,
        @Parameter(description = "Handle server identifier",
            required = true)
        @PathVariable
            Integer handleServerId,
        @Parameter(hidden = true)
            HttpServletRequest request,
        @Parameter(hidden = true)
            HttpSession session
    ) throws Exception {
        AbstractMetadata metadata = ApiUtils.canEditRecord(metadataUuid, request);
        ServiceContext serviceContext = ApiUtils.createServiceContext(request);

        DoiServer handleServer = retrieveHandleServer(handleServerId);
        Map<String, String> handleInfo = handleManager.register(serviceContext, handleServer, metadata);
        return new ResponseEntity<>(handleInfo, HttpStatus.CREATED);
    }

    private DoiServer retrieveHandleServer(Integer serverId) throws ResourceNotFoundException {
        Optional<DoiServer> serverOpt = doiServerRepository.findOneById(serverId);
        if (serverOpt.isEmpty() || serverOpt.get().getType() != DoiServerType.HANDLE) {
            throw new ResourceNotFoundException(String.format(
                "Handle server with id '%s' not found.",
                serverId
            ));
        }
        return serverOpt.get();
    }
}
