package org.fao.geonet.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of supported persistent identifier server types.
 */
public enum DoiServerType {
    DOI,
    HANDLE;

    @JsonCreator
    public static DoiServerType fromValue(String value) {
        if (value == null) {
            return DOI;
        }
        return DoiServerType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
