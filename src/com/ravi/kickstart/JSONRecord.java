package com.ravi.kickstart;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Class to represent the raw of the flattened json.
 */
@Getter
@Setter
public class JSONRecord implements Serializable,Cloneable{
	private static final long serialVersionUID = 1L;
	private Map<String, Object> cells;
    private long sys_partition;
    private long sys_offset;
    private long sys_timestamp;

    public JSONRecord() {

        cells = new LinkedHashMap<>();
    }

    public JSONRecord(String columName, Object columnValue) {

        this.cells = new LinkedHashMap<>();
        this.cells.put(columName, columnValue);
    }

    public Map<String, Object> getCells() {

        return cells;
    }

    public JSONRecord mergeRecord(JSONRecord record) {

        this.cells.putAll(record.getCells());
        return this;
    }

    public void addCell(String columName, Object columnValue) {

        if (this.cells == null) {
            this.cells = new LinkedHashMap<>();
        }
        this.cells.put(columName, columnValue);
    }

    public Object clone()throws CloneNotSupportedException{
        return super.clone();
    }
}
