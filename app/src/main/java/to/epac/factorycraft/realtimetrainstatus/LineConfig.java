package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;

import java.util.HashMap;

public class LineConfig {
    public String apiUrl;
    public int[] stationIDs;
    public HashMap<Integer, Long> runTimeUpMap = new HashMap<>();
    public HashMap<Integer, Long> runTimeDnMap = new HashMap<>();
    public HashMap<Integer, Long> dwellTimeUpMap = new HashMap<>();
    public HashMap<Integer, Long> dwellTimeDnMap = new HashMap<>();

    public static LineConfig get(Context context, String lineCode) {
        LineConfig config = new LineConfig();
        String rawIDs = "";

        if (lineCode.equalsIgnoreCase("eal")) {
            rawIDs = context.getString(R.string.eal_station_id);
            setupEAL(config);
        } else if (lineCode.equalsIgnoreCase("tml")) {
            rawIDs = context.getString(R.string.tml_station_id);
            setupTML(config);
        }

        // 直接在方法內解析字串
        String[] parts = rawIDs.trim().split("\\s+");
        config.stationIDs = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            config.stationIDs[i] = Integer.parseInt(parts[i]);
        }

        return config;
    }

    private static void setupEAL(LineConfig config) {
        // 北行行車時間
        config.runTimeUpMap.put(22, 94L);  // ADM -> EXC
        config.runTimeUpMap.put(21, 185L); // EXC -> HUH
        config.runTimeUpMap.put(2, 211L);  // HUH -> MKK
        config.runTimeUpMap.put(3, 123L);  // MKK -> KOT
        config.runTimeUpMap.put(4, 216L);  // KOT -> TAW
        config.runTimeUpMap.put(5, 99L);   // TAW -> SHT
        config.runTimeUpMap.put(6, 120L);  // SHT -> FOT
        config.runTimeUpMap.put(7, 120L);  // SHT -> RAC
        config.runTimeUpMap.put(8, 163L);  // FOT/RAC -> UNI
        config.runTimeUpMap.put(9, 303L);  // UNI -> TAP
        config.runTimeUpMap.put(10, 97L);  // TAP -> TWO
        config.runTimeUpMap.put(11, 265L); // TWO -> FAN
        config.runTimeUpMap.put(12, 106L); // FAN -> SHS
        config.runTimeUpMap.put(13, 202L); // SHS -> LOW
        config.runTimeUpMap.put(14, 408L); // SHS -> LMC

        // Dwell Time (停站)
        config.dwellTimeUpMap.put(23, 47L); // ADM -> EXC
        config.dwellTimeUpMap.put(1, 44L);  // EXC -> HUH
        config.dwellTimeUpMap.put(2, 44L);  // HUH -> MKK
        config.dwellTimeUpMap.put(3, 47L);  // MKK -> KOT
        config.dwellTimeUpMap.put(4, 47L);  // KOT -> TAW
        config.dwellTimeUpMap.put(5, 35L);  // TAW -> SHT
        config.dwellTimeUpMap.put(6, 35L);  // SHT -> FOT
        config.dwellTimeUpMap.put(7, 35L);  // SHT -> RAC
        config.dwellTimeUpMap.put(8, 35L);  // FOT/RAC -> UNI
        config.dwellTimeUpMap.put(9, 35L);  // UNI -> TAP
        config.dwellTimeUpMap.put(10, 35L); // TAP -> TWO
        config.dwellTimeUpMap.put(11, 35L); // TWO -> FAN
        config.dwellTimeUpMap.put(12, 47L); // FAN -> SHS

        // --- 南行 (Dn: Key 是起點站) ---
        config.runTimeDnMap.put(14, 422L); // LMC -> SHS
        config.runTimeDnMap.put(13, 197L); // LOW -> SHS
        config.runTimeDnMap.put(12, 106L); // SHS -> FAN
        config.runTimeDnMap.put(11, 262L); // FAN -> TWO
        config.runTimeDnMap.put(10, 96L);  // TWO -> TAP
        config.runTimeDnMap.put(9, 301L);  // TAP -> UNI
        config.runTimeDnMap.put(8, 160L);  // UNI -> FOT
        config.runTimeDnMap.put(7, 120L);  // RAC -> SHT
        config.runTimeDnMap.put(6, 120L);  // FOT -> SHT
        config.runTimeDnMap.put(5, 98L);   // SHT -> TAW
        config.runTimeDnMap.put(4, 220L);  // TAW -> KOT
        config.runTimeDnMap.put(3, 125L);  // KOT -> MKK
        config.runTimeDnMap.put(2, 158L);  // MKK -> HUH
        config.runTimeDnMap.put(21, 184L); // HUH -> EXC
        config.runTimeDnMap.put(22, 93L);  // EXC -> ADM

        // Dwell Time (停站)
        config.dwellTimeDnMap.put(14, 47L); // LMC -> SHS
        config.dwellTimeDnMap.put(13, 47L); // LOW -> SHS
        config.dwellTimeDnMap.put(12, 35L); // SHS -> FAN
        config.dwellTimeDnMap.put(11, 35L); // FAN -> TWO
        config.dwellTimeDnMap.put(10, 40L); // TWO -> TAP
        config.dwellTimeDnMap.put(9, 30L);  // TAP -> UNI
        config.dwellTimeDnMap.put(8, 35L);  // UNI -> FOT
        config.dwellTimeDnMap.put(7, 40L);  // RAC -> SHT
        config.dwellTimeDnMap.put(6, 40L);  // FOT -> SHT
        config.dwellTimeDnMap.put(5, 47L);  // SHT -> TAW
        config.dwellTimeDnMap.put(4, 47L);  // TAW -> KOT
        config.dwellTimeDnMap.put(3, 44L);  // KOT -> MKK
        config.dwellTimeDnMap.put(2, 44L);  // MKK -> HUH
        config.dwellTimeDnMap.put(1, 44L);  // HUH -> EXC
    }

    private static void setupTML(LineConfig config) {
        // --- 北行 (Up Track: 往烏溪沙 WKS) ---
        // 以 Non-peak 數據為準，Key 為「下一站」代碼
        config.runTimeUpMap.put(30, 118L); // WKS -> MOS
        config.runTimeUpMap.put(29, 108L); // MOS -> HEO
        config.runTimeUpMap.put(28, 107L); // HEO -> TSH
        config.runTimeUpMap.put(27, 183L); // TSH -> SHM
        config.runTimeUpMap.put(26, 76L);  // SHM -> CIO
        config.runTimeUpMap.put(25, 97L);  // CIO -> STW
        config.runTimeUpMap.put(24, 91L);  // STW -> CKT
        config.runTimeUpMap.put(5, 85L);   // CKT -> TAW
        config.runTimeUpMap.put(66, 119L); // TAW -> HIK
        config.runTimeUpMap.put(65, 255L); // HIK -> DIH
        config.runTimeUpMap.put(64, 119L); // DIH -> KAT
        config.runTimeUpMap.put(63, 91L);  // KAT -> SUW
        config.runTimeUpMap.put(62, 110L); // SUW -> TKW
        config.runTimeUpMap.put(61, 106L); // TKW -> HOM
        config.runTimeUpMap.put(52, 92L);  // HOM -> HUH
        config.runTimeUpMap.put(51, 142L); // HUH -> ETS
        config.runTimeUpMap.put(50, 147L); // ETS -> AUS
        config.runTimeUpMap.put(41, 142L); // AUS -> NAC
        config.runTimeUpMap.put(42, 156L); // NAC -> MEF
        config.runTimeUpMap.put(43, 253L); // MEF -> TWW
        config.runTimeUpMap.put(44, 352L); // TWW -> KSR
        config.runTimeUpMap.put(45, 191L); // KSR -> YUL
        config.runTimeUpMap.put(46, 94L);  // YUL -> LOP
        config.runTimeUpMap.put(47, 155L); // LOP -> TIS
        config.runTimeUpMap.put(48, 243L); // TIS -> SIH
        config.runTimeUpMap.put(49, 156L); // SIH -> TUM

        // 北行停站時間 (Dwell Time)
        config.dwellTimeUpMap.put(30, 24L); // MOS
        config.dwellTimeUpMap.put(29, 24L); // HEO
        config.dwellTimeUpMap.put(28, 24L); // TSH
        config.dwellTimeUpMap.put(27, 25L); // SHM
        config.dwellTimeUpMap.put(26, 25L); // CIO
        config.dwellTimeUpMap.put(25, 25L); // STW
        config.dwellTimeUpMap.put(24, 25L); // CKT
        config.dwellTimeUpMap.put(5, 35L);  // TAW
        config.dwellTimeUpMap.put(66, 25L); // HIK
        config.dwellTimeUpMap.put(65, 35L); // DIH
        config.dwellTimeUpMap.put(64, 25L); // KAT
        config.dwellTimeUpMap.put(63, 25L); // SUW
        config.dwellTimeUpMap.put(62, 25L); // TKW
        config.dwellTimeUpMap.put(61, 35L); // HOM
        config.dwellTimeUpMap.put(52, 35L); // HUH
        config.dwellTimeUpMap.put(51, 30L); // ETS
        config.dwellTimeUpMap.put(50, 30L); // AUS
        config.dwellTimeUpMap.put(41, 30L); // NAC
        config.dwellTimeUpMap.put(42, 27L); // MEF
        config.dwellTimeUpMap.put(43, 27L); // TWW
        config.dwellTimeUpMap.put(44, 27L); // KSR
        config.dwellTimeUpMap.put(45, 27L); // YUL
        config.dwellTimeUpMap.put(46, 27L); // LOP
        config.dwellTimeUpMap.put(47, 27L); // TIS
        config.dwellTimeUpMap.put(48, 28L); // SIH

        // --- 南行 (Down Track: 往屯門 TUM) ---
        // 以 Non-peak 數據為準，Key 為「當前站」代碼
        config.runTimeDnMap.put(49, 142L); // TUM -> SIH
        config.runTimeDnMap.put(48, 237L); // SIH -> TIS
        config.runTimeDnMap.put(47, 153L); // TIS -> LOP
        config.runTimeDnMap.put(46, 93L);  // LOP -> YUL
        config.runTimeDnMap.put(45, 181L); // YUL -> KSR
        config.runTimeDnMap.put(44, 353L); // KSR -> TWW
        config.runTimeDnMap.put(43, 238L); // TWW -> MEF
        config.runTimeDnMap.put(42, 150L); // MEF -> NAC
        config.runTimeDnMap.put(41, 149L); // NAC -> AUS
        config.runTimeDnMap.put(50, 138L); // AUS -> ETS
        config.runTimeDnMap.put(51, 131L); // ETS -> HUH
        config.runTimeDnMap.put(52, 89L);  // HUH -> HOM
        config.runTimeDnMap.put(61, 105L); // HOM -> TKW
        config.runTimeDnMap.put(62, 100L); // TKW -> SUW
        config.runTimeDnMap.put(63, 91L);  // SUW -> KAT
        config.runTimeDnMap.put(64, 118L); // KAT -> DIH
        config.runTimeDnMap.put(65, 242L); // DIH -> HIK
        config.runTimeDnMap.put(66, 120L); // HIK -> TAW
        config.runTimeDnMap.put(5, 86L);   // TAW -> CKT
        config.runTimeDnMap.put(24, 93L);  // CKT -> STW
        config.runTimeDnMap.put(25, 107L); // STW -> CIO
        config.runTimeDnMap.put(26, 84L);  // CIO -> SHM
        config.runTimeDnMap.put(27, 178L); // SHM -> TSH
        config.runTimeDnMap.put(28, 95L);  // TSH -> HEO
        config.runTimeDnMap.put(29, 98L);  // HEO -> MOS
        config.runTimeDnMap.put(30, 185L); // MOS -> WKS

        // 南行停站時間 (Dwell Time)
        config.dwellTimeDnMap.put(49, 25L); // TUM
        config.dwellTimeDnMap.put(48, 24L); // SIH
        config.dwellTimeDnMap.put(47, 24L); // TIS
        config.dwellTimeDnMap.put(46, 24L); // LOP
        config.dwellTimeDnMap.put(45, 25L); // YUL
        config.dwellTimeDnMap.put(44, 25L); // KSR
        config.dwellTimeDnMap.put(43, 25L); // TWW
        config.dwellTimeDnMap.put(42, 25L); // MEF
        config.dwellTimeDnMap.put(41, 28L); // NAC
        config.dwellTimeDnMap.put(50, 28L); // AUS
        config.dwellTimeDnMap.put(51, 35L); // ETS
        config.dwellTimeDnMap.put(52, 35L); // HUH
        config.dwellTimeDnMap.put(61, 25L); // HOM
        config.dwellTimeDnMap.put(62, 25L); // TKW
        config.dwellTimeDnMap.put(63, 25L); // SUW
        config.dwellTimeDnMap.put(64, 35L); // KAT
        config.dwellTimeDnMap.put(65, 25L); // DIH
        config.dwellTimeDnMap.put(66, 35L); // HIK
        config.dwellTimeDnMap.put(5, 25L);  // TAW
        config.dwellTimeDnMap.put(24, 25L); // CKT
        config.dwellTimeDnMap.put(25, 25L); // STW
        config.dwellTimeDnMap.put(26, 25L); // CIO
        config.dwellTimeDnMap.put(27, 27L); // SHM
        config.dwellTimeDnMap.put(28, 27L); // TSH
        config.dwellTimeDnMap.put(29, 27L); // HEO
        config.dwellTimeDnMap.put(30, 27L); // MOS
    }
}