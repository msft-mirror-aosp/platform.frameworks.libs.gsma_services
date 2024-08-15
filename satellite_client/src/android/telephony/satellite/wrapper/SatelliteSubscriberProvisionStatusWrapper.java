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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.telephony.flags.Flags;

import java.util.Objects;

@FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
public class SatelliteSubscriberProvisionStatusWrapper implements Parcelable {
    @NonNull
    private SatelliteSubscriberInfoWrapper mSubscriberInfo;
    /** {@code true} mean the satellite subscriber is provisioned, {@code false} otherwise. */
    private boolean mProvisionStatus;

    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public SatelliteSubscriberProvisionStatusWrapper(@NonNull Builder builder) {
        mSubscriberInfo = builder.mSubscriberInfo;
        mProvisionStatus = builder.mProvisionStatus;
    }

    private SatelliteSubscriberProvisionStatusWrapper(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Builder class for constructing SatelliteSubscriberProvisionStatusWrapper objects
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public static class Builder {
        private SatelliteSubscriberInfoWrapper mSubscriberInfo;
        private boolean mProvisionStatus;

        /**
         * Set the SatelliteSubscriberInfo and returns the Builder class.
         *
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
        @NonNull
        public Builder setSatelliteSubscriberInfo(
                SatelliteSubscriberInfoWrapper satelliteSubscriberInfo) {
            mSubscriberInfo = satelliteSubscriberInfo;
            return this;
        }

        /**
         * Set the SatelliteSubscriberInfo's provisionStatus and returns the Builder class.
         *
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
        @NonNull
        public Builder setProvisionStatus(boolean provisionStatus) {
            mProvisionStatus = provisionStatus;
            return this;
        }

        /**
         * Returns SatelliteSubscriberProvisionStatus object.
         *
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
        @NonNull
        public SatelliteSubscriberProvisionStatusWrapper build() {
            return new SatelliteSubscriberProvisionStatusWrapper(this);
        }
    }

    /**
     * @hide
     */
    @Override
    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeParcelable(mSubscriberInfo, flags);
        out.writeBoolean(mProvisionStatus);
    }

    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public static final @android.annotation.NonNull
            Creator<SatelliteSubscriberProvisionStatusWrapper> CREATOR =
            new Creator<SatelliteSubscriberProvisionStatusWrapper>() {
                @Override
                public SatelliteSubscriberProvisionStatusWrapper createFromParcel(Parcel in) {
                    return new SatelliteSubscriberProvisionStatusWrapper(in);
                }

                @Override
                public SatelliteSubscriberProvisionStatusWrapper[] newArray(int size) {
                    return new SatelliteSubscriberProvisionStatusWrapper[size];
                }
            };

    /**
     * @hide
     */
    @Override
    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public int describeContents() {
        return 0;
    }

    /**
     * SatelliteSubscriberInfo that has a provisioning state.
     *
     * @return SatelliteSubscriberInfo.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public @NonNull SatelliteSubscriberInfoWrapper getSatelliteSubscriberInfo() {
        return mSubscriberInfo;
    }

    /**
     * SatelliteSubscriberInfo's provisioning state.
     *
     * @return {@code true} means provisioning. {@code false} means deprovisioning.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public @NonNull boolean getProvisionStatus() {
        return mProvisionStatus;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("SatelliteSubscriberInfoWrapper:");
        sb.append(mSubscriberInfo);
        sb.append(",");

        sb.append("ProvisionStatus:");
        sb.append(mProvisionStatus);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSubscriberInfo, mProvisionStatus);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SatelliteSubscriberProvisionStatusWrapper)) return false;
        SatelliteSubscriberProvisionStatusWrapper that =
                (SatelliteSubscriberProvisionStatusWrapper) o;
        return Objects.equals(mSubscriberInfo, that.mSubscriberInfo)
                && mProvisionStatus == that.mProvisionStatus;
    }

    private void readFromParcel(Parcel in) {
        mSubscriberInfo = in.readParcelable(SatelliteSubscriberInfoWrapper.class.getClassLoader());
        mProvisionStatus = in.readBoolean();
    }
}
