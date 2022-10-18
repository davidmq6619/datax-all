package com.sinohealth.datax.entity.source;

import com.sinohealth.datax.reflection.annotations.db_alias;

import java.util.Date;


public class StandardCustomerRecord {

    public String vid;
    public String name;//用户姓名
    public String sex;//性别
    public String mobile;
    @db_alias("apply_time")
    public String applyTime;//体检时间
    @db_alias("customer_csrq")
    public String customerCsrq;//出生年月
    @db_alias("marital_status")
    public String maritalStatus;//婚姻状态
    public String address;
    @db_alias("customer_sfzh")
    public String customerSfzh;
    public String age;
    public String nation;//民族
    @db_alias("clean_time")
    public Date cleanTime;
    public String remark;

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(String applyTime) {
        this.applyTime = applyTime;
    }

    public String getCustomerCsrq() {
        return customerCsrq;
    }

    public void setCustomerCsrq(String customerCsrq) {
        this.customerCsrq = customerCsrq;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCustomerSfzh() {
        return customerSfzh;
    }

    public void setCustomerSfzh(String customerSfzh) {
        this.customerSfzh = customerSfzh;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public Date getCleanTime() {
        return cleanTime;
    }

    public void setCleanTime(Date cleanTime) {
        this.cleanTime = cleanTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
