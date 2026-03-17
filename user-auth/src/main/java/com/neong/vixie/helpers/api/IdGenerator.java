package com.neong.vixie.helpers.api;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

public final class IdGenerator {

    private IdGenerator() {
    }

    public static String generateId(String tableName) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int number = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        String numberPart = String.format("%06d", number);
        return tableName.toLowerCase() + "_id_" + numberPart + "_" + today;
    }
}
