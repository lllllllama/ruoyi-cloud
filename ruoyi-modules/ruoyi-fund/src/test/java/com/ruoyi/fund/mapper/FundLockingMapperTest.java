package com.ruoyi.fund.mapper;

import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class FundLockingMapperTest
{
    @Test
    public void planRowQueriesUseForUpdate() throws Exception
    {
        assertForUpdate("mapper/fund/FundAllocationPlanMapper.xml", "selectForUpdate");
        assertForUpdate("mapper/fund/FundUsePlanMapper.xml", "selectForUpdate");
    }

    @Test
    public void budgetAndRecordQueriesUseForUpdate() throws Exception
    {
        assertForUpdate("mapper/fund/FundProjectBudgetMapper.xml", "selectByTopicIdForUpdate");
        assertForUpdate("mapper/fund/FundAllocationRecordMapper.xml", "selectForUpdate");
        assertForUpdate("mapper/fund/FundUseRecordMapper.xml", "selectForUpdate");
    }

    private void assertForUpdate(String resource, String statement) throws Exception
    {
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        assertTrue("missing mapper resource " + resource, input != null);
        String xml;
        try
        {
            xml = IOUtils.toString(input, StandardCharsets.UTF_8);
        }
        finally
        {
            input.close();
        }
        int start = xml.indexOf("id=\"" + statement + "\"");
        int end = xml.indexOf("</select>", start);
        assertTrue("missing " + statement + " in " + resource, start >= 0 && end > start);
        assertTrue(statement + " must lock the plan row", xml.substring(start, end).toLowerCase().contains("for update"));
    }
}
