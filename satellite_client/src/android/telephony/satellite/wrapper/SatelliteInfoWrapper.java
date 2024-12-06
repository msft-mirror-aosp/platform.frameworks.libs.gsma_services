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

import android.annotation.Nullable;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * SatelliteInfoWrapper stores a satellite's identification, position, and frequency information
 * facilitating efficient satellite communications.
 *
 * @hide
 */
public class SatelliteInfoWrapper implements Parcelable {
    /**
     * Unique identification number for the satellite.
     * This ID is used to distinguish between different satellites in the network.
     */
    @NonNull
    private UUID mId;

    /**
     * Position information of a geostationary satellite.
     * This includes the longitude and altitude of the satellite.
     * If the SatellitePosition is invalid,
     * longitudeDegree and altitudeKm will be represented as DOUBLE.NaN.
     */
    @NonNull
    private SatellitePositionWrapper mPosition;

    /**
     * The frequency band list to scan. Bands and earfcns won't overlap.
     * Bands will be filled only if the whole band is needed.
     * Maximum length of the vector is 8.
     */
    private List<Integer> mBandList;

    /**
     * EARFCN (E-UTRA Absolute Radio Frequency Channel Number) range list
     * The supported frequency range list.
     * Maximum length of the vector is 8.
     */
    private final List<EarfcnRangeWrapper> mEarfcnRangeList;

    protected SatelliteInfoWrapper(Parcel in) {
        ParcelUuid parcelUuid = in.readParcelable(
                ParcelUuid.class.getClassLoader(), ParcelUuid.class);
        if (parcelUuid != null) {
            mId = parcelUuid.getUuid();
        }
        mPosition = in.readParcelable(SatellitePositionWrapper.class.getClassLoader(),
                SatellitePositionWrapper.class);
        mBandList = new ArrayList<>();
        in.readList(mBandList, Integer.class.getClassLoader(), Integer.class);
        mEarfcnRangeList = in.createTypedArrayList(EarfcnRangeWrapper.CREATOR);
    }

    /**
     * Constructor for {@link SatelliteInfoWrapper}.
     *
     * @param satelliteId       The ID of the satellite.
     * @param satellitePosition The {@link SatellitePositionWrapper} of the satellite.
     * @param bandList          The list of frequency bandList supported by the satellite.
     * @param earfcnRanges      The list of {@link EarfcnRangeWrapper} objects representing the
     *                          EARFCN ranges supported by the satellite.
     */
    public SatelliteInfoWrapper(@NonNull UUID satelliteId,
            @NonNull SatellitePositionWrapper satellitePosition,
            @NonNull List<Integer> bandList, @NonNull List<EarfcnRangeWrapper> earfcnRanges) {
        mId = satelliteId;
        mPosition = satellitePosition;
        mBandList = bandList;
        mEarfcnRangeList = earfcnRanges;
    }

    public static final Parcelable.Creator<SatelliteInfoWrapper>
            CREATOR = new Parcelable.Creator<SatelliteInfoWrapper>() {
        @Override
        public SatelliteInfoWrapper createFromParcel(Parcel in) {
            return new SatelliteInfoWrapper(in);
        }

        @Override
        public SatelliteInfoWrapper[] newArray(int size) {
            return new SatelliteInfoWrapper[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(new ParcelUuid(mId), flags);
        dest.writeParcelable(mPosition, flags);
        dest.writeList(mBandList);
        dest.writeTypedList(mEarfcnRangeList);
    }

    /**
     * Returns the ID of the satellite.
     *
     * @return The satellite ID.
     */
    @NonNull
    public UUID getSatelliteId() {
        return mId;
    }

    /**
     * Returns the position of the satellite.
     *
     * @return The {@link SatellitePositionWrapper} of the satellite.
     */
    @NonNull
    public SatellitePositionWrapper getSatellitePosition() {
        return mPosition;
    }

    /**
     * Returns the list of frequency bands supported by the satellite.
     *
     * @return The list of frequency bands.
     */
    @NonNull
    public List<Integer> getBands() {
        return mBandList;
    }

    /**
     * Returns the list of EARFCN ranges supported by the satellite.
     *
     * @return The list of {@link EarfcnRangeWrapper} objects.
     */
    @NonNull
    public List<EarfcnRangeWrapper> getEarfcnRanges() {
        return mEarfcnRangeList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SatelliteInfoWrapper that)) return false;

        return mId.equals(that.mId)
                && Objects.equals(mPosition, that.mPosition)
                && Objects.equals(mBandList, that.mBandList)
                && mEarfcnRangeList.equals(that.mEarfcnRangeList);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mId, mPosition, mEarfcnRangeList);
        result = 31 * result + Objects.hashCode(mBandList);
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SatelliteInfoWrapper{");
        sb.append("mId=").append(mId);
        sb.append(", mPosition=").append(mPosition);
        sb.append(", mBandList=").append(mBandList);
        sb.append(", mEarfcnRangeList=").append(mEarfcnRangeList);
        sb.append('}');
        return sb.toString();
    }
}
