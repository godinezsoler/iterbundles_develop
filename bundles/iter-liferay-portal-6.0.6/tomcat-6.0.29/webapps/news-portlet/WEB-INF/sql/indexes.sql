create index IX_D183C59B on News_ArticlePoll (groupId);
create unique index IX_7003D9A9 on News_ArticlePoll (groupId, contentId);

create index IX_7C4CDE0 on News_Comments (groupId, contentId);
create index IX_BB43D29 on News_Comments (groupId, contentId, active_);
create index IX_A0CF2551 on News_Comments (groupId, contentId, moderated);
create unique index IX_1ADC8E0D on News_Comments (groupId, contentId, numComment);

create unique index IX_A648AA88 on News_Counters (contentId, groupId, operation);
create index IX_45A425A3 on News_Counters (groupId, contentId);
create index IX_649646D6 on News_Counters (groupId, operation);

create unique index IX_F7165F05 on News_Metadata (groupId, structureName);
create unique index IX_5573EC53 on News_Metadata (id_);
create index IX_609F9673 on News_Metadata (uuid_);
create unique index IX_4A0C6F97 on News_Metadata (uuid_, groupId);

create index IX_C59C53E0 on News_PageContent (contentId);
create index IX_2742E128 on News_PageContent (contentId, online_, vigenciahasta, vigenciadesde);
create index IX_1BF66966 on News_PageContent (groupId);
create index IX_C07B78FE on News_PageContent (groupId, contentId);
create index IX_B1D56B40 on News_PageContent (groupId, contentId, articleModelId);
create index IX_22611AD6 on News_PageContent (groupId, contentId, defaultSection);
create index IX_8BCEB17 on News_PageContent (groupId, contentId, layoutId);
create index IX_48D6564A on News_PageContent (groupId, contentId, online_, vigenciahasta, vigenciadesde);
create index IX_B834797F on News_PageContent (groupId, layoutId);
create unique index IX_3575352A on News_PageContent (groupId, layoutId, contentId, typeContent);
create index IX_DC511D1B on News_PageContent (groupId, layoutId, orden);
create index IX_FE0151B4 on News_PageContent (groupId, layoutId, qualificationId, typeContent);
create index IX_F3593930 on News_PageContent (groupId, layoutId, typeContent);
create unique index IX_C60C72EF on News_PageContent (groupId, pageContentId);
create index IX_18431D70 on News_PageContent (uuid_);
create unique index IX_5794753A on News_PageContent (uuid_, groupId);

create index IX_CFA65641 on News_Qualification (groupId);
create unique index IX_261D3140 on News_Qualification (groupId, name);
create unique index IX_FB4BDB1C on News_Qualification (groupId, qualifId);
create unique index IX_31BD7853 on News_Qualification (groupId, uuid_);
create index IX_592E850B on News_Qualification (uuid_);
create unique index IX_64046BFF on News_Qualification (uuid_, groupId);