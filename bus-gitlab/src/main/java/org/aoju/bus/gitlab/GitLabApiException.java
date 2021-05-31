/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2021 aoju.org Greg Messner and other contributors.         *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 *                                                                               *
 ********************************************************************************/
package org.aoju.bus.gitlab;

import com.fasterxml.jackson.databind.JsonNode;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.Symbol;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import java.util.*;
import java.util.Map.Entry;

/**
 * This is the exception that will be thrown if any exception occurs while communicating
 * with a GitLab API endpoint.
 *
 * @author Kimi Liu
 * @version 6.2.3
 * @since JDK 1.8+
 */
public class GitLabApiException extends Exception {

    private static final long serialVersionUID = 1L;
    private StatusType statusInfo;
    private int httpStatus;
    private String message;
    private Map<String, List<String>> validationErrors;

    /**
     * Create a GitLabApiException instance with the specified message.
     *
     * @param message the message for the exception
     */
    public GitLabApiException(String message) {
        super(message);
        this.message = message;
    }

    /**
     * Create a GitLabApiException instance with the specified message and HTTP status code.
     *
     * @param message    the message for the exception
     * @param httpStatus the HTTP status code for the exception
     */
    public GitLabApiException(String message, int httpStatus) {
        super(message);
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * Create a GitLabApiException instance based on the ClientResponse.
     *
     * @param response the JAX-RS response that caused the exception
     */
    public GitLabApiException(Response response) {
        super();
        statusInfo = response.getStatusInfo();
        httpStatus = response.getStatus();
        if (response.hasEntity()) {
            try {
                String message = response.readEntity(String.class);
                this.message = message;
                // Determine what is in the content of the response and process it accordingly
                MediaType mediaType = response.getMediaType();
                if (null != mediaType && "json".equals(mediaType.getSubtype())) {
                    JsonNode json = JacksonJson.toJsonNode(message);
                    // First see if it is a "message", if so it is either a simple message,
                    // or a Map<String, List<String>> of validation errors
                    JsonNode jsonMessage = json.get("message");
                    if (null != jsonMessage) {
                        // If the node is an object, then it is validation errors
                        if (jsonMessage.isObject()) {

                            StringBuilder buf = new StringBuilder();
                            validationErrors = new HashMap<>();
                            Iterator<Entry<String, JsonNode>> fields = jsonMessage.fields();
                            while (fields.hasNext()) {
                                Entry<String, JsonNode> field = fields.next();
                                String fieldName = field.getKey();
                                List<String> values = new ArrayList<>();
                                validationErrors.put(fieldName, values);
                                for (JsonNode value : field.getValue()) {
                                    values.add(value.asText());
                                }

                                if (values.size() > 0) {
                                    buf.append((buf.length() > 0 ? ", " : Normal.EMPTY)).append(fieldName);
                                }
                            }

                            if (buf.length() > 0) {
                                this.message = "The following fields have validation errors: " + buf.toString();
                            }

                        } else if (jsonMessage.isArray()) {
                            List<String> values = new ArrayList<>();
                            for (JsonNode value : jsonMessage) {
                                values.add(value.asText());
                            }

                            if (values.size() > 0) {
                                this.message = String.join(Symbol.LF, values);
                            }

                        } else if (jsonMessage.isTextual()) {
                            this.message = jsonMessage.asText();
                        } else {
                            this.message = jsonMessage.toString();
                        }

                    } else {
                        JsonNode jsonError = json.get("error");
                        if (null != jsonError) {
                            this.message = jsonError.asText();
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Create a GitLabApiException instance based on the exception.
     *
     * @param e the Exception to wrap
     */
    public GitLabApiException(Exception e) {
        super(e);
        message = e.getMessage();
    }

    /**
     * Get the message associated with the exception.
     *
     * @return the message associated with the exception
     */
    @Override
    public final String getMessage() {
        return null != message ? message : getReason();
    }

    /**
     * Returns the HTTP status reason message, returns null if the
     * causing error was not an HTTP related exception.
     *
     * @return the HTTP status reason message
     */
    public final String getReason() {
        return null != statusInfo ? statusInfo.getReasonPhrase() : null;
    }

    /**
     * Returns the HTTP status code that was the cause of the exception. returns 0 if the
     * causing error was not an HTTP related exception
     *
     * @return the HTTP status code, returns 0 if the causing error was not an HTTP related exception
     */
    public final int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Returns true if this GitLabApiException was caused by validation errors on the GitLab server,
     * otherwise returns false.
     *
     * @return true if this GitLabApiException was caused by validation errors on the GitLab server,
     * otherwise returns false
     */
    public boolean hasValidationErrors() {
        return null != validationErrors;
    }

    /**
     * Returns a Map&lt;String, List&lt;String&gt;&gt; instance containing validation errors if this GitLabApiException
     * was caused by validation errors on the GitLab server, otherwise returns null.
     *
     * @return a Map&lt;String, List&lt;String&gt;&gt; instance containing validation errors if this GitLabApiException
     * was caused by validation errors on the GitLab server, otherwise returns null
     */
    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + httpStatus;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((statusInfo == null) ? 0 : statusInfo.hashCode());
        result = prime * result + ((validationErrors == null) ? 0 : validationErrors.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        GitLabApiException other = (GitLabApiException) obj;
        if (httpStatus != other.httpStatus) {
            return false;
        }

        if (message == null) {
            if (null != other.message)
                return false;
        } else if (!message.equals(other.message)) {
            return false;
        }

        if (statusInfo == null) {
            if (null != other.statusInfo)
                return false;
        } else if (!statusInfo.equals(other.statusInfo)) {
            return false;
        }

        if (validationErrors == null) {
            if (null != other.validationErrors)
                return false;
        } else if (!validationErrors.equals(other.validationErrors)) {
            return false;
        }

        return true;
    }

}
