package com.ss.app.artcontact.beacon;

import java.util.Comparator;

/**
 * Created by steppschuh on 24.11.17.
 */

public abstract class BeaconUtil {

    /**
     * Estimates a maximum distance at which advertising packages sent using the specified
     * transmission power can be received.
     *
     * @param transmissionPower the tx power (in dBm) of the beacon
     * @return estimated range in meters
     * @see <a href="https://support.kontakt.io/hc/en-gb/articles/201621521-Transmission-power-Range-and-RSSI">Kontakt.io
     *         Knowledge Base</a>
     */
    public static float getAdvertisingRange(int transmissionPower) {
        if (transmissionPower < -30) {
            return 1;
        } else if (transmissionPower < -25) {
            return getAdvertisingRange(transmissionPower, -30, 2);
        } else if (transmissionPower < -18) {
            return getAdvertisingRange(transmissionPower, -20, 4);
        } else if (transmissionPower < -14) {
            return getAdvertisingRange(transmissionPower, -16, 16);
        } else if (transmissionPower < -10) {
            return getAdvertisingRange(transmissionPower, -12, 20);
        } else if (transmissionPower < -6) {
            return getAdvertisingRange(transmissionPower, -8, 30);
        } else if (transmissionPower < -2) {
            return getAdvertisingRange(transmissionPower, -4, 40);
        } else if (transmissionPower < 2) {
            return getAdvertisingRange(transmissionPower, 0, 60);
        } else {
            return getAdvertisingRange(transmissionPower, 4, 70);
        }
    }

    /**
     * Calculates the RSSI using the reverse <a href="https://en.wikipedia.org/wiki/Log-distance_path_loss_model">log-distance
     * path loss model</a>.
     *
     * @param distance          Distance for which a rssi should be estimated
     * @param calibratedRssi    the RSSI measured at 1m distance
     * @param pathLossParameter the path-loss adjustment parameter
     */
    public static int calculateRssi(float distance, float calibratedRssi, float pathLossParameter) {
        if (distance < 0) {
            throw new IllegalArgumentException("Distance must be greater than 0");
        }
        return (int) (calibratedRssi - ((Math.log(distance) / Math.log(10)) * (10 * pathLossParameter)));
    }

    /**
     * Uses a simple rule of three equation. Transmission power values will be incremented by 100 to
     * compensate for negative values.
     */
    public static float getAdvertisingRange(int transmissionPower, int calibratedTransmissionPower, int calibratedRange) {
        return (calibratedRange * (transmissionPower + 100)) / (float) (calibratedTransmissionPower + 100);
    }

    /**
     * Used to sort beacons from highest rssi to lowest rssi.
     */
    public static Comparator<Beacon> DescendingRssiComparator = new Comparator<Beacon>() {
        public int compare(Beacon firstBeacon, Beacon secondBeacon) {
            if (firstBeacon.equals(secondBeacon)) {
                return 0;
            }
            return Integer.compare(secondBeacon.rssi, firstBeacon.rssi);
        }
    };

    /**
     * Used to sort beacons from lowest rssi to highest rssi.
     */
    public static Comparator<Beacon> AscendingRssiComparator = new Comparator<Beacon>() {
        public int compare(Beacon firstBeacon, Beacon secondBeacon) {
            if (firstBeacon.equals(secondBeacon)) {
                return 0;
            }
            return Integer.compare(firstBeacon.rssi, secondBeacon.rssi);
        }
    };

}
