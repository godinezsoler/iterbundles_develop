create table Base_Communities (
	id_ LONG not null primary key,
	groupId LONG,
	privateSearchUrl VARCHAR(75) null,
	publicSearchUrl VARCHAR(75) null,
	fuzzySearch BOOLEAN,
	primarySectionBots VARCHAR(75) null,
	secondarySectionBots VARCHAR(75) null,
	noSectionBots VARCHAR(75) null,
	lastUpdated STRING null,
	loginconf STRING null,
	registerconf STRING null,
	facebookLanguage VARCHAR(75) null
);

create table Base_Iter (
	id_ LONG not null primary key,
	name VARCHAR(75) null,
	version VARCHAR(75) null,
	publicKey STRING null,
	cookieKey STRING null,
	environment VARCHAR(75) null
);