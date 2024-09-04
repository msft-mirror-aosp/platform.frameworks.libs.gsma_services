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
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.telephony.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

@FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
public class SatelliteSubscriberInfoWrapper implements Parcelable {
    @NonNull private final String mSubscriberId;
    @NonNull private final int mCarrierId;
    @NonNull private final String mNiddApn;
    @NonNull private int mSubId;

    /** SubscriberId format is the ICCID. */
    @FlaggedApi(Flags.FLAG_OEM_ENABLED_SATELLITE_FLAG)
    public static final int ICCID = 0;
    /** SubscriberId format is the 6 digit of IMSI + MSISDN. */
    @FlaggedApi(Flags.FLAG_OEM_ENABLED_SATELLITE_FLAG)
    public static final int IMSI_MSISDN = 1;

    /** Type of subscriber id */
    @SubscriberIdType
    @NonNull private int mSubscriberIdType;

    /** @hide */
    @IntDef(prefix = "SubscriberId_Type_", value = {
            ICCID,
            IMSI_MSISDN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SubscriberIdType {
    }

    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public SatelliteSubscriberInfoWrapper(@NonNull Builder builder) {
        this.mSubscriberId = builder.mSubscriberId;
        this.mCarrierId = builder.mCarrierId;
        this.mNiddApn = builder.mNiddApn;
        this.mSubId = builder.mSubId;
        this.mSubscriberIdType = builder.mSubscriberIdType;
    }

    /**
     * Builder class for constructing SatelliteSubscriberInfoWrapper objects
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public static class Builder {
        @NonNull private String mSubscriberId;
        @NonNull private int mCarrierId;
        @NonNull private String mNiddApn;
        @NonNull private int mSubId;
        @NonNull @SubscriberIdType private int mSubscriberIdType;

        /**
         * Set the SubscriberId and returns the Builder class.
         *
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
        @NonNull
        public Builder setSubscriberId(String subscriberId) {
            mSubscriberId = subscriberId;
            return this;
        }

        /**
         * Set the CarrierId and returns the Builder class.
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
        @NonNull
        public Builder setCarrierId(int carrierId) {
            mCarrierId = carrierId;
            return this;
        }

        /**
         * Set the niddApn and returns the Builder class.
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
        @NonNull
        public Builder setNiddApn(String niddApn) {
            mNiddApn = niddApn;
            return this;
        }

        /**
         * Set the subId and returns the Builder class.
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
        @NonNull
        public Builder setSubId(int subId) {
            mSubId = subId;
            return this;
        }

        /**
         * Set the SubscriberIdType and returns the Builder class.
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
        @NonNull
        public Builder setSubscriberIdType(@SubscriberIdType int subscriberIdType) {
            mSubscriberIdType = subscriberIdType;
            return this;
        }

        /**
         * Returns SatelliteSubscriberInfoWrapper object.
         * @hide
         */
        @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
        @NonNull
        public SatelliteSubscriberInfoWrapper build() {
            return new SatelliteSubscriberInfoWrapper(this);
        }
    }

    private SatelliteSubscriberInfoWrapper(Parcel in) {
        mSubscriberId = in.readString();
        mCarrierId = in.readInt();
        mNiddApn = in.readString();
        mSubId = in.readInt();
        mSubscriberIdType = in.readInt();
    }

    /**
     * @hide
     */
    @Override
    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeString(mSubscriberId);
        out.writeInt(mCarrierId);
        out.writeString(mNiddApn);
        out.writeInt(mSubId);
        out.writeInt(mSubscriberIdType);
    }

    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    public static final @android.annotation.NonNull Creator<SatelliteSubscriberInfoWrapper>
            CREATOR =
            new Creator<SatelliteSubscriberInfoWrapper>() {
                @Override
                public SatelliteSubscriberInfoWrapper createFromParcel(Parcel in) {
                    return new SatelliteSubscriberInfoWrapper(in);
                }

                @Override
                public SatelliteSubscriberInfoWrapper[] newArray(int size) {
                    return new SatelliteSubscriberInfoWrapper[size];
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

    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    @NonNull
    public String getSubscriberId() {
        return mSubscriberId;
    }

    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    @NonNull
    public int getCarrierId() {
        return mCarrierId;
    }

    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    @NonNull
    public String getNiddApn() {
        return mNiddApn;
    }

    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    @NonNull
    public int getSubId() {
        return mSubId;
    }

    @FlaggedApi(Flags.FLAG_CARRIER_ROAMING_NB_IOT_NTN)
    @NonNull
    public @SubscriberIdType int getSubscriberIdType() {
        return mSubscriberIdType;
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SubscriberId:");
        sb.append(mSubscriberId);
        sb.append(",");

        sb.append("carrierId:");
        sb.append(mCarrierId);
        sb.append(",");

        sb.append("niddApn:");
        sb.append(mNiddApn);
        sb.append(",");

        sb.append("SubId:");
        sb.append(mSubId);
        sb.append(",");

        sb.append("SubscriberIdType:");
        sb.append(mSubscriberIdType);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SatelliteSubscriberInfoWrapper)) return false;
        SatelliteSubscriberInfoWrapper that = (SatelliteSubscriberInfoWrapper) o;
        return Objects.equals(mSubscriberId, that.mSubscriberId)
                && mCarrierId == that.mCarrierId && Objects.equals(mNiddApn, that.mNiddApn)
                && mSubId == that.mSubId && mSubscriberIdType == that.mSubscriberIdType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSubscriberId, mCarrierId, mNiddApn, mSubId, mSubscriberIdType);
    }
}
