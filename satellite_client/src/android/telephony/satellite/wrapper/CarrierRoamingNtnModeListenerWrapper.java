/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;

/** Interface for carrier roaming non-terrestrial network listener. */
public interface CarrierRoamingNtnModeListenerWrapper {
    /**
     * Callback invoked when carrier roaming non-terrestrial network mode changes.
     *
     * @param active {@code true} If the device is connected to carrier roaming
     *                           non-terrestrial network or was connected within the
     *                           {CarrierConfigManager
     *                           #KEY_SATELLITE_CONNECTION_HYSTERESIS_SEC_INT} duration,
     *                           {code false} otherwise.
     */
    void onCarrierRoamingNtnModeChanged(boolean active);

    /**
     * Callback invoked when eligibility to connect to carrier roaming non-terrestrial network
     * changes.
     *
     * @param eligible {@code true} when the device is eligible for satellite
     * communication if all the following conditions are met:
     * <ul>
     * <li>Any subscription on the device supports P2P satellite messaging which is defined by
     * {@link CarrierConfigManager#KEY_SATELLITE_ATTACH_SUPPORTED_BOOL} </li>
     * <li>{@link CarrierConfigManager#KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT} set to
     * {@link CarrierConfigManager#CARRIER_ROAMING_NTN_CONNECT_MANUAL} </li>
     * <li>The device is in {@link ServiceState#STATE_OUT_OF_SERVICE}, not connected to Wi-Fi,
     * and the hysteresis timer defined by {@link CarrierConfigManager
     * #KEY_CARRIER_SUPPORTED_SATELLITE_NOTIFICATION_HYSTERESIS_SEC_INT} is expired. </li>
     * </ul>
     */
    default void onCarrierRoamingNtnEligibleStateChanged(boolean eligible) {}
}
