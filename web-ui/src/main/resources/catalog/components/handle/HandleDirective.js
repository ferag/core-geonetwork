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
  goog.provide("gn_handle_directive");
  goog.require("gn_handle_service");

  var module = angular.module("gn_handle_directive", ["gn_handle_service"]);

  module.directive("gnHandleWizard", [
    "gnHandleService",
    function (gnHandleService) {
      return {
        restrict: "A",
        replace: true,
        scope: {
          uuid: "=gnHandleWizard",
          handleUrl: "=?",
          xsMode: "@?"
        },
        templateUrl: "../../catalog/components/handle/partials/handlewidget.html",
        link: function (scope, element, attrs) {
          scope.gnHandleService = gnHandleService;
          scope.response = {};
          scope.isUpdate = angular.isDefined(scope.handleUrl);

          scope.handleServers = [];
          scope.selectedHandleServer = null;

          gnHandleService
            .getHandleServersForMetadata(scope.uuid)
            .then(function (response) {
              scope.handleServers = response.data;
              if (scope.handleServers.length > 0) {
                scope.selectedHandleServer = scope.handleServers[0].id;
              }
            });

          scope.updateHandleServer = function () {
            scope.response = {};
          };

          scope.check = function () {
            scope.response = {};
            scope.response["check"] = null;
            return gnHandleService.check(scope.uuid, scope.selectedHandleServer).then(
              function (r) {
                scope.response["check"] = r;
                scope.isUpdate = angular.isDefined(scope.handleUrl);
              },
              function (r) {
                scope.response["check"] = r;
                scope.isUpdate = r.data && r.data.code === "resource_already_exist";
              }
            );
          };

          scope.create = function () {
            return gnHandleService.create(scope.uuid, scope.selectedHandleServer).then(
              function (r) {
                scope.response["create"] = r;
                delete scope.response["check"];
                scope.handleUrl = r.data.handleUrl;
              },
              function (r) {
                scope.response["create"] = r;
              }
            );
          };
        }
      };
    }
  ]);
})();
