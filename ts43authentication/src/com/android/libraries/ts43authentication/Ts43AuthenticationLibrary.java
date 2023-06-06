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

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringDef;
import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.OutcomeReceiver;
import android.os.PersistableBundle;
import android.telephony.SubscriptionInfo;

import com.android.libraries.entitlement.ServiceEntitlementException;
import com.android.libraries.entitlement.Ts43Authentication;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TS.43 authentication library that directs EAP-AKA and OIDC authentication requests to the
 * entitlement server and returns a {@link Ts43Authentication.Ts43AuthToken} on success or an
 * {@link AuthenticationException} on failure.
 */
public class Ts43AuthenticationLibrary extends Handler {
    /**
     * Configuration key for the list of {@code SHA256} signing certificates that are permitted
     * to make authentication requests. This will be used to verify the {@code appName} that is
     * passed to authentication requests.
     * If this is a {@code null} or an empty list, all requests will be allowed to go through.
     * <p>
     * {@code null} by default
     */
    public static final String KEY_ALLOWED_CERTIFICATES_STRING_ARRAY = "allowed_certificates";

    /**
     * Configuration key for whether the {@code appName} passed to the entitlement server should
     * have the signing certificate of the calling application appended to it.
     * If this is {@code true}, the {@code appName} will be {@code "<SHA>|<packageName>"}, where
     * {@code <SHA>} is the {@code SHA256} hash of the package's signing certificate and
     * {@code <packageName>} is the package name of the calling application.
     * If this is {@code false}, the {@code appName} will just be the package name of the
     * calling application.
     * <p>
     * {@code false} by default
     */
    public static final String KEY_APPEND_SHA_TO_APP_NAME_BOOL = "append_sha_to_app_name";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef(prefix = {"KEY_"}, value = {
            KEY_ALLOWED_CERTIFICATES_STRING_ARRAY,
            KEY_APPEND_SHA_TO_APP_NAME_BOOL,
    })
    public @interface ConfigurationKey {}

    private static final int EVENT_REQUEST_EAP_AKA_AUTHENTICATION = 0;
    private static final int EVENT_REQUEST_OIDC_AUTHENTICATION_SERVER = 1;
    private static final int EVENT_REQUEST_OIDC_AUTHENTICATION = 2;

    @NonNull private final ReentrantLock mLock = new ReentrantLock();
    @NonNull private final Context mContext;

    /**
     * Create an instance of the TS.43 Authentication Library.
     * @param context The application context.
     * @param looper The looper to run authentication requests on.
     */
    public Ts43AuthenticationLibrary(@NonNull Context context, @NonNull Looper looper) {
        super(looper);
        mContext = context;
    }

    private static class EapAkaAuthenticationRequest {
        @NonNull private final String mAppName;
        @Nullable private final String mAppVersion;
        private final int mSlotIndex;
        @NonNull private final URL mEntitlementServerAddress;
        @Nullable private final String mEntitlementVersion;
        @NonNull private final String mAppId;
        @NonNull private final Executor mExecutor;
        @NonNull private final OutcomeReceiver<
                Ts43Authentication.Ts43AuthToken, AuthenticationException> mCallback;

        private EapAkaAuthenticationRequest(@NonNull String appName, @Nullable String appVersion,
                int slotIndex, @NonNull URL entitlementServerAddress,
                @Nullable String entitlementVersion, @NonNull String appId,
                @NonNull @CallbackExecutor Executor executor,
                @NonNull OutcomeReceiver<
                        Ts43Authentication.Ts43AuthToken, AuthenticationException> callback) {
            mAppName = appName;
            mAppVersion = appVersion;
            mSlotIndex = slotIndex;
            mEntitlementServerAddress = entitlementServerAddress;
            mEntitlementVersion = entitlementVersion;
            mAppId = appId;
            mExecutor = executor;
            mCallback = callback;
        }
    }

    private static class OidcAuthenticationServerRequest {
        @NonNull private final String mAppName;
        @Nullable private final String mAppVersion;
        private final int mSlotIndex;
        @NonNull private final URL mEntitlementServerAddress;
        @Nullable private final String mEntitlementVersion;
        @NonNull private final String mAppId;
        @NonNull private final Executor mExecutor;
        @NonNull private final OutcomeReceiver<URL, AuthenticationException> mCallback;

        private OidcAuthenticationServerRequest(@NonNull String appName,
                @Nullable String appVersion, int slotIndex, @NonNull URL entitlementServerAddress,
                @Nullable String entitlementVersion, @NonNull String appId,
                @NonNull @CallbackExecutor Executor executor,
                @NonNull OutcomeReceiver<URL, AuthenticationException> callback) {
            mAppName = appName;
            mAppVersion = appVersion;
            mSlotIndex = slotIndex;
            mEntitlementServerAddress = entitlementServerAddress;
            mEntitlementVersion = entitlementVersion;
            mAppId = appId;
            mExecutor = executor;
            mCallback = callback;
        }
    }

    private static class OidcAuthenticationRequest {
        @NonNull private final URL mEntitlementServerAddress;
        @Nullable private final String mEntitlementVersion;
        @NonNull private final URL mAesUrl;
        @NonNull private final Executor mExecutor;
        @NonNull private final OutcomeReceiver<
                Ts43Authentication.Ts43AuthToken, AuthenticationException> mCallback;

        private OidcAuthenticationRequest(@NonNull URL entitlementServerAddress,
                @Nullable String entitlementVersion, @NonNull URL aesUrl,
                @NonNull @CallbackExecutor Executor executor,
                @NonNull OutcomeReceiver<
                        Ts43Authentication.Ts43AuthToken, AuthenticationException> callback) {
            mEntitlementServerAddress = entitlementServerAddress;
            mEntitlementVersion = entitlementVersion;
            mAesUrl = aesUrl;
            mExecutor = executor;
            mCallback = callback;
        }
    }

    /**
     * Request authentication from the TS.43 server with EAP-AKA as described in
     * TS.43 Service Entitlement Configuration section 2.8.1.
     *
     * @param configs The configurations that should be applied to this authentication request.
     *        The keys of the bundle must be one of the {@link ConfigurationKey}s.
     * @param packageName The package name for the calling application, used to validate the
     *        identity of the calling application. This will be sent as-is as the {@code app_name}
     *        in the HTTP GET request to the entitlement server unless
     *        {@link #KEY_APPEND_SHA_TO_APP_NAME_BOOL} is set in the configuration bundle.
     * @param appVersion The optional appVersion of the calling application, passed as the
     *        {@code app_version} in the HTTP GET request to the entitlement server.
     * @param slotIndex The logical SIM slot index involved in ODSA operation.
     *        See {@link SubscriptionInfo#getSubscriptionId()}.
     * @param entitlementServerAddress The entitlement server address.
     * @param entitlementVersion The TS.43 entitlement version to use. For example, {@code "9.0"}.
     *        If this is {@code null}, version {@code "2.0"} will be used by default.
     * @param appId Application id.
     *        For example, {@code "ap2004"} for VoWifi and {@code "ap2009"} for ODSA primary device.
     *        Refer to GSMA Service Entitlement Configuration section 2.3.
     * @param executor The executor on which the callback will be called.
     * @param callback The callback to receive the results of the authentication request.
     *        If authentication is successful, {@link OutcomeReceiver#onResult(Object)} will return
     *        a {@link Ts43Authentication.Ts43AuthToken} with the token and validity.
     *        If the authentication fails, {@link OutcomeReceiver#onError(Throwable)} will return an
     *        {@link AuthenticationException} with the failure details.
     */
    public void requestEapAkaAuthentication(@NonNull PersistableBundle configs,
            @NonNull String packageName, @Nullable String appVersion, int slotIndex,
            @NonNull URL entitlementServerAddress, @Nullable String entitlementVersion,
            @NonNull String appId, @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<
                    Ts43Authentication.Ts43AuthToken, AuthenticationException> callback) {
        if (isCallingPackageValid(configs, packageName)) {
            obtainMessage(EVENT_REQUEST_EAP_AKA_AUTHENTICATION, new EapAkaAuthenticationRequest(
                    getAppName(configs, packageName), appVersion, slotIndex,
                    entitlementServerAddress, entitlementVersion, appId, executor, callback))
                    .sendToTarget();
        } else {
            executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
                    new AuthenticationException(AuthenticationException.ERROR_INVALID_APP_NAME,
                            "Failed to verify the identity of the calling application"))));
        }
    }

    /**
     * Get the URL of OIDC (OpenID Connect) server as described in
     * TS.43 Service Entitlement Configuration section 2.8.2.
     * The client should present the content of the URL to the user to continue the authentication
     * process. After receiving a response from the authentication server, the client can call
     * {@link #requestOidcAuthentication(
     * PersistableBundle, String, URL, String, URL, Executor, OutcomeReceiver)} to get the
     * authentication token.
     *
     * @param configs The configurations that should be applied to this authentication request.
     *        The keys of the bundle must be one of the {@link ConfigurationKey}s.
     * @param packageName The package name for the calling application, used to validate the
     *        identity of the calling application. This will be sent as-is as the {@code app_name}
     *        in the HTTP GET request to the entitlement server unless
     *        {@link #KEY_APPEND_SHA_TO_APP_NAME_BOOL} is set in the configuration bundle.
     * @param appVersion The optional appVersion of the calling application, passed as the
     *        {@code app_version} in the HTTP GET request to the entitlement server.
     * @param slotIndex The logical SIM slot index involved in ODSA operation.
     *        See {@link SubscriptionInfo#getSubscriptionId()}.
     * @param entitlementServerAddress The entitlement server address.
     * @param entitlementVersion The TS.43 entitlement version to use. For example, {@code "9.0"}.
     *        If this is {@code null}, version {@code "2.0"} will be used by default.
     * @param appId Application id.
     *        For example, {@code "ap2004"} for VoWifi and {@code "ap2009"} for ODSA primary device.
     *        Refer to GSMA Service Entitlement Configuration section 2.3.
     * @param executor The executor on which the callback will be called.
     * @param callback The callback to receive the results of the authentication server request.
     *        If the request is successful, {@link OutcomeReceiver#onResult(Object)} will return a
     *        {@link URL} with all the required parameters for the client to launch a user interface
     *        for users to complete the authentication process. The parameters in URL include
     *        {@code client_id}, {@code redirect_uri}, {@code state}, and {@code nonce}.
     *        If the authentication fails, {@link OutcomeReceiver#onError(Throwable)} will return an
     *        {@link AuthenticationException} with the failure details.
     */
    public void requestOidcAuthenticationServer(@NonNull PersistableBundle configs,
            @NonNull String packageName, @Nullable String appVersion, int slotIndex,
            @NonNull URL entitlementServerAddress, @Nullable String entitlementVersion,
            @NonNull String appId, @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<URL, AuthenticationException> callback) {
        if (isCallingPackageValid(configs, packageName)) {
            obtainMessage(EVENT_REQUEST_OIDC_AUTHENTICATION_SERVER,
                    new OidcAuthenticationServerRequest(getAppName(configs, packageName),
                            appVersion, slotIndex, entitlementServerAddress, entitlementVersion,
                            appId, executor, callback)).sendToTarget();
        } else {
            executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
                    new AuthenticationException(AuthenticationException.ERROR_INVALID_APP_NAME,
                            "Failed to verify the identity of the calling application"))));
        }
    }

    /**
     * Request authentication from the TS.43 server with OIDC (OpenID Connect) as described in
     * TS.43 Service Entitlement Configuration section 2.8.2.
     *
     * @param configs The configurations that should be applied to this authentication request.
     *        The keys of the bundle must be one of the {@link ConfigurationKey}s.
     * @param packageName The package name for the calling application, used to validate the
     *        identity of the calling application.
     * @param entitlementServerAddress The entitlement server address.
     * @param entitlementVersion The TS.43 entitlement version to use. For example, {@code "9.0"}.
     *        If this is {@code null}, version {@code "2.0"} will be used by default.
     * @param aesUrl The AES URL used to retrieve the authentication token. The parameters in the
     *        URL include the OIDC authentication code {@code code} and {@code state}.
     * @param executor The executor on which the callback will be called.
     * @param callback The callback to receive the results of the authentication request.
     *        If authentication is successful, {@link OutcomeReceiver#onResult(Object)} will return
     *        a {@link Ts43Authentication.Ts43AuthToken} with the token and validity.
     *        If the authentication fails, {@link OutcomeReceiver#onError(Throwable)} will return an
     *        {@link AuthenticationException} with the failure details.
     */
    public void requestOidcAuthentication(@NonNull PersistableBundle configs,
            @NonNull String packageName, @NonNull URL entitlementServerAddress,
            @Nullable String entitlementVersion, @NonNull URL aesUrl,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<
                    Ts43Authentication.Ts43AuthToken, AuthenticationException> callback) {
        if (isCallingPackageValid(configs, packageName)) {
            obtainMessage(EVENT_REQUEST_OIDC_AUTHENTICATION, new OidcAuthenticationRequest(
                    entitlementServerAddress, entitlementVersion, aesUrl, executor, callback))
                    .sendToTarget();
        } else {
            executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
                    new AuthenticationException(AuthenticationException.ERROR_INVALID_APP_NAME,
                            "Failed to verify the identity of the calling application"))));
        }
    }

    private static boolean isCallingPackageValid(@NonNull PersistableBundle configs,
            @NonNull String packageName) {
        // TODO: implement
        return true;
    }

    @NonNull private static String getAppName(@NonNull PersistableBundle configs,
            @NonNull String packageName) {
        if (configs.getBoolean(KEY_APPEND_SHA_TO_APP_NAME_BOOL)) {
            return getPackageSha(packageName) + "|" + packageName;
        } else {
            return packageName;
        }
    }

    private static String getPackageSha(@NonNull String packageName) {
        // TODO: implement
        return "";
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case EVENT_REQUEST_EAP_AKA_AUTHENTICATION:
                onRequestEapAkaAuthentication((EapAkaAuthenticationRequest) msg.obj);
                break;
            case EVENT_REQUEST_OIDC_AUTHENTICATION_SERVER:
                onRequestOidcAuthenticationServer((OidcAuthenticationServerRequest) msg.obj);
                break;
            case EVENT_REQUEST_OIDC_AUTHENTICATION:
                onRequestOidcAuthentication((OidcAuthenticationRequest) msg.obj);
                break;
            default:
                // unknown request
        }
    }

    private void onRequestEapAkaAuthentication(@NonNull EapAkaAuthenticationRequest request) {
        mLock.lock();
        try {
            Ts43Authentication authLibrary = new Ts43Authentication(mContext,
                    request.mEntitlementServerAddress, request.mEntitlementVersion);
            Ts43Authentication.Ts43AuthToken authToken = authLibrary.getAuthToken(
                    request.mSlotIndex, request.mAppId, request.mAppName, request.mAppVersion);
            request.mExecutor.execute(() -> Binder.withCleanCallingIdentity(
                    () -> request.mCallback.onResult(authToken)));
        } catch (ServiceEntitlementException exception) {
            request.mExecutor.execute(() -> Binder.withCleanCallingIdentity(
                    () -> request.mCallback.onError(new AuthenticationException(exception))));
        } finally {
            mLock.unlock();
        }
    }

    private void onRequestOidcAuthenticationServer(
            @NonNull OidcAuthenticationServerRequest request) {
        mLock.lock();
        try {
            Ts43Authentication authLibrary = new Ts43Authentication(mContext,
                    request.mEntitlementServerAddress, request.mEntitlementVersion);
            URL url = authLibrary.getOidcAuthServer(
                    mContext, request.mSlotIndex, request.mEntitlementServerAddress,
                    request.mEntitlementVersion, request.mAppId, request.mAppName,
                    request.mAppVersion);
            request.mExecutor.execute(() -> Binder.withCleanCallingIdentity(
                    () -> request.mCallback.onResult(url)));
        } catch (ServiceEntitlementException exception) {
            request.mExecutor.execute(() -> Binder.withCleanCallingIdentity(
                    () -> request.mCallback.onError(new AuthenticationException(exception))));
        } finally {
            mLock.unlock();
        }
    }

    private void onRequestOidcAuthentication(@NonNull OidcAuthenticationRequest request) {
        mLock.lock();
        try {
            Ts43Authentication authLibrary = new Ts43Authentication(mContext,
                    request.mEntitlementServerAddress, request.mEntitlementVersion);
            Ts43Authentication.Ts43AuthToken authToken = authLibrary.getAuthToken(
                    request.mAesUrl);
            request.mExecutor.execute(() -> Binder.withCleanCallingIdentity(
                    () -> request.mCallback.onResult(authToken)));
        } catch (ServiceEntitlementException exception) {
            request.mExecutor.execute(() -> Binder.withCleanCallingIdentity(
                    () -> request.mCallback.onError(new AuthenticationException(exception))));
        } finally {
            mLock.unlock();
        }
    }
}
