package com.ravi.kickstart;



import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    Object value;
    int recordCount;
    String nodeName;
    String nodePath;
    NodeType nodeType;
    List<TreeNode> childNodes;

    // list of records for the node
    List<JSONRecord> records;

    enum NodeType {
        ARRAY, OBJECT, PREMITIVE
    }

    TreeNode(Object value, String nodeName, NodeType nodeType) {

        this(nodeName, nodeType);
        this.value = value;
    }

    TreeNode(String nodeName, NodeType nodeType) {

        this.nodeName = nodeName;
        this.nodeType = nodeType;
    }

    TreeNode(Object value, String nodeName, NodeType nodeType, String nodePath) {

        this(value, nodeName, nodeType);
        this.nodePath = nodePath;
    }

    String getNodePath() {

        return nodePath;
    }

    @SuppressWarnings("unused")
    void setNodePath(String nodePath) {

        this.nodePath = nodePath;
    }

    Object getValue() {

        return value;
    }

    void setValue(Object value) {

        this.value = value;
    }

    @SuppressWarnings("unused")
    String getNodeName() {

        return nodeName;
    }

    void setNodeName(String nodeName) {

        this.nodeName = nodeName;
    }

    NodeType getNodeType() {

        return nodeType;
    }

    void setNodeType(NodeType nodeType) {

        this.nodeType = nodeType;
    }

    List<TreeNode> getChildNodes() {

        return childNodes;
    }

    void addChildNode(TreeNode dataNode) {

        if (this.childNodes == null || this.childNodes.isEmpty()) {
            this.childNodes = new ArrayList<>();
        }
        this.childNodes.add(dataNode);
    }

    @SuppressWarnings("unused")
    void setChildNodes(List<TreeNode> nextNodes) {

        this.childNodes = nextNodes;
    }

    int getRecordCount() {

        return recordCount;
    }

    void setRecordCount(int recordCount) {

        this.recordCount = recordCount;
    }

    List<JSONRecord> getRecords() {

        return records;
    }

    void setRecords(List<JSONRecord> records) {

        this.records = records;
    }

    void addRecord(JSONRecord record) {

        if (this.records == null) {
            this.records = new ArrayList<>();
        }
        this.records.add(record);
    }
}

