create table News_ArticlePoll (
	id_ LONG not null primary key,
	groupId LONG,
	contentId VARCHAR(75) null,
	pollId LONG
);

create table News_Categorize (
	id_ LONG not null primary key,
	dummy VARCHAR(75) null
);

create table News_Comments (
	id_ LONG not null primary key,
	contentId VARCHAR(75) null,
	groupId LONG,
	userId LONG,
	userName VARCHAR(75) null,
	message STRING null,
	email VARCHAR(75) null,
	publicationDate DATE null,
	numComment LONG,
	active_ BOOLEAN,
	moderated BOOLEAN
);

create table News_Counters (
	id_ LONG not null primary key,
	contentId VARCHAR(75) null,
	groupId LONG,
	counter LONG,
	value LONG,
	counterLast LONG,
	operation INTEGER,
	date_ DATE null
);

create table News_JournalArticle (
	id_ LONG not null primary key,
	dummy VARCHAR(75) null
);

create table News_Metadata (
	uuid_ VARCHAR(75) null,
	id_ LONG not null primary key,
	groupId LONG,
	structureName VARCHAR(75) null,
	preferences STRING null
);

create table News_PageContent (
	uuid_ VARCHAR(75) null,
	id_ LONG not null primary key,
	pageContentId VARCHAR(75) null,
	contentId VARCHAR(75) null,
	contentGroupId LONG,
	qualificationId VARCHAR(75) null,
	layoutId VARCHAR(75) null,
	groupId LONG,
	defaultSection BOOLEAN,
	online_ BOOLEAN,
	typeContent VARCHAR(75) null,
	orden INTEGER,
	articleModelId LONG,
	modifiedDate DATE null,
	vigenciahasta DATE null,
	vigenciadesde DATE null
);

create table News_Qualification (
	uuid_ VARCHAR(75) null,
	id_ LONG not null primary key,
	name VARCHAR(75) null,
	groupId LONG,
	modifiedDate DATE null,
	qualifId VARCHAR(75) null
);