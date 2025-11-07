package org.fao.geonet.handle;

import org.fao.geonet.exceptions.LocalizedException;

import java.util.Locale;

/**
 * Exception raised when communicating with a Handle server.
 */
public class HandleClientException extends LocalizedException {
    public HandleClientException(String message) {
        super(message);
    }

    public HandleClientException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public HandleClientException(Throwable cause) {
        super(cause);
    }

    @Override
    protected String getResourceBundleBeanQualifier() {
        return "apiMessages";
    }

    @Override
    public HandleClientException withMessageKey(String messageKey) {
        super.withMessageKey(messageKey);
        return this;
    }

    @Override
    public HandleClientException withMessageKey(String messageKey, Object[] messageKeyArgs) {
        super.withMessageKey(messageKey, messageKeyArgs);
        return this;
    }

    @Override
    public HandleClientException withDescriptionKey(String descriptionKey) {
        super.withDescriptionKey(descriptionKey);
        return this;
    }

    @Override
    public HandleClientException withDescriptionKey(String descriptionKey, Object[] descriptionKeyArgs) {
        super.withDescriptionKey(descriptionKey, descriptionKeyArgs);
        return this;
    }

    @Override
    public HandleClientException withLocale(Locale locale) {
        super.withLocale(locale);
        return this;
    }
}
