package com.ruoyi.fund.domain.vo;
import java.math.BigDecimal;
public class FundFinishCheckVo {
    private BigDecimal planAmount;
    private BigDecimal actualAmount;
    private BigDecimal differenceAmount;
    private String finishType;
    private boolean needConfirm;
    public BigDecimal getPlanAmount(){return planAmount;} public void setPlanAmount(BigDecimal v){planAmount=v;}
    public BigDecimal getActualAmount(){return actualAmount;} public void setActualAmount(BigDecimal v){actualAmount=v;}
    public BigDecimal getDifferenceAmount(){return differenceAmount;} public void setDifferenceAmount(BigDecimal v){differenceAmount=v;}
    public String getFinishType(){return finishType;} public void setFinishType(String v){finishType=v;}
    public boolean isNeedConfirm(){return needConfirm;} public void setNeedConfirm(boolean v){needConfirm=v;}
}
