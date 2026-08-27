USE `ry-config`;

SET @research_content = '# Spring configuration\nspring:\n  redis:\n    host: localhost\n    port: 6379\n    password:\n  datasource:\n    dynamic:\n      primary: master\n      datasource:\n        master:\n          driver-class-name: com.mysql.cj.jdbc.Driver\n          url: jdbc:mysql://localhost:3306/ry-research?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8\n          username: root\n          password: password\n\nmybatis:\n  typeAliasesPackage: com.ruoyi.research.domain\n  mapperLocations: classpath:mapper/**/*.xml\n\nswagger:\n  title: Research management module API\n  license: Powered By ruoyi\n  licenseUrl: https://ruoyi.vip\n\nseata:\n  enabled: false\n  enable-auto-data-source-proxy: false\n';

INSERT INTO config_info
    (data_id, group_id, content, md5, gmt_create, gmt_modified, src_user,
     src_ip, app_name, tenant_id, c_desc, c_use, effect, type, c_schema)
VALUES
    ('ruoyi-research-dev.yml', 'DEFAULT_GROUP', @research_content,
     MD5(@research_content), NOW(), NOW(), 'research-install', '127.0.0.1',
     '', '', 'Research management module', 'null', 'null', 'yaml', 'null')
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    md5 = VALUES(md5),
    gmt_modified = NOW();

-- The gateway has discovery.locator.enabled=true, so /ruoyi-research/** is
-- discovered automatically and needs no additional static route.
