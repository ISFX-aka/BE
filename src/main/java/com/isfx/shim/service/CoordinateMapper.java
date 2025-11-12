package com.isfx.shim.service;

import java.util.Locale;
import java.util.Map;

public final class CoordinateMapper {

    private static final Map<String, Coordinate> PREDEFINED_COORDINATES = Map.ofEntries(
            Map.entry("종로구", new Coordinate(60, 127)),
            Map.entry("중구", new Coordinate(60, 127)),
            Map.entry("용산구", new Coordinate(60, 126)),
            Map.entry("성동구", new Coordinate(61, 127)),
            Map.entry("광진구", new Coordinate(62, 126)),
            Map.entry("동대문구", new Coordinate(61, 127)),
            Map.entry("중랑구", new Coordinate(62, 128)),
            Map.entry("성북구", new Coordinate(61, 128)),
            Map.entry("강북구", new Coordinate(61, 129)),
            Map.entry("도봉구", new Coordinate(61, 130)),
            Map.entry("노원구", new Coordinate(61, 130)),
            Map.entry("은평구", new Coordinate(59, 128)),
            Map.entry("서대문구", new Coordinate(59, 127)),
            Map.entry("마포구", new Coordinate(59, 127)),
            Map.entry("양천구", new Coordinate(58, 126)),
            Map.entry("강서구", new Coordinate(58, 126)),
            Map.entry("구로구", new Coordinate(58, 125)),
            Map.entry("금천구", new Coordinate(59, 124)),
            Map.entry("영등포구", new Coordinate(58, 126)),
            Map.entry("동작구", new Coordinate(59, 125)),
            Map.entry("관악구", new Coordinate(59, 124)),
            Map.entry("서초구", new Coordinate(61, 125)),
            Map.entry("강남구", new Coordinate(61, 125)),
            Map.entry("송파구", new Coordinate(62, 125)),
            Map.entry("강동구", new Coordinate(63, 126))
    );

    private static final Coordinate DEFAULT_COORDINATE = new Coordinate(60, 127); // 서울 중구

    private CoordinateMapper() {
    }

    public static Coordinate toGridXY(String districtName) {
        if (districtName == null || districtName.isBlank()) {
            return DEFAULT_COORDINATE;
        }
        String normalized = districtName.trim().toLowerCase(Locale.KOREAN);
        return PREDEFINED_COORDINATES.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.KOREAN).equals(normalized))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(DEFAULT_COORDINATE);
    }
}
