package implement;

import com.oocourse.specs3.models.NodeIdNotFoundException;
import com.oocourse.specs3.models.NodeNotConnectedException;
import com.oocourse.specs3.models.Path;
import com.oocourse.specs3.models.PathIdNotFoundException;
import com.oocourse.specs3.models.PathNotFoundException;
import com.oocourse.specs3.models.RailwaySystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class MyRailwaySystem implements RailwaySystem {
    /* Graph class*/
    private static ArrayList<Path> paList = new ArrayList<>();
    private static ArrayList<Integer> pidList = new ArrayList<>();
    // <idPath, path>
    private static HashMap<Integer, Path> map = new HashMap<>();
    // nodeId map <nodeId, nodeNum>
    private static HashMap<Integer, Integer> nodeMap = new HashMap<>();
    private static int idPath = 0; // unique path id
    private static int count = 0; // store all distinct nodes
    private static int nodeNum = 0; // nodeNum of map nodeId
    private ShortestPath struct = new ShortestPath();
    /* Railway class */
    // blockCount
    private static int blockCount; // block count
    private ConnectB block = new ConnectB(); // connected block
    private static boolean firstAdd = true; // addPath weather first
    // transCount
    private LeastTrans trans = new LeastTrans();
    // UnpleasentCount
    private LeastUnp unPleasent = new LeastUnp();
    // Price
    private LeastPrice price = new LeastPrice();

    public MyRailwaySystem() {
    }

    /* get private property */
    protected boolean[][] getGraph() {
        return this.struct.getGraph();
    }

    /* methods extend Graph class */
    public boolean containsNode(int nodeId) { // all Path
        for (Path path : map.values()) { // containsPath(path) is true
            if (path.isValid() && containsPath(path) &&
                    path.containsNode(nodeId)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsEdge(int fromNodeId, int toNodeId) {
        for (Path path : map.values()) {
            if (path.isValid() && containsPath(path)) {
                for (int i = 0; i < path.size() - 1; i++) {
                    if ((path.getNode(i) == fromNodeId && path.getNode(i + 1)
                            == toNodeId) || (path.getNode(i) == toNodeId &&
                            path.getNode(i + 1) == fromNodeId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isConnected(int fromNodeId, int toNodeId) throws
            NodeIdNotFoundException {
        if (!containsNode(fromNodeId)) {
            throw new NodeIdNotFoundException(fromNodeId);
        } else if (!containsNode(toNodeId)) {
            throw new NodeIdNotFoundException(toNodeId);
        } else {
            if (fromNodeId != toNodeId) {
                int valueFrom = nodeMap.get(fromNodeId);
                int valueTo = nodeMap.get(toNodeId);
                return (struct.getConnected())[valueFrom][valueTo];
            }
            return true;
        }
    }

    public int getShortestPathLength(int fromNodeId, int toNodeId) throws
            NodeIdNotFoundException, NodeNotConnectedException {
        if (!containsNode(fromNodeId)) {
            throw new NodeIdNotFoundException(fromNodeId);
        } else if (!containsNode(toNodeId)) {
            throw new NodeIdNotFoundException(toNodeId);
        } else if (!isConnected(fromNodeId, toNodeId)) { // ensure connected
            throw new NodeNotConnectedException(fromNodeId, toNodeId);
        } else {
            if (fromNodeId == toNodeId) {
                return 0;
            }
            int valueFrom = nodeMap.get(fromNodeId);
            int valueTo = nodeMap.get(toNodeId);
            return (struct.getDis())[valueFrom][valueTo]; // dis is valid
        }
    }

    public int size() {
        return map.size(); // equals key.size()
    }

    public boolean containsPath(Path path) {
        if (path != null) {
            return map.containsValue(path);
        }
        return false;
    }

    public boolean containsPathId(int pathId) {
        return map.containsKey(pathId);
    }

    public Path getPathById(int pathId) throws PathIdNotFoundException {
        if (!containsPathId(pathId)) {
            throw new PathIdNotFoundException(pathId); // return error pathId
        } else {
            return map.get(pathId);
        }
    }

    public int getPathId(Path path) throws PathNotFoundException {
        if (path == null || !path.isValid() || !containsPath(path)) {
            throw new PathNotFoundException(path); // return error path
        } else {
            return pidList.get(paList.indexOf(path));
        }
    }

    public int addPath(Path path) {
        if (path != null && path.isValid()) {
            boolean hasPath = containsPath(path); // old(containsPath(path))
            if (!hasPath) { // Do not add already exist path
                paList.add(path);
                idPath++;
                pidList.add(idPath);
                map.put(idPath, path);
                distinctNodeCount(); // calculate node count
                if (firstAdd) { // first addPath create
                    struct.create(map, nodeMap);
                    block.create(nodeMap, getGraph());
                    trans.create(map, nodeMap);
                    firstAdd = false;
                } else { // add path nodes
                    ArrayList<Integer> nodes = ((MyPath) path).getMyPath();
                    for (int i = 0; i < nodes.size() - 1; i++) {
                        int nodeU = nodeMap.get(nodes.get(i));
                        int nodeV = nodeMap.get(nodes.get(i + 1));
                        block.put(nodeU); // father.put()
                        block.put(nodeV); // father.put()
                        block.unio(nodeU, nodeV); // add unio(x, y)
                    }
                    struct.put(path, nodeMap); // add struct
                    trans.put(path, nodeMap); // add trans
                }
                price.create(map, nodeMap);
                unPleasent.create(map, nodeMap);
                getBlockCount(); // get updated blocks
                return idPath;
            }
            return pidList.get(paList.indexOf(path)); // path exist,return id
        }
        return 0;
    }

    public int removePath(Path path) throws PathNotFoundException {
        if (path == null || !path.isValid() || !containsPath(path)) {
            throw new PathNotFoundException(path);
        } else {
            int id = getPathId(path);
            paList.remove(path);
            // ps: id isn't an index, it is an object
            pidList.remove(pidList.lastIndexOf(id));
            map.remove(id, path);
            distinctNodeCount(); // calculate node count
            /* update */
            struct.create(map, nodeMap); // calculate floyd
            trans.create(map, nodeMap);
            price.create(map, nodeMap);
            unPleasent.create(map, nodeMap);
            block.create(nodeMap, getGraph()); // afresh connected blocks
            getBlockCount(); // get updated blocks
            return id;
        }
    }

    public void removePathById(int pathId) throws PathIdNotFoundException {
        if (!containsPathId(pathId)) {
            throw new PathIdNotFoundException(pathId);
        } else {
            Path path = getPathById(pathId);
            paList.remove(path);
            pidList.remove(pidList.lastIndexOf(pathId));
            map.remove(pathId, path);
            distinctNodeCount(); // calculate node count
            /* update */
            struct.create(map, nodeMap); // calculate floyd
            trans.create(map, nodeMap); // afresh trans
            price.create(map, nodeMap);
            unPleasent.create(map, nodeMap);
            block.create(nodeMap, getGraph()); // afresh connected blocks
            getBlockCount(); // get updated blocks
        }
    }

    public int getDistinctNodeCount() { //在容器全局范围内查找不同的节点数
        return count;
    }

    private void distinctNodeCount() { // all
        nodeNum = 0; // assign 0
        nodeMap.clear(); // clear
        HashSet<Integer> arr = new HashSet<>();
        for (Path it : paList) { // optimize
            HashSet<Integer> nodes = new HashSet<>(((MyPath) it).getMyPath());
            arr.addAll(nodes);
            mapNodeId(it); // map nodeId to 0-249
        }
        count = arr.size();
    }

    private void mapNodeId(Path path) { // 映射结点号
        LinkedHashSet<Integer> myNode = new LinkedHashSet<>(
                ((MyPath) path).getMyPath());
        for (Integer nodeId : myNode) {
            if (!nodeMap.containsKey(nodeId)) {
                nodeMap.put(nodeId, nodeNum); // 0-249
                nodeNum++;
            }
        }
    }

    /* this class methods */
    public int getLeastTicketPrice(int fromNodeId, int toNodeId) throws
            NodeIdNotFoundException, NodeNotConnectedException {
        if (!containsNode(fromNodeId)) {
            throw new NodeIdNotFoundException(fromNodeId);
        } else  if (!containsNode(toNodeId)) {
            throw new NodeIdNotFoundException(toNodeId);
        } else if (!isConnected(fromNodeId, toNodeId)) {
            throw new NodeNotConnectedException(fromNodeId, toNodeId);
        } else {
            if (fromNodeId == toNodeId) {
                return 0;
            }
            int valueFrom = nodeMap.get(fromNodeId);
            int valueTo = nodeMap.get(toNodeId);
            return (price.getWeight())[valueFrom][valueTo] - 2; // weight - 2
        }
    }

    public int getLeastTransferCount(int fromNodeId, int toNodeId) throws
            NodeIdNotFoundException, NodeNotConnectedException {
        if (!containsNode(fromNodeId)) {
            throw new NodeIdNotFoundException(fromNodeId);
        } else  if (!containsNode(toNodeId)) {
            throw new NodeIdNotFoundException(toNodeId);
        } else if (!isConnected(fromNodeId, toNodeId)) {
            throw new NodeNotConnectedException(fromNodeId, toNodeId);
        } else {
            if (fromNodeId == toNodeId) {
                return 0;
            }
            int valueFrom = nodeMap.get(fromNodeId);
            int valueTo = nodeMap.get(toNodeId);
            return (trans.getWeight())[valueFrom][valueTo] - 1; // weight - 1
        }
    }

    public int getLeastUnpleasantValue(int fromNodeId, int toNodeId) throws
            NodeIdNotFoundException, NodeNotConnectedException {
        if (!containsNode(fromNodeId)) {
            throw new NodeIdNotFoundException(fromNodeId);
        } else  if (!containsNode(toNodeId)) {
            throw new NodeIdNotFoundException(toNodeId);
        } else if (!isConnected(fromNodeId, toNodeId)) {
            throw new NodeNotConnectedException(fromNodeId, toNodeId);
        } else {
            if (fromNodeId == toNodeId) {
                return 0;
            }
            int valueFrom = nodeMap.get(fromNodeId);
            int valueTo = nodeMap.get(toNodeId);
            // weight - 32
            return (unPleasent.getWeight())[valueFrom][valueTo] - 32;
        }
    }

    private void getBlockCount() {
        blockCount = block.getBlockCount(nodeMap);
    }

    public int getConnectedBlockCount() {
        return blockCount;
    }

    private Path isContainsPalist(Path[] pseq) { // weather contains Path[]
        if (pseq != null) {
            for (Path path : pseq) {
                if (!containsPath(path)) {
                    return path; // return not exist path
                }
            }
        }
        return null; // null is contains Path[]
    }

    public boolean containsPathSequence(Path[] pseq) {
        return false;
    }

    public boolean isConnectedInPathSequence(
            Path[] pseq, int fromNodeId, int toNodeId) throws
            NodeIdNotFoundException, PathNotFoundException {
        Path path = isContainsPalist(pseq);
        if (!containsNode(fromNodeId)) {
            throw new NodeIdNotFoundException(fromNodeId);
        } else  if (!containsNode(toNodeId)) {
            throw new NodeIdNotFoundException(toNodeId);
        } else if (pseq == null || path != null) {
            throw new PathNotFoundException(path);
        } else {
            return true;
        }
    }

    public int getTicketPrice(
            Path[] pseq, int fromNodeId, int toNodeId) {
        return 0;
    }

    public int getUnpleasantValue(
            Path path, int fromIndex, int toIndex) { // get this path value
        return 0;
    }

    public int getUnpleasantValue(Path path, int[] idx) {
        return 0;
    }

    public int getUnpleasantValue(
            Path[] pseq, int fromNodeId, int toNodeId) {
        return 0;
    }
}
