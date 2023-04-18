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

import static android.telephony.satellite.SatelliteManager.DatagramType;
import static android.telephony.satellite.SatelliteManager.SatelliteDatagramTransferState;
import static android.telephony.satellite.SatelliteManager.SatelliteError;
import static android.telephony.satellite.SatelliteManager.SatelliteException;
import static android.telephony.satellite.SatelliteManager.SatelliteModemState;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.telephony.satellite.PointingInfo;
import android.telephony.satellite.SatelliteCapabilities;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteDatagramCallback;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.SatelliteProvisionStateCallback;
import android.telephony.satellite.SatelliteStateCallback;
import android.telephony.satellite.SatelliteTransmissionUpdateCallback;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Wrapper for satellite operations such as provisioning, pointing, messaging, location sharing,
 * etc. To get the object, call {@link Context#getSystemService(String)}.
 */
public class SatelliteManagerWrapper {
  private static final String TAG = "SatelliteManagerWrapper";

  private static final ConcurrentHashMap<
          SatelliteProvisionStateCallbackWrapper, SatelliteProvisionStateCallback>
      sSatelliteProvisionStateCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<SatelliteStateCallbackWrapper, SatelliteStateCallback>
      sSatelliteStateCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<
          SatelliteTransmissionUpdateCallbackWrapper, SatelliteTransmissionUpdateCallback>
      sSatelliteTransmissionUpdateCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<
          SatelliteDatagramCallbackWrapper, SatelliteDatagramCallback>
      sSatelliteDatagramCallbackWrapperMap = new ConcurrentHashMap<>();

  private final SatelliteManager mSatelliteManager;

  SatelliteManagerWrapper(Context context) {
    mSatelliteManager = (SatelliteManager) context.getSystemService("satellite");
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
   * Request to enable or disable the satellite modem and demo mode. If the satellite modem is
   * enabled, this may also disable the cellular modem, and if the satellite modem is disabled, this
   * may also re-enable the cellular modem.
   */
  public void requestSatelliteEnabled(
      boolean enableSatellite,
      boolean enableDemoMode,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    mSatelliteManager.requestSatelliteEnabled(
        enableSatellite, enableDemoMode, executor, resultListener);
  }

  /** Request to get whether the satellite modem is enabled. */
  public void requestIsSatelliteEnabled(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteException> callback) {
    mSatelliteManager.requestIsSatelliteEnabled(executor, callback);
  }

  /** Request to get whether the satellite service demo mode is enabled. */
  public void requestIsDemoModeEnabled(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteException> callback) {
    mSatelliteManager.requestIsDemoModeEnabled(executor, callback);
  }

  /** Request to get whether the satellite service is supported on the device. */
  public void requestIsSatelliteSupported(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteException> callback) {
    mSatelliteManager.requestIsSatelliteSupported(executor, callback);
  }

  /** Request to get the {@link SatelliteCapabilities} of the satellite service. */
  public void requestSatelliteCapabilities(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<SatelliteCapabilities, SatelliteException> callback) {
    mSatelliteManager.requestSatelliteCapabilities(executor, callback);
  }

  /**
   * Start receiving satellite transmission updates. This can be called by the pointing UI when the
   * user starts pointing to the satellite. Modem should continue to report the pointing input as
   * the device or satellite moves. Satellite transmission updates are started only on {@link
   * #SATELLITE_ERROR_NONE}. All other results indicate that this operation failed. Once satellite
   * transmission updates begin, position and datagram transfer state updates will be sent through
   * {@link SatelliteTransmissionUpdateCallback}.
   */
  public void startSatelliteTransmissionUpdates(
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener,
      @NonNull SatelliteTransmissionUpdateCallbackWrapper callback) {

    SatelliteTransmissionUpdateCallback internalCallback =
        new SatelliteTransmissionUpdateCallback() {

          @Override
          public void onSendDatagramStateChanged(
              @SatelliteDatagramTransferState int state,
              int sendPendingCount,
              @SatelliteError int errorCode) {
            callback.onSendDatagramStateChanged(state, sendPendingCount, errorCode);
          }

          @Override
          public void onReceiveDatagramStateChanged(
              @SatelliteDatagramTransferState int state,
              int receivePendingCount,
              @SatelliteError int errorCode) {
            callback.onReceiveDatagramStateChanged(state, receivePendingCount, errorCode);
          }

          @Override
          public void onSatellitePositionChanged(@NonNull PointingInfo pointingInfo) {
            callback.onSatellitePositionChanged(pointingInfo);
          }
        };
    sSatelliteTransmissionUpdateCallbackWrapperMap.put(callback, internalCallback);

    mSatelliteManager.startSatelliteTransmissionUpdates(executor, resultListener, internalCallback);
  }

  /**
   * Stop receiving satellite transmission updates. This can be called by the pointing UI when the
   * user stops pointing to the satellite. Satellite transmission updates are stopped and the
   * callback is unregistered only on {@link #SATELLITE_ERROR_NONE}. All other results that this
   * operation failed.
   */
  public void stopSatelliteTransmissionUpdates(
      @NonNull SatelliteTransmissionUpdateCallbackWrapper callback,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    SatelliteTransmissionUpdateCallback internalCallback =
        sSatelliteTransmissionUpdateCallbackWrapperMap.get(callback);
    if (internalCallback != null) {
      mSatelliteManager.stopSatelliteTransmissionUpdates(
          internalCallback, executor, resultListener);
    }
  }

  /**
   * Provision the device with a satellite provider. This is needed if the provider allows dynamic
   * registration.
   */
  public void provisionSatelliteService(
      @NonNull String token,
      @NonNull byte[] provisionData,
      @Nullable CancellationSignal cancellationSignal,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    mSatelliteManager.provisionSatelliteService(
        token, provisionData, cancellationSignal, executor, resultListener);
  }

  /**
   * Deprovision the device with the satellite provider. This is needed if the provider allows
   * dynamic registration. Once deprovisioned, {@link
   * SatelliteProvisionStateCallback#onSatelliteProvisionStateChanged(boolean)} should report as
   * deprovisioned.
   */
  public void deprovisionSatelliteService(
      @NonNull String token,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    mSatelliteManager.deprovisionSatelliteService(token, executor, resultListener);
  }

  /** Registers for the satellite provision state changed. */
  @SatelliteError
  public int registerForSatelliteProvisionStateChanged(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull SatelliteProvisionStateCallbackWrapper callback) {
    SatelliteProvisionStateCallback internalCallback =
        new SatelliteProvisionStateCallback() {
          @Override
          public void onSatelliteProvisionStateChanged(boolean provisioned) {
            callback.onSatelliteProvisionStateChanged(provisioned);
          }
        };
    sSatelliteProvisionStateCallbackWrapperMap.put(callback, internalCallback);
    int result =
        mSatelliteManager.registerForSatelliteProvisionStateChanged(executor, internalCallback);
    return result;
  }

  /**
   * Unregisters for the satellite provision state changed. If callback was not registered before,
   * the request will be ignored.
   */
  public void unregisterForSatelliteProvisionStateChanged(
      @NonNull SatelliteProvisionStateCallbackWrapper callback) {
    SatelliteProvisionStateCallback internalCallback =
        sSatelliteProvisionStateCallbackWrapperMap.get(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForSatelliteProvisionStateChanged(internalCallback);
    }
  }

  /** Request to get whether this device is provisioned with a satellite provider. */
  public void requestIsSatelliteProvisioned(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteException> callback) {
    mSatelliteManager.requestIsSatelliteProvisioned(executor, callback);
  }

  /** Registers for modem state changed from satellite modem. */
  @SatelliteError
  public int registerForSatelliteModemStateChanged(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull SatelliteStateCallbackWrapper callback) {
    SatelliteStateCallback internalCallback =
        new SatelliteStateCallback() {
          public void onSatelliteModemStateChanged(@SatelliteModemState int state) {
            callback.onSatelliteModemStateChanged(state);
          }
        };
    sSatelliteStateCallbackWrapperMap.put(callback, internalCallback);

    int result =
        mSatelliteManager.registerForSatelliteModemStateChanged(executor, internalCallback);
    return result;
  }

  /**
   * Unregisters for modem state changed from satellite modem. If callback was not registered
   * before, the request will be ignored.
   */
  public void unregisterForSatelliteModemStateChanged(
      @NonNull SatelliteStateCallbackWrapper callback) {
    SatelliteStateCallback internalCallback = sSatelliteStateCallbackWrapperMap.get(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForSatelliteModemStateChanged(internalCallback);
    }
  }

  /** Register to receive incoming datagrams over satellite. */
  @SatelliteError
  public int registerForSatelliteDatagram(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull SatelliteDatagramCallbackWrapper callback) {
    SatelliteDatagramCallback internalCallback =
        new SatelliteDatagramCallback() {
          @Override
          public void onSatelliteDatagramReceived(
              long datagramId,
              @NonNull SatelliteDatagram datagram,
              int pendingCount,
              @NonNull Consumer<Void> internalCallback) {
            callback.onSatelliteDatagramReceived(
                datagramId, datagram, pendingCount, internalCallback);
          }
        };
    sSatelliteDatagramCallbackWrapperMap.put(callback, internalCallback);
    int result = mSatelliteManager.registerForSatelliteDatagram(executor, internalCallback);
    return result;
  }

  /**
   * Unregister to stop receiving incoming datagrams over satellite. If callback was not registered
   * before, the request will be ignored.
   */
  public void unregisterForSatelliteDatagram(@NonNull SatelliteDatagramCallbackWrapper callback) {
    SatelliteDatagramCallback internalCallback = sSatelliteDatagramCallbackWrapperMap.get(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForSatelliteDatagram(internalCallback);
    }
  }

  /** Poll pending satellite datagrams over satellite. */
  public void pollPendingSatelliteDatagrams(
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    mSatelliteManager.pollPendingSatelliteDatagrams(executor, resultListener);
  }

  /**
   * Send datagram over satellite.
   *
   * <p>Gateway encodes SOS message or location sharing message into a datagram and passes it as
   * input to this method. Datagram received here will be passed down to modem without any encoding
   * or encryption.
   */
  public void sendSatelliteDatagram(
      @DatagramType int datagramType,
      @NonNull SatelliteDatagram datagram,
      boolean needFullScreenPointingUI,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    mSatelliteManager.sendSatelliteDatagram(
        datagramType, datagram, needFullScreenPointingUI, executor, resultListener);
  }

  /** Request to get whether satellite communication is allowed for the current location. */
  public void requestIsSatelliteCommunicationAllowedForCurrentLocation(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteException> callback) {
    mSatelliteManager.requestIsSatelliteCommunicationAllowedForCurrentLocation(executor, callback);
  }

  /**
   * Request to get the duration in seconds after which the satellite will be visible. This will be
   * {@link Duration#ZERO} if the satellite is currently visible.
   */
  public void requestTimeForNextSatelliteVisibility(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Duration, SatelliteException> callback) {
    mSatelliteManager.requestTimeForNextSatelliteVisibility(executor, callback);
  }
}