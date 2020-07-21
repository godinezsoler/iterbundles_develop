/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.util;



public class TrackingSearchObject implements Comparable<Object> {		
		
	public TrackingSearchObject() {
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	public long getViews() {
		return views;
	}

	public void setViews(long views) {
		this.views = views;
	}

	public long getSent() {
		return sent;
	}

	public void setSent(long sent) {
		this.sent = sent;
	}

	public long getComments() {
		return comments;
	}

	public void setComments(long comments) {
		this.comments = comments;
	}

	public boolean isModeration() {
		return moderation;
	}

	public void setModeration(boolean moderation) {
		this.moderation = moderation;
	}

	public long getVotings() {
		return votings;
	}

	public void setVotings(long votings) {
		this.votings = votings;
	}	

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	protected String name;	
	protected String type;	
	protected double rating;	
	protected long views;	
	protected long sent;
	protected long comments;	
	protected boolean moderation;	
	protected long votings;
	protected String contentId;	
	
	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
		
}
