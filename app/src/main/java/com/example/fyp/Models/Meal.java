package com.example.fyp.Models;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class Meal implements Serializable {
    @Expose
    String strMeal;
    @Expose
    String strInstructions;
    @Expose
    String strIngredient1;
    @Expose
    String strIngredient2;
    @Expose
    String strIngredient3;
    @Expose
    String strIngredient4;
    @Expose
    String strIngredient5;
    @Expose
    String strIngredient6;
    @Expose
    String strIngredient7;
    @Expose
    String strIngredient8;
    @Expose
    String strIngredient9;
    @Expose
    String strIngredient10;
    @Expose
    String strIngredient11;
    @Expose
    String strIngredient12;
    @Expose
    String strIngredient13;
    @Expose
    String strIngredient14;
    @Expose
    String strIngredient15;
    @Expose
    String strIngredient16;
    @Expose
    String strIngredient17;
    @Expose
    String strIngredient18;
    @Expose
    String strIngredient19;
    @Expose
    String strIngredient20;
    @Expose
    String strMealThumb;

    public String getStrMeal() {
        return strMeal;
    }

    public void setStrMeal(String strMeal) {
        this.strMeal = strMeal;
    }

    public String getStrInstructions() {
        return strInstructions;
    }

    public void setStrInstructions(String strInstructions) {
        this.strInstructions = strInstructions;
    }

    public String getStrIngredient1() {
        return strIngredient1;
    }

    public void setStrIngredient1(String strIngredient1) {
        this.strIngredient1 = strIngredient1;
    }

    public String getStrIngredient2() {
        return strIngredient2;
    }

    public void setStrIngredient2(String strIngredient2) {
        this.strIngredient2 = strIngredient2;
    }

    public String getStrIngredient3() {
        return strIngredient3;
    }

    public void setStrIngredient3(String strIngredient3) {
        this.strIngredient3 = strIngredient3;
    }

    public String getStrIngredient4() {
        return strIngredient4;
    }

    public void setStrIngredient4(String strIngredient4) {
        this.strIngredient4 = strIngredient4;
    }

    public String getStrIngredient5() {
        return strIngredient5;
    }

    public void setStrIngredient5(String strIngredient5) {
        this.strIngredient5 = strIngredient5;
    }

    public String getStrIngredient6() {
        return strIngredient6;
    }

    public void setStrIngredient6(String strIngredient6) {
        this.strIngredient6 = strIngredient6;
    }

    public String getStrIngredient7() {
        return strIngredient7;
    }

    public void setStrIngredient7(String strIngredient7) {
        this.strIngredient7 = strIngredient7;
    }

    public String getStrIngredient8() {
        return strIngredient8;
    }

    public void setStrIngredient8(String strIngredient8) {
        this.strIngredient8 = strIngredient8;
    }

    public String getStrIngredient9() {
        return strIngredient9;
    }

    public void setStrIngredient9(String strIngredient9) {
        this.strIngredient9 = strIngredient9;
    }

    public String getStrIngredient10() {
        return strIngredient10;
    }

    public void setStrIngredient10(String strIngredient10) {
        this.strIngredient10 = strIngredient10;
    }

    public String getStrIngredient11() {
        return strIngredient11;
    }

    public void setStrIngredient11(String strIngredient11) {
        this.strIngredient11 = strIngredient11;
    }

    public String getStrIngredient12() {
        return strIngredient12;
    }

    public void setStrIngredient12(String strIngredient12) {
        this.strIngredient12 = strIngredient12;
    }

    public String getStrIngredient13() {
        return strIngredient13;
    }

    public void setStrIngredient13(String strIngredient13) {
        this.strIngredient13 = strIngredient13;
    }

    public String getStrIngredient14() {
        return strIngredient14;
    }

    public void setStrIngredient14(String strIngredient14) {
        this.strIngredient14 = strIngredient14;
    }

    public String getStrIngredient15() {
        return strIngredient15;
    }

    public void setStrIngredient15(String strIngredient15) {
        this.strIngredient15 = strIngredient15;
    }

    public String getStrIngredient16() {
        return strIngredient16;
    }

    public void setStrIngredient16(String strIngredient16) {
        this.strIngredient16 = strIngredient16;
    }

    public String getStrIngredient17() {
        return strIngredient17;
    }

    public void setStrIngredient17(String strIngredient17) {
        this.strIngredient17 = strIngredient17;
    }

    public String getStrIngredient18() {
        return strIngredient18;
    }

    public void setStrIngredient18(String strIngredient18) {
        this.strIngredient18 = strIngredient18;
    }

    public String getStrIngredient19() {
        return strIngredient19;
    }

    public void setStrIngredient19(String strIngredient19) {
        this.strIngredient19 = strIngredient19;
    }

    public String getStrIngredient20() {
        return strIngredient20;
    }

    public void setStrIngredient20(String strIngredient20) {
        this.strIngredient20 = strIngredient20;
    }

    public String getStrMealThumb() {
        return strMealThumb;
    }

    public void setStrMealThumb(String strMealThumb) {
        this.strMealThumb = strMealThumb;
    }

    public List<String> getAllIngredients(){
        List<String> res = new ArrayList<>();
        res.add(getStrIngredient1());
        res.add(getStrIngredient2());
        res.add(getStrIngredient3());
        res.add(getStrIngredient4());
        res.add(getStrIngredient5());
        res.add(getStrIngredient6());
        res.add(getStrIngredient7());
        res.add(getStrIngredient8());
        res.add(getStrIngredient9());
        res.add(getStrIngredient10());
        res.add(getStrIngredient11());
        res.add(getStrIngredient12());
        res.add(getStrIngredient13());
        res.add(getStrIngredient14());
        res.add(getStrIngredient15());
        res.add(getStrIngredient16());
        res.add(getStrIngredient17());
        res.add(getStrIngredient18());
        res.add(getStrIngredient19());
        res.add(getStrIngredient20());
        return res;
    }
}
