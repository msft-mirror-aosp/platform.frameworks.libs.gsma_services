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

import android.annotation.NonNull;

import java.util.List;

/** A callback class for monitoring satellite provision state change events. */
public interface SatelliteProvisionStateCallbackWrapper {
  /**
   * Called when satellite provision state changes.
   *
   * @param provisioned The new provision state. {@code true} means satellite is provisioned {@code
   *     false} means satellite is not provisioned.
   */
  void onSatelliteProvisionStateChanged(boolean provisioned);

  /**
   * Called when the provisioning state of one or more SatelliteSubscriberInfos changes.
   *
   * @param satelliteSubscriberProvisionStatus The list contains the latest provisioning states
   *                                           of the SatelliteSubscriberInfos.
   */
  default void onSatelliteSubscriptionProvisionStateChanged(@NonNull
        List<SatelliteSubscriberProvisionStatusWrapper> satelliteSubscriberProvisionStatus) {}
}
