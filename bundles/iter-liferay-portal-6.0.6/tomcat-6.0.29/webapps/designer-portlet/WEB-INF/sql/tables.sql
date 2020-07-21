create table Designer_PageTemplate (
	id_ LONG not null primary key,
	pageTemplateId VARCHAR(75) null,
	groupId LONG,
	layoutId LONG,
	imageId LONG,
	name VARCHAR(75) null,
	description VARCHAR(75) null,
	type_ VARCHAR(75) null,
	defaultTemplate BOOLEAN,
	defaultMobileTemplate BOOLEAN
);