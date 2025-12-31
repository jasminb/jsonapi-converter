package com.github.jasminb.jsonapi.discovery;

import java.util.*;

/**
 * Diagnostics information about JsonProcessor discovery and selection.
 * Useful for debugging classpath and configuration issues.
 *
 * Golden Path Phase 3 - Code Generation (Diff 3)
 * Epic 2: Service Discovery Framework
 */
public class JsonProcessorDiagnostics {

    private final List<String> detectedProviders;
    private final String selectedProvider;
    private final Map<String, String> classpathInfo;
    private final List<String> discoveryErrors;

    public JsonProcessorDiagnostics() {
        this.detectedProviders = new ArrayList<>();
        this.classpathInfo = new HashMap<>();
        this.discoveryErrors = new ArrayList<>();

        // Collect diagnostic information
        detectProviders();
        this.selectedProvider = determineSelectedProvider();
    }

    public List<String> getDetectedProviders() {
        return Collections.unmodifiableList(detectedProviders);
    }

    public String getSelectedProvider() {
        return selectedProvider;
    }

    public Map<String, String> getClasspathInfo() {
        return Collections.unmodifiableMap(classpathInfo);
    }

    public List<String> getDiscoveryErrors() {
        return Collections.unmodifiableList(discoveryErrors);
    }

    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("JsonProcessor Discovery Report\n");
        report.append("==============================\n\n");

        report.append("Selected Provider: ").append(selectedProvider).append("\n\n");

        report.append("Detected Providers:\n");
        if (detectedProviders.isEmpty()) {
            report.append("  None\n");
        } else {
            for (String provider : detectedProviders) {
                report.append("  - ").append(provider).append("\n");
            }
        }
        report.append("\n");

        report.append("Classpath Information:\n");
        if (classpathInfo.isEmpty()) {
            report.append("  No library versions detected\n");
        } else {
            for (Map.Entry<String, String> entry : classpathInfo.entrySet()) {
                report.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        report.append("\n");

        if (!discoveryErrors.isEmpty()) {
            report.append("Discovery Errors:\n");
            for (String error : discoveryErrors) {
                report.append("  - ").append(error).append("\n");
            }
        }

        return report.toString();
    }

    private void detectProviders() {
        // Check Jackson
        checkProvider("Jackson", "com.fasterxml.jackson.databind.ObjectMapper");

        // Check Gson
        checkProvider("Gson", "com.google.gson.Gson");

        // Check JSON-B
        checkProvider("JSON-B", "javax.json.bind.Jsonb");
    }

    private void checkProvider(String name, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            detectedProviders.add(name);

            // Try to get version information
            Package pkg = clazz.getPackage();
            String version = pkg != null ? pkg.getImplementationVersion() : "unknown";
            classpathInfo.put(name, version != null ? version : "unknown");

        } catch (ClassNotFoundException e) {
            discoveryErrors.add(name + " not found on classpath: " + className);
        }
    }

    private String determineSelectedProvider() {
        try {
            JsonProcessorFactory.createDefault();
            // If we can create a default, get the first available provider
            List<String> available = JsonProcessorFactory.getAvailableProcessors();
            return available.isEmpty() ? "None" : available.get(0);
        } catch (Exception e) {
            return "None (error: " + e.getMessage() + ")";
        }
    }
}