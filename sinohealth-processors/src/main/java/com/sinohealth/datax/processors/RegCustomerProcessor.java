package com.sinohealth.datax.processors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.sinohealth.datax.common.CommonData;
import com.sinohealth.datax.common.Processor;
import com.sinohealth.datax.entity.source.BasCustomer;
import com.sinohealth.datax.entity.source.StandardCustomerRecord;
import com.sinohealth.datax.entity.target.StandardCustomerRecordList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mingqiang
 * @date 2022/08/29
 * 获取用户基本信息
 **/
public class RegCustomerProcessor implements Processor<BasCustomer, StandardCustomerRecordList> {

    @Override
    public StandardCustomerRecordList dataProcess(BasCustomer customer, StandardCustomerRecordList list, CommonData commonData) {
        ArrayList<StandardCustomerRecord> listRecord = new ArrayList<>();
        StandardCustomerRecord standardCustomerRecord = new StandardCustomerRecord();
        BeanUtil.copyProperties(customer, standardCustomerRecord);
        standardCustomerRecord.setCleanTime(DateUtil.date(System.currentTimeMillis()));
        standardCustomerRecord.setCustomerCsrq(customer.getBirthday());
        standardCustomerRecord.setApplyTime(customer.getCheckTime());
        standardCustomerRecord.setMobile(customer.getTel());
        standardCustomerRecord.setVid(customer.getMemberId());
        if("男".equals(customer.getSex())){
            standardCustomerRecord.setSex("1");
        }else{
            standardCustomerRecord.setSex("0");
        }
        String csrq = standardCustomerRecord.getCustomerCsrq();
        String applyTime =standardCustomerRecord.getApplyTime();
        if(StrUtil.isNotBlank(csrq) && StrUtil.isNotBlank(applyTime)){
            long year = DateUtil.betweenYear(DateUtil.parse(csrq), DateUtil.parse(applyTime), false);
            standardCustomerRecord.setAge(String.valueOf(year));
        }
        listRecord.add(standardCustomerRecord);
        list.setList(listRecord);
        return list;
    }
}
