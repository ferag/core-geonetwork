/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
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
          includeAdmin: "=?",
          autoAssign: "=?",
          xsMode: "@?"
        },
        templateUrl: "../../catalog/components/handle/partials/handlewidget.html",
        link: function (scope) {
          scope.response = {};
          scope.includeAdmin = scope.includeAdmin || false;
          scope.autoAssign = scope.autoAssign !== false;
          scope.hasAttempted = false;

          var buildDefaultUrl = function () {
            if (!scope.uuid) {
              return scope.handleUrl;
            }

            var protocol =
              (window.location.protocol || "http:").replace(":", "") || "http";
            var host = window.location.hostname;
            var port = window.location.port;

            var portPart = "";
            if (
              port &&
              ((protocol === "http" && port !== "80") ||
                (protocol === "https" && port !== "443"))
            ) {
              portPart = ":" + port;
            }

            var baseUrl = (window.gnConfig && window.gnConfig.env.baseURL) || "";
            baseUrl = baseUrl.endsWith("/")
              ? baseUrl.substring(0, baseUrl.length - 1)
              : baseUrl;

            return (
              protocol +
              "://" +
              host +
              portPart +
              baseUrl +
              "/srv/api/records/" +
              scope.uuid
            );
          };

          scope.$watch(
            function () {
              return scope.uuid;
            },
            function () {
              if (!scope.handleUrl) {
                scope.handleUrl = buildDefaultUrl();
              }
            }
          );

          scope.assign = function (isAuto) {
            scope.response["assign"] = null;
            scope.hasAttempted = true;
            return gnHandleService
              .assign(scope.uuid, scope.handleUrl, scope.includeAdmin)
              .then(
                function (r) {
                  scope.response["assign"] = r;
                  if (r.data && r.data.handle) {
                    scope.handleUrl = r.data.targetUrl || scope.handleUrl;
                    scope.response.handle = r.data.handle;
                    scope.response.adminIncluded = r.data.adminIncluded;
                  }
                },
                function (r) {
                  scope.response["assign"] = r;
                }
              );
          };

          scope.$watch(
            "handleUrl",
            function (n) {
              if (scope.autoAssign && n && !scope.hasAttempted) {
                scope.assign(true);
              }
            }
          );
        }
      };
    }
  ]);
})();
