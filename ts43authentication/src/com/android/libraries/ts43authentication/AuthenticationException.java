/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.libraries.ts43authentication;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.OutcomeReceiver;
import android.os.PersistableBundle;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.concurrent.Executor;

/**
 * Exception class holding the error codes and information for an authentication request to the
 * {@link Ts43AuthenticationLibrary}.
 */
public class AuthenticationException extends Exception {
    /**
     * Unspecified error preventing the authentication request.
     */
    public static final int ERROR_UNSPECIFIED = 0;

    /**
     * Authentication request failed because the {@code appName} does not correspond to the one
     * expected from the calling application.
     */
    public static final int ERROR_INVALID_APP_NAME = 1;

    /**
     * Authentication request failed using EAP-AKA because OIDC authentication must be used instead.
     * Clients should call {@link Ts43AuthenticationLibrary#requestOidcAuthenticationServer(
     * PersistableBundle, String, String, int, URL, String, String, Executor, OutcomeReceiver)}
     * and {@link Ts43AuthenticationLibrary#requestOidcAuthentication(
     * PersistableBundle, String, URL, Executor, OutcomeReceiver)} instead.
     */
    public static final int ERROR_MUST_USE_OIDC = 2;

    /**
     * Authentication request failed because one or more of the services required to complete the
     * request (e.g. Telephony, SIM, TS.43 entitlement server) are not currently available.
     */
    public static final int ERROR_SERVICE_UNAVAILABLE = 3;

    /**
     * Authentication request failed because the SIM is not returning a response to the EAP-AKA
     * challenge, e.g. when the challenge is invalid. This can happen only when an embedded EAP-AKA
     * challenge is conducted, as per GMSA spec TS.43 section 2.6.1.
     */
    public static final int ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE = 4;

    /**
     * Authentication request failed due to an EAP-AKA synchronization failure even after the
     * "Sequence number synchronization" procedure defined in RFC 4187 was completed.
     */
    public static final int ERROR_EAP_AKA_SYNCHRONIZATION_FAILURE = 5;

    /**
     * Authentication request failed because the maximum number of EAP-AKA attempts failed.
     */
    public static final int ERROR_MAXIMUM_EAP_AKA_ATTEMPTS = 6;

    /**
     * Authentication request failed because the HTTP response received contained a status code
     * indicating failure, e.g. 4xx and 5xx.
     */
    public static final int ERROR_HTTP_RESPONSE_FAILED = 7;

    /**
     * Authentication request failed because the HTTP response received was malformed,
     * e.g. the content-type is JSON, but it fails the JSON parser.
     */
    public static final int ERROR_INVALID_HTTP_RESPONSE = 8;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"ERROR_"}, value = {
            ERROR_UNSPECIFIED,
            ERROR_INVALID_APP_NAME,
            ERROR_MUST_USE_OIDC,
            ERROR_SERVICE_UNAVAILABLE,
            ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE,
            ERROR_EAP_AKA_SYNCHRONIZATION_FAILURE,
            ERROR_MAXIMUM_EAP_AKA_ATTEMPTS,
            ERROR_HTTP_RESPONSE_FAILED,
            ERROR_INVALID_HTTP_RESPONSE,
    })
    public @interface AuthenticationError {}

    /**
     * The HTTP status code has not been specified.
     */
    public static final int HTTP_STATUS_CODE_UNSPECIFIED = -1;

    /**
     * An empty string indicating that the {@code Retry-After} header in the HTTP response has not
     * been specified.
     */
    public static final String RETRY_AFTER_UNSPECIFIED = "";

    /**
     * The error code for why authentication failed, or {@link #ERROR_UNSPECIFIED} if it is
     * unspecified.
     */
    @AuthenticationError public int getError() {
        return ERROR_UNSPECIFIED;
    }

    /**
     * The HTTP status code (i.e. 200) returned by the entitlement server, or
     * {@link #HTTP_STATUS_CODE_UNSPECIFIED} if it is unspecified.
     */
    public int getHttpStatusCode() {
        return HTTP_STATUS_CODE_UNSPECIFIED;
    }

    /**
     * The {@code Retry-After} header in the HTTP response, or {@link #RETRY_AFTER_UNSPECIFIED} if
     * it is unspecified. This is often sent with HTTP status code {@code 503} and is an
     * {@code HTTP-date} or the number of seconds to delay, as defined in
     * <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">RFC 7231</a>
     */
    @NonNull public String getRetryAfter() {
        return RETRY_AFTER_UNSPECIFIED;
    }
}
