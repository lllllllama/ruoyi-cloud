package com.ruoyi.fund.controller;
import java.util.List; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.validation.annotation.Validated; import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.controller.BaseController; import com.ruoyi.common.core.web.domain.AjaxResult; import com.ruoyi.common.core.web.page.TableDataInfo; import com.ruoyi.common.log.annotation.Log; import com.ruoyi.common.log.enums.BusinessType; import com.ruoyi.common.security.annotation.RequiresPermissions; import com.ruoyi.fund.domain.FundTopic; import com.ruoyi.fund.service.IFundTopicService;
@RestController @RequestMapping("/topic") public class FundTopicController extends BaseController { @Autowired private IFundTopicService service;
 @GetMapping("/list") public TableDataInfo list(FundTopic q){startPage();List<FundTopic> list=service.selectList(q);return getDataTable(list);} @GetMapping("/accessible") public AjaxResult accessible(){return AjaxResult.success(service.selectAccessibleList());}
 @GetMapping("/{id}") public AjaxResult get(@PathVariable Long id){return AjaxResult.success(service.selectById(id));}
 @RequiresPermissions("fund:topic:add") @Log(title="资金课题配置",businessType=BusinessType.INSERT) @PostMapping public AjaxResult add(@Validated @RequestBody FundTopic t){return toAjax(service.insert(t));}
 @RequiresPermissions("fund:topic:edit") @Log(title="资金课题配置",businessType=BusinessType.UPDATE) @PutMapping public AjaxResult edit(@Validated @RequestBody FundTopic t){return toAjax(service.update(t));}
 @RequiresPermissions("fund:topic:remove") @Log(title="资金课题配置",businessType=BusinessType.DELETE) @DeleteMapping("/{id}") public AjaxResult del(@PathVariable Long id){return toAjax(service.delete(id));}
}
