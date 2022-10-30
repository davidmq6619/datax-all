package com.sinohealth.datax.processors;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.sinohealth.datax.common.CommonData;
import com.sinohealth.datax.common.Processor;
import com.sinohealth.datax.entity.common.StandardBasTestItem;
import com.sinohealth.datax.entity.source.RegTest;
import com.sinohealth.datax.entity.source.StandardTestRecord;
import com.sinohealth.datax.entity.zktarget.StandardTestRecordList;
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
public class TestResultEtlProcessor implements Processor<RegTest, StandardTestRecordList> {
    public static final Logger LOG = LoggerFactory.getLogger(TestResultEtlProcessor.class);
    private static final HashMap<String, RegTest> hashMapData = new HashMap<>();
    //只需要结果，不需要离散型
    private static final List<String> TEST_RESULT_DATA = Arrays.asList("ABO血型");

    static {
        /*RegTest basTestItemTemp = new RegTest();
        basTestItemTemp.setItemName("白带清洁度");
        String str1 = "I°,II°,Ⅰ°,Ⅱ°,I,II,Ⅰ,Ⅱ";
        String str2 = "III,Ⅲ,IV,Ⅳ,III°,Ⅲ°,IV°,Ⅳ°,3,4";
        basTestItemTemp.setReference(str1);//阴性范围
        basTestItemTemp.setUnit(str2); //阳性范围
        hashMapData.put("白带清洁度", basTestItemTemp);*/
    }

    @Override
    public StandardTestRecordList dataProcess(RegTest itemTemp, StandardTestRecordList o, CommonData commonData) {
        StandardTestRecordList recordList = new StandardTestRecordList();
        List<StandardTestRecord> listRecord = new ArrayList<>();
        if (Objects.isNull(itemTemp)) {
            LOG.error("检验项结果不能为空");
            return recordList;
        }
        String itemName = itemTemp.getItemName();
        StandardTestRecord testRecord = new StandardTestRecord();
        String resultValue = TextUtils.textTrim(itemTemp.getResults());

        String name = StrUtil.isNotBlank(itemTemp.getItemName()) ? itemTemp.getItemName().trim() : "";
        itemTemp.setResults(resultValue);
        itemTemp.setItemName(name);

        testRecord.setItemName(itemTemp.getItemName());
        testRecord.setItemResults(itemTemp.getResults());
        testRecord.setNormalL(itemTemp.getNormalL());
        testRecord.setNormalH(itemTemp.getNormalH());
        testRecord.setItemUnit(itemTemp.getUnit());
        testRecord.setCleanTime(new Date());
        testRecord.setVid(itemTemp.getVid());
        testRecord.setClassName(itemTemp.getItemFt());
        testRecord.setCleanStatus(1);
        String normalL = itemTemp.getNormalL();
        String normalH = itemTemp.getNormalH();
        String reference = "";
        try {
            if (StringUtils.isBlank(itemName)) {
                //清洗失败 || StrUtil.isBlank(testRecord.getItemResults())
                testRecord.setRemark("清洗失败，itemName为空");
                testRecord.setCleanStatus(EtlStatus.ETL_ERROR.getCode());
                listRecord.add(testRecord);
                recordList.setList(listRecord);
                return recordList;
            }
            if(StringUtils.isNotBlank(resultValue) && resultValue.contains(EtlConst.BAO_GAO)){
                testRecord.setRemark(EtlStatus.ETL_DATA_ERROR.getMessage());
                testRecord.setCleanStatus(EtlStatus.ETL_DATA_ERROR.getCode());
                testRecord.setResultsDiscrete("0");
                listRecord.add(testRecord);
                recordList.setList(listRecord);
                return recordList;
            }
            //匹配规则统一小写+英文括号
            Map<String, StandardBasTestItem> basTestItemMap = commonData.getBasTestItemMap();
            Map<String, StandardBasTestItem> basTestMethodItemMap = commonData.getBasTestMethodItemMap();
            String itemFt = "";
            if(StrUtil.isNotBlank(itemTemp.getItemFt())){
                itemFt = itemTemp.getItemFt()
                            .replace("（", "(")
                            .replace("）",")").toLowerCase().trim();
            }
            if(StrUtil.isNotBlank(itemName)){
                itemName = itemName .replace("（", "(")
                                .replace("）",")")
                                .toLowerCase().trim();
            }
            String item = itemFt + ":" + itemName;
            StandardBasTestItem basTestItem = basTestMethodItemMap.get(item);
            if (Objects.isNull(basTestItem)) {
                basTestItem = basTestItemMap.get(itemName);
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
            String results = itemTemp.getResults();

            //去除括号
            results = results.replaceAll(EtlConst.KH_REGX, "");
            if (StrUtil.isNotBlank(results) && (results.contains("小于") || results.contains("<") || results.contains(">=") || results.contains("<=") ||
                    results.contains(">") || results.contains("大于") || results.contains("＜") || results.contains("《") ||
                    results.contains("〈") || results.contains("＞") || results.contains("》") || results.contains("〉") ||
                    results.contains("﹤") || results.contains("﹥"))) {
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
            } else if (StrUtil.isNotBlank(results) && ((results.contains("+") || results.contains("-")) && results.contains("."))) {
                results = results.replaceAll("\\.+\\d*", "");

            } else if (StrUtil.isNotBlank(results) && (results.contains("↑") || results.contains("↓"))) {
                results = results.replace("↑", "");
                results = results.replace("↓", "");
            } else if (StrUtil.isNotBlank(results) && (StrUtil.isNotBlank(itemTemp.getUnit()) && results.contains(itemTemp.getUnit()))) {
                results = results.replace(itemTemp.getUnit(), "");
            } else if (StrUtil.isNotBlank(results)) {
                results = results
                        .replace("(此结果已复核确认)", "")
                        .replace("（此结果已复核确认）", "")
                        .replace("此结果已复核确认", "")
                        .replace("(已复核)", "")
                        .replace("(已复查)", "")
                        .replace("（已复核）", "")
                        .replace("（已复查）", "")
                        .replace("已复核", "")
                        .replace("已复查", "")
                        .trim();

                //清理优健康数值结果中夹带的中文V1.4.6.1
                if(EtlConst.itemNames.contains(testRecord.getItemNameComn())&&!NumberUtil.isNumber(results)) {
                    for (String s : EtlConst.itemNames.split(EtlConst.SPLIT_DOUHAO)) {
                        if (s.equals(testRecord.getItemNameComn())) {
                            String numStr=	ReUtil.getGroup0(EtlConst.regx, results);
                            if(StrUtil.isNotBlank(numStr)){
                                results=numStr.trim();
                            }
                            break;
                        }
                    }
                }
            }
            itemTemp.setResults(results);
            //优化上下限也移除去掉特殊符号
            if (StringUtils.isNotBlank(normalH)) {
                normalH = normalH.replace("[", "").replace("]", "").trim();
                normalH = normalH.replace("～", "-").trim();
              //把上下限的包含单位过滤掉
                if (StringUtils.isNotBlank(itemTemp.getUnit())  && normalH.contains(itemTemp.getUnit())) {
                    normalH = normalH.replace(itemTemp.getUnit(), "");
                }
                normalH = normalH.replace(">", "")
                        .replace("》", "")
                        .replace("≥", "")
                        .replace(">=", "")
                        .replace("》=", "")
                        .replace("<", "")
                        .replace("＜", "")
                        .replace("《", "")
                        .replace("≤", "")
                        .replace("<=", "")
                        .replace("《=", "")
                        .replace("=", "")
                        .trim();
            }

            if (StringUtils.isNotBlank(normalL)) {
                normalL=normalL.replace("[", "").replace("]", "").trim();
                normalL=normalL.replace("～", "-").trim();
                //把上下限的包含单位过滤掉
                if (StringUtils.isNotBlank(itemTemp.getUnit())  && normalL.contains(itemTemp.getUnit())) {
                    normalL = normalL.replace(itemTemp.getUnit(), "");
                }
                normalL = normalL.replace(">", "")
                        .replace("》", "")
                        .replace("≥", "")
                        .replace(">=", "")
                        .replace("》=", "")
                        .replace("<", "")
                        .replace("＜", "")
                        .replace("《", "")
                        .replace("≤", "")
                        .replace("<=", "")
                        .replace("《=", "")
                        .replace("=", "")
                        .trim();
            }

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
        } catch (Exception e) {
            LOG.error("清洗检验异常，源数据入参[{}],清洗异常[{}]", JSON.toJSONString(itemTemp), e.getMessage(), e);
        }
        return recordList;
    }

    public void resultsDiscreteProcess(StandardTestRecord str, RegTest lisTestResult, CommonData commonData) {

        if (NumberUtil.isNumber(str.getItemResults()) && NumberUtil.isNumber(str.getNormalL())
                && NumberUtil.isNumber(str.getNormalH())) {
            double value = Double.parseDouble(str.getItemResults());
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

        } else if (NumberUtil.isNumber(str.getItemResults()) && (NumberUtil.isNumber(str.getNormalL())
                || NumberUtil.isNumber(str.getNormalH()))) {
            double value = Double.valueOf(str.getItemResults()).doubleValue();
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
        } else if (!NumberUtil.isNumber(str.getItemResults())) {
            boolean flag = false;
            boolean flagNot = false;
            boolean flagSuspicious = false;
            Map<String, String> discreteMap = commonData.getResultDiscreteMap();
            String results = str.getItemResults();
            if (results == null && (str.getNormalL() == null || str.getNormalH() == null)) {
                str.setResultsDiscrete("0");
                str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                str.setRemark(EtlStatus.ETL_SUCCESS.getMessage());
            } else {
                if (str.getNormalL() != null && str.getNormalH() != null && results != null &&
                        NumberUtil.isNumber(results.replace("<", "").replace("＜", "")
                                .replace("《", "").trim()) && NumberUtil.isNumber(str.getNormalL()) &&
                        Double.doubleToLongBits(Double.valueOf(str.getNormalL()).doubleValue()) == Double.doubleToLongBits(0d) &&
                        NumberUtil.isNumber(str.getNormalH())) {
                    //保存清洗后值
                    str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                    str.setRemark(EtlStatus.ETL_SUCCESS.getMessage());
                    str.setResultsDiscrete("0");
                } else if (str.getNormalL() != null && str.getNormalH() != null && results != null
                        && results.split("-").length > 1 && NumberUtil.isNumber(str.getNormalL())
                        && NumberUtil.isNumber(str.getNormalH())) {
                    str.setResultsDiscrete("0");
                    if (NumberUtil.isNumber(results.split("-")[0].trim()) &&
                            NumberUtil.isNumber(results.split("-")[1].trim())) {

                        int l = Integer.valueOf(results.split("-")[0].trim());
                        int h = Integer.valueOf(results.split("-")[1].trim());
                        if (((h - l) / 2 + l) > Double.valueOf(str.getNormalH()).doubleValue()) {
                            //保存清洗后值
                            str.setResultsDiscrete("2");
                            str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                            str.setRemark(EtlStatus.ETL_SUCCESS.getMessage());
                        }
                        if (h < Double.valueOf(str.getNormalL()).doubleValue()) {
                            //保存清洗后值
                            str.setResultsDiscrete("-2");
                            str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                            str.setRemark(EtlStatus.ETL_SUCCESS.getMessage());
                        }
                    }
                } else if (NumberUtil.isNumber(replaceValue(results)) &&
                        NumberUtil.isNumber(replaceValue(str.getNormalL())) && NumberUtil.isNumber(replaceValue(str.getNormalH()))) {
                    double value = Double.valueOf(replaceValue(results)).doubleValue();
                    double normalL = Double.valueOf(replaceValue(str.getNormalL())).doubleValue();
                    double normalH = Double.valueOf(replaceValue(str.getNormalH())).doubleValue();
                    if (value < normalL) {
                        str.setResultsDiscrete("-2"); //低
                    } else if (value > normalH) {
                        str.setResultsDiscrete("2"); // 高
                    } else {
                        str.setResultsDiscrete("0"); // 正常
                    }
                    //保存清洗后值
                    str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                    str.setRemark(EtlStatus.ETL_SUCCESS.getMessage());
                } else {
                    if (discreteMap.get(results.toLowerCase()) != null) {
                        str.setResultsDiscrete(discreteMap.get(results.toLowerCase()));
                        str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                        str.setRemark(EtlStatus.ETL_SUCCESS.getMessage());
                    } else {
                        if (results != null && (results.equals(str.getNormalL()) || results.equals(str.getNormalH()))) {
                            str.setResultsDiscrete("0");
                            str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                            str.setRemark(EtlStatus.ETL_SUCCESS.getMessage());
                        } else if (str.getNormalL() != null && str.getNormalH() != null && results != null &&
                                NumberUtil.isNumber(results.replace(">", "").
                                        replace("》", "").trim()) && NumberUtil.isNumber(str.getNormalL()) &&
                                NumberUtil.isNumber(str.getNormalH()) && Double.valueOf(str.getNormalH()).doubleValue() < Double.valueOf
                                (results.replace(">", "").replace("》", "").trim())
                                .doubleValue()) {
                            //保存清洗后值
                            str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                            str.setRemark(EtlStatus.ETL_SUCCESS.getMessage());
                            str.setResultsDiscrete("2");
                        } else {
                            //保存清洗后值
                            String resultOk = results.replace(">", "")
                                    .replace("》", "")
                                    .replace("≥", "")
                                    .replace(">=", "")
                                    .replace("》=", "")
                                    .replace("<", "")
                                    .replace("＜", "")
                                    .replace("《", "")
                                    .replace("≤", "")
                                    .replace("<=", "")
                                    .replace("《=", "")
                                    .replace("=", "")
                                    .trim();
                            //最后判断替换后的值是否数字
                            if (NumberUtil.isNumber(resultOk) && NumberUtil.isNumber(str.getNormalL()) && NumberUtil.isNumber(str.getNormalH())) {
                                double value = Double.valueOf(resultOk).doubleValue();
                                double normalL = Double.valueOf(str.getNormalL()).doubleValue();
                                double normalH = Double.valueOf(str.getNormalH()).doubleValue();
                                str.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
                                str.setRemark(EtlStatus.ETL_SUCCESS.getMessage());
                                //计算结果
                                if (value >= normalH) {
                                    str.setResultsDiscrete("2"); // 高
                                } else if (value <= normalL) {
                                    str.setResultsDiscrete("-2"); // 低
                                } else {
                                    str.setResultsDiscrete("0"); // 正常
                                }
                            } else {
                                for (String s : EtlConst.NORMAL_LIST) {
                                    if (str.getItemResults().contains(s)) {
                                        flag = true;
                                        break;
                                    }
                                }
                                for (String s : EtlConst.ETL_NOT_DOING) {
                                    if (str.getItemResults().contains(s)) {
                                        flagNot = true;
                                        break;
                                    }
                                }
                                for (String s : EtlConst.ETL_SUSPICIOUS) {
                                    if (str.getItemResults().contains(s)) {
                                        flagSuspicious = true;
                                        break;
                                    }
                                }
                                if (flag) {
                                    str.setResultsDiscrete("0");
                                    str.setCleanStatus(EtlStatus.ETL_SUCCESS_NORMAL.getCode());
                                } else if (flagNot) {
                                    str.setResultsDiscrete("0");
                                    str.setCleanStatus(EtlStatus.ETL_NOT_DOING.getCode());
                                    str.setRemark(EtlStatus.ETL_NOT_DOING.getMessage());
                                } else if (flagSuspicious) {
                                    str.setResultsDiscrete("0");
                                    str.setCleanStatus(EtlStatus.ETL_SUSPICIOUS.getCode());
                                    str.setRemark(EtlStatus.ETL_SUSPICIOUS.getMessage());
                                } else {
                                    str.setResultsDiscrete("0");
                                    str.setCleanStatus(EtlStatus.ETL_DATA_ERROR.getCode());
                                    str.setRemark(EtlStatus.ETL_DATA_ERROR.getMessage());
                                }
                            }
                        }

                    }
                }
            }
        } else {
            str.setCleanStatus(EtlStatus.ETL_DATA_ERROR.getCode());
            str.setRemark(EtlStatus.ETL_DATA_ERROR.getMessage());
        }
    }

    //罗马数字转换
    public String replaceValue(String result) {
        if (result == null) {
            return "";
        } else if (NumberUtil.isNumber(result)) {
            return result;
        } else if (result.equals("Ⅰ")) {
            return "1";
        } else if (result.equals("Ⅱ")) {
            return "2";
        } else if (result.equals("Ⅲ")) {
            return "3";
        } else if (result.equals("Ⅳ")) {
            return "4";
        } else if (result.equals("Ⅴ")) {
            return "5";
        } else if (result.equals("Ⅵ")) {
            return "6";
        } else if (result.equals("Ⅶ")) {
            return "7";
        } else if (result.equals("Ⅷ")) {
            return "8";
        } else if (result.equals("Ⅸ")) {
            return "9";
        } else {
            return "";
        }
    }

    public static void main(String[] args) {
        String str = "+1.00";
        String s = str.replaceAll("\\.+\\d*", "");
        System.out.println(s);
    }

}


