package com.lx.tester.api;

public class LxApiParameter {

    private String key;
    private String account;
    private int agent;
    private String companyKey;
    private String url;
    private String appUrl;

    public LxApiParameter(String key, String account, int agent, String companyKey, String url, String appUrl) {
        this.key = key;
        this.account = account;
        this.agent = agent;
        this.companyKey = companyKey;
        this.url = url;
        this.appUrl = appUrl;
    }

    public String getKey() {
        return key;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public int getAgent() {
        return agent;
    }

    public String getCompanyKey() {
        return companyKey;
    }

    public String getUrl() {
        return url;
    }

    public String getAppUrl() {
        return appUrl;
    }
}
