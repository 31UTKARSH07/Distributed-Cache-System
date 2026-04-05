package distribution;

import java.util.List;

public interface INodeMapper <K> {
    String getNode(K key);
    List<String> getNodes(K key, int replicationFactor);
    void addNode(String node);
    void removeNode(String node);
    List<String> allNodes();
}
