package com.sungho.letterpick.support.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public static MutableClock fixedAt(String instant) {
        return new MutableClock(Instant.parse(instant), ZoneOffset.UTC);
    }

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void setInstant(String instant) {
        this.instant = Instant.parse(instant);
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public void plus(Duration duration) {
        this.instant = this.instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
