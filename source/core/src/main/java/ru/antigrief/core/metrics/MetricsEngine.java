package ru.antigrief.core.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class MetricsEngine {
    private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();

    public void increment(String metric) {
        counters.computeIfAbsent(metric, k -> new LongAdder()).increment();
    }

    public long getCount(String metric) {
        LongAdder adder = counters.get(metric);
        return adder != null ? adder.sum() : 0;
    }

    public Map<String, Long> getAllMetrics() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> result.put(k, v.sum()));
        return result;
    }
}
