package com.example.vmac.WatBot.models;

import java.util.List;

public class InfoModel {

    private List<MenuModel> info;

    public List<MenuModel> getInfo() {
        return info;
    }

    public void setInfo(List<MenuModel> info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return "InfoModel{" +
                "menuModels=" + info +
                '}';
    }
}
