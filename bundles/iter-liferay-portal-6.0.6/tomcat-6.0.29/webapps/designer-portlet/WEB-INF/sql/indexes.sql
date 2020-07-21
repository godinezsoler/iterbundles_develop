create index IX_5A19E87 on Designer_PageTemplate (groupId);
create unique index IX_529C7960 on Designer_PageTemplate (groupId, layoutId);
create unique index IX_C20BF92F on Designer_PageTemplate (groupId, name, description, type_);
create unique index IX_5580F01F on Designer_PageTemplate (groupId, pageTemplateId);
create index IX_E9E3B56E on Designer_PageTemplate (groupId, type_);
create index IX_2DFB821F on Designer_PageTemplate (groupId, type_, defaultMobileTemplate);
create index IX_6205909D on Designer_PageTemplate (groupId, type_, defaultTemplate);