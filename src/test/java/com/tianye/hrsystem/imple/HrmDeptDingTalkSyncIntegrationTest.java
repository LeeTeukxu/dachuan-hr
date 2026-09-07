package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * 部门同步钉钉-真实集成测试(直连测试库与钉钉)：
 * 验证分公司模式下排除关键字覆盖逻辑。会真实修改 hr_0004 部门数据，仅在测试环境运行。
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("local")
public class HrmDeptDingTalkSyncIntegrationTest {

    @Autowired
    private HrmDeptDingTalkSyncService deptDingTalkSyncService;

    @Before
    public void setUp() {
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId("0004");
        info.setUserId("1");
        info.setUserName("集成测试");
        CompanyContext.set(info);
    }

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void syncHeadquartersFullCover() {
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId("0003");
        info.setUserId("1");
        info.setUserName("集成测试");
        CompanyContext.set(info);
        Map<String, Object> result = deptDingTalkSyncService.syncDingTalkDept("headquarters", "");
        System.out.println("[HQ SYNC RESULT] " + result);
        CompanyContext.clear();
    }

    @Test
    public void syncBranchWithExcludeKeywords() {
        Map<String, Object> result = deptDingTalkSyncService.syncDingTalkDept("branch",
                "攀枝花田野创新农业科技有限公司,湖北田野农谷生物科技有限公司,湖北宜昌田野创新食品有限公司,湖北田野源味生物科技有限责任公司");
        System.out.println("[SYNC RESULT] " + result);
    }
}
