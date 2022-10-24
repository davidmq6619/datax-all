package com.sinohealth.datax.utils;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import com.sinohealth.datax.entity.source.CheckResultMsS;
import com.sinohealth.datax.entity.source.StandardCheckRecord;
import com.sinohealth.datax.entity.target.CheckResultMsEtl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mingqiang
 * @date 2022/8/30 - 15:14
 * @desc
 */
public class EtlFeiUtils {
    /**
     * 肺CT的检测方式别名
     */
    public static final List<String> feictMethod = Arrays.asList("CT胸部检查",
            "CT胸部",
            "CT检查(胸部)-不出片",
            "低剂量胸部螺旋CT扫描",
            "C（胸部）",
            "低剂量胸部螺旋CT扫描(哺乳,备孕,孕者禁)",
            "低剂量胸部CT扫描",
            "CT检查(胸部)",
            "胸片升级肺部CT（不出片）",
            "胸部CT（送（不含片））",
            "胸部CT(不含片)",
            "低剂量螺旋胸部CT",
            "CT-胸部",
            "胸部CT（64排）",
            "CT-胸部（不出片）",
            "低剂量胸部CT扫描(不含胶片）",
            "胸部CT(体验）",
            "低剂量胸部CT扫描（不出片）",
            "低剂量胸部CT扫描（团）",
            "胸部DR免费升级CT",
            "胸部CT+三维重建",
            "胸部CT（64排）【不含胶片】",
            "胸部CT平扫",
            "螺旋CT胸部平扫",
            "低剂量胸部CT扫描（Z）",
            "CT检查(升级胸部)",
            "低剂量螺旋CT（胸部）",
            "CT（胸部）",
            "胸部CT低剂量筛查+三维重建",
            "胸部螺旋CT平扫(不出片)",
            "胸部螺旋CT断层扫描",
            "胸部CT（低剂量）",
            "胸部CT（不出片）",
            "胸部（肺部）CT",
            "胸部CT（送（不含片）",
            "优惠胸部CT80(不出片)",
            "优惠胸部CT100(出片)",
            "C(胸部)",
            "胸部螺旋CT平扫",
            "胸部CT（16排）",
            "胸部CT",
            "低剂量螺旋CT胸部平扫",
            "胸部CT(团检单价)",
            "胸部CT（团单）",
            "CT肺部扫描（不含片）",
            "CT肺部平扫",
            "CT-肺部（含片）",
            "肺部CT(不含出片)",
            "肺CT",
            "低剂量肺部CT扫描(哺乳,备孕,孕者禁)",
            "CT-肺部(不含片)",
            "CT肺部",
            "CT检查（肺部）不含片",
            "CT检查（肺部）",
            "CT肺部(ZS)",
            "CT检查（肺）",
            "CT检查（肺部）【不含片】",
            "双肺CT平扫",
            "CT肺部（团单）",
            "肺部CT",
            "肺部CT平扫+增强+三维成像",
            "胸部（肺部）CT",
            "肺CT(不出片)",
            "肺部CT平扫",
            "CT(肺部)",
            "胸部低剂量CT",
            "胸部低剂量CT检查");

    public static final List<String> feiMethod = Arrays.asList(
            "结节", "磨玻璃", "毛玻璃", "团块", "高密度影", "肿块", "占位", "肿瘤", "癌", "Ca", "软组织", "灶", "囊肿", "瘤", "肿物", "包块"
    );

    /**
     * 是否做肺CT 0未做 1已做
     */
    public static final String itemNameCommA = "肺CT";

    /**
     * 是否有肺结节 0否 1是
     */
    public static final String itemNameCommB = "肺结节";

    /**
     * 最大结节直径 统一单位cm
     */
    public static final String itemNameCommC = "最大肺结节直径";
    /**
     * 结节直径提取正则
     */
    public static final String regx = "([1-9]\\d*\\.?\\d*)|(0\\.\\d*[1-9])";

    public static void etl(StandardCheckRecord checkResultMsS, List<StandardCheckRecord> list) {

        //取出描述，抛弃附件后的字
        String result = checkResultMsS.getItemResults();
        if (result.contains("附件")) {
            result = result.substring(0, result.indexOf("附件"));
        }
        //命中的语句
        List<Map<String, String>> hits2 = new ArrayList<>();
        boolean flagFei = false;
        int feiI = 0;
        String[] splitStrings = TextUtils.splitSignsToArrByParam(result, ";；。");
        for (String str1 : splitStrings) {
            for (String s : EtlConst.NORMAL_LIST) {
                if (str1.contains(s)) {
                    feiI++;
                    break;
                }
            }
            if (str1.contains("肺") || str1.contains("胸膜") || str1.contains("气管")) {
                if (str1.contains("毛玻璃") || str1.contains("磨玻璃") || str1.contains("团块") || str1.contains("结节") || str1.contains("占位") || str1.contains("肿块") || str1.contains("高密度影")) {
                    boolean flag7 = ReUtil.contains(regx, str1) && (str1.contains("cm") || str1.contains("CM") || str1.contains("Cm") || str1.contains("cM") || str1.contains("mM") || str1.contains("MM") || str1.contains("mm") || str1.contains("Mm"));
                    boolean flagNoraml = false;
                    for (String s : EtlConst.NORMAL_LIST) {
                        if (str1.contains(s)) {
                            flagNoraml = true;
                            flagFei = true;
                            break;
                        }
                    }
                    if (flagNoraml) {
                        break;
                    }
                    if (str1.contains("结节状钙化") || str1.contains("钙化结节影")) {
                        if (flag7) {
                            Map<String, String> map = new HashMap<>();
                            map.put("str", str1);
                            map.put("zhijing", "true");
                            hits2.add(map);
                        }
                    } else if ((str1.contains("高密度影") || str1.contains("密度增高影")) && flag7) {
                        Map<String, String> map = new HashMap<>();
                        map.put("str", str1);
                        map.put("zhijing", "true");
                        hits2.add(map);
                    } else if (str1.contains("毛玻璃") || str1.contains("磨玻璃") || str1.contains("团块")
                            || str1.contains("结节") || str1.contains("占位") || str1.contains("肿块")) {
                        Map<String, String> map = new HashMap<>();
                        map.put("str", str1);
                        if (flag7) {
                            map.put("zhijing", "true");
                        } else {
                            map.put("zhijing", "false");
                        }
                        hits2.add(map);
                    }
                }
            }
        }
        int count = splitStrings.length;
        if(!hits2.isEmpty()){
            list.add(buildResultByItemNameCommA(checkResultMsS, itemNameCommB, "1"));
        }else if (flagFei || count == feiI) {
            list.add(buildResultByItemNameCommA(checkResultMsS, itemNameCommB, "2"));
            return;
        } else {
            list.add(buildResultByItemNameCommA(checkResultMsS, itemNameCommB, "0"));
            return;
        }

        //小结中有结节，再跑出最大直径
        //命中的语句
        hits2 = new ArrayList<>();
        splitStrings = TextUtils.splitSignsToArrByParam(result, ";；。");
        for (String str1 : splitStrings) {
            if (str1.contains("肺") || str1.contains("胸膜") || str1.contains("气管")) {
                if (str1.contains("毛玻璃") || str1.contains("磨玻璃") || str1.contains("团块") || str1.contains("结节") || str1.contains("占位") || str1.contains("肿块") || str1.contains("高密度影")) {
                    boolean flag7 = ReUtil.contains(regx, str1) && (str1.contains("cm") || str1.contains("CM") || str1.contains("Cm") || str1.contains("cM") || str1.contains("mM") || str1.contains("MM") || str1.contains("mm") || str1.contains("Mm"));
                    if (str1.contains("结节状钙化") || str1.contains("钙化结节影")) {
                        if (flag7) {
                            Map<String, String> map = new HashMap<>();
                            map.put("str", str1);
                            map.put("zhijing", "true");
                            hits2.add(map);
                        }
                    } else if ((str1.contains("高密度影") || str1.contains("密度增高影")) && flag7) {
                        Map<String, String> map = new HashMap<>();
                        map.put("str", str1);
                        map.put("zhijing", "true");
                        hits2.add(map);
                    } else if (str1.contains("毛玻璃") || str1.contains("磨玻璃") || str1.contains("团块")
                            || str1.contains("结节") || str1.contains("占位") || str1.contains("肿块")) {
                        Map<String, String> map = new HashMap<>();
                        map.put("str", str1);
                        if (flag7) {
                            map.put("zhijing", "true");
                        } else {
                            map.put("zhijing", "false");
                        }
                        hits2.add(map);
                    }
                }
            }
        }

        //按优先级取出最高的那句话：（结节 = 磨玻璃 = 毛玻璃 = 团块) > 高密度影&&目标语句有结节直径 > 直径大小
        Map<String, List<Map<String, String>>> zhijingMap = hits2.stream().collect(Collectors.groupingBy(x -> x.get("zhijing")));
        String finalHit = "";
        List<Map<String, String>> zhijingtrue = zhijingMap.get("true");
        if (zhijingtrue != null && !zhijingtrue.isEmpty()) {
            //目标语句有结节直径的语句
            List<String> hits5 = zhijingtrue.stream().map(y -> y.get("str")).collect(Collectors.toList());
            if (hits5.size() == 1) {
                finalHit = hits5.get(0);
            } else {
                finalHit = zhijingOrder(hits5);
            }
            //添加最大结节直径
            list.add(buildResultByItemNameCommA(checkResultMsS, itemNameCommC, String.valueOf(zhijingOrderCM(finalHit))));
        }
        //定位上述条件在哪一句中 end
    }

    //构建etl结果
    public static StandardCheckRecord buildResultByItemNameCommA(StandardCheckRecord checkResultMsS, String itemnameComm, String result) {
        StandardCheckRecord etl = new StandardCheckRecord();
        etl.setCleanTime(new Date());
        etl.setImageDiagnose(checkResultMsS.getImageDiagnose());
        etl.setVid(checkResultMsS.getVid());
        etl.setInitResult(itemnameComm);
        etl.setClassName(checkResultMsS.getClassName());
        etl.setItemResults(result);
        etl.setItemName(checkResultMsS.getItemName());
        etl.setItemNameComn(itemnameComm);
        etl.setCleanStatus(EtlStatus.ETL_SUCCESS.getCode());
        if ("0".equals(result) || "2".equals(result)) {
            etl.setRemark(EtlStatus.ETL_SUCCESS_NORMAL.getMessage());
            etl.setCleanStatus(EtlStatus.ETL_SUCCESS_NORMAL.getCode());
        }
        if (itemnameComm.equals(itemNameCommC)) {
            if (checkResultMsS.getItemResults().toLowerCase().contains("cm") || checkResultMsS.getItemResults().toLowerCase().contains("mm")) {
                etl.setUnitComm("cm");
                etl.setItemUnit("cm");
            }
        }
        return etl;
    }

    public static String zhijingOrder(List<String> hits) {
        //毫米
        double cur_zhijing = 0.00;
        String cur_hit = "";
        for (String hit : hits) {
            List<String> listS = new ArrayList<>();
            listS.addAll(ReUtil.findAll(regx, hit, 0));
            if (!listS.isEmpty()) {
                listS.sort((x1, x2) -> Double.valueOf(x1).compareTo(Double.valueOf(x2)));
                String s = listS.get(listS.size() - 1);

                if (NumberUtil.isNumber(s)) {
                    double tempValue = Double.valueOf(s);
                    if (hit.contains("cm") || hit.contains("CM") || hit.contains("Cm") || hit.contains("cM")) {
                        tempValue = tempValue * 10;
                    } else if (hit.contains("mm") || hit.contains("MM") || hit.contains("Mm") || hit.contains("mM")) {
                        tempValue = tempValue;
                    }
                    if (cur_zhijing == 0.00 || tempValue > cur_zhijing) {
                        cur_zhijing = tempValue;
                        cur_hit = hit;
                    }
                }

            }


        }
        return cur_hit;
    }

    public static double zhijingOrderCM(String hit) {
        //cm
        double cur_zhijing = 0;
        List<String> listS = new ArrayList<>();
        String tempResult = hit.toLowerCase();
        String[] tempResultList = tempResult.split("[,，]");
        for (String s : tempResultList) {
            if(s.contains("cm") || s.contains("mm")){
                listS.addAll(ReUtil.findAll(regx, s, 0));
            }
        }
        if (!listS.isEmpty()) {
            listS.sort((x1, x2) -> Double.valueOf(x1).compareTo(Double.valueOf(x2)));
            String s = listS.get(listS.size() - 1);

            if (NumberUtil.isNumber(s)) {
                double tempValue = Double.valueOf(s);
                if (hit.contains("cm") || hit.contains("CM") || hit.contains("Cm") || hit.contains("cM")) {
                    cur_zhijing = tempValue;
                } else if (hit.contains("mm") || hit.contains("MM") || hit.contains("Mm") || hit.contains("mM")) {
                    cur_zhijing = tempValue / 10;
                }
            }
        }

        return cur_zhijing;
    }

    public static void main(String[] args) {
        String str = "两侧胸廓对称。右肺上叶可见小结节影。两侧肺门不大。纵隔窗示心影及大血管形态正常，纵隔内未见肿块及明显肿大淋巴结。无胸腔积液及胸膜增厚。右胸膜下见结节影。";
        CheckResultMsS checkResultMsS = new CheckResultMsS();
        checkResultMsS.setId(1);
        checkResultMsS.setBookTime(new Date());
        checkResultMsS.setInitResult("胸部（肺部）CT@#刘杰 审核者:李明");
        checkResultMsS.setItemNameComm("描述");
        checkResultMsS.setResults(str);
        checkResultMsS.setVid("1111");
        List<CheckResultMsEtl> list = new ArrayList<>();
        System.out.println("1");
    }

}
