package com.smzdm.client.android.bean.component_bean;

import java.util.List;

/** Minimal stand-in for the recommendation list component. */
public class ComponentListBean {
    public String zz_type = "list";
    public List<?> zz_content;

    public ComponentListBean(List<?> content) {
        zz_content = content;
    }
}
