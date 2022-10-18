package com.sinohealth.datax.entity.source;

import com.sinohealth.datax.reflection.annotations.db_alias;

import java.util.Date;

@db_alias("view_yyqkb")
public class RegCustomerS2 {

    /**
     * 单号
     */
    @db_alias("VID")
    private String vid;
    @db_alias("JJZH")
    private String jjzh;
    @db_alias("TEMP_BZ")
    private String tempBz;

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getJjzh() {
        return jjzh;
    }

    public void setJjzh(String jjzh) {
        this.jjzh = jjzh;
    }

    public String getTempBz() {
        return tempBz;
    }

    public void setTempBz(String tempBz) {
        this.tempBz = tempBz;
    }
}
