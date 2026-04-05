package api;

public interface ICacheMetrics {
    double hitRate();
    double missRate();
    long totalRequests();
    long totalHits();
    long totalMisses();
    long totalEvictions();
    long totalExpiries();
    void resetMetrics();
}
