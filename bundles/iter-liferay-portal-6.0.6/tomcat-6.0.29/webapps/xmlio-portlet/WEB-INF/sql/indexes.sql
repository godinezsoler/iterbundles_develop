create index IX_5886303 on Xmlio_Channel (groupId);
create index IX_6A67A16D on Xmlio_Channel (status);

create index IX_68DCCE08 on Xmlio_ChannelControl (groupId);
create index IX_1B0741C8 on Xmlio_ChannelControl (status);
create index IX_91AE82B3 on Xmlio_ChannelControl (type_);
create index IX_D04A9EC4 on Xmlio_ChannelControl (type_, operation);
create index IX_2F6FD9AA on Xmlio_ChannelControl (type_, operation, status);

create index IX_44C36117 on Xmlio_ChannelControlLog (channelControlId);

create index IX_FA29ADF6 on Xmlio_Live (classNameValue);
create index IX_EB3A6E08 on Xmlio_Live (classNameValue, globalId);
create index IX_C1390A36 on Xmlio_Live (groupId);
create index IX_42E3CCCA on Xmlio_Live (groupId, classNameValue, globalId);
create index IX_E53E6F5E on Xmlio_Live (groupId, classNameValue, localId);
create index IX_54B7551F on Xmlio_Live (groupId, classNameValue, operation);
create index IX_1D038D2A on Xmlio_Live (groupId, classNameValue, operation, modifiedDate);
create index IX_26C3BE1E on Xmlio_Live (groupId, classNameValue, status);
create index IX_A7E41A20 on Xmlio_Live (groupId, localId);
create index IX_87F63906 on Xmlio_Live (groupId, localId, status);
create index IX_62DB1C3F on Xmlio_Live (modifiedDate);
create index IX_893BC7DA on Xmlio_Live (status);
create index IX_33CF30FB on Xmlio_Live (status, classNameValue, operation);
create index IX_6923E210 on Xmlio_Live (status, groupId);

create unique index IX_8560FEBA on Xmlio_LiveConfiguration (companyId);

create index IX_5AFED2E4 on Xmlio_LiveControl (endDate);
create index IX_8EAC315B on Xmlio_LiveControl (groupId, status);
create index IX_C33153A6 on Xmlio_LiveControl (groupId, type_, status);
create index IX_937CD925 on Xmlio_LiveControl (processId);
create index IX_D3C76BB on Xmlio_LiveControl (status);
create index IX_682A4246 on Xmlio_LiveControl (type_, status);

create index IX_4C4DB10B on Xmlio_LivePool (liveChildId);
create index IX_DF461955 on Xmlio_LivePool (liveParentId);
create index IX_30BD0747 on Xmlio_LivePool (livePoolId);
create index IX_87F3492C on Xmlio_LivePool (livePoolId, liveParentId);
create unique index IX_C200648F on Xmlio_LivePool (livePoolId, liveParentId, liveChildId);
create index IX_ECC71373 on Xmlio_LivePool (livePoolId, processId);
create index IX_21A55BEA on Xmlio_LivePool (processId);