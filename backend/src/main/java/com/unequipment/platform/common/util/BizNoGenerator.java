package com.unequipment.platform.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class BizNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private BizNoGenerator() {
    }

    public static String next(String prefix) {
        int seq = COUNTER.updateAndGet(value -> value >= 999 ? 0 : value + 1);
        int random = ThreadLocalRandom.current().nextInt(10, 99);
        return prefix + "-" + LocalDateTime.now().format(FORMATTER) + String.format("%03d", seq) + random;
    }
}
