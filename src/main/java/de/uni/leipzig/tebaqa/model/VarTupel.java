package de.uni.leipzig.tebaqa.model;

public class VarTupel {
    String var1;
    String var2;
    String type;
    public VarTupel(String var1,String var2,String type){
        this.var1=var1;
        this.var2=var2;
        this.type=type;
    }

    public String getVar1() {
        return var1;
    }

    public String getVar2() {
        return var2;
    }

    public String getType() {
        return type;
    }
}
