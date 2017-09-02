package kr.co.future.sslvpn.auth.sm;

import kr.co.future.api.FieldOption;

/**
 * Created by CRChoi on 2015-04-23.
 */
public class SMAuthUrl {
    @FieldOption(nullable = false)
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
