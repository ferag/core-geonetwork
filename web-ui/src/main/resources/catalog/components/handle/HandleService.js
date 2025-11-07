/*
 * Copyright (C) 2001-2024 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

(function () {
  goog.provide("gn_handle_service");

  var module = angular.module("gn_handle_service", []);

  module.service("gnHandleService", [
    "$http",
    "gnConfig",
    function ($http, gnConfig) {
      function check(id, serverId) {
        return $http.get(
          "../api/records/" + id + "/handle/" + serverId + "/checkPreConditions"
        );
      }

      function create(id, serverId) {
        return $http.put("../api/records/" + id + "/handle/" + serverId);
      }

      function getHandleServersForMetadata(metadataId) {
        return $http.get(
          "../api/doiservers/metadata/" + metadataId + "?type=HANDLE"
        );
      }

      function isHandleApplicableForMetadata(md) {
        return (
          gnConfig["system.publication.handle.handleenabled"] &&
          md.isTemplate === "n" &&
          md.isPublished() &&
          JSON.parse(md.isHarvested) === false
        );
      }

      function canPublishHandleForResource(md) {
        var isMdWorkflowEnableForMetadata =
          gnConfig["metadata.workflow.enable"] && md.draft === "y";
        return isHandleApplicableForMetadata(md) && !isMdWorkflowEnableForMetadata;
      }

      return {
        check: check,
        create: create,
        getHandleServersForMetadata: getHandleServersForMetadata,
        isHandleApplicableForMetadata: isHandleApplicableForMetadata,
        canPublishHandleForResource: canPublishHandleForResource
      };
    }
  ]);
})();
