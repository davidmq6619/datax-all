package com.sinohealth.datax.entity.target;

import com.sinohealth.datax.entity.source.StandardCustomerRecord;
import com.sinohealth.datax.entity.source.StandardTestRecord;

import java.util.List;

/**
 * @author mingqiang
 * @date 2022/8/22 - 15:34
 * @desc
 */
public class StandardCustomerRecordList {

    public List<StandardCustomerRecord> list;

    public List<StandardCustomerRecord> getList() {
        return list;
    }

    public void setList(List<StandardCustomerRecord> list) {
        this.list = list;
    }
}
