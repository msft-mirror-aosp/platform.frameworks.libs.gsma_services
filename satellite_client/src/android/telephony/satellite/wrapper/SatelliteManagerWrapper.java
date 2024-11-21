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

package android.telephony.satellite.wrapper;

import static android.telephony.AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
import static android.telephony.NetworkRegistrationInfo.DOMAIN_PS;
import static android.telephony.satellite.SatelliteManager.SatelliteException;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.satellite.AntennaPosition;
import android.telephony.satellite.EarfcnRange;
import android.telephony.satellite.EnableRequestAttributes;
import android.telephony.satellite.NtnSignalStrength;
import android.telephony.satellite.NtnSignalStrengthCallback;
import android.telephony.satellite.PointingInfo;
import android.telephony.satellite.SatelliteAccessConfiguration;
import android.telephony.satellite.SatelliteCapabilities;
import android.telephony.satellite.SatelliteCapabilitiesCallback;
import android.telephony.satellite.SatelliteCommunicationAllowedStateCallback;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteDatagramCallback;
import android.telephony.satellite.SatelliteInfo;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.SatelliteModemStateCallback;
import android.telephony.satellite.SatelliteProvisionStateCallback;
import android.telephony.satellite.SatelliteSessionStats;
import android.telephony.satellite.SatelliteSubscriberInfo;
import android.telephony.satellite.SatelliteSubscriberProvisionStatus;
import android.telephony.satellite.SatelliteSupportedStateCallback;
import android.telephony.satellite.SatelliteTransmissionUpdateCallback;

import com.android.internal.telephony.flags.Flags;
import com.android.telephony.Rlog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Wrapper for satellite operations such as provisioning, pointing, messaging, location sharing,
 * etc. To get the object, call {@link Context#getSystemService(String)}.
 */
public class SatelliteManagerWrapper {
  private static final String TAG = "SatelliteManagerWrapper";

  private static final ConcurrentHashMap<
      SatelliteDatagramCallbackWrapper, SatelliteDatagramCallback>
      sSatelliteDatagramCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<
          SatelliteProvisionStateCallbackWrapper, SatelliteProvisionStateCallback>
      sSatelliteProvisionStateCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<SatelliteModemStateCallbackWrapper,
          SatelliteModemStateCallback> sSatelliteModemStateCallbackWrapperMap =
          new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<SatelliteModemStateCallbackWrapper2,
          SatelliteModemStateCallback> sSatelliteModemStateCallbackWrapperMap2 =
          new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<
          SatelliteTransmissionUpdateCallbackWrapper, SatelliteTransmissionUpdateCallback>
      sSatelliteTransmissionUpdateCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<
          SatelliteTransmissionUpdateCallbackWrapper2, SatelliteTransmissionUpdateCallback>
          sSatelliteTransmissionUpdateCallbackWrapperMap2 = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<
          NtnSignalStrengthCallbackWrapper, NtnSignalStrengthCallback>
      sNtnSignalStrengthCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<
          SatelliteCapabilitiesCallbackWrapper, SatelliteCapabilitiesCallback>
          sSatelliteCapabilitiesCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<
          SatelliteSupportedStateCallbackWrapper, SatelliteSupportedStateCallback>
          sSatelliteSupportedStateCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<CarrierRoamingNtnModeListenerWrapper,
          CarrierRoamingNtnModeListener>
          sCarrierRoamingNtnModeListenerWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<CarrierRoamingNtnModeListenerWrapper2,
          CarrierRoamingNtnModeListener>
          sCarrierRoamingNtnModeListenerWrapperMap2 = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<SatelliteCommunicationAllowedStateCallbackWrapper,
          SatelliteCommunicationAllowedStateCallback>
          sSatelliteCommunicationAllowedStateCallbackWrapperMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<SatelliteCommunicationAllowedStateCallbackWrapper2,
            SatelliteCommunicationAllowedStateCallback>
            sSatelliteCommunicationAllowedStateCallbackWrapperMap2 = new ConcurrentHashMap<>();

  private final SatelliteManager mSatelliteManager;
  private final SubscriptionManager mSubscriptionManager;
  private final TelephonyManager mTelephonyManager;

  SatelliteManagerWrapper(Context context) {
    mSatelliteManager = context.getSystemService(SatelliteManager.class);
    mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
    mTelephonyManager = context.getSystemService(TelephonyManager.class);
  }

  /**
   * Factory method.
   *
   * @param context context of application
   */
  public static SatelliteManagerWrapper getInstance(Context context) {
    return new SatelliteManagerWrapper(context);
  }

  /**
   * Datagram type is unknown. This generic datagram type should be used only when the datagram type
   * cannot be mapped to other specific datagram types.
   */
  public static final int DATAGRAM_TYPE_UNKNOWN = 0;
  /** Datagram type indicating that the datagram to be sent or received is of type SOS message. */
  public static final int DATAGRAM_TYPE_SOS_MESSAGE = 1;
  /**
   * Datagram type indicating that the datagram to be sent or received is of type location sharing.
   */
  public static final int DATAGRAM_TYPE_LOCATION_SHARING = 2;
  /**
   * This type of datagram is used to keep the device in satellite connected state.
   */
  public static final int DATAGRAM_TYPE_KEEP_ALIVE = 3;
  /**
   * Datagram type indicating that the datagram to be sent or received is of type SOS message and
   * is the last message to emergency service provider indicating still needs help.
   */
  public static final int DATAGRAM_TYPE_LAST_SOS_MESSAGE_STILL_NEED_HELP = 4;
  /**
   * Datagram type indicating that the datagram to be sent or received is of type SOS message and
   * is the last message to emergency service provider indicating no more help is needed.
   */
  public static final int DATAGRAM_TYPE_LAST_SOS_MESSAGE_NO_HELP_NEEDED = 5;
  /**
   * Datagram type indicating that the message to be sent or received is of type SMS.
   */
  public static final int DATAGRAM_TYPE_SMS = 6;
  /**
   * Datagram type indicating that the message to be sent is an SMS checking
   * for pending incoming SMS.
   * @hide
   */
    public static final int DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS = 7;

  /** @hide */
  @IntDef(
      prefix = "DATAGRAM_TYPE_",
      value = {
              DATAGRAM_TYPE_UNKNOWN,
              DATAGRAM_TYPE_SOS_MESSAGE,
              DATAGRAM_TYPE_LOCATION_SHARING,
              DATAGRAM_TYPE_KEEP_ALIVE,
              DATAGRAM_TYPE_LAST_SOS_MESSAGE_STILL_NEED_HELP,
              DATAGRAM_TYPE_LAST_SOS_MESSAGE_NO_HELP_NEEDED,
              DATAGRAM_TYPE_SMS,
              DATAGRAM_TYPE_CHECK_PENDING_INCOMING_SMS
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface DatagramType {}

  /**
   * Unknown Non-Terrestrial radio technology. This generic radio technology should be used only
   * when the radio technology cannot be mapped to other specific radio technologies.
   */
  public static final int NT_RADIO_TECHNOLOGY_UNKNOWN = 0;
  /** 3GPP NB-IoT (Narrowband Internet of Things) over Non-Terrestrial-Networks technology. */
  public static final int NT_RADIO_TECHNOLOGY_NB_IOT_NTN = 1;
  /** 3GPP 5G NR over Non-Terrestrial-Networks technology. */
  public static final int NT_RADIO_TECHNOLOGY_NR_NTN = 2;
  /** 3GPP eMTC (enhanced Machine-Type Communication) over Non-Terrestrial-Networks technology. */
  public static final int NT_RADIO_TECHNOLOGY_EMTC_NTN = 3;
  /** Proprietary technology. */
  public static final int NT_RADIO_TECHNOLOGY_PROPRIETARY = 4;

  /** @hide */
  @IntDef(
      prefix = "NT_RADIO_TECHNOLOGY_",
      value = {
        NT_RADIO_TECHNOLOGY_UNKNOWN,
        NT_RADIO_TECHNOLOGY_NB_IOT_NTN,
        NT_RADIO_TECHNOLOGY_NR_NTN,
        NT_RADIO_TECHNOLOGY_EMTC_NTN,
        NT_RADIO_TECHNOLOGY_PROPRIETARY
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface NTRadioTechnology {}

  /** Satellite modem is in idle state. */
  public static final int SATELLITE_MODEM_STATE_IDLE = 0;
  /** Satellite modem is listening for incoming datagrams. */
  public static final int SATELLITE_MODEM_STATE_LISTENING = 1;
  /** Satellite modem is sending and/or receiving datagrams. */
  public static final int SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING = 2;
  /** Satellite modem is retrying to send and/or receive datagrams. */
  public static final int SATELLITE_MODEM_STATE_DATAGRAM_RETRYING = 3;
  /** Satellite modem is powered off. */
  public static final int SATELLITE_MODEM_STATE_OFF = 4;
  /** Satellite modem is unavailable. */
  public static final int SATELLITE_MODEM_STATE_UNAVAILABLE = 5;
  /**
   * The satellite modem is powered on but the device is not registered to a satellite cell.
   */
  public static final int SATELLITE_MODEM_STATE_NOT_CONNECTED = 6;
  /**
   * The satellite modem is powered on and the device is registered to a satellite cell.
   */
  public static final int SATELLITE_MODEM_STATE_CONNECTED = 7;
  /**
   * The satellite modem is being powered on.
   */
  public static final int SATELLITE_MODEM_STATE_ENABLING_SATELLITE = 8;
  /**
   * The satellite modem is being powered off.
   */
  public static final int SATELLITE_MODEM_STATE_DISABLING_SATELLITE = 9;
  /**
   * Satellite modem state is unknown. This generic modem state should be used only when the modem
   * state cannot be mapped to other specific modem states.
   */
  public static final int SATELLITE_MODEM_STATE_UNKNOWN = -1;

  /** @hide */
  @IntDef(
      prefix = {"SATELLITE_MODEM_STATE_"},
      value = {
        SATELLITE_MODEM_STATE_IDLE,
        SATELLITE_MODEM_STATE_LISTENING,
        SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING,
        SATELLITE_MODEM_STATE_DATAGRAM_RETRYING,
        SATELLITE_MODEM_STATE_OFF,
        SATELLITE_MODEM_STATE_UNAVAILABLE,
        SATELLITE_MODEM_STATE_NOT_CONNECTED,
        SATELLITE_MODEM_STATE_CONNECTED,
        SATELLITE_MODEM_STATE_ENABLING_SATELLITE,
        SATELLITE_MODEM_STATE_DISABLING_SATELLITE,
        SATELLITE_MODEM_STATE_UNKNOWN
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface SatelliteModemState {}

  /**
   * The default state indicating that datagram transfer is idle. This should be sent if there are
   * no message transfer activity happening.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE = 0;
  /** A transition state indicating that a datagram is being sent. */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING = 1;
  /**
   * An end state indicating that datagram sending completed successfully. After datagram transfer
   * completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE} will be sent if no more messages are
   * pending.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS = 2;
  /**
   * An end state indicating that datagram sending completed with a failure. After datagram transfer
   * completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE} must be sent before reporting any
   * additional datagram transfer state changes. All pending messages will be reported as failed, to
   * the corresponding applications.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED = 3;
  /** A transition state indicating that a datagram is being received. */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING = 4;
  /**
   * An end state indicating that datagram receiving completed successfully. After datagram transfer
   * completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE} will be sent if no more messages are
   * pending.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS = 5;
  /**
   * An end state indicating that datagram receive operation found that there are no messages to be
   * retrieved from the satellite. After datagram transfer completes, {@link
   * #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE} will be sent if no more messages are pending.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_NONE = 6;
  /**
   * An end state indicating that datagram receive completed with a failure. After datagram transfer
   * completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE} will be sent if no more messages are
   * pending.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED = 7;
  /**
   * A transition state indicating that Telephony is waiting for satellite modem to connect to a
   * satellite network before sending a datagram or polling for datagrams. If the satellite modem
   * successfully connects to a satellite network, either
   * {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING} or
   * {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING} will be sent. Otherwise,
   * either {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED} or
   * {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED} will be sent.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT = 8;
  /**
   * The datagram transfer state is unknown. This generic datagram transfer state should be used
   * only when the datagram transfer state cannot be mapped to other specific datagram transfer
   * states.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_UNKNOWN = -1;

  /** @hide */
  @IntDef(
      prefix = {"SATELLITE_DATAGRAM_TRANSFER_STATE_"},
      value = {
        SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
        SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
        SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS,
        SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
        SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING,
        SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS,
        SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_NONE,
        SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED,
        SATELLITE_DATAGRAM_TRANSFER_STATE_WAITING_TO_CONNECT,
        SATELLITE_DATAGRAM_TRANSFER_STATE_UNKNOWN
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface SatelliteDatagramTransferState {}

  /** The request was successfully processed. */
  public static final int SATELLITE_RESULT_SUCCESS = 0;
  /** A generic error which should be used only when other specific errors cannot be used. */
  public static final int SATELLITE_RESULT_ERROR = 1;
  /** Error received from the satellite server. */
  public static final int SATELLITE_RESULT_SERVER_ERROR = 2;
  /**
   * Error received from the vendor service. This generic error code should be used only when the
   * error cannot be mapped to other specific service error codes.
   */
  public static final int SATELLITE_RESULT_SERVICE_ERROR = 3;
  /**
   * Error received from satellite modem. This generic error code should be used only when the error
   * cannot be mapped to other specific modem error codes.
   */
  public static final int SATELLITE_RESULT_MODEM_ERROR = 4;
  /**
   * Error received from the satellite network. This generic error code should be used only when the
   * error cannot be mapped to other specific network error codes.
   */
  public static final int SATELLITE_RESULT_NETWORK_ERROR = 5;
  /** Telephony is not in a valid state to receive requests from clients. */
  public static final int SATELLITE_RESULT_INVALID_TELEPHONY_STATE = 6;
  /** Satellite modem is not in a valid state to receive requests from clients. */
  public static final int SATELLITE_RESULT_INVALID_MODEM_STATE = 7;
  /**
   * Either vendor service, or modem, or Telephony framework has received a request with invalid
   * arguments from its clients.
   */
  public static final int SATELLITE_RESULT_INVALID_ARGUMENTS = 8;
  /**
   * Telephony framework failed to send a request or receive a response from the vendor service or
   * satellite modem due to internal error.
   */
  public static final int SATELLITE_RESULT_REQUEST_FAILED = 9;
  /** Radio did not start or is resetting. */
  public static final int SATELLITE_RESULT_RADIO_NOT_AVAILABLE = 10;
  /** The request is not supported by either the satellite modem or the network. */
  public static final int SATELLITE_RESULT_REQUEST_NOT_SUPPORTED = 11;
  /** Satellite modem or network has no resources available to handle requests from clients. */
  public static final int SATELLITE_RESULT_NO_RESOURCES = 12;
  /** Satellite service is not provisioned yet. */
  public static final int SATELLITE_RESULT_SERVICE_NOT_PROVISIONED = 13;
  /** Satellite service provision is already in progress. */
  public static final int SATELLITE_RESULT_SERVICE_PROVISION_IN_PROGRESS = 14;
  /**
   * The ongoing request was aborted by either the satellite modem or the network. This error is
   * also returned when framework decides to abort current send request as one of the previous send
   * request failed.
   */
  public static final int SATELLITE_RESULT_REQUEST_ABORTED = 15;
  /** The device/subscriber is barred from accessing the satellite service. */
  public static final int SATELLITE_RESULT_ACCESS_BARRED = 16;
  /**
   * Satellite modem timeout to receive ACK or response from the satellite network after sending a
   * request to the network.
   */
  public static final int SATELLITE_RESULT_NETWORK_TIMEOUT = 17;
  /** Satellite network is not reachable from the modem. */
  public static final int SATELLITE_RESULT_NOT_REACHABLE = 18;
  /** The device/subscriber is not authorized to register with the satellite service provider. */
  public static final int SATELLITE_RESULT_NOT_AUTHORIZED = 19;
  /** The device does not support satellite. */
  public static final int SATELLITE_RESULT_NOT_SUPPORTED = 20;
  /** The current request is already in-progress. */
  public static final int SATELLITE_RESULT_REQUEST_IN_PROGRESS = 21;
  /** Satellite modem is currently busy due to which current request cannot be processed. */
  public static final int SATELLITE_RESULT_MODEM_BUSY = 22;
  /** Telephony process is not currently available or satellite is not supported. */
  public static final int SATELLITE_RESULT_ILLEGAL_STATE = 23;
  /**
   * Telephony framework timeout to receive ACK or response from the satellite modem after
   * sending a request to the modem.
   */
  public static final int SATELLITE_RESULT_MODEM_TIMEOUT = 24;

  /**
   * Telephony framework needs to access the current location of the device to perform the
   * request. However, location in the settings is disabled by users.
   */
  public static final int SATELLITE_RESULT_LOCATION_DISABLED = 25;

  /**
   * Telephony framework needs to access the current location of the device to perform the
   * request. However, Telephony fails to fetch the current location from location service.
   */
  public static final int SATELLITE_RESULT_LOCATION_NOT_AVAILABLE = 26;

  /**
   * Emergency call is in progress.
   */
  public static final int SATELLITE_RESULT_EMERGENCY_CALL_IN_PROGRESS = 27;

  /** @hide */
  @IntDef(
      prefix = {"SATELLITE_RESULT_"},
      value = {
        SATELLITE_RESULT_SUCCESS,
        SATELLITE_RESULT_ERROR,
        SATELLITE_RESULT_SERVER_ERROR,
        SATELLITE_RESULT_SERVICE_ERROR,
        SATELLITE_RESULT_MODEM_ERROR,
        SATELLITE_RESULT_NETWORK_ERROR,
        SATELLITE_RESULT_INVALID_TELEPHONY_STATE,
        SATELLITE_RESULT_INVALID_MODEM_STATE,
        SATELLITE_RESULT_INVALID_ARGUMENTS,
        SATELLITE_RESULT_REQUEST_FAILED,
        SATELLITE_RESULT_RADIO_NOT_AVAILABLE,
        SATELLITE_RESULT_REQUEST_NOT_SUPPORTED,
        SATELLITE_RESULT_NO_RESOURCES,
        SATELLITE_RESULT_SERVICE_NOT_PROVISIONED,
        SATELLITE_RESULT_SERVICE_PROVISION_IN_PROGRESS,
        SATELLITE_RESULT_REQUEST_ABORTED,
        SATELLITE_RESULT_ACCESS_BARRED,
        SATELLITE_RESULT_NETWORK_TIMEOUT,
        SATELLITE_RESULT_NOT_REACHABLE,
        SATELLITE_RESULT_NOT_AUTHORIZED,
        SATELLITE_RESULT_NOT_SUPPORTED,
        SATELLITE_RESULT_REQUEST_IN_PROGRESS,
        SATELLITE_RESULT_MODEM_BUSY,
        SATELLITE_RESULT_ILLEGAL_STATE,
        SATELLITE_RESULT_MODEM_TIMEOUT,
        SATELLITE_RESULT_LOCATION_DISABLED,
        SATELLITE_RESULT_LOCATION_NOT_AVAILABLE,
        SATELLITE_RESULT_EMERGENCY_CALL_IN_PROGRESS
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface SatelliteResult {}

  /** Suggested device hold position is unknown. */
  public static final int DEVICE_HOLD_POSITION_UNKNOWN = 0;
  /** User is suggested to hold the device in portrait mode. */
  public static final int DEVICE_HOLD_POSITION_PORTRAIT = 1;
  /** User is suggested to hold the device in landscape mode with left hand. */
  public static final int DEVICE_HOLD_POSITION_LANDSCAPE_LEFT = 2;
  /** User is suggested to hold the device in landscape mode with right hand. */
  public static final int DEVICE_HOLD_POSITION_LANDSCAPE_RIGHT = 3;

  /** @hide */
  @IntDef(
      prefix = {"DEVICE_HOLD_POSITION_"},
      value = {
        DEVICE_HOLD_POSITION_UNKNOWN,
        DEVICE_HOLD_POSITION_PORTRAIT,
        DEVICE_HOLD_POSITION_LANDSCAPE_LEFT,
        DEVICE_HOLD_POSITION_LANDSCAPE_RIGHT
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface DeviceHoldPosition {}

  /**
   * Satellite communication restricted by user.
   * @hide
   */
  public static final int SATELLITE_COMMUNICATION_RESTRICTION_REASON_USER = 0;

  /**
   * Satellite communication restricted by geolocation. This can be
   * triggered based upon geofence input provided by carrier to enable or disable satellite.
   */
  public static final int SATELLITE_COMMUNICATION_RESTRICTION_REASON_GEOLOCATION = 1;

  /**
   * Satellite communication restricted by entitlement server. This can be triggered based on
   * the EntitlementStatus value received from the entitlement server to enable or disable
   * satellite.
   */
  public static final int SATELLITE_COMMUNICATION_RESTRICTION_REASON_ENTITLEMENT = 2;

  /** @hide */
  @IntDef(prefix = "SATELLITE_COMMUNICATION_RESTRICTION_REASON_", value = {
          SATELLITE_COMMUNICATION_RESTRICTION_REASON_USER,
          SATELLITE_COMMUNICATION_RESTRICTION_REASON_GEOLOCATION,
          SATELLITE_COMMUNICATION_RESTRICTION_REASON_ENTITLEMENT
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface SatelliteCommunicationRestrictionReason {}

  /** Exception from the satellite service containing the {@link SatelliteResult} error code. */
  public static class SatelliteExceptionWrapper extends Exception {
    private final int mErrorCode;

    /** Create a SatelliteException with a given error code. */
    public SatelliteExceptionWrapper(int errorCode) {
      mErrorCode = errorCode;
    }

    /** Get the error code returned from the satellite service. */
    public int getErrorCode() {
      return mErrorCode;
    }
  }

  /**
   * Request to enable or disable the satellite modem and demo mode. If the satellite modem is
   * enabled, this may also disable the cellular modem, and if the satellite modem is disabled, this
   * may also re-enable the cellular modem.
   */
  public void requestEnabled(
      boolean enableSatellite,
      boolean enableDemoMode,
      boolean isEmergency,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteResult @NonNull Consumer<Integer> resultListener) {
    if (mSatelliteManager == null) {
      logd("requestEnabled: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    mSatelliteManager.requestEnabled(new EnableRequestAttributes.Builder(enableSatellite)
            .setDemoMode(enableDemoMode)
            .setEmergencyMode(isEmergency)
            .build(), executor, resultListener);
  }

  /** Request to get whether the satellite modem is enabled. */
  public void requestIsEnabled(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestIsEnabled: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsEnabled(executor, internalCallback);
  }

  /** Request to get whether the satellite service demo mode is enabled. */
  public void requestIsDemoModeEnabled(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestIsDemoModeEnabled: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsDemoModeEnabled(executor, internalCallback);
  }

  /** Request to get whether the satellite service is enabled for emergency mode */
  public void requestIsEmergencyModeEnabled(
          @NonNull @CallbackExecutor Executor executor,
          @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestIsEmergencyModeEnabled: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsEmergencyModeEnabled(executor, internalCallback);
  }

  /** Request to get whether the satellite service is supported on the device. */
  public void requestIsSupported(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestIsSupported: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onResult(false)));
      return;
    }

    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsSupported(executor, internalCallback);
  }

  /** Request to get the {@link SatelliteCapabilities} of the satellite service. */
  public void requestCapabilities(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<SatelliteCapabilitiesWrapper, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestCapabilities: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
        new OutcomeReceiver<SatelliteCapabilities, SatelliteException>() {
          @Override
          public void onResult(SatelliteCapabilities result) {
            callback.onResult(
                new SatelliteCapabilitiesWrapper(
                    result.getSupportedRadioTechnologies(),
                    result.isPointingRequired(),
                    result.getMaxBytesPerOutgoingDatagram(),
                    transformToAntennaPositionWrapperMap(result.getAntennaPositionMap())));
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestCapabilities(executor, internalCallback);
  }

  /**
   * Start receiving satellite transmission updates. This can be called by the pointing UI when the
   * user starts pointing to the satellite. Modem should continue to report the pointing input as
   * the device or satellite moves. Satellite transmission updates are started only on {@link
   * #SATELLITE_RESULT_SUCCESS}. All other results indicate that this operation failed.
   * Once satellite transmission updates begin, position and datagram transfer state updates
   * will be sent through {@link SatelliteTransmissionUpdateCallback}.
   */
  public void startTransmissionUpdates(
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteResult @NonNull Consumer<Integer> resultListener,
      @NonNull SatelliteTransmissionUpdateCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("startTransmissionUpdates: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    SatelliteTransmissionUpdateCallback internalCallback =
        new SatelliteTransmissionUpdateCallback() {

          @Override
          public void onSendDatagramStateChanged(
              @SatelliteDatagramTransferState int state,
              int sendPendingCount,
              @SatelliteResult int errorCode) {
            callback.onSendDatagramStateChanged(state, sendPendingCount, errorCode);
          }

          @Override
          public void onSendDatagramStateChanged(
                  @SatelliteManager.DatagramType int datagramType,
                  @SatelliteDatagramTransferState int state,
                  int sendPendingCount,
                  @SatelliteResult int errorCode) {
            callback.onSendDatagramStateChanged(datagramType, state, sendPendingCount, errorCode);
          }

          @Override
          public void onReceiveDatagramStateChanged(
              @SatelliteDatagramTransferState int state,
              int receivePendingCount,
              @SatelliteResult int errorCode) {
            callback.onReceiveDatagramStateChanged(state, receivePendingCount, errorCode);
          }

          @Override
          public void onSatellitePositionChanged(@NonNull PointingInfo pointingInfo) {
            callback.onSatellitePositionChanged(
                new PointingInfoWrapper(
                    pointingInfo.getSatelliteAzimuthDegrees(),
                    pointingInfo.getSatelliteElevationDegrees()));
          }
        };
    sSatelliteTransmissionUpdateCallbackWrapperMap.put(callback, internalCallback);

    mSatelliteManager.startTransmissionUpdates(executor, resultListener, internalCallback);
  }

  /**
   * Start receiving satellite transmission updates. This can be called by the pointing UI when the
   * user starts pointing to the satellite. Modem should continue to report the pointing input as
   * the device or satellite moves. Satellite transmission updates are started only on {@link
   * #SATELLITE_RESULT_SUCCESS}. All other results indicate that this operation failed.
   * Once satellite transmission updates begin, position and datagram transfer state updates
   * will be sent through {@link SatelliteTransmissionUpdateCallback}.
   */
  public void startTransmissionUpdates2(
          @NonNull @CallbackExecutor Executor executor,
          @SatelliteResult @NonNull Consumer<Integer> resultListener,
          @NonNull SatelliteTransmissionUpdateCallbackWrapper2 callback) {
    if (mSatelliteManager == null) {
      logd("startTransmissionUpdates2: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    SatelliteTransmissionUpdateCallback internalCallback =
            new SatelliteTransmissionUpdateCallback() {

              @Override
              public void onSendDatagramStateChanged(
                      @SatelliteDatagramTransferState int state,
                      int sendPendingCount,
                      @SatelliteResult int errorCode) {
                callback.onSendDatagramStateChanged(state, sendPendingCount, errorCode);
              }

              @Override
              public void onSendDatagramStateChanged(
                      @SatelliteManager.DatagramType int datagramType,
                      @SatelliteDatagramTransferState int state,
                      int sendPendingCount,
                      @SatelliteResult int errorCode) {
                callback.onSendDatagramStateChanged(
                        datagramType, state, sendPendingCount, errorCode);
              }

              @Override
              public void onReceiveDatagramStateChanged(
                      @SatelliteDatagramTransferState int state,
                      int receivePendingCount,
                      @SatelliteResult int errorCode) {
                callback.onReceiveDatagramStateChanged(state, receivePendingCount, errorCode);
              }

              @Override
              public void onSatellitePositionChanged(@NonNull PointingInfo pointingInfo) {
                callback.onSatellitePositionChanged(
                        new PointingInfoWrapper(
                                pointingInfo.getSatelliteAzimuthDegrees(),
                                pointingInfo.getSatelliteElevationDegrees()));
              }

              @Override
              public void onSendDatagramRequested(@SatelliteManager.DatagramType int datagramType) {
                callback.onSendDatagramRequested(datagramType);
              }
            };
    sSatelliteTransmissionUpdateCallbackWrapperMap2.put(callback, internalCallback);

    mSatelliteManager.startTransmissionUpdates(executor, resultListener, internalCallback);
  }

  /**
   * Stop receiving satellite transmission updates. This can be called by the pointing UI when the
   * user stops pointing to the satellite. Satellite transmission updates are stopped and the
   * callback is unregistered only on {@link #SATELLITE_RESULT_SUCCESS}. All other results that this
   * operation failed.
   */
  public void stopTransmissionUpdates(
      @NonNull SatelliteTransmissionUpdateCallbackWrapper callback,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteResult @NonNull Consumer<Integer> resultListener) {
    if (mSatelliteManager == null) {
      logd("stopTransmissionUpdates: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    SatelliteTransmissionUpdateCallback internalCallback =
        sSatelliteTransmissionUpdateCallbackWrapperMap.remove(callback);
    if (internalCallback != null) {
      mSatelliteManager.stopTransmissionUpdates(
          internalCallback, executor, resultListener);
    }
  }

  /**
   * Stop receiving satellite transmission updates. This can be called by the pointing UI when the
   * user stops pointing to the satellite. Satellite transmission updates are stopped and the
   * callback is unregistered only on {@link #SATELLITE_RESULT_SUCCESS}. All other results that this
   * operation failed.
   */
  public void stopTransmissionUpdates2(
          @NonNull SatelliteTransmissionUpdateCallbackWrapper2 callback,
          @NonNull @CallbackExecutor Executor executor,
          @SatelliteResult @NonNull Consumer<Integer> resultListener) {
    if (mSatelliteManager == null) {
      logd("stopTransmissionUpdates2: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    SatelliteTransmissionUpdateCallback internalCallback =
            sSatelliteTransmissionUpdateCallbackWrapperMap2.remove(callback);
    if (internalCallback != null) {
      mSatelliteManager.stopTransmissionUpdates(
              internalCallback, executor, resultListener);
    }
  }

  /**
   * Provision the device with a satellite provider. This is needed if the provider allows dynamic
   * registration.
   */
  public void provisionService(
      @NonNull String token,
      @NonNull byte[] provisionData,
      @Nullable CancellationSignal cancellationSignal,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteResult @NonNull Consumer<Integer> resultListener) {
    if (mSatelliteManager == null) {
      logd("provisionService: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    mSatelliteManager.provisionService(
        token, provisionData, cancellationSignal, executor, resultListener);
  }

  /**
   * Deprovision the device with the satellite provider. This is needed if the provider allows
   * dynamic registration. Once deprovisioned, {@link
   * SatelliteProvisionStateCallback#onSatelliteProvisionStateChanged(boolean)} should report as
   * deprovisioned.
   */
  public void deprovisionService(
      @NonNull String token,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteResult @NonNull Consumer<Integer> resultListener) {
    if (mSatelliteManager == null) {
      logd("deprovisionService: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    mSatelliteManager.deprovisionService(token, executor, resultListener);
  }

  /** Registers for the satellite provision state changed. */
  @SatelliteResult
  public int registerForProvisionStateChanged(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull SatelliteProvisionStateCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("registerForProvisionStateChanged: mSatelliteManager is null");
      return SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED;
    }

    SatelliteProvisionStateCallback internalCallback =
        new SatelliteProvisionStateCallback() {
          @Override
          public void onSatelliteProvisionStateChanged(boolean provisioned) {
            callback.onSatelliteProvisionStateChanged(provisioned);
          }

          @Override
          public void onSatelliteSubscriptionProvisionStateChanged(@NonNull
          List<SatelliteSubscriberProvisionStatus> satelliteSubscriberProvisionStatus) {
            callback.onSatelliteSubscriptionProvisionStateChanged(
                    transformToWrapperList(satelliteSubscriberProvisionStatus));
          }
        };
    sSatelliteProvisionStateCallbackWrapperMap.put(callback, internalCallback);
    int result =
        mSatelliteManager.registerForProvisionStateChanged(executor, internalCallback);
    return result;
  }

  /**
   * Unregisters for the satellite provision state changed. If callback was not registered before,
   * the request will be ignored.
   */
  public void unregisterForProvisionStateChanged(
      @NonNull SatelliteProvisionStateCallbackWrapper callback) {
    if (mSatelliteManager == null){
      logd("unregisterForProvisionStateChanged: mSatelliteManager is null");
      return;
    }

    SatelliteProvisionStateCallback internalCallback =
        sSatelliteProvisionStateCallbackWrapperMap.remove(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForProvisionStateChanged(internalCallback);
    }
  }

  /** Request to get whether this device is provisioned with a satellite provider. */
  public void requestIsProvisioned(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestIsProvisioned: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsProvisioned(executor, internalCallback);
  }

  /** Registers for modem state changed from satellite modem. */
  @SatelliteResult
  public int registerForModemStateChanged(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull SatelliteModemStateCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("registerForModemStateChanged: mSatelliteManager is null");
      return SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED;
    }

    SatelliteModemStateCallback internalCallback =
        new SatelliteModemStateCallback() {
          public void onSatelliteModemStateChanged(@SatelliteModemState int state) {
            callback.onSatelliteModemStateChanged(state);
          }
        };
    sSatelliteModemStateCallbackWrapperMap.put(callback, internalCallback);

    int result =
        mSatelliteManager.registerForModemStateChanged(executor, internalCallback);
    return result;
  }

  /** Registers for modem state changed from satellite modem. */
  @SatelliteResult
  public int registerForModemStateChanged(
          @NonNull @CallbackExecutor Executor executor,
          @NonNull SatelliteModemStateCallbackWrapper2 callback) {
    if (mSatelliteManager == null) {
      logd("registerForModemStateChanged: mSatelliteManager is null");
      return SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED;
    }

    SatelliteModemStateCallback internalCallback =
            new SatelliteModemStateCallback() {
              public void onSatelliteModemStateChanged(@SatelliteModemState int state) {
                callback.onSatelliteModemStateChanged(state);
              }

              public void onEmergencyModeChanged(boolean isEmergency) {
                callback.onEmergencyModeChanged(isEmergency);
              }

              public void onRegistrationFailure(int causeCode) {
                callback.onRegistrationFailure(causeCode);
              }

              public void onTerrestrialNetworkAvailableChanged(boolean isAvailable) {
                callback.onTerrestrialNetworkAvailableChanged(isAvailable);
              }
            };
    sSatelliteModemStateCallbackWrapperMap2.put(callback, internalCallback);

    int result =
            mSatelliteManager.registerForModemStateChanged(executor, internalCallback);
    return result;
  }

  /**
   * Unregisters for modem state changed from satellite modem. If callback was not registered
   * before, the request will be ignored.
   */
  public void unregisterForModemStateChanged(
      @NonNull SatelliteModemStateCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("unregisterForModemStateChanged: mSatelliteManager is null");
      return;
    }

    SatelliteModemStateCallback internalCallback = sSatelliteModemStateCallbackWrapperMap.remove(
            callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForModemStateChanged(internalCallback);
    }
  }

  /**
   * Unregisters for modem state changed from satellite modem. If callback was not registered
   * before, the request will be ignored.
   */
  public void unregisterForModemStateChanged(
          @NonNull SatelliteModemStateCallbackWrapper2 callback) {
    if (mSatelliteManager == null) {
      logd("unregisterForModemStateChanged: mSatelliteManager is null");
      return;
    }

    SatelliteModemStateCallback internalCallback = sSatelliteModemStateCallbackWrapperMap2.remove(
            callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForModemStateChanged(internalCallback);
    }
  }

  /** Register to receive incoming datagrams over satellite. */
  @SatelliteResult
  public int registerForIncomingDatagram(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull SatelliteDatagramCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("registerForIncomingDatagram: mSatelliteManager is null");
      return SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED;
    }

    SatelliteDatagramCallback internalCallback =
        new SatelliteDatagramCallback() {
          @Override
          public void onSatelliteDatagramReceived(
              long datagramId,
              @NonNull SatelliteDatagram datagram,
              int pendingCount,
              @NonNull Consumer<Void> internalCallback) {
            callback.onSatelliteDatagramReceived(
                datagramId,
                new SatelliteDatagramWrapper(datagram.getSatelliteDatagram()),
                pendingCount,
                internalCallback);
          }
        };
    sSatelliteDatagramCallbackWrapperMap.put(callback, internalCallback);
    int result = mSatelliteManager.registerForIncomingDatagram(executor, internalCallback);
    return result;
  }

  /**
   * Unregister to stop receiving incoming datagrams over satellite. If callback was not registered
   * before, the request will be ignored.
   */
  public void unregisterForIncomingDatagram(@NonNull SatelliteDatagramCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("unregisterForIncomingDatagram: mSatelliteManager is null");
      return;
    }

    SatelliteDatagramCallback internalCallback =
            sSatelliteDatagramCallbackWrapperMap.remove(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForIncomingDatagram(internalCallback);
    }
  }

  private class CarrierRoamingNtnModeListener extends TelephonyCallback
          implements TelephonyCallback.CarrierRoamingNtnModeListener {

    private CarrierRoamingNtnModeListenerWrapper mListenerWrapper;
    private CarrierRoamingNtnModeListenerWrapper2 mListenerWrapper2;

    public CarrierRoamingNtnModeListener(CarrierRoamingNtnModeListenerWrapper listenerWrapper) {
      mListenerWrapper = listenerWrapper;
      mListenerWrapper2 = null;
    }

    public CarrierRoamingNtnModeListener(CarrierRoamingNtnModeListenerWrapper2 listenerWrapper) {
      mListenerWrapper = null;
      mListenerWrapper2 = listenerWrapper;
    }

    @Override
    public void onCarrierRoamingNtnModeChanged(boolean active) {
      logd("onCarrierRoamingNtnModeChanged: active=" + active);
      if (mListenerWrapper2 != null) {
        mListenerWrapper2.onCarrierRoamingNtnModeChanged(active);
      } else if (mListenerWrapper != null) {
        mListenerWrapper.onCarrierRoamingNtnModeChanged(active);
      }
    }

    @Override
    public void onCarrierRoamingNtnEligibleStateChanged(boolean eligible) {
      logd("onCarrierRoamingNtnEligibleStateChanged: eligible=" + eligible);
      if (mListenerWrapper2 != null) {
        mListenerWrapper2.onCarrierRoamingNtnEligibleStateChanged(eligible);
      }
    }

    @Override
    public void onCarrierRoamingNtnAvailableServicesChanged(
            @NetworkRegistrationInfo.ServiceType List<Integer> availableServices) {
      logd("onCarrierRoamingNtnAvailableServicesChanged: availableServices="
              + availableServices.stream().map(String::valueOf).collect(Collectors.joining(", ")));
    }
  }

  /** Register for carrier roaming non-terrestrial network mode changes. */
  public void registerForCarrierRoamingNtnModeChanged(int subId,
          @NonNull @CallbackExecutor Executor executor,
          @NonNull CarrierRoamingNtnModeListenerWrapper listener) {
    logd("registerForCarrierRoamingNtnModeChanged: subId=" + subId);
    CarrierRoamingNtnModeListener internalListener = new CarrierRoamingNtnModeListener(listener);
    sCarrierRoamingNtnModeListenerWrapperMap.put(listener, internalListener);

    TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
    tm.registerTelephonyCallback(executor, internalListener);
  }

  public void registerForCarrierRoamingNtnModeChanged(int subId,
          @NonNull @CallbackExecutor Executor executor,
          @NonNull CarrierRoamingNtnModeListenerWrapper2 listener) {
    logd("registerForCarrierRoamingNtnModeChanged: subId=" + subId);
    CarrierRoamingNtnModeListener internalListener = new CarrierRoamingNtnModeListener(listener);
    sCarrierRoamingNtnModeListenerWrapperMap2.put(listener, internalListener);

    TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
    tm.registerTelephonyCallback(executor, internalListener);
  }

  /** Unregister for carrier roaming non-terrestrial network mode changes. */
  public void unregisterForCarrierRoamingNtnModeChanged(int subId,
          @NonNull CarrierRoamingNtnModeListenerWrapper listener) {
    logd("unregisterForCarrierRoamingNtnModeChanged: subId=" + subId);
    CarrierRoamingNtnModeListener internalListener =
            sCarrierRoamingNtnModeListenerWrapperMap.remove(listener);
    if (internalListener != null) {
      TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
      tm.unregisterTelephonyCallback(internalListener);
    }
  }

  /** Unregister for carrier roaming non-terrestrial network mode changes. */
  public void unregisterForCarrierRoamingNtnModeChanged(int subId,
          @NonNull CarrierRoamingNtnModeListenerWrapper2 listener) {
    logd("unregisterForCarrierRoamingNtnModeChanged: subId=" + subId);
    CarrierRoamingNtnModeListener internalListener =
            sCarrierRoamingNtnModeListenerWrapperMap.remove(listener);
    if (internalListener != null) {
      TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
      tm.unregisterTelephonyCallback(internalListener);
    }
  }

  /** Poll pending satellite datagrams over satellite. */
  public void pollPendingDatagrams(
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteResult @NonNull Consumer<Integer> resultListener) {
    if (mSatelliteManager == null) {
      logd("pollPendingDatagrams: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    mSatelliteManager.pollPendingDatagrams(executor, resultListener);
  }

  /**
   * Send datagram over satellite.
   *
   * <p>Gateway encodes SOS message or location sharing message into a datagram and passes it as
   * input to this method. Datagram received here will be passed down to modem without any encoding
   * or encryption.
   */
  public void sendDatagram(
      @DatagramType int datagramType,
      @NonNull SatelliteDatagramWrapper datagram,
      boolean needFullScreenPointingUI,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteResult @NonNull Consumer<Integer> resultListener) {
    if (mSatelliteManager == null) {
      logd("sendDatagram: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    SatelliteDatagram datagramInternal = new SatelliteDatagram(datagram.getSatelliteDatagram());
    mSatelliteManager.sendDatagram(
        datagramType, datagramInternal, needFullScreenPointingUI, executor, resultListener);
  }

  /** Request to get whether satellite communication is allowed for the current location. */
  public void requestIsCommunicationAllowedForCurrentLocation(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestIsCommunicationAllowedForCurrentLocation: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsCommunicationAllowedForCurrentLocation(
        executor, internalCallback);
  }

  /** Request to get satellite configuration for the current location. */
  public void requestSatelliteConfigurationForCurrentLocation(
          @NonNull @CallbackExecutor Executor executor,
          @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestSatelliteConfigurationForCurrentLocation: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
            new OutcomeReceiver<Boolean, SatelliteException>() {
              @Override
              public void onResult(Boolean result) {
                callback.onResult(result);
              }

              @Override
              public void onError(SatelliteException exception) {
                callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
              }
            };
    mSatelliteManager.requestSatelliteAccessConfigurationForCurrentLocation(executor,
            internalCallback);
  }

  /**
   * Request to get the duration in seconds after which the satellite will be visible. This will be
   * {@link Duration#ZERO} if the satellite is currently visible.
   */
  public void requestTimeForNextSatelliteVisibility(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Duration, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestTimeForNextSatelliteVisibility: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Duration, SatelliteException>() {
          @Override
          public void onResult(Duration result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestTimeForNextSatelliteVisibility(executor, internalCallback);
  }

  /**
   * Request to get the name to display for Satellite as a {@link String}.
   */
  public void requestSatelliteDisplayName(
          @NonNull @CallbackExecutor Executor executor,
          @NonNull OutcomeReceiver<String, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestSatelliteDisplayName: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
            new OutcomeReceiver<String, SatelliteException>() {
              @Override
              public void onResult(String result) {
                callback.onResult(result);
              }

              @Override
              public void onError(SatelliteException exception) {
                callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
              }
            };
    mSatelliteManager.requestSatelliteDisplayName(executor, internalCallback);
  }

  /**
   * Request to get the currently selected satellite subscription id as an {@link Integer}.
   */
  public void requestSelectedNbIotSatelliteSubscriptionId(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Integer, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestSelectedNbIotSatelliteSubscriptionId: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Integer, SatelliteException>() {
          @Override
          public void onResult(Integer result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestSelectedNbIotSatelliteSubscriptionId(executor, internalCallback);
  }

  /**
   * Inform whether the device is aligned with the satellite for demo mode.
   */
  public void setDeviceAlignedWithSatellite(boolean isAligned) {
    if (mSatelliteManager == null) {
      logd("setDeviceAlignedWithSatellite: mSatelliteManager is null");
      return;
    }

    mSatelliteManager.setDeviceAlignedWithSatellite(isAligned);
  }

  private Map<Integer, AntennaPositionWrapper> transformToAntennaPositionWrapperMap(
      Map<Integer, AntennaPosition> input) {
    Map<Integer, AntennaPositionWrapper> output = new HashMap<>();
    for (Map.Entry<Integer, AntennaPosition> entry : input.entrySet()) {
      AntennaPosition position = entry.getValue();

      output.put(
          entry.getKey(),
          new AntennaPositionWrapper(
              new AntennaDirectionWrapper(
                  position.getAntennaDirection().getX(),
                  position.getAntennaDirection().getY(),
                  position.getAntennaDirection().getZ()),
              position.getSuggestedHoldPosition()));
    }

    return output;
  }

  /** Request to get the signal strength of the satellite connection. */
  @FlaggedApi(Flags.FLAG_OEM_ENABLED_SATELLITE_FLAG)
  @NonNull
  public void requestNtnSignalStrength(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<NtnSignalStrengthWrapper, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestNtnSignalStrength: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
            new OutcomeReceiver<NtnSignalStrength, SatelliteException>() {
              @Override
              public void onResult(NtnSignalStrength result) {
                callback.onResult(new NtnSignalStrengthWrapper(result.getLevel()));
              }

              @Override
              public void onError(SatelliteException exception) {
                callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestNtnSignalStrength(executor, internalCallback);
  }

  /** Registers for NTN signal strength changed from satellite modem. */
  @FlaggedApi(Flags.FLAG_OEM_ENABLED_SATELLITE_FLAG)
  public void registerForNtnSignalStrengthChanged(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull NtnSignalStrengthCallbackWrapper callback) {
    if (mSatelliteManager == null){
      logd("registerForNtnSignalStrengthChanged: mSatelliteManager is null");
      return;
    }

    NtnSignalStrengthCallback internalCallback =
        new NtnSignalStrengthCallback() {
          @Override
          public void onNtnSignalStrengthChanged(@NonNull NtnSignalStrength ntnSignalStrength) {
            callback.onNtnSignalStrengthChanged(
                new NtnSignalStrengthWrapper(ntnSignalStrength.getLevel()));
          }
        };
    sNtnSignalStrengthCallbackWrapperMap.put(callback, internalCallback);
    mSatelliteManager.registerForNtnSignalStrengthChanged(executor, internalCallback);
  }

  /**
   * Unregisters for NTN signal strength changed from satellite modem.
   * If callback was not registered before, the request will be ignored.
   */
  @FlaggedApi(Flags.FLAG_OEM_ENABLED_SATELLITE_FLAG)
  public void unregisterForNtnSignalStrengthChanged(
      @NonNull NtnSignalStrengthCallbackWrapper callback) {
    if (mSatelliteManager == null){
      logd("unregisterForNtnSignalStrengthChanged: mSatelliteManager is null");
      return;
    }

    NtnSignalStrengthCallback internalCallback =
            sNtnSignalStrengthCallbackWrapperMap.remove(callback);
    if (internalCallback != null) {
      try {
        mSatelliteManager.unregisterForNtnSignalStrengthChanged(internalCallback);
      } catch (Exception ex) {
        throw ex;
      }
    }
  }

  /**
   * Wrapper API to provide a way to check if the subscription is exclusively for non-terrestrial
   * networks.
   *
   * @param subId The unique SubscriptionInfo key in database.
   * @return {@code true} if it is a non-terrestrial network subscription, {@code false}
   * otherwise.
   * Note: The method returns {@code false} if the parameter is invalid or any other error occurs.
   */
  @FlaggedApi(Flags.FLAG_OEM_ENABLED_SATELLITE_FLAG)
  public boolean isOnlyNonTerrestrialNetworkSubscription(int subId) {
    List<SubscriptionInfo> subInfoList = mSubscriptionManager.getAvailableSubscriptionInfoList();

    for (SubscriptionInfo subInfo : subInfoList) {
      if (subInfo.getSubscriptionId() == subId) {
        logd("found matched subscription info");
        return subInfo.isOnlyNonTerrestrialNetwork();
      }
    }
    logd("failed to found matched subscription info");
    return false;
  }

  /**
   * Wrapper API to register for satellite capabilities change event from the satellite service.
   *
   * @param executor The executor on which the callback will be called.
   * @param callback The callback to handle the satellite capabilities changed event.
   */
  @FlaggedApi(Flags.FLAG_OEM_ENABLED_SATELLITE_FLAG)
  public int registerForCapabilitiesChanged(
          @NonNull @CallbackExecutor Executor executor,
          @NonNull SatelliteCapabilitiesCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("requestForCapabilitiesChanged: mSatelliteManager is null");
      return SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED;
    }

    SatelliteCapabilitiesCallback internalCallback =
            capabilities -> callback.onSatelliteCapabilitiesChanged(
                    new SatelliteCapabilitiesWrapper(
                            capabilities.getSupportedRadioTechnologies(),
                            capabilities.isPointingRequired(),
                            capabilities.getMaxBytesPerOutgoingDatagram(),
                            transformToAntennaPositionWrapperMap(
                                    capabilities.getAntennaPositionMap())));
    sSatelliteCapabilitiesCallbackWrapperMap.put(callback, internalCallback);
    return mSatelliteManager.registerForCapabilitiesChanged(executor, internalCallback);
  }

  /**
   * Wrapper API to unregisters for satellite capabilities change event from the satellite service.
   * If callback was not registered before, the request will be ignored.
   *
   * @param callback The callback that was passed to.
   */
  @FlaggedApi(Flags.FLAG_OEM_ENABLED_SATELLITE_FLAG)
  public void unregisterForCapabilitiesChanged(
          @NonNull SatelliteCapabilitiesCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("unregisterForCapabilitiesChanged: mSatelliteManager is null");
      return;
    }

    SatelliteCapabilitiesCallback internalCallback =
            sSatelliteCapabilitiesCallbackWrapperMap.remove(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForCapabilitiesChanged(internalCallback);
    }
  }

  /**
   * Wrapper API to provide whether current network is non-terrestrial network or not.
   *
   * @param subId Subscription ID.
   *
   * @return {@code true} if current network is a non-terrestrial network, {@code false} otherwise.
   * Note: The method returns {@code false} if the no available network info or any other error
   * occurs.
   */
  public boolean isNonTerrestrialNetwork(int subId) {
    ServiceState ss = getServiceStateForSubscriptionId(subId);

    if (ss == null) {
      logd("isNonTerrestrialNetwork(): ServiceState is null, return");
      return false;
    }

    NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(DOMAIN_PS, TRANSPORT_TYPE_WWAN);
    if (nri == null) {
      logd("isNonTerrestrialNetwork(): NetworkRegistrationInfo is null, return");
      return false;
    }

    boolean isNonTerrestrialNetwork = nri.isNonTerrestrialNetwork();
    logd("isNonTerrestrialNetwork = " + isNonTerrestrialNetwork);
    return isNonTerrestrialNetwork;
  }

  /**
   * Wrapper API to provide the list of available services.
   *
   * @param subId Subscription ID.
   *
   * @return the list of available service types for given subscription ID.
   * Note: The method returns empty list if no service is available or any other error occurs.
   */
  @NonNull
  public List<Integer> getAvailableServices(int subId) {
    ServiceState ss = getServiceStateForSubscriptionId(subId);
    if (ss == null) {
      logd("getAvailableServices(): ServiceState is null, return");
      return new ArrayList<>();
    }

    NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(DOMAIN_PS, TRANSPORT_TYPE_WWAN);
    if (nri == null) {
      logd("getAvailableServices(): NetworkRegistrationInfo is null, return empty list");
      return new ArrayList<>();
    }

    List<Integer> serviceType = nri.getAvailableServices();
    logd("getAvailableServices() : serviceType=" + serviceType.stream().map(
            Object::toString).collect(Collectors.joining(", ")));
    return serviceType;
  }

  /**
   * Wrapper API to get whether device is connected to a non-terrestrial network.
   *
   * @param subId Subscription ID.
   *
   * @return {@code true} if device is connected to a non-terrestrial network, {@code false}
   * otherwise.
   * Note: The method returns {@code false} if the no available network info or any other error
   * occurs.
   */
  public boolean isUsingNonTerrestrialNetwork(int subId) {
    ServiceState ss = getServiceStateForSubscriptionId(subId);

    if (ss == null) {
      logd("isUsingNonTerrestrialNetwork(): ServiceState is null, return");
      return false;
    }

    boolean isUsingNonTerrestrialNetwork = ss.isUsingNonTerrestrialNetwork();
    logd("isUsingNonTerrestrialNetwork() returns " + isUsingNonTerrestrialNetwork);
    return isUsingNonTerrestrialNetwork;
  }

  /**
   * User request to enable or disable carrier supported satellite plmn scan and attach by modem.
   * <p>
   * This API should be called by only settings app to pass down the user input for
   * enabling/disabling satellite. This user input will be persisted across device reboots.
   * <p>
   * Satellite will be enabled only when the following conditions are met:
   * <ul>
   * <li>Users want to enable it.</li>
   * <li>There is no satellite communication restriction, which is added by
   * {@link #addAttachRestrictionForCarrier(int, int, Executor, Consumer)}</li>
   * <li>The carrier config {@link
   * android.telephony.CarrierConfigManager#KEY_SATELLITE_ATTACH_SUPPORTED_BOOL} is set to
   * {@code true}.</li>
   * </ul>
   *
   * @param subId The subscription ID of the carrier.
   * @param enableSatellite {@code true} to enable the satellite and {@code false} to disable.
   * @param executor The executor on which the error code listener will be called.
   * @param resultListener Listener for the {@link SatelliteResult} result of the operation.
   *
   * @throws SecurityException if the caller doesn't have required permission.
   * @throws IllegalArgumentException if the subscription is invalid.
   */
  public void requestAttachEnabledForCarrier(int subId, boolean enableSatellite,
          @NonNull @CallbackExecutor Executor executor,
          @SatelliteResult @NonNull Consumer<Integer> resultListener) {
    if (mSatelliteManager == null) {
      logd("requestAttachEnabledForCarrier: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    mSatelliteManager.requestAttachEnabledForCarrier(subId, enableSatellite, executor,
            resultListener);
  }

  /**
   * Request to get whether the carrier supported satellite plmn scan and attach by modem is
   * enabled by user.
   *
   * @param subId The subscription ID of the carrier.
   * @param executor The executor on which the callback will be called.
   * @param callback The callback object to which the result will be delivered.
   *                 If the request is successful, {@link OutcomeReceiver#onResult(Object)}
   *                 will return a {@code boolean} with value {@code true} if the satellite
   *                 is enabled and {@code false} otherwise.
   *                 If the request is not successful, {@link OutcomeReceiver#onError(Throwable)}
   *                 will return a {@link SatelliteExceptionWrapper} with the
   *                 {@link SatelliteResult}.
   *
   * @throws SecurityException if the caller doesn't have required permission.
   * @throws IllegalStateException if the Telephony process is not currently available.
   * @throws IllegalArgumentException if the subscription is invalid.
   */
  public void requestIsAttachEnabledForCarrier(int subId,
          @NonNull @CallbackExecutor Executor executor,
          @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestIsAttachEnabledForCarrier: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
            new OutcomeReceiver<Boolean, SatelliteException>() {
              @Override
              public void onResult(Boolean result) {
                callback.onResult(result);
              }

              @Override
              public void onError(SatelliteException exception) {
                callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
              }
            };
    mSatelliteManager.requestIsAttachEnabledForCarrier(subId, executor, internalCallback);
  }

  /**
   * Add a restriction reason for disallowing carrier supported satellite plmn scan and attach
   * by modem.
   *
   * @param subId The subscription ID of the carrier.
   * @param reason Reason for disallowing satellite communication.
   * @param executor The executor on which the error code listener will be called.
   * @param resultListener Listener for the {@link SatelliteResult} result of the
   * operation.
   *
   * @throws SecurityException if the caller doesn't have required permission.
   * @throws IllegalArgumentException if the subscription is invalid.
   */
  public void addAttachRestrictionForCarrier(int subId,
          @SatelliteCommunicationRestrictionReason int reason,
          @NonNull @CallbackExecutor Executor executor,
          @SatelliteResult @NonNull Consumer<Integer> resultListener) {
    if (mSatelliteManager == null) {
      logd("addAttachRestrictionForCarrier: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    mSatelliteManager.addAttachRestrictionForCarrier(subId, reason, executor, resultListener);
  }

  /**
   * Remove a restriction reason for disallowing carrier supported satellite plmn scan and attach
   * by modem.
   *
   * @param subId The subscription ID of the carrier.
   * @param reason Reason for disallowing satellite communication.
   * @param executor The executor on which the error code listener will be called.
   * @param resultListener Listener for the {@link SatelliteResult} result of the
   * operation.
   *
   * @throws SecurityException if the caller doesn't have required permission.
   * @throws IllegalArgumentException if the subscription is invalid.
   */
  public void removeAttachRestrictionForCarrier(int subId,
          @SatelliteCommunicationRestrictionReason int reason,
          @NonNull @CallbackExecutor Executor executor,
          @SatelliteResult @NonNull Consumer<Integer> resultListener) {
    if (mSatelliteManager == null) {
      logd("removeAttachRestrictionForCarrier: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> resultListener.accept(
              SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED)));
      return;
    }

    mSatelliteManager.removeAttachRestrictionForCarrier(subId, reason, executor, resultListener);
  }

  /**
   * Get reasons for disallowing satellite attach, as requested by
   * {@link #addAttachRestrictionForCarrier(int, int, Executor, Consumer)}
   *
   * @param subId The subscription ID of the carrier.
   * @return Set of reasons for disallowing satellite communication.
   *
   * @throws SecurityException if the caller doesn't have required permission.
   * @throws IllegalStateException if the Telephony process is not currently available.
   * @throws IllegalArgumentException if the subscription is invalid.
   */
  @SatelliteCommunicationRestrictionReason
  @NonNull public Set<Integer> getAttachRestrictionReasonsForCarrier(int subId) {
    if (mSatelliteManager == null) {
      logd("getAttachRestrictionReasonsForCarrier: mSatelliteManager is null");
      return Collections.emptySet();
    }

    return mSatelliteManager.getAttachRestrictionReasonsForCarrier(subId);
  }

  /**
   * Get all satellite PLMNs for which attach is enable for carrier.
   *
   * @param subId subId The subscription ID of the carrier.
   *
   * @return List of plmn for carrier satellite service. If no plmn is available, empty list will
   * be returned.
   */
  @NonNull public List<String> getSatellitePlmnsForCarrier(int subId) {
    if (mSatelliteManager == null) {
      logd("getSatellitePlmnsForCarrier: mSatelliteManager is null");
      return new ArrayList<>();
    }

    return mSatelliteManager.getSatellitePlmnsForCarrier(subId);
  }

  /** Registers for the satellite supported state changed. */
  @SatelliteResult
  public int registerForSupportedStateChanged(
          @NonNull @CallbackExecutor Executor executor,
          @NonNull SatelliteSupportedStateCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("registerForSupportedStateChanged: mSatelliteManager is null");
      return SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED;
    }

    SatelliteSupportedStateCallback internalCallback =
            new SatelliteSupportedStateCallback() {
              @Override
              public void onSatelliteSupportedStateChanged(boolean supported) {
                callback.onSatelliteSupportedStateChanged(supported);
              }
            };
    sSatelliteSupportedStateCallbackWrapperMap.put(callback, internalCallback);
    int result =
            mSatelliteManager.registerForSupportedStateChanged(executor, internalCallback);
    return result;
  }

  /** Request to get the {@link SatelliteSessionStatsWrapper} of the satellite service. */
  public void requestSessionStats(
          @NonNull @CallbackExecutor Executor executor,
          @NonNull OutcomeReceiver<SatelliteSessionStatsWrapper,
                  SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestSessionStats: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
            new OutcomeReceiver<SatelliteSessionStats, SatelliteException>() {
              @Override
              public void onResult(SatelliteSessionStats result) {
                SatelliteSessionStatsWrapper statsWrapper = new SatelliteSessionStatsWrapper
                        .Builder()
                        .setCountOfSuccessfulUserMessages(result.getCountOfSuccessfulUserMessages())
                        .setCountOfUnsuccessfulUserMessages(
                                result.getCountOfUnsuccessfulUserMessages())
                        .setCountOfTimedOutUserMessagesWaitingForConnection(
                                result.getCountOfTimedOutUserMessagesWaitingForConnection())
                        .setCountOfTimedOutUserMessagesWaitingForAck(
                                result.getCountOfTimedOutUserMessagesWaitingForAck())
                        .setCountOfUserMessagesInQueueToBeSent(
                                result.getCountOfUserMessagesInQueueToBeSent())
                        .build();
                callback.onResult(statsWrapper);
              }

              @Override
              public void onError(SatelliteException exception) {
                callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
              }
            };
    mSatelliteManager.requestSessionStats(executor, internalCallback);
  }

  /**
   * Unregisters for the satellite supported state changed. If callback was not registered before,
   * the request will be ignored.
   */
  public void unregisterForSupportedStateChanged(
          @NonNull SatelliteSupportedStateCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("unregisterForSupportedStateChanged: mSatelliteManager is null");
      return;
    }

    SatelliteSupportedStateCallback internalCallback =
            sSatelliteSupportedStateCallbackWrapperMap.remove(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForSupportedStateChanged(internalCallback);
    }
  }

  /** Registers for the satellite communication allowed state changed. */
  @SatelliteResult
  public int registerForCommunicationAllowedStateChanged(
          @NonNull @CallbackExecutor Executor executor,
          @NonNull SatelliteCommunicationAllowedStateCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("requestForCommunicationAllowedStateChanged: mSatelliteManager is null");
      return SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED;
    }

    SatelliteCommunicationAllowedStateCallback internalCallback =
            new SatelliteCommunicationAllowedStateCallback() {
              @Override
              public void onSatelliteCommunicationAllowedStateChanged(boolean supported) {
                callback.onSatelliteCommunicationAllowedStateChanged(supported);
              }
            };
    sSatelliteCommunicationAllowedStateCallbackWrapperMap.put(callback, internalCallback);
    int result = mSatelliteManager.registerForCommunicationAllowedStateChanged(executor,
            internalCallback);
    return result;
  }

  private List<SatelliteInfoWrapper> getSatelliteInfoListWrapper(
          List<SatelliteInfo> satelliteInfoList) {
      List<SatelliteInfoWrapper> satelliteInfoWrapperList = new ArrayList<>();
      for (SatelliteInfo info : satelliteInfoList) {
          SatellitePositionWrapper satellitePositionWrapperWrapper = null;
          if (info.getSatellitePosition() != null) {
              satellitePositionWrapperWrapper = new SatellitePositionWrapper(
                      info.getSatellitePosition().getLongitudeDegrees(),
                      info.getSatellitePosition().getAltitudeKm());
          }
          List<EarfcnRangeWrapper> earfcnRangeWrapperList = new ArrayList<>();
          for (EarfcnRange range : info.getEarfcnRanges()) {
              earfcnRangeWrapperList.add(new EarfcnRangeWrapper(
                      range.getStartEarfcn(), range.getEndEarfcn()));
          }
          SatelliteInfoWrapper satelliteInfoWrapper = new SatelliteInfoWrapper(
                  info.getSatelliteId(), satellitePositionWrapperWrapper,
                  info.getBands(), earfcnRangeWrapperList);

          satelliteInfoWrapperList.add(satelliteInfoWrapper);
      }
      return satelliteInfoWrapperList;
  }

  /** Registers for the satellite communication allowed state changed. */
  @SatelliteResult
  public int registerForCommunicationAllowedStateChanged2(
          @NonNull @CallbackExecutor Executor executor,
          @NonNull SatelliteCommunicationAllowedStateCallbackWrapper2 callback) {
    if (mSatelliteManager == null) {
      logd("registerForCommunicationAllowedStateChanged2: mSatelliteManager is null");
      return SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED;
    }

    SatelliteCommunicationAllowedStateCallback internalCallback =
            new SatelliteCommunicationAllowedStateCallback() {
              @Override
              public void onSatelliteCommunicationAllowedStateChanged(boolean supported) {
                callback.onSatelliteCommunicationAllowedStateChanged(supported);
              }

              @Override
              public void onSatelliteAccessConfigurationChanged(SatelliteAccessConfiguration
                      config) {
                if (config != null) {
                  callback.onSatelliteAccessConfigurationChanged(
                          new SatelliteAccessConfigurationWrapper(
                                  getSatelliteInfoListWrapper(config.getSatelliteInfos()),
                                  config.getTagIds()));
                }
              }
            };
    sSatelliteCommunicationAllowedStateCallbackWrapperMap2.put(callback, internalCallback);
    int result = mSatelliteManager.registerForCommunicationAllowedStateChanged(executor,
            internalCallback);
    return result;
  }

  /**
   * Unregisters for the satellite communication allowed state changed. If callback was not
   * registered before, the request will be ignored.
   */
  public void unregisterForCommunicationAllowedStateChanged(
          @NonNull SatelliteCommunicationAllowedStateCallbackWrapper callback) {
    if (mSatelliteManager == null) {
      logd("unregisterForCommunicationAllowedStateChanged: mSatelliteManager is null");
      return;
    }

    SatelliteCommunicationAllowedStateCallback internalCallback =
            sSatelliteCommunicationAllowedStateCallbackWrapperMap.remove(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForCommunicationAllowedStateChanged(internalCallback);
    }
  }

  /**
   * Unregisters for the satellite communication allowed state changed. If callback was not
   * registered before, the request will be ignored.
   */
  public void unregisterForCommunicationAllowedStateChanged2(
          @NonNull SatelliteCommunicationAllowedStateCallbackWrapper2 callback) {
    SatelliteCommunicationAllowedStateCallback internalCallback =
            sSatelliteCommunicationAllowedStateCallbackWrapperMap2.remove(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForCommunicationAllowedStateChanged(internalCallback);
    }
  }

  /**
   * Wrapper API to provide a way to check if the subscription is capable for non-terrestrial
   * networks for the carrier.
   *
   * @param subId The unique SubscriptionInfo key in database.
   * @return {@code true} if it is a non-terrestrial network capable subscription,
   * {@code false} otherwise.
   * Note: The method returns {@code false} if the parameter is invalid or any other error occurs.
   */
  @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
  public boolean isSatelliteESOSSupportedSubscription(int subId) {
    if (!mSubscriptionManager.isValidSubscriptionId(subId)) {
      return false;
    }

    List<SubscriptionInfo> subInfoList = mSubscriptionManager.getAvailableSubscriptionInfoList();
    for (SubscriptionInfo subInfo : subInfoList) {
      if (subInfo.getSubscriptionId() == subId) {
        logd("found matched subscription info");
        return subInfo.isSatelliteESOSSupported();
      }
    }
    logd("failed to found matched subscription info");
    return false;
  }

  /**
   * Request to get list of prioritized satellite subscriber ids to be used for provision.
   *
   * @param executor, The executor on which the callback will be called.
   * @param callback, The callback object to which the result will be delivered.
   * If successful, the callback returns a list of subscriberIds sorted in ascending priority
   * order index 0 has the highest priority. Otherwise, it returns an error with a
   * SatelliteException.
   */
  @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
  public void requestSatelliteSubscriberProvisionStatus(
          @NonNull @CallbackExecutor Executor executor,
          @NonNull OutcomeReceiver<List<SatelliteSubscriberProvisionStatusWrapper>,
                  SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("requestSatelliteSubscriberProvisionStatus: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    Objects.requireNonNull(executor);
    Objects.requireNonNull(callback);

    OutcomeReceiver internalCallback =
            new OutcomeReceiver<List<SatelliteSubscriberProvisionStatus>, SatelliteException>() {
              @Override
              public void onResult(List<SatelliteSubscriberProvisionStatus> result) {
                callback.onResult(transformToWrapperList(result));
              }

              @Override
              public void onError(SatelliteException exception) {
                callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
              }
            };
    mSatelliteManager.requestSatelliteSubscriberProvisionStatus(executor, internalCallback);
  }

  /**
   * Deliver the list of provisioned satellite subscriber ids.
   *
   * @param list List of SatelliteSubscriberInfo.
   * @param executor The executor on which the callback will be called.
   * @param callback The callback object to which the result will be delivered.
   */
  @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
  public void provisionSatellite(@NonNull List<SatelliteSubscriberInfoWrapper> list,
          @NonNull @CallbackExecutor Executor executor,
          @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("provisionSatellite: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
            new OutcomeReceiver<Boolean, SatelliteException>() {
              @Override
              public void onResult(Boolean result) {
                callback.onResult(result);
              }

              @Override
              public void onError(SatelliteException exception) {
                callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
              }
            };
    mSatelliteManager.provisionSatellite(list.stream()
            .map(info -> new SatelliteSubscriberInfo.Builder()
                    .setSubscriberId(info.getSubscriberId())
                    .setCarrierId(info.getCarrierId()).setNiddApn(info.getNiddApn())
                    .setSubId(info.getSubId()).setSubscriberIdType(info.getSubscriberIdType())
                    .build())
            .collect(Collectors.toList()), executor, internalCallback);
  }

  private List<SatelliteSubscriberProvisionStatusWrapper> transformToWrapperList(
          List<SatelliteSubscriberProvisionStatus> input) {
    List<SatelliteSubscriberProvisionStatusWrapper> output = new ArrayList<>();
    if (Flags.carrierRoamingNbIotNtn()) {
      for (SatelliteSubscriberProvisionStatus status : input) {
        SatelliteSubscriberInfo info = status.getSatelliteSubscriberInfo();
        output.add(new SatelliteSubscriberProvisionStatusWrapper.Builder()
                .setProvisionStatus(status.getProvisionStatus())
                .setSatelliteSubscriberInfo(
                        new SatelliteSubscriberInfoWrapper.Builder()
                                .setSubscriberId(info.getSubscriberId())
                                .setCarrierId(info.getCarrierId()).setNiddApn(info.getNiddApn())
                                .setSubId(info.getSubId())
                                .setSubscriberIdType(info.getSubscriberIdType())
                                .build()).build());
      }
    }
    return output;
  }

  public boolean isSatelliteSubscriberIdSupported() {
    if (mSatelliteManager == null) {
      logd("isSatelliteSubscriberIdSupported: mSatelliteManager is null");
      return false;
    }

    try {
      final String methodName = "requestSatelliteSubscriberProvisionStatus";
      Method method = mSatelliteManager.getClass().getMethod(methodName, Executor.class,
              OutcomeReceiver.class);
      return method != null;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  /**
   * Deliver the list of deprovisioned satellite subscriber ids.
   *
   * @param list List of deprovisioned SatelliteSubscriberInfo.
   * @param executor The executor on which the callback will be called.
   * @param callback The callback object to which the result will be delivered.
   */
  @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
  public void deprovisionSatellite(@NonNull List<SatelliteSubscriberInfoWrapper> list,
          @NonNull @CallbackExecutor Executor executor,
          @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    if (mSatelliteManager == null) {
      logd("deprovisionSatellite: mSatelliteManager is null");
      executor.execute(() -> Binder.withCleanCallingIdentity(() -> callback.onError(
              new SatelliteExceptionWrapper(
                      SatelliteManager.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED))));
      return;
    }

    OutcomeReceiver internalCallback =
            new OutcomeReceiver<Boolean, SatelliteException>() {
              @Override
              public void onResult(Boolean result) {
                callback.onResult(result);
              }

              @Override
              public void onError(SatelliteException exception) {
                callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
              }
            };
    mSatelliteManager.deprovisionSatellite(list.stream()
            .map(info -> new SatelliteSubscriberInfo.Builder()
                    .setSubscriberId(info.getSubscriberId())
                    .setCarrierId(info.getCarrierId()).setNiddApn(info.getNiddApn())
                    .setSubId(info.getSubId()).setSubscriberIdType(info.getSubscriberIdType())
                    .build())
            .collect(Collectors.toList()), executor, internalCallback);
  }

  /**
   * Inform whether application supports NTN SMS in satellite mode.
   *
   * This method is used by default messaging application to inform framework whether it supports
   * NTN SMS or not.
   *
   * @param ntnSmsSupported {@code true} If application supports NTN SMS, else {@code false}.
   */
  public void setNtnSmsSupported(boolean ntnSmsSupported) {
    if (mSatelliteManager == null) {
      logd("setNtnSmsSupported: mSatelliteManager is null");
      return;
    }

    mSatelliteManager.setNtnSmsSupported(ntnSmsSupported);
  }

  @Nullable
  private ServiceState getServiceStateForSubscriptionId(int subId) {
    if (!mSubscriptionManager.isValidSubscriptionId(subId)) {
      return null;
    }

    TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
    return tm.getServiceState();
  }

  private void logd(String message) {
    Rlog.d(TAG, message);
  }
}
