package com.ravi.kickstart;



import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.LinkedHashSet;

import net.minidev.json.JSONArray;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Class is used to flatten the json data. This will only select the values
 * specified by jsonPaths.
 *
 * @author Vijay
 * @since 03/24/18
 */
public final class JsonFlattener {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(JsonFlattener.class);
    private String jsonName = null;
    private List<String> jsonPaths = null;
    private char separator = ',';
    private Map<String, Map<String, Set<String>>> jsonPathMapCache = new LinkedHashMap<>();

    /**
     * @param jsonName  name of the json which is used to cache map structure created
     *                  using json paths.
     * @param jsonPaths list of json paths. @see
     *                  <a href="https://github.com/json-path/JsonPath">this</a> for json
     *                  path.
     */
    public JsonFlattener(String jsonName, List<String> jsonPaths) {

        requireNonNull(jsonPaths, "json path list should not be null");
        checkArgument(!jsonPaths.isEmpty(), "json path list should not be empty");
        this.jsonName = jsonName;
        this.jsonPaths = jsonPaths;
    }

    public JsonFlattener(String jsonName, List<String> jsonPaths, char separator) {

        this(jsonName, jsonPaths);
        this.setSeparator(separator);
    }

    /**
     * Flattens json passed as argument. It will have values specified by
     * {@link JsonFlattener#jsonPaths}. {@link JsonFlattener#separator} will be used
     * to separate the column values.
     *
     * @param json input json that needs to be flattened.
     * @return list of raw values.
     */
    public List<JSONRecord> flatten(String json) {
        if(LOGGER.isTraceEnabled()){
            LOGGER.trace("JSON Payload:{}",json);
        }
        if (json == null || json.trim().isEmpty()) {
            // return empty list
            return new ArrayList<>(0);
        }
        Map<String, Set<String>> objectMap = toJsonPathMap();
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        List<JSONRecord> finalRecords = new ArrayList<>();
        if (document instanceof JSONArray) {
            JSONArray array = (JSONArray) document;
            array.forEach((eachDocument) -> {
                Optional<List<JSONRecord>> records = flattenObject(eachDocument, objectMap);
                if (records.isPresent()) {
                    finalRecords.addAll(records.get());
                }
            });
        } else {
            Optional<List<JSONRecord>> records = flattenObject(document, objectMap);
            if (records.isPresent()) {
                finalRecords.addAll(records.get());
            }
        }
        return finalRecords;
    }

    private Optional<List<JSONRecord>> flattenObject(Object document, Map<String, Set<String>> objectMap) {

        TreeNode rootNode = new TreeNode(document, "$", TreeNode.NodeType.OBJECT, "$");
        generateTree(objectMap, rootNode);
        return Optional.ofNullable(createRecords(rootNode));
    }

    /**
     * Creates map structure which will have key as the json path element and value
     * as set of elements which are under key according to json paths. e.g for $.Id
     * an $.Name will have $ as the key and set of {Id,Name} as the value.
     */
    private Map<String, Set<String>> toJsonPathMap() {

        LOGGER.debug("Creating json path map for jsonName : %s", jsonName);
        Map<String, Set<String>> jsonPathMap = jsonPathMapCache.get(jsonName);

        if (jsonPathMap != null) {
            return jsonPathMap;
        } else {
            jsonPathMap = new LinkedHashMap<>();
            jsonPathMap.put("$", new LinkedHashSet<>());
            for (String path : jsonPaths) {
                String[] components = path.split("\\.");
                String parentComponent = "$";
                for (int i = 1; i < components.length; i++) {
                    if (!jsonPathMap.containsKey(parentComponent)) {
                        Set<String> childSet = new LinkedHashSet<>();
                        childSet.add(components[i]);
                        jsonPathMap.put(parentComponent, childSet);
                    } else {
                        jsonPathMap.get(parentComponent).add(components[i]);
                    }
                    parentComponent += ".".concat(components[i]);
                }
            }
            jsonPathMapCache.putIfAbsent(jsonName, jsonPathMap);
            LOGGER.debug("JsonPath map : %s", jsonPathMap);
            return jsonPathMap;
        }
    }

    /**
     * Generates the tree which maps to json structure which will be used then to
     * traverse and generate the records. Here, tree is n-ary tree which represenets
     * json. Tree node can be of 3 types which can be Object, array or any primitive
     * value.
     * <p>
     * If node is of type is Object, it represents JsonObject and it has
     * corresponding JsonObject as value. If node is of type array, it as list of
     * JsonObjects as the value of the node where each JsonObject represent each
     * object in JsonArray. If node is of type primitive, it should be leaf node and
     * represents record of the flattened json.
     *
     * @param jsonPathMap jsonPathMap generated using {@link JsonFlattener#toJsonPathMap()}
     * @param rootNode    currently running node in tree.
     * @return number of records under the node.
     */
    private int generateTree(Map<String, Set<String>> jsonPathMap, TreeNode rootNode) {

        LOGGER.debug("Generating tree for flattening json");
        Set<String> childNodeList = jsonPathMap.get(rootNode.getNodePath());

        int levelRecordCount = 1;
        if (childNodeList != null && !childNodeList.isEmpty()) {
            for (String childNodeName : childNodeList) {
                boolean isArray = childNodeName.contains("[*]");
                String jsonPath = "$.".concat(childNodeName);
                String childNodeJsonPath = rootNode.getNodePath().concat(".").concat(childNodeName);
                Object value = null;

                try {
                    if (rootNode.getValue() != null) {
                        value = JsonPath.read(rootNode.getValue(), jsonPath);
                        if(isArray) {
                            JSONArray jsonArray = (JSONArray) value;
                            if (jsonArray.isEmpty())
                                value = null;
                        }
                    }
                } catch (Exception exp) {
                    // catch the exception so that we can put null as the value
                    if(LOGGER.isDebugEnabled()){
                        LOGGER.debug("Exception while reading value from JSONPath:",exp);
                    }
                }

                TreeNode childNode = new TreeNode(value, childNodeName, TreeNode.NodeType.OBJECT, childNodeJsonPath);
                rootNode.addChildNode(childNode);

                int records = 0;

                if (isArray) {
                    // If array, visit each of the object in JsonArray and add it to the value of
                    // the tree node.
                    List<TreeNode> arrayNodes = new ArrayList<>();
                    childNode.setValue(arrayNodes);
                    childNode.setNodeType(TreeNode.NodeType.ARRAY);

                    if (value != null) {
                        JSONArray jsonArray = (JSONArray) value;
                        for (Object eachJsonObject : jsonArray) {
                            TreeNode arrayNode = new TreeNode(eachJsonObject, childNodeName, TreeNode.NodeType.OBJECT,
                                    childNode.getNodePath());
                            arrayNodes.add(arrayNode);
                            int nodeRecords = generateTree(jsonPathMap, arrayNode);
                            arrayNode.setRecordCount(nodeRecords);
                            records += nodeRecords;
                        }
                    } else {
                        TreeNode arrayNode = new TreeNode(null, childNodeName, TreeNode.NodeType.OBJECT,
                                childNode.getNodePath());
                        arrayNodes.add(arrayNode);
                        int nodeRecords = generateTree(jsonPathMap, arrayNode);
                        arrayNode.setRecordCount(nodeRecords);
                        records += nodeRecords;
                    }

                    childNode.setRecordCount(records);

                } else {
                    boolean isObject = jsonPathMap.get(childNode.getNodePath()) != null;
                    if (isObject) {
                        int nodeRecords = generateTree(jsonPathMap, childNode);
                        childNode.setRecordCount(nodeRecords);
                        records = nodeRecords;
                    } else {
                        childNode.setNodeType(TreeNode.NodeType.PREMITIVE);
                        childNode.setRecordCount(1);
                        records = 1;
                    }
                }

                levelRecordCount *= records;
            }
        }
        return levelRecordCount;
    }

    /**
     * Create the records by traversing the whole tree in depth first manner and
     * uses bottom up approach.
     *
     * @param rootNode root node of the tree generated using
     *                 {@link JsonFlattener#generateTree(Map, TreeNode)}
     * @return list of flattened records which is represented as
     * {@link JSONRecord}
     */
    @SuppressWarnings("unchecked")
    private List<JSONRecord> createRecords(TreeNode rootNode) {

        LOGGER.debug("Creating records for json");
        List<TreeNode> childNodes = rootNode.getChildNodes();

        for (TreeNode childNode : childNodes) {
            if (childNode.getNodeType() == TreeNode.NodeType.ARRAY) {
                // This is array of nodes, hence we need tovisit each of the array node and
                // create
                // records for each.
                List<TreeNode> arrayNodes = (List<TreeNode>) childNode.getValue();
                List<JSONRecord> recordsForArrayNodes = new ArrayList<>();
                for (TreeNode arrayNode : arrayNodes) {
                    List<JSONRecord> records = createRecords(arrayNode);
                    arrayNode.setRecords(records);
                    // Records get added for each of the node in the array.
                    recordsForArrayNodes.addAll(records);
                }
                childNode.setRecords(recordsForArrayNodes);
            } else {
                if (childNode.getChildNodes() == null) {
                    // This is leaf node, hence required for our flattened record.
                    if (childNode.getNodeType() != TreeNode.NodeType.OBJECT) {
                        childNode.addRecord(new JSONRecord(childNode.getNodePath(), childNode.getValue()));
                    }
                } else {
                    // This is single object node. Create record by traversing childs under this
                    // node.
                    List<JSONRecord> records = createRecords(childNode);
                    childNode.setRecords(records);
                }
            }
        }

        // Travers each element at the same level and do cross join of records of each
        // node.
        List<JSONRecord> recordList = new ArrayList<>();
        int prevRecordCount = 1;
        for (TreeNode childNode : childNodes) {
            List<JSONRecord> newRecordList = new ArrayList<>();
            for (int j = 0; j < prevRecordCount; j++) {
                for (int k = 0; k < childNode.getRecordCount(); k++) {
                    // if it is first record, then we don't need to copy the record while merging as
                    // it is already
                    // there.
                    JSONRecord record = recordList.isEmpty() ? new JSONRecord()
                            : ((k == 0) ? recordList.get(j) : new JSONRecord().mergeRecord(recordList.get(j)));
                    if (childNode.getRecords() != null) {
                        record.mergeRecord(childNode.getRecords().get(k));
                    }
                    newRecordList.add(record);
                }
            }
            prevRecordCount = newRecordList.size();
            recordList = newRecordList;
        }
        return recordList;
    }

	public char getSeparator() {
		return separator;
	}

	public void setSeparator(char separator) {
		this.separator = separator;
	}
}
