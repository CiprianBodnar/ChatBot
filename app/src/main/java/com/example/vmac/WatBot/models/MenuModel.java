package com.example.vmac.WatBot.models;

public class MenuModel {

    private String menu;
    private int points;

    public String getMenu() {
        return menu;
    }

    public void setMenu(String menu) {
        this.menu = menu;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return "MenuModel{" +
                "menuString='" + menu + '\'' +
                ", score=" + points +
                '}';
    }
}
