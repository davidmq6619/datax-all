package com.sinohealth.datax.processors;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.sinohealth.datax.common.CommonData;
import com.sinohealth.datax.common.Processor;
import com.sinohealth.datax.entity.common.BasInspectionKeyword;
import com.sinohealth.datax.entity.common.BasItemAlias;
import com.sinohealth.datax.entity.source.BasCheckItemTemp;
import com.sinohealth.datax.entity.source.StandardDiagnoseRecord;
import com.sinohealth.datax.entity.target.StandardDiagnoseRecordList;
import com.sinohealth.datax.utils.DiagnoseUtils;
import com.sinohealth.datax.utils.EtlConst;
import com.sinohealth.datax.utils.EtlStatus;
import com.sinohealth.datax.utils.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mingqiang
 * @date 20220905
 **/
public class DiagnoseResultProcessor implements Processor<BasCheckItemTemp, StandardDiagnoseRecordList> {

    public static final Logger logger = LoggerFactory.getLogger(DiagnoseResultProcessor.class);
    private static final List<String> negativeWordList = Lists.newArrayList("无", "没有", "未见", "未发现", "弃查", "弃检", "趋势");

    private static final List<String> specialProcess2List = Lists.newArrayList("未见明显异常", "未发现明显异常", "未见异常", "未发现异常", "未见", "无异常");

    public static volatile List<BasInspectionKeyword> inspectionKeywordList;

    @Override
    public StandardDiagnoseRecordList dataProcess(BasCheckItemTemp check, StandardDiagnoseRecordList list, CommonData commonData) {
        String imageDiagnose = check.getImageDiagnose();
        ArrayList<StandardDiagnoseRecord> records = new ArrayList<>();
        if(StrUtil.isBlank(imageDiagnose)){
            imageDiagnose = check.getResultValue();
        }
        String className = check.getClassName();
        StandardDiagnoseRecord record = new StandardDiagnoseRecord();
        record.setClassName(className);
        record.setVid(check.getVid());
        record.setImageDiagnose(check.getImageDiagnose());
        record.setCleanTime(new Date());
        record.setItemResults(check.getResultValue());
        if(StrUtil.isBlank(imageDiagnose) || StrUtil.isBlank(className)){
            record.setCleanStatus(EtlStatus.ETL_MISSING.getCode());
            record.setRemark(EtlStatus.ETL_MISSING.getMessage());
            records.add(record);
            list.setList(records);
            return list;
        }
        imageDiagnose = specialProcess4(imageDiagnose);
        imageDiagnose = imageDiagnose.replaceAll(" ", "");
        check.setImageDiagnose(imageDiagnose);
        inspectionKeywordList = commonData.getBasInspectionKeywordList();
        BasItemAlias basItemAlias = DiagnoseUtils.calcuAliasContain(className, commonData.getBasItemAliasList());

        if (Objects.isNull(basItemAlias)) {
            record.setCleanStatus(EtlStatus.ETL_ERROR_MATCH.getCode());
            record.setRemark(EtlStatus.ETL_ERROR_MATCH.getMessage());
            records.add(record);
            list.setList(records);
            return list;
        }
        StandardDiagnoseRecordList recordList = preProcessItemResult(check, basItemAlias.getName(), check.getImageDiagnose());
        if (Objects.nonNull(recordList) && Objects.nonNull(recordList.getList()) && recordList.getList().size() > 0) {
            return recordList;
        }
        //判断是否无异常
        String[] splitStrings = TextUtils.splitSignsToArrByParam(check.getImageDiagnose(), ";；。");
        int count = splitStrings.length;
        int i = 0;
        boolean flagI = false;
        int j = 0;
        boolean flagJ = false;
        int k = 0;
        boolean flagK = false;
        for (String s : splitStrings) {
            for (String str : EtlConst.NORMAL_LIST) {
                if (s.contains(str)) {
                    flagI = true;
                    break;
                }
            }
            if (flagI) {
                i++;
            }
            flagI = false;
        }

        for (String s : splitStrings) {
            for (String str : EtlConst.ETL_NOT_DOING) {
                if (s.contains(str)) {
                    flagJ = true;
                    break;
                }
            }
            if (flagJ) {
                j++;
            }
            flagJ = false;
        }
        for (String s : splitStrings) {
            for (String str : EtlConst.ETL_SUSPICIOUS) {
                if (s.contains(str)) {
                    flagK = true;
                    break;
                }
            }
            if (flagK) {
                k++;
            }
            flagK = false;
        }
        //特殊处理
        boolean hpvStatus = false;
        if("人乳头瘤病毒(HPV)核酸检测(HC2法)".equals(record.getClassName())){
            String[] stringHPV = record.getImageDiagnose().split("\n");
            int htvI = 0;
            for (String s : stringHPV) {
                if(s.contains("低危型：")){
                    String result1 = s.replace("低危型：", "");
                    if("阴性".equals(result1)){
                        htvI++;
                    }
                }else if(s.contains("高危型：")){
                    String result1 = s.replace("高危型：", "");
                    if("阴性".equals(result1)){
                        htvI++;
                    }
                }
            }
            if(stringHPV.length == htvI){
                hpvStatus = true;
            }
        }
        if (count == i || hpvStatus) {
            record.setCleanStatus(EtlStatus.ETL_SUCCESS_NORMAL.getCode());
            record.setRemark(EtlStatus.ETL_SUCCESS_NORMAL.getMessage());
        } else if (count == j) {
            record.setCleanStatus(EtlStatus.ETL_NOT_DOING.getCode());
            record.setRemark(EtlStatus.ETL_NOT_DOING.getMessage());
        } else if (count == k) {
            record.setCleanStatus(EtlStatus.ETL_SUSPICIOUS.getCode());
            record.setRemark(EtlStatus.ETL_SUSPICIOUS.getMessage());
        } else {
            record.setCleanStatus(EtlStatus.ETL_MISSING.getCode());
            record.setRemark(EtlStatus.ETL_MISSING.getMessage());
        }
        records.add(record);
        list.setList(records);
        return list;
    }

    private StandardDiagnoseRecordList preProcessItemResult(BasCheckItemTemp check, String itemName, String itemResults) {

        StandardDiagnoseRecordList recordList = new StandardDiagnoseRecordList();
        ArrayList<StandardDiagnoseRecord> list = new ArrayList<>();
        /*
         * start 判断总检结果是否正常 开始 请逐个分句匹配关键字，（没有逗号的按照句号匹配）
         * 若去掉建议后该分句后仍然大于40个字符，去掉40个字符以后的内容；
         */
        // 如果整个句子中出现了否定副词，则判断"肝、胆、脾、肾、膀胱、输尿管、附件、"是否存在在分句中
        itemResults = specialProcess(itemResults);

        // 当出现未见/未发现时，则在异常/明显异常后面加个逗号分割
        itemResults = specialProcess2(itemResults);

        // 当同时出现未见/未发现和病变时，则在病变后面加个逗号分割
        itemResults = specialProcess3(itemResults);

        // 预处理语句：特殊处理部位针对新一部部位的检测前未加标点的问题，如肝囊肿肾：肾结石
        itemResults = TextUtils.specialAddSigns(itemResults);

        // 根据特殊符号，将整句切分成String数组
        String[] temp = splitSignsToArr(itemResults);

        List<String> tempList1 = null;// 用来判断“肝胆脾肾输尿管膀胱附件”的特殊逻辑1
        for (int i = 0; i < temp.length; i++) {
            String clause = temp[i];
            if (":".equals(clause) || "：".equals(clause)) {
                continue;
            }
            if (StrUtil.isNotBlank(clause)) {
                int index = clause.indexOf("建议");
                if (index > -1) {
                    clause = clause.substring(0, index);
                }
                index = clause.indexOf("多见于");
                if (index > -1) {
                    clause = clause.substring(0, index);
                }
                if (clause.length() >= 40) {
                    clause = clause.substring(0, 40);
                }
                list.addAll(addRelateDisease(itemName, clause, check));
            }
        }
        recordList.setList(list);
        return recordList;
    }

    // 预处理语句:如果整句出现否定副词，就删掉"**、"
    private String specialProcess(String itemResults) {
        if (StrUtil.isNotBlank(itemResults)) {
            for (int j = 0; j < negativeWordList.size(); j++) {
                if (itemResults.contains(negativeWordList.get(j))) {
                    // 删掉"肝、胆、脾、肾、膀胱、输尿管、附件、前列腺、胰、"
                    itemResults = itemResults.replaceAll(":.+?趋势", "****").replaceAll("：.+?趋势", "****").replace("肝、", "").replace("胆、", "").replace("脾、", "").replace("肾、", "")
                            .replace("输尿管、", "").replace("膀胱、", "").replace("附件、", "").replace("前列腺、", "")
                            .replace("胰、", "");
                    break;
                }
            }
        }
        return itemResults;
    }

    private String specialProcess2(String itemResults) {
        if (StrUtil.isNotBlank(itemResults)) {
            for (int j = 0; j < specialProcess2List.size(); j++) {
                if (itemResults.contains(specialProcess2List.get(j))) {
                    int index = itemResults.indexOf(specialProcess2List.get(j)) + specialProcess2List.get(j).length();
                    itemResults = addSigns(itemResults, index);
                }
            }
        }
        return itemResults;
    }

    private String specialProcess3(String itemResults) {
        if (StrUtil.isNotBlank(itemResults)) {
            if (itemResults.contains("病变")) {
                if (itemResults.contains("未见") || itemResults.contains("未发现")) {
                    int index = itemResults.indexOf("病变") + 2;
                    itemResults = addSigns(itemResults, index);
                }
            }
        }
        return itemResults;
    }

    private String specialProcess4(String itemResults) {
        if (StrUtil.isNotBlank(itemResults)) {
            if (itemResults.endsWith("\r\r \n\r")) {
                itemResults = itemResults.replace("\r\r \n\r", "");
            }
            if (itemResults.contains("\r\n")) {
                String[] strings = itemResults.split("\r\n");
                itemResults = Arrays.asList(strings).stream().collect(Collectors.joining("。"));
            }
            if (itemResults.contains("。。")) {
                itemResults = itemResults.replace("。。", "。");
            }
        }
        return itemResults;
    }

    private String addSigns(String itemResults, int index) {
        StringBuffer stringBuffer = new StringBuffer(itemResults);
        return stringBuffer.insert(index, ",").toString();
    }

    private String[] splitSignsToArr(String s) {
        StringTokenizer str1 = new StringTokenizer(s, ",，.。 ；;");
        int count = 0;
        String[] temp = new String[str1.countTokens()];

        while (str1.hasMoreTokens()) {
            temp[count] = str1.nextToken();
            count++;
        }
        return temp;
    }



    private List<StandardDiagnoseRecord> addRelateDisease(String itemName, String clause, BasCheckItemTemp check) {
        //StandardDiagnoseRecordList list = new StandardDiagnoseRecordList();
        List<BasInspectionKeyword> mostSimuList = new ArrayList<>();
        List<StandardDiagnoseRecord> list = new ArrayList<>();
        if (StringUtils.isNotBlank(clause) && inspectionKeywordList != null) {
            List<BasInspectionKeyword> mostSimuList2 = new ArrayList<>();
            //检测方法匹配过滤
            List<BasInspectionKeyword> mostSimuList3 = inspectionKeywordList.stream().filter(x -> x.getMethod().equals(itemName)).collect(Collectors.toList());
            //关键字匹配过滤
            for (BasInspectionKeyword bsd : mostSimuList3) {
                if (StrUtil.isNotBlank(bsd.getKeyword())) {
                    if (bsd.getKeyword().trim().split("、").length == 1) {
                        if (clause.contains(bsd.getKeyword().trim())) {
                            mostSimuList2.add(bsd);
                        }
                    } else {
                        boolean flag = true;
                        for (String str : bsd.getKeyword().trim().split("、")) {
                            if (!clause.contains(str)) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag) {
                            mostSimuList2.add(bsd);
                        }
                    }
                } else {
                    mostSimuList2.add(bsd);
                }
            }
            //结果匹配过滤
            for (BasInspectionKeyword bsd : mostSimuList2) {
                if (StrUtil.isNotBlank(bsd.getResult())) {
                    if (clause.contains(bsd.getResult().trim())) {
                        mostSimuList.add(bsd);
                    }
                } else {
                    mostSimuList.add(bsd);
                }
            }
        }
        if (!mostSimuList.isEmpty()) {// 有可能不止一种疾病
            // 根据DiseaseName是否包含，唯一最大匹配，然后new对象
            List<BasInspectionKeyword> removeContainsList = removeContains(mostSimuList);
            //List<StandardDiagnoseRecord> recordList = new ArrayList<>();
            for (BasInspectionKeyword inspectionKeyword : removeContainsList) {
                // start 初始化风险疾病 开始
                StandardDiagnoseRecord record = new StandardDiagnoseRecord();
                record.setVid(check.getVid());
                record.setItemResults(inspectionKeyword.getDiseaseSigns());
                //record.setDiseaseName(inspectionKeyword.getDiseaseSigns());
                record.setItemName(inspectionKeyword.getShowName());
                record.setImageDiagnose(check.getImageDiagnose());
                record.setCleanStatus(1);
                record.setCleanTime(new Date());
                record.setItemId(inspectionKeyword.getId().toString());
                record.setClassName(check.getClassName());
                list.add(record);
            }
        }
        return list;
    }

    // 根据字段是否互相包含去重（冒泡排序）
    private List<BasInspectionKeyword> removeContains(List<BasInspectionKeyword> oriList) {
        Map<String, BasInspectionKeyword> newMap = new HashMap<>();
        for (int i = 0; i < oriList.size(); i++) {
            BasInspectionKeyword ori1 = oriList.get(i);
            for (int j = 0; j < oriList.size(); j++) {
                BasInspectionKeyword ori2 = oriList.get(j);
                if (ori2.getDiseaseSigns().contains(ori1.getDiseaseSigns())) {
                    ori1 = ori2;
                }
            }
            newMap.put(ori1.getDiseaseSigns(), ori1);
        }
        List<BasInspectionKeyword> newList = new ArrayList<>(newMap.values());
        return newList;
    }
}
