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
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.android.internal.telephony.flags.Flags;

import java.util.Objects;

/**
 * The position of a satellite in Earth orbit.
 *
 * Longitude is the angular distance, measured in degrees, east or west of the prime longitude line
 * ranging from -180 to 180 degrees
 * Altitude is the distance from the center of the Earth to the satellite, measured in kilometers
 *
 * @hide
 */
public class SatellitePositionWrapper implements Parcelable {

    /**
     * The longitude of the satellite in degrees, ranging from -180 to 180 degrees
     */
    private double mLongitudeDegree;

    /**
     * The distance from the center of the earth to the satellite, measured in kilometers
     */
    private double mAltitudeKm;

    /**
     * Constructor for {@link SatellitePositionWrapper} used to create an instance from a
     * {@link Parcel}.
     *
     * @param in The {@link Parcel} to read the satellite position data from.
     */
    public SatellitePositionWrapper(Parcel in) {
        mLongitudeDegree = in.readDouble();
        mAltitudeKm = in.readDouble();
    }

    /**
     * Constructor for {@link SatellitePositionWrapper}.
     *
     * @param longitudeDegree The longitude of the satellite in degrees.
     * @param altitudeKm      The altitude of the satellite in kilometers.
     */
    public SatellitePositionWrapper(double longitudeDegree, double altitudeKm) {
        mLongitudeDegree = longitudeDegree;
        mAltitudeKm = altitudeKm;
    }

    public static final Creator<SatellitePositionWrapper> CREATOR =
            new Creator<SatellitePositionWrapper>() {
                @Override
                public SatellitePositionWrapper createFromParcel(Parcel in) {
                    return new SatellitePositionWrapper(in);
                }

                @Override
                public SatellitePositionWrapper[] newArray(int size) {
                    return new SatellitePositionWrapper[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeDouble(mLongitudeDegree);
        dest.writeDouble(mAltitudeKm);
    }

    /**
     * Returns the longitude of the satellite in degrees, ranging from -180 to 180 degrees.
     *
     * @return The longitude of the satellite.
     */
    public double getLongitudeDegrees() {
        return mLongitudeDegree;
    }

    /**
     * Returns the altitude of the satellite in kilometers
     *
     * @return The altitude of the satellite.
     */
    public double getAltitudeKm() {
        return mAltitudeKm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SatellitePositionWrapper that)) return false;

        return Double.compare(that.mLongitudeDegree, mLongitudeDegree) == 0
                && Double.compare(that.mAltitudeKm, mAltitudeKm) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLongitudeDegree, mAltitudeKm);
    }

    @Override
    @NonNull
    public String toString() {
        return "mLongitudeDegree: " + mLongitudeDegree + ", " + "mAltitudeKm: " + mAltitudeKm;
    }
}
