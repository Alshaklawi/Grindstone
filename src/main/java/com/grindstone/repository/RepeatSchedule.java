package com.grindstone.repository;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public enum RepeatSchedule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY;

    public long start(long now) {
        LocalDate date = Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate();
        if (this == WEEKLY) {
            date = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if (this == MONTHLY) {
            date = date.withDayOfMonth(1);
        }
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public long next(long start) {
        LocalDate date = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate();
        date = this == MONTHLY ? date.plusMonths(1) : this == WEEKLY ? date.plusWeeks(1) : date.plusDays(1);
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }
}
