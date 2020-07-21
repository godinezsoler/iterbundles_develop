create table Xmlio_Channel (
	id_ LONG not null primary key,
	groupId LONG,
	name VARCHAR(75) null,
	description VARCHAR(75) null,
	type_ VARCHAR(75) null,
	mode VARCHAR(75) null,
	filePath VARCHAR(75) null,
	xslPath VARCHAR(75) null,
	ftpServer VARCHAR(75) null,
	ftpUser VARCHAR(75) null,
	ftpPassword VARCHAR(75) null,
	status BOOLEAN,
	program BOOLEAN,
	programHour INTEGER,
	programMin INTEGER,
	rangeType VARCHAR(75) null,
	rangeTimeAll BOOLEAN,
	rangeTimeValue INTEGER,
	rangeTimeUnit VARCHAR(75) null
);

create table Xmlio_ChannelControl (
	id_ LONG not null primary key,
	groupId LONG,
	userId LONG,
	type_ VARCHAR(75) null,
	operation VARCHAR(75) null,
	status VARCHAR(75) null,
	startDate DATE null,
	endDate DATE null,
	operations LONG,
	errors LONG,
	fileSize LONG,
	errorLog STRING null
);

create table Xmlio_ChannelControlLog (
	id_ LONG not null primary key,
	channelControlId LONG,
	groupId LONG,
	globalId VARCHAR(75) null,
	operation VARCHAR(75) null,
	classNameValue VARCHAR(75) null,
	errorLog STRING null
);

create table Xmlio_Live (
	id_ LONG not null primary key,
	groupId LONG,
	classNameValue VARCHAR(75) null,
	globalId VARCHAR(75) null,
	localId VARCHAR(75) null,
	operation VARCHAR(75) null,
	status VARCHAR(75) null,
	modifiedDate DATE null,
	performDate DATE null,
	errorLog STRING null,
	existInLive VARCHAR(75) null
);

create table Xmlio_LiveConfiguration (
	id_ LONG not null primary key,
	companyId LONG,
	remoteIterServer VARCHAR(75) null,
	gatewayHost VARCHAR(75) null,
	localPath VARCHAR(75) null,
	outputMethod VARCHAR(75) null,
	ftpPath VARCHAR(75) null,
	ftpUser VARCHAR(75) null,
	ftpPassword VARCHAR(75) null,
	remotePath VARCHAR(75) null,
	destinationType VARCHAR(75) null,
	remoteChannelId LONG,
	remoteUserId LONG,
	remoteUserName VARCHAR(75) null,
	remoteUserPassword VARCHAR(75) null,
	remoteGlobalGroupId LONG,
	remoteCompanyId LONG,
	archive BOOLEAN,
	connectionTimeOut LONG,
	operationTimeOut LONG
);

create table Xmlio_LiveControl (
	id_ LONG not null primary key,
	groupId LONG,
	userId LONG,
	processId VARCHAR(75) null,
	subprocessId VARCHAR(75) null,
	type_ VARCHAR(75) null,
	status VARCHAR(75) null,
	fileSize LONG,
	operations LONG,
	errors LONG,
	startDate DATE null,
	endDate DATE null,
	errorLog STRING null
);

create table Xmlio_LivePool (
	id_ LONG not null primary key,
	livePoolId LONG,
	liveParentId LONG,
	liveChildId LONG,
	processId VARCHAR(75) null
);