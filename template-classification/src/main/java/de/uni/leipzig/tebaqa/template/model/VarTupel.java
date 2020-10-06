package de.uni.leipzig.tebaqa.template.model;

public class VarTupel {
    private String var1;
    private String var2;
    private String type;

    public VarTupel(String var1, String var2, String type) {
        this.var1 = var1;
        this.var2 = var2;
        this.type = type;
    }

    public String getVar1() {
        return var1;
    }

    public void setVar1(String var1) {
        this.var1 = var1;
    }

    public String getVar2() {
        return var2;
    }

    public void setVar2(String var2) {
        this.var2 = var2;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
