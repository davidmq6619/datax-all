package com.sinohealth.datax.processors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.sinohealth.datax.common.CommonData;
import com.sinohealth.datax.common.Processor;
import com.sinohealth.datax.entity.source.RegCustomer;
import com.sinohealth.datax.entity.source.StandardCustomerRecord;
import com.sinohealth.datax.entity.zktarget.StandardCustomerRecordList;

import java.util.ArrayList;

/**
 * @author mingqiang
 * @date 2022/08/29
 * 获取用户基本信息
 **/
public class RegCustomerProcessor implements Processor<RegCustomer, StandardCustomerRecordList> {

    @Override
    public StandardCustomerRecordList dataProcess(RegCustomer customer, StandardCustomerRecordList list, CommonData commonData) {
        ArrayList<StandardCustomerRecord> listRecord = new ArrayList<>();
        StandardCustomerRecord standardCustomerRecord = new StandardCustomerRecord();
        BeanUtil.copyProperties(customer, standardCustomerRecord);
        standardCustomerRecord.setCleanTime(DateUtil.date(System.currentTimeMillis()));
        String formatBirthDate = DateUtil.format(customer.getBirthDate(), "yyyy-MM-dd");
        standardCustomerRecord.setCustomerCsrq(formatBirthDate);
        String format = DateUtil.format(customer.getCheckTime(), "yyyy-MM-dd");
        standardCustomerRecord.setApplyTime(format);
        //standardCustomerRecord.setMobile();
        long age = DateUtil.betweenYear(customer.getBirthDate(), customer.getCheckTime(), true);
        standardCustomerRecord.setAge(String.valueOf(age));
        standardCustomerRecord.setVid(customer.getVid());
        standardCustomerRecord.setSex(customer.getSex().toString());
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
