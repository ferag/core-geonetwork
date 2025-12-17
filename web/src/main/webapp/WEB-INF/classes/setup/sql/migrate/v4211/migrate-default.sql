UPDATE Settings SET value='4.2.11' WHERE name='system/platform/version';
UPDATE Settings SET value='0' WHERE name='system/platform/subVersion';

INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('system/translation/provider', '', 0, 7301, 'n');
INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('system/translation/serviceUrl', '', 0, 7302, 'n');
INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('system/translation/apiKey', '', 0, 7303, 'y');
INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('system/userSelfRegistration/domainsAllowed', '', 0, 1911, 'y');

INSERT INTO Settings (name, value, datatype, position, internal) SELECT distinct 'system/feedback/languages', '', 0, 646, 'n' from settings WHERE NOT EXISTS (SELECT name FROM Settings WHERE name = 'system/feedback/languages');
INSERT INTO Settings (name, value, datatype, position, internal) SELECT distinct 'system/feedback/translationFollowsText', '', 0, 647, 'n' from settings WHERE NOT EXISTS (SELECT name FROM Settings WHERE name = 'system/feedback/translationFollowsText');
INSERT INTO Settings (name, value, datatype, position, internal) SELECT distinct 'system/publication/handle/handleenabled', 'false', 2, 100198, 'n' from settings WHERE NOT EXISTS (SELECT name FROM Settings WHERE name = 'system/publication/handle/handleenabled');
INSERT INTO Settings (name, value, datatype, position, internal) SELECT distinct 'system/publication/handle/url', '', 0, 100199, 'n' from settings WHERE NOT EXISTS (SELECT name FROM Settings WHERE name = 'system/publication/handle/url');
INSERT INTO Settings (name, value, datatype, position, internal) SELECT distinct 'system/publication/handle/prefix', '', 0, 100200, 'n' from settings WHERE NOT EXISTS (SELECT name FROM Settings WHERE name = 'system/publication/handle/prefix');
INSERT INTO Settings (name, value, datatype, position, internal) SELECT distinct 'system/publication/handle/username', '', 0, 100201, 'n' from settings WHERE NOT EXISTS (SELECT name FROM Settings WHERE name = 'system/publication/handle/username');
INSERT INTO Settings (name, value, datatype, position, internal, encrypted) SELECT distinct 'system/publication/handle/password', '', 0, 100202, 'y', 'y' from settings WHERE NOT EXISTS (SELECT name FROM Settings WHERE name = 'system/publication/handle/password');
INSERT INTO Settings (name, value, datatype, position, internal) SELECT distinct 'system/publication/handle/adminPermissions', '', 0, 100203, 'n' from settings WHERE NOT EXISTS (SELECT name FROM Settings WHERE name = 'system/publication/handle/adminPermissions');

UPDATE Settings SET editable='y' WHERE name LIKE 'system/publication/handle/%' AND (editable IS NULL OR editable='');
