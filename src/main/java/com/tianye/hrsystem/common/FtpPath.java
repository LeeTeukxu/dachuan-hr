package com.tianye.hrsystem.common;

import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;

public class FtpPath {
    String companyId;

    public FtpPath(String companyId) {
        this.companyId = companyId;
    }

    public String getAttachment() {
        return companyId + "/Attachment/";
    }

    public String combine(String... paths) {
        List<String> Fs = new ArrayList<>();
        for (int i = 0; i < paths.length; i++) {
            String pp = paths[i].trim();
            String[] VK = pp.split("/");
            for (int n = 0; n < VK.length; n++) {
                String V = VK[i];
                if (Strings.isEmpty(V) == false) {
                    Fs.add(V);
                }
            }
        }
        return "/" + String.join("/", Fs);
    }
}
