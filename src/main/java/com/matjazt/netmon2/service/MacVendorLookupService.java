package com.matjazt.netmon2.service;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * Singleton service for MAC address vendor (OUI) lookup.
 *
 * <p>Downloads the IEEE OUI registry CSV at startup and holds it in memory as a fast hash map. The
 * CSV is fetched once from {@value #OUI_CSV_URL} and never refreshed during the lifetime of the
 * application.
 *
 * <p>Lookup key: first 6 hex characters of a MAC address, without separators, upper-cased (e.g.
 * {@code "7CD1C3"}).
 *
 * <p>Usage:
 *
 * <pre>{@code
 * String vendor = macVendorLookup.lookupVendor("7C:D1:C3:AA:BB:CC");
 * // → "Huawei Technologies Co., Ltd"  (or null if unknown)
 * }</pre>
 */
@Service
@Slf4j
public class MacVendorLookupService {

    private static final String OUI_CSV_URL = "https://standards-oui.ieee.org/oui/oui.csv";
    private static final long RELOAD_COOLDOWN_MS = 900_000L; // 15 minutes
    public static final String UNKNOWN_VENDOR = "unknown";

    /**
     * OUI → vendor name. Key is 6 uppercase hex chars (e.g. {@code "7CD1C3"}). Pre-sized to avoid
     * rehashing - the IEEE list has ~37 000 entries.
     */
    private volatile Map<String, String> ouiMap = Map.of();

    private volatile long lastLoadAttemptMs = 0L;
    private volatile boolean loadInProgress = false;

    @PostConstruct
    public void init() {
        triggerAsyncLoadIfAllowed();
    }

    /** Downloads and parses the OUI CSV file. */
    public void loadVendorData() {
        try {
            log.info("Downloading IEEE OUI list from {}", OUI_CSV_URL);
            long startMs = System.currentTimeMillis();

            var tmpMap = new HashMap<String, String>(50_000);
            var url = URI.create(OUI_CSV_URL).toURL();
            try (var stream = url.openStream();
                    var reader =
                            new BufferedReader(
                                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                int linesRead = 0;
                int entriesLoaded = 0;
                int parseErrors = 0;

                String line;
                while ((line = reader.readLine()) != null) {
                    linesRead++;

                    // Skip header row
                    if (linesRead == 1) {
                        log.debug("CSV header: {}", line);
                        continue;
                    }

                    // Skip blank lines
                    if (line.isBlank()) {
                        continue;
                    }

                    try {
                        List<String> fields = parseCsvLine(line);

                        // Expected columns: Registry(0), Assignment/OUI(1), Org Name(2), Address(3)
                        if (fields.size() < 3) {
                            parseErrors++;
                            log.trace("Skipping short CSV line {}: {}", linesRead, line);
                            continue;
                        }

                        String oui = fields.get(1).trim().toUpperCase();
                        String vendor = fields.get(2).trim();

                        if (oui.length() != 6 || vendor.isEmpty()) {
                            parseErrors++;
                            continue;
                        }

                        tmpMap.put(oui, vendor);
                        entriesLoaded++;

                    } catch (Exception e) {
                        parseErrors++;
                        log.trace("Failed to parse CSV line {}: {}", linesRead, e.getMessage());
                    }
                }

                long elapsed = System.currentTimeMillis() - startMs;

                if (tmpMap.isEmpty()) {
                    log.error(
                            "No valid OUI entries loaded from IEEE CSV - check logs for parsing"
                                    + " errors.");
                } else {
                    log.info(
                            "IEEE OUI list loaded: {} entries in {} ms (lines read: {}, parse"
                                    + " errors: {})",
                            entriesLoaded,
                            elapsed,
                            linesRead,
                            parseErrors);

                    // Log one sample entry to confirm parsing is working correctly
                    var sample = tmpMap.entrySet().iterator().next();
                    log.debug("Sample OUI entry: {} → {}", sample.getKey(), sample.getValue());
                    // publish the fully loaded map
                    ouiMap = tmpMap;
                }
            }
        } catch (Exception e) {
            log.warn(
                    "Could not load IEEE OUI list from {}. Cause: {}", OUI_CSV_URL, e.getMessage());
        } finally {
            synchronized (this) {
                loadInProgress = false;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Looks up the vendor name for a given MAC address.
     *
     * <p>Accepts common MAC address formats:
     *
     * <ul>
     *   <li>{@code "7C:D1:C3:AA:BB:CC"} (colon-separated)
     *   <li>{@code "7C-D1-C3-AA-BB-CC"} (dash-separated)
     *   <li>{@code "7CD1C3AABBCC"} (plain hex string)
     * </ul>
     *
     * @param macAddress the MAC address to look up
     * @return the vendor name, or {@code null} if unknown or input is invalid
     */
    public String lookupVendor(String macAddress) {
        if (macAddress == null || macAddress.isBlank()) {
            return null;
        }

        var mapSnapshot = ouiMap;
        if (mapSnapshot == null || mapSnapshot.isEmpty()) {
            triggerAsyncLoadIfAllowed();
            return null;
        }

        try {
            // Strip separators, uppercase, take first 6 hex chars (= OUI)
            String normalized =
                    macAddress.replace(":", "").replace("-", "").replace(".", "").toUpperCase();

            if (normalized.length() < 6) {
                return null;
            }

            var vendor = mapSnapshot.get(normalized.substring(0, 6));
            return vendor != null ? vendor : UNKNOWN_VENDOR;

        } catch (Exception e) {
            log.warn("Error during vendor lookup for MAC {}: {}", macAddress, e.getMessage());
            return null;
        }
    }

    @PostConstruct
    public void triggerAsyncLoadIfAllowed() {
        synchronized (this) {
            if (loadInProgress) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastLoadAttemptMs < RELOAD_COOLDOWN_MS) {
                return;
            }

            lastLoadAttemptMs = now;
            loadInProgress = true;

            try {
                var thread = new Thread(this::loadVendorData, "mac-lookup-loader");
                thread.setDaemon(true);
                thread.start();
            } catch (RejectedExecutionException ex) {
                log.error("Failed to start async OUI loader thread - will retry later", ex);
                loadInProgress = false;
            }
        }
    }

    // -------------------------------------------------------------------------
    // CSV parsing
    // -------------------------------------------------------------------------

    /**
     * Parses a single CSV line into fields, correctly handling:
     *
     * <ul>
     *   <li>Quoted fields (may contain commas or newlines)
     *   <li>Escaped double-quotes ({@code ""}) inside quoted fields
     *   <li>Unquoted fields (no surrounding processing needed)
     * </ul>
     *
     * @param line a single line from the CSV file
     * @return ordered list of field values, with surrounding quotes stripped
     */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // Peek ahead: "" inside a quoted field is an escaped quote
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // consume the second quote
                    } else {
                        inQuotes = false; // closing quote
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true; // opening quote
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString()); // last field

        return fields;
    }
}
