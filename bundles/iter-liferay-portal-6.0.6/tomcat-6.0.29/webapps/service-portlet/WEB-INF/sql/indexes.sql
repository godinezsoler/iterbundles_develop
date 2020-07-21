create index IX_E85E4750 on Services_Service (groupId);
create index IX_7B576439 on Services_Service (groupId, linkId);
create unique index IX_619B910 on Services_Service (groupId, serviceId);
create unique index IX_F5D461B8 on Services_Service (groupId, title);