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

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SatelliteSessionStatsWrapper2 is used to represent the usage stats of the satellite service.
 */
public class SatelliteSessionStatsWrapper2 implements Parcelable {

    private static final int VERSION = 2;
    private int mCountOfSuccessfulUserMessages;
    private int mCountOfUnsuccessfulUserMessages;
    private int mCountOfTimedOutUserMessagesWaitingForConnection;
    private int mCountOfTimedOutUserMessagesWaitingForAck;
    private int mCountOfUserMessagesInQueueToBeSent;
    private long mLatencyOfSuccessfulUserMessages;

    private Map<Integer, SatelliteSessionStatsWrapper2> datagramStats;
    private long mMaxLatency;
    private long mLastMessageLatency;


    public SatelliteSessionStatsWrapper2() {
        this.datagramStats = new HashMap<>();
    }


    /**
     * SatelliteSessionStatsWrapper2 constructor
     *
     * @param builder Builder to create SatelliteSessionStatsWrapper2 object/
     */
    public SatelliteSessionStatsWrapper2(@NonNull Builder builder) {
        mCountOfSuccessfulUserMessages = builder.mCountOfSuccessfulUserMessages;
        mCountOfUnsuccessfulUserMessages = builder.mCountOfUnsuccessfulUserMessages;
        mCountOfTimedOutUserMessagesWaitingForConnection =
                builder.mCountOfTimedOutUserMessagesWaitingForConnection;
        mCountOfTimedOutUserMessagesWaitingForAck =
                builder.mCountOfTimedOutUserMessagesWaitingForAck;
        mCountOfUserMessagesInQueueToBeSent = builder.mCountOfUserMessagesInQueueToBeSent;
        mLatencyOfSuccessfulUserMessages = builder.mLatencyOfSuccessfulUserMessages;
    }

    private SatelliteSessionStatsWrapper2(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(mCountOfSuccessfulUserMessages);
        out.writeInt(mCountOfUnsuccessfulUserMessages);
        out.writeInt(mCountOfTimedOutUserMessagesWaitingForConnection);
        out.writeInt(mCountOfTimedOutUserMessagesWaitingForAck);
        out.writeInt(mCountOfUserMessagesInQueueToBeSent);
        out.writeLong(mLatencyOfSuccessfulUserMessages);
        out.writeLong(mMaxLatency);
        out.writeLong(mLastMessageLatency);

        if (datagramStats != null && !datagramStats.isEmpty()) {
            out.writeInt(datagramStats.size());
            for (Map.Entry<Integer, SatelliteSessionStatsWrapper2> entry :
                    datagramStats.entrySet()) {
                out.writeInt(entry.getKey());
                out.writeParcelable(entry.getValue(), flags);
            }
        } else {
            out.writeInt(0);
        }
    }

    @NonNull
    public static final Creator<SatelliteSessionStatsWrapper2> CREATOR = new Creator<>() {

        @Override
        public SatelliteSessionStatsWrapper2 createFromParcel(Parcel in) {
            return new SatelliteSessionStatsWrapper2(in);
        }

        @Override
        public SatelliteSessionStatsWrapper2[] newArray(int size) {
            return new SatelliteSessionStatsWrapper2[size];
        }
    };

    @Override
    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (datagramStats != null) {
            sb.append(" ====== SatelliteSessionStatsWrapper2 Info =============");
            for (Map.Entry<Integer, SatelliteSessionStatsWrapper2> entry :
                    datagramStats.entrySet()) {
                Integer key = entry.getKey();
                SatelliteSessionStatsWrapper2 value = entry.getValue();
                sb.append("\n");
                sb.append("Key:");
                sb.append(key);
                sb.append(", SatelliteSessionStatsWrapper2:[");
                value.getPrintableCounters(sb);
                sb.append(",");
                sb.append(" LatencyOfSuccessfulUserMessages:");
                sb.append(value.mLatencyOfSuccessfulUserMessages);
                sb.append(",");
                sb.append(" mMaxLatency:");
                sb.append(value.mMaxLatency);
                sb.append(",");
                sb.append(" mLastMessageLatency:");
                sb.append(value.mLastMessageLatency);
                sb.append(",");
                sb.append(" VERSION:");
                sb.append(value.VERSION);
                sb.append("]");
                sb.append("\n");
            }
            sb.append(" ============== ================== ===============");
            sb.append("\n");
            sb.append("\n");
        } else {
            sb.append("\n");
            getPrintableCounters(sb);
        }
        sb.append("\n");
        return sb.toString();
    }

    private void getPrintableCounters(StringBuilder sb) {
        sb.append("countOfSuccessfulUserMessages:");
        sb.append(mCountOfSuccessfulUserMessages);
        sb.append(",");

        sb.append("countOfUnsuccessfulUserMessages:");
        sb.append(mCountOfUnsuccessfulUserMessages);
        sb.append(",");

        sb.append("countOfTimedOutUserMessagesWaitingForConnection:");
        sb.append(mCountOfTimedOutUserMessagesWaitingForConnection);
        sb.append(",");

        sb.append("countOfTimedOutUserMessagesWaitingForAck:");
        sb.append(mCountOfTimedOutUserMessagesWaitingForAck);
        sb.append(",");

        sb.append("countOfUserMessagesInQueueToBeSent:");
        sb.append(mCountOfUserMessagesInQueueToBeSent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SatelliteSessionStatsWrapper2 that = (SatelliteSessionStatsWrapper2) o;
        return mCountOfSuccessfulUserMessages == that.mCountOfSuccessfulUserMessages
                && mLatencyOfSuccessfulUserMessages == that.mLatencyOfSuccessfulUserMessages
                && mCountOfUnsuccessfulUserMessages == that.mCountOfUnsuccessfulUserMessages
                && mCountOfTimedOutUserMessagesWaitingForConnection
                == that.mCountOfTimedOutUserMessagesWaitingForConnection
                && mCountOfTimedOutUserMessagesWaitingForAck
                == that.mCountOfTimedOutUserMessagesWaitingForAck
                && mCountOfUserMessagesInQueueToBeSent == that.mCountOfUserMessagesInQueueToBeSent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCountOfSuccessfulUserMessages, mLatencyOfSuccessfulUserMessages,
                mCountOfUnsuccessfulUserMessages, mCountOfTimedOutUserMessagesWaitingForConnection,
                mCountOfTimedOutUserMessagesWaitingForAck, mCountOfUserMessagesInQueueToBeSent);
    }

    public int getCountOfSuccessfulUserMessages() {
        return mCountOfSuccessfulUserMessages;
    }

    public void incrementSuccessfulUserMessageCount() {
        mCountOfSuccessfulUserMessages++;
    }

    public int getCountOfUnsuccessfulUserMessages() {
        return mCountOfUnsuccessfulUserMessages;
    }

    public void incrementUnsuccessfulUserMessageCount() {
        mCountOfUnsuccessfulUserMessages++;
    }

    public int getCountOfTimedOutUserMessagesWaitingForConnection() {
        return mCountOfTimedOutUserMessagesWaitingForConnection;
    }

    public void incrementTimedOutUserMessagesWaitingForConnection() {
        mCountOfTimedOutUserMessagesWaitingForConnection++;
    }

    public int getCountOfTimedOutUserMessagesWaitingForAck() {
        return mCountOfTimedOutUserMessagesWaitingForAck;
    }

    public void incrementTimedOutUserMessagesWaitingForAck() {
        mCountOfTimedOutUserMessagesWaitingForAck++;
    }

    public int getCountOfUserMessagesInQueueToBeSent() {
        return mCountOfUserMessagesInQueueToBeSent;
    }

    public long getLatencyOfAllSuccessfulUserMessages() {
        return mLatencyOfSuccessfulUserMessages;
    }

    public void updateLatencyOfAllSuccessfulUserMessages(long messageLatency) {
        mLatencyOfSuccessfulUserMessages += messageLatency;
    }

    public void recordSuccessfulOutgoingDatagramStats(
            @SatelliteManager.DatagramType int datagramType, long latency) {
        try {
            datagramStats.putIfAbsent(datagramType, new Builder().build());
            SatelliteSessionStatsWrapper2 data = datagramStats.get(datagramType);
            data.incrementSuccessfulUserMessageCount();
            if (data.mMaxLatency < latency) {
                data.mMaxLatency = latency;
            }
            data.mLastMessageLatency = latency;
            data.updateLatencyOfAllSuccessfulUserMessages(latency);
        } catch (Exception e) {
            Log.e("SatelliteSessionStatsWrapper2",
                    "Error while recordSuccessfulOutgoingDatagramStats: " + e.getMessage());
        }
    }

    public int getCountOfSuccessfulOutgoingDatagram(
            @SatelliteManager.DatagramType int datagramType) {
        SatelliteSessionStatsWrapper2 data = datagramStats.getOrDefault(datagramType,
                new SatelliteSessionStatsWrapper2());
        return data.getCountOfSuccessfulUserMessages();
    }

    public long getMaxLatency() {
        return this.mMaxLatency;
    }

    public void setMaxLatency(long latency) {
        this.mMaxLatency = latency;
    }

    public Long getLatencyOfAllSuccessfulUserMessages(
            @SatelliteManager.DatagramType int datagramType) {
        SatelliteSessionStatsWrapper2 data = datagramStats.getOrDefault(datagramType,
                new SatelliteSessionStatsWrapper2());
        return data.getLatencyOfAllSuccessfulUserMessages();
    }

    public long getLastMessageLatency() {
        return this.mLastMessageLatency;
    }

    public void setLastMessageLatency(long latency) {
        this.mLastMessageLatency = latency;
    }

    public void addCountOfUnsuccessfulUserMessages(@SatelliteManager.DatagramType int datagramType,
            @SatelliteManager.SatelliteResult int resultCode) {
        try {
            datagramStats.putIfAbsent(datagramType, new Builder().build());
            SatelliteSessionStatsWrapper2 data = datagramStats.get(datagramType);
            data.incrementUnsuccessfulUserMessageCount();
            if (resultCode == SatelliteManager.SATELLITE_RESULT_NOT_REACHABLE) {
                data.incrementTimedOutUserMessagesWaitingForConnection();
            } else if (resultCode == SatelliteManager.SATELLITE_RESULT_MODEM_TIMEOUT) {
                data.incrementTimedOutUserMessagesWaitingForAck();
            }
        } catch (Exception e) {
            Log.e("SatelliteSessionStatsWrapper2",
                    "Error while addCountOfUnsuccessfulUserMessages: " + e.getMessage());
        }
    }

    public int getCountOfUnsuccessfulUserMessages(@SatelliteManager.DatagramType int datagramType) {
        SatelliteSessionStatsWrapper2 data = datagramStats.get(datagramType);
        return data.getCountOfUnsuccessfulUserMessages();
    }

    public int getCountOfTimedOutUserMessagesWaitingForConnection(
            @SatelliteManager.DatagramType int datagramType) {
        SatelliteSessionStatsWrapper2 data = datagramStats.get(datagramType);
        return data.getCountOfTimedOutUserMessagesWaitingForConnection();
    }

    public int getCountOfTimedOutUserMessagesWaitingForAck(
            @SatelliteManager.DatagramType int datagramType) {
        SatelliteSessionStatsWrapper2 data = datagramStats.get(datagramType);
        return data.getCountOfTimedOutUserMessagesWaitingForAck();
    }

    public int getCountOfUserMessagesInQueueToBeSent(
            @SatelliteManager.DatagramType int datagramType) {
        SatelliteSessionStatsWrapper2 data = datagramStats.get(datagramType);
        return data.getCountOfUserMessagesInQueueToBeSent();
    }

    public int getVersion() {
        return VERSION;
    }

    public void clear() {
        datagramStats.clear();
    }

    @NonNull
    public Map<Integer, SatelliteSessionStatsWrapper2> getSatelliteSessionStats() {
        return datagramStats;
    }

    public void setSatelliteSessionStats(Map<Integer, SatelliteSessionStatsWrapper2> sessionStats) {
        this.datagramStats = sessionStats;
    }

    public void setCountOfSuccessfulUserMessages(int count) {
        mCountOfSuccessfulUserMessages = count;
    }

    public void setCountOfUnsuccessfulUserMessages(int count) {
        mCountOfUnsuccessfulUserMessages = count;
    }

    public void setCountOfTimedOutUserMessagesWaitingForConnection(int count) {
        mCountOfTimedOutUserMessagesWaitingForConnection = count;
    }


    public void setCountOfTimedOutUserMessagesWaitingForAck(int count) {
        mCountOfTimedOutUserMessagesWaitingForAck = count;
    }


    public void setCountOfUserMessagesInQueueToBeSent(int count) {
        mCountOfUserMessagesInQueueToBeSent = count;
    }

    private void readFromParcel(Parcel in) {
        mCountOfSuccessfulUserMessages = in.readInt();
        mCountOfUnsuccessfulUserMessages = in.readInt();
        mCountOfTimedOutUserMessagesWaitingForConnection = in.readInt();
        mCountOfTimedOutUserMessagesWaitingForAck = in.readInt();
        mCountOfUserMessagesInQueueToBeSent = in.readInt();
        mLatencyOfSuccessfulUserMessages = in.readLong();
        mMaxLatency = in.readLong();
        mLastMessageLatency = in.readLong();

        int size = in.readInt();
        datagramStats = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Integer key = in.readInt();
            SatelliteSessionStatsWrapper2 value = in.readParcelable(
                    SatelliteSessionStatsWrapper2.class.getClassLoader());
            datagramStats.put(key, value);
        }
    }

    /**
     * A builder class to create {@link SatelliteSessionStatsWrapper2} data object.
     */
    public static final class Builder {
        private int mCountOfSuccessfulUserMessages;
        private int mCountOfUnsuccessfulUserMessages;
        private int mCountOfTimedOutUserMessagesWaitingForConnection;
        private int mCountOfTimedOutUserMessagesWaitingForAck;
        private int mCountOfUserMessagesInQueueToBeSent;
        private long mLatencyOfSuccessfulUserMessages;

        /**
         * Sets countOfSuccessfulUserMessages value of {@link SatelliteSessionStatsWrapper2}
         * and then returns the Builder class.
         */
        @NonNull
        public Builder setCountOfSuccessfulUserMessages(int count) {
            mCountOfSuccessfulUserMessages = count;
            return this;
        }

        /**
         * Sets countOfUnsuccessfulUserMessages value of {@link SatelliteSessionStatsWrapper2}
         * and then returns the Builder class.
         */
        @NonNull
        public Builder setCountOfUnsuccessfulUserMessages(int count) {
            mCountOfUnsuccessfulUserMessages = count;
            return this;
        }

        /**
         * Sets countOfTimedOutUserMessagesWaitingForConnection value of
         * {@link SatelliteSessionStatsWrapper2} and then returns the Builder class.
         */
        @NonNull
        public Builder setCountOfTimedOutUserMessagesWaitingForConnection(int count) {
            mCountOfTimedOutUserMessagesWaitingForConnection = count;
            return this;
        }

        /**
         * Sets countOfTimedOutUserMessagesWaitingForAck value of
         * {@link SatelliteSessionStatsWrapper2}
         * and then returns the Builder class.
         */
        @NonNull
        public Builder setCountOfTimedOutUserMessagesWaitingForAck(int count) {
            mCountOfTimedOutUserMessagesWaitingForAck = count;
            return this;
        }

        /**
         * Sets countOfUserMessagesInQueueToBeSent value of {@link SatelliteSessionStatsWrapper2}
         * and then returns the Builder class.
         */
        @NonNull
        public Builder setCountOfUserMessagesInQueueToBeSent(int count) {
            mCountOfUserMessagesInQueueToBeSent = count;
            return this;
        }

        @NonNull
        public Builder setLatencyOfSuccessfulUserMessages(long latency) {
            mLatencyOfSuccessfulUserMessages = latency;
            return this;
        }

        /** Returns SatelliteSessionStatsWrapper2 object. */
        @NonNull
        public SatelliteSessionStatsWrapper2 build() {
            return new SatelliteSessionStatsWrapper2(this);
        }
    }
}