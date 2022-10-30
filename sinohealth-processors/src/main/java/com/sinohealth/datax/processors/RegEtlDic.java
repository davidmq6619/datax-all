package com.sinohealth.datax.processors;


import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.sinohealth.datax.common.CommonData;
import com.sinohealth.datax.common.Processor;
import com.sinohealth.datax.entity.common.BasCheckItem;
import com.sinohealth.datax.entity.common.StandardBasTestItem;
import com.sinohealth.datax.entity.source.*;
import com.sinohealth.datax.entity.zktarget.StandardRegStdInfoList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author mingqiang
 * @date 2022/08/29
 * 获取用户基本信息
 **/
public class RegEtlDic implements Processor<RegStdInfo, StandardRegStdInfoList> {
    public static final Logger LOG = LoggerFactory.getLogger(RegEtlDic.class);

    @Override
    public StandardRegStdInfoList dataProcess(RegStdInfo stdInfo, StandardRegStdInfoList list, CommonData commonData) {

        ArrayList<StandardRegStdInfo> recordList = new ArrayList<>();
        String zkItemName = stdInfo.getItemName();
        String detailItemName = stdInfo.getFieldName();
        try {
            StandardRegStdInfo standardMnStdInfo = new StandardRegStdInfo();
            standardMnStdInfo.setItemType(stdInfo.getItemType());
            standardMnStdInfo.setItemName(stdInfo.getItemName());
            standardMnStdInfo.setFieldName(stdInfo.getFieldName());
            standardMnStdInfo.setStd3MedicalName(stdInfo.getStd3MedicalName());
            standardMnStdInfo.setResultNum(stdInfo.getResultNum());
            standardMnStdInfo.setBranchNum(stdInfo.getBranchNum());
            //匹配规则统一小写+英文括号
            Map<String, StandardBasTestItem> basTestItemMap = commonData.getBasTestItemMap();
            Map<String, StandardBasTestItem> basTestMethodItemMap = commonData.getBasTestMethodItemMap();
            Map<String, BasCheckItem> basCheckItemMap = commonData.getBasCheckItemMap();
            String itemFt = "";
            String itemName = stdInfo.getFieldName();
            if (StrUtil.isNotBlank(stdInfo.getItemName())) {
                itemFt = stdInfo.getItemName()
                        .replace("（", "(")
                        .replace("）", ")").toLowerCase().trim();
            }
            if (StrUtil.isNotBlank(itemName)) {
                itemName = itemName.replace("（", "(")
                        .replace("）", ")")
                        .toLowerCase().trim();
            }
            String item = itemFt + ":" + itemName;
            StandardBasTestItem basTestItem = basTestMethodItemMap.get(item);
            if (Objects.isNull(basTestItem)) {
                basTestItem = basTestItemMap.get(itemName);
            }
            if (Objects.nonNull(basTestItem)) {
                standardMnStdInfo.setZkStdMedicalMame(basTestItem.getItemNameCStandard());
            }
            if(StrUtil.isNotBlank(zkItemName) && StrUtil.isBlank(standardMnStdInfo.getZkStdMedicalMame())) {
                if(detailItemName.contains("X光") || detailItemName.contains("报告") ||
                        detailItemName.contains("详") || detailItemName.contains("结果")){
                    zkItemName = zkItemName.replaceAll("[(（].*[)）]","");
                    boolean flag = false;
                    if(zkItemName.startsWith("DR")){
                        flag = true;
                        zkItemName = zkItemName.substring(2);
                    }
                    if(zkItemName.contains("DR")){
                        zkItemName = zkItemName.substring(0, zkItemName.indexOf("DR")+2);
                        standardMnStdInfo.setZkStdMedicalMame(zkItemName);
                    }else if(zkItemName.contains("正")){
                        zkItemName = zkItemName.replace("正", "DR");
                        zkItemName = zkItemName.substring(0, zkItemName.indexOf("DR")+2);
                        standardMnStdInfo.setZkStdMedicalMame(zkItemName);
                    }else if(zkItemName.contains("侧")){
                        zkItemName = zkItemName.replace("侧", "DR");
                        zkItemName = zkItemName.substring(0, zkItemName.indexOf("DR")+2);
                        standardMnStdInfo.setZkStdMedicalMame(zkItemName);
                    } else if(zkItemName.contains("片")){
                        zkItemName = zkItemName.replace("片", "DR");
                        zkItemName = zkItemName.substring(0, zkItemName.indexOf("DR")+2);
                        standardMnStdInfo.setZkStdMedicalMame(zkItemName);
                    }else if(flag){
                        standardMnStdInfo.setZkStdMedicalMame(zkItemName+"DR");
                    }
                }
            }

            //检验方法
            String zkStd = "";
             if (StrUtil.isNotBlank(detailItemName) && StrUtil.isBlank(standardMnStdInfo.getZkStdMedicalMame())) {
                //MR,CT,DR,核磁-》MR,彩超 E超 B超-》超声，红外-》TMT
                if (detailItemName.contains("心电图")) {
                    zkStd = "心电图";
                } else if (detailItemName.contains("胃镜")) {
                    zkStd = "胃镜";
                } else if (detailItemName.contains("肠镜")) {
                    zkStd = "肠镜";
                } else if (detailItemName.contains("MR")) {
                    zkStd = detailItemName.substring(0, detailItemName.indexOf("MR") + 2);
                } else if (detailItemName.contains("CT")) {
                    zkStd = detailItemName.substring(0, detailItemName.indexOf("CT") + 2);
                } else if (detailItemName.contains("核磁")) {
                    detailItemName = detailItemName.replace("核磁", "MR");
                    zkStd = detailItemName.substring(0, detailItemName.indexOf("MR") + 2);
                } else if (detailItemName.contains("彩超") || detailItemName.contains("E超") || detailItemName.contains("B超") || detailItemName.contains("超声")) {
                    detailItemName = detailItemName.replace("彩超", "超声")
                            .replace("B超", "超声")
                            .replace("E超", "超声");
                    zkStd = detailItemName.substring(0, detailItemName.indexOf("超声") + 2);
                } else if (detailItemName.contains("红外")) {
                    detailItemName = detailItemName.replace("红外", "TMT");
                    zkStd = detailItemName.substring(0, detailItemName.indexOf("TMT") + 3);
                } else if (detailItemName.contains("DR")) {
                    zkStd = detailItemName.substring(0, detailItemName.indexOf("DR") + 2);
                }
            }
            if (StrUtil.isNotBlank(zkStd)) {
                standardMnStdInfo.setZkStdMedicalMame(zkStd);
            }
            //检查项
            if (StrUtil.isBlank(standardMnStdInfo.getZkStdMedicalMame())) {
                if (StrUtil.isNotBlank(detailItemName)) {
                    detailItemName = detailItemName.replace("（", "(")
                            .replace("）", ")").toLowerCase().trim();
                }
                BasCheckItem basCheckItem = basCheckItemMap.get(detailItemName);
                if (Objects.nonNull(basCheckItem)) {
                    standardMnStdInfo.setZkStdMedicalMame(basCheckItem.getItemNameStandard());
                }
            }

            if (StrUtil.isBlank(standardMnStdInfo.getZkStdMedicalMame())) {
                String itmeName = standardMnStdInfo.getItemName();
                if (StrUtil.isNotBlank(standardMnStdInfo.getItemName())) {
                    itmeName = itmeName.replace("（", "(")
                            .replace("）", ")").toLowerCase().trim();
                }
                StandardBasTestItem basTestItemT = basTestItemMap.get(itmeName);
                if (Objects.nonNull(basTestItemT)) {
                    standardMnStdInfo.setZkStdMedicalMame(basTestItemT.getItemNameCStandard());
                }
            }
            if (StrUtil.isBlank(standardMnStdInfo.getZkStdMedicalMame())) {
                String itmeName = standardMnStdInfo.getItemName();
                if (StrUtil.isNotBlank(standardMnStdInfo.getItemName())) {
                    itmeName = itmeName.replace("（", "(")
                            .replace("）", ")").trim();
                }
                BasCheckItem basCheckItem = basCheckItemMap.get(itmeName);
                if (Objects.nonNull(basCheckItem)) {
                    standardMnStdInfo.setZkStdMedicalMame(basCheckItem.getItemNameStandard());
                }
            }

            recordList.add(standardMnStdInfo);
            list.setList(recordList);
        } catch (Exception e) {
            LOG.error("数据异常，入参数据{},数据异常{}", JSON.toJSONString(stdInfo),
                    e.getMessage(), e);
        }
        return list;
    }
}
