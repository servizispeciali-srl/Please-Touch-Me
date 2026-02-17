package com.ss.app.artcontact.filter;

import com.ss.app.artcontact.beacon.Beacon;

/**
 * Created by leon on 03.01.18.
 */

public interface RssiFilter {

    float filter(Beacon beacon);

}
