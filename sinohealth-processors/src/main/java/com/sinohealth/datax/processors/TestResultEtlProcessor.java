package com.sinohealth.datax.processors;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.sinohealth.datax.common.CommonData;
import com.sinohealth.datax.common.Processor;
import com.sinohealth.datax.entity.source.BasTestItemTemp;
import com.sinohealth.datax.entity.common.StandardBasTestItem;
import com.sinohealth.datax.entity.source.StandardTestRecord;
import com.sinohealth.datax.entity.target.StandardTestRecordList;
import com.sinohealth.datax.utils.EtlConst;
import com.sinohealth.datax.utils.EtlStatus;
import com.sinohealth.datax.utils.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author mingqiang
 * @date 2022/08/29
 **/
public class TestResultEtlProcessor implements Processor<BasTestItemTemp, StandardTestRecordList> {
    public static final Logger LOG = LoggerFactory.getLogger(TestResultEtlProcessor.class);
    private static final HashMap<String, BasTestItemTemp> hashMapData = new HashMap<>();
    //只需要结果，不需要离散型
    private static final List<String> TEST_RESULT_DATA = Arrays.asList("ABO血型");

    static {
        BasTestItemTemp basTestItemTemp = new BasTestItemTemp();
        basTestItemTemp.setItemName("白带清洁度");
        String str1 = "I°,II°,Ⅰ°,Ⅱ°,I,II,Ⅰ,Ⅱ";
        String str2 = "III,Ⅲ,IV,Ⅳ,III°,Ⅲ°,IV°,Ⅳ°,3,4";
        basTestItemTemp.setReference(str1);//阴性范围
        basTestItemTemp.setUnit(str2);//阳性范围
        hashMapData.put("白带清洁度", basTestItemTemp);
    }

    @Override
    public StandardTestRecordList dataProcess(BasTestItemTemp itemTemp, StandardTestRecordList o, CommonData commonData) {
        StandardTestRecordList recordList = new StandardTestRecordList();
        List<StandardTestRecord> listRecord = new ArrayList<>();
        if (Objects.isNull(itemTemp)) {
            LOG.error("检验项结果不能为空");
            return recordList;
        }
        String itemName = itemTemp.getItemName();
        StandardTestRecord testRecord = new StandardTestRecord();
        String resultValue = TextUtils.textTrim(itemTemp.getResultValue());
        String references = TextUtils.textTrim(itemTemp.getReference());
        String name = itemTemp.getItemName();
        itemTemp.setResultValue(resultValue);
        itemTemp.setReference(references);
        itemTemp.setItemName(name);

        testRecord.setItemName(itemTemp.getItemName());
        testRecord.setItemId(itemTemp.getItemCode());
        testRecord.setItemResults(itemTemp.getResultValue());
        testRecord.setNormalL(itemTemp.getReference());
        testRecord.setItemUnit(itemTemp.getUnit());
        testRecord.setCleanTime(new Date());
        testRecord.setVid(itemTemp.getVid());
        testRecord.setClassName(itemTemp.getClassName());
        testRecord.setCleanStatus(1);
        String normalL = "";
        String normalH = "";
        if (StringUtils.isBlank(itemName) || StrUtil.isBlank(testRecord.getItemResults())) {
            //清洗失败
            testRecord.setRemark("清洗失败，itemName为空或检验结果为空");
            testRecord.setCleanStatus(EtlStatus.ETL_ERROR.getCode());
            listRecord.add(testRecord);
            recordList.setList(listRecord);
            return recordList;
        }
        Map<String, StandardBasTestItem> basTestItemMap = commonData.getBasTestItemMap();
        Map<String, StandardBasTestItem> basTestMethodItemMap = commonData.getBasTestMethodItemMap();
        String item = itemTemp.getClassName()+":"+itemName;
        StandardBasTestItem basTestItem = basTestMethodItemMap.get(item);
        if(Objects.isNull(basTestItem)){
             basTestItem = basTestItemMap.get(itemName.trim());
        }
        if (Objects.isNull(basTestItem)) {
            //字典匹配失败
            testRecord.setRemark(EtlStatus.ETL_ERROR_MATCH.getMessage());
            testRecord.setCleanStatus(EtlStatus.ETL_ERROR_MATCH.getCode());
            listRecord.add(testRecord);
            recordList.setList(listRecord);
            return recordList;
        }
        //进行标准化清洗
        testRecord.setItemNameComn(basTestItem.getItemNameCStandard());
        String unitComm = commonData.getBasTestUnitMap().get(itemTemp.getUnit());
        testRecord.setUnitComm(unitComm);
        String results = itemTemp.getResultValue();
        String reference = itemTemp.getReference();
        //去除括号
        results = results.replaceAll(EtlConst.KH_REGX,"");
        if (results.contains("小于") || results.contains("<") || results.contains(">=") || results.contains("<=") ||
                results.contains(">") || results.contains("大于") || results.contains("＜")|| results.contains("《") ||
                results.contains("〈") || results.contains("＞") || results.contains("》") || results.contains("〉") ||
                results.contains("﹤") || results.contains("﹥")) {
            results = results.replaceAll("小于", "")
                    .replaceAll("=", "")
                    .replaceAll("＝", "")
                    .replaceAll("<", "")
                    .replaceAll(">", "")
                    .replaceAll("＜", "")
                    .replaceAll("《", "")
                    .replaceAll("〈", "")
                    .replaceAll("＞", "")
                    .replaceAll("》", "")
                    .replaceAll("〉", "")
                    .replaceAll("﹤", "")
                    .replaceAll("﹥", "")
                    .replaceAll("大于", "");
        } else if ((results.contains("+") || results.contains("-")) && results.contains(".")) {
            results = results.replaceAll("\\.+\\d*", "");

        }else if (results.contains("↑") || results.contains("↓")){
            results = results.replace("↑","");
            results = results.replace("↓","");
        }else if(StrUtil.isNotBlank(itemTemp.getUnit()) && results.contains(itemTemp.getUnit())){
            results = results.replace(itemTemp.getUnit(),"");
        }
        itemTemp.setResultValue(results);
        if (StrUtil.isNotBlank(reference) && !reference.contains("阴") && !reference.contains("阳")) {
            reference = reference.replace("--", "-");
            reference = reference.replace("～", "-");
            if (reference.contains("-")) {
                String[] arrays = reference.split("-");
                if (arrays.length != 0) {
                    List<String> referenceList = Arrays.asList(arrays);
                    if (referenceList.size() == 2) {
                        normalL = referenceList.get(0).trim();
                        normalH = referenceList.get(1).trim();
                    } else {
                        normalL = referenceList.get(0).trim();
                    }
                }
            } else if (reference.contains("<") || reference.contains("≤") || reference.contains("小于")) {
                reference = reference.replace("<", "").replace("≤", "").replace("小于", "");
                normalH = reference;
            } else if (reference.contains(">") || reference.contains("≥") || reference.contains("大于")) {
                reference = reference.replace(">", "").replace("≥", "").replace("大于", "");
                normalL = reference;
            }
        }
        testRecord.setItemResults(results);
        testRecord.setNormalL(normalL);
        testRecord.setNormalH(normalH);
        resultsDiscreteProcess(testRecord, itemTemp, commonData);
        listRecord.add(testRecord);
        recordList.setList(listRecord);
        return recordList;
    }

    public void resultsDiscreteProcess(StandardTestRecord str, BasTestItemTemp lisTestResult, CommonData commonData) {

        if (NumberUtil.isNumber(lisTestResult.getResultValue()) && NumberUtil.isNumber(str.getNormalL())
                && NumberUtil.isNumber(str.getNormalH())) {
            double value = Double.parseDouble(lisTestResult.getResultValue());
            double normalL = Double.parseDouble(str.getNormalL());
            double normalH = Double.parseDouble(str.getNormalH());

            if (value <= normalH && value >= normalH * 0.9) {
                str.setResultsDiscrete(String.valueOf(1)); // 临界高
            } else if (value >= normalL && value <= normalL * 1.1) {
                str.setResultsDiscrete(String.valueOf(-1)); // 临界低
            } else if (value > normalH) {
                str.setResultsDiscrete(String.valueOf(2)); // 高
            } else if (value < normalL) {
                str.setResultsDiscrete(String.valueOf(-2)); // 低
            } else {
                str.setResultsDiscrete(String.valueOf(0)); // 正常
            }

        } else if (NumberUtil.isNumber(lisTestResult.getResultValue()) && (NumberUtil.isNumber(str.getNormalL())
                || NumberUtil.isNumber(str.getNormalH()))) {
            double value = Double.valueOf(lisTestResult.getResultValue()).doubleValue();
            if (NumberUtil.isNumber(str.getNormalL())) {
                double normalL = Double.valueOf(str.getNormalL()).doubleValue();
                if (value > normalL) {
                    str.setResultsDiscrete(String.valueOf(0)); //正常
                } else if (value < normalL) {
                    str.setResultsDiscrete(String.valueOf(-2)); // 低
                } else {
                    str.setResultsDiscrete(String.valueOf(0)); // 正常
                }
            } else if (NumberUtil.isNumber(str.getNormalH())) {
                double normalH = Double.valueOf(str.getNormalH()).doubleValue();
                if (value < normalH) {
                    str.setResultsDiscrete(String.valueOf(0)); //正常
                } else if (value > normalH) {
                    str.setResultsDiscrete(String.valueOf(2)); // 高
                } else {
                    str.setResultsDiscrete(String.valueOf(0)); // 正常
                }
            }
        } else if (!NumberUtil.isNumber(lisTestResult.getResultValue())) {
            boolean flag = false;
            boolean flagNot = false;
            boolean flagSuspicious = false;
            Map<String, String> discreteMap = commonData.getResultDiscreteMap();
            String resultvalue = lisTestResult.getResultValue();
            String resultValue = discreteMap.get(resultvalue.toLowerCase());
            for (String s : EtlConst.NORMAL_LIST) {
                if (str.getItemResults().contains(s)) {
                    flag = true;
                    break;
                }
            }
            for(String s : EtlConst.ETL_NOT_DOING){
                if(str.getItemResults().contains(s)){
                    flagNot = true;
                    break;
                }
            }
            for(String s : EtlConst.ETL_SUSPICIOUS){
                if(str.getItemResults().contains(s)){
                    flagSuspicious = true;
                    break;
                }
            }
            if (flag) {
                str.setResultsDiscrete("0");
                str.setCleanStatus(EtlStatus.ETL_SUCCESS_NORMAL.getCode());
            }else if(flagNot){
                str.setResultsDiscrete("0");
                str.setCleanStatus(EtlStatus.ETL_NOT_DOING.getCode());
                str.setRemark(EtlStatus.ETL_NOT_DOING.getMessage());
            }else if(flagSuspicious){
                str.setResultsDiscrete("0");
                str.setCleanStatus(EtlStatus.ETL_SUSPICIOUS.getCode());
                str.setRemark(EtlStatus.ETL_SUSPICIOUS.getMessage());
            } else if (StrUtil.isNotBlank(resultValue)) {
                str.setResultsDiscrete(resultValue);
                str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                str.setRemark(EtlStatus.ETL_SUCCESS.getMessage());
            } else if (hashMapData.get(str.getItemNameComn()) != null) {
                BasTestItemTemp temp = hashMapData.get(str.getItemNameComn());
                List<String> list1 = Arrays.asList(temp.getReference().split(","));
                List<String> list2 = Arrays.asList(temp.getUnit().split(","));
                if (list1.contains(lisTestResult.getResultValue())) {
                    str.setResultsDiscrete("0");
                    str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                }else if(list2.contains(lisTestResult.getResultValue())){
                    str.setResultsDiscrete("2");
                    str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                }else{
                    str.setCleanStatus(EtlStatus.ETL_ERROR_DISPERSE.getCode());
                    str.setRemark(EtlStatus.ETL_ERROR_DISPERSE.getMessage());
                }
            } else {
                //离散型
                String result = "0";
                boolean flagNegative = false;
                for (String s : EtlConst.NEGATIVE_DATA) {
                    if (str.getItemResults().contains(s)) {
                        result = "-1";
                        flagNegative = true;
                        break;
                    }
                }
                if (!flagNegative) {
                    for (String s : EtlConst.POSITIVE_DATA) {
                        if (str.getItemResults().contains(s)) {
                            result = "2";
                            flagNegative = true;
                            break;
                        }
                    }
                }
                 if (StrUtil.isBlank(str.getNormalH()) && StrUtil.isBlank(str.getNormalL())){
                    str.setCleanStatus(EtlStatus.ETL_MISSING.getCode());
                }else if (flagNegative) {
                    str.setResultsDiscrete(result);
                    str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                } else if(TEST_RESULT_DATA.contains(str.getItemNameComn())){
                    str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                }else {
                    str.setCleanStatus(EtlStatus.ETL_ERROR_DISPERSE.getCode());
                    str.setRemark(EtlStatus.ETL_ERROR_DISPERSE.getMessage());
                }
            }
        } else {
            if(TEST_RESULT_DATA.contains(str.getItemNameComn())){
                str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
            }else{
                str.setCleanStatus(EtlStatus.ETL_MISSING.getCode());
                str.setRemark(EtlStatus.ETL_MISSING.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        String str = "+1.00";
        String s = str.replaceAll("\\.+\\d*", "");
        System.out.println(s);
    }
}


