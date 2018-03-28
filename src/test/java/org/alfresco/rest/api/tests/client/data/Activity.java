/*
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.rest.api.tests.client.data;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.json.JsonUtil;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;

public class Activity implements Serializable, ExpectedComparison
{
	private static final long serialVersionUID = 1869724448167732060L;

	private Long id;
    private String networkId;
	private String siteId;
    private String feedPersonId;
    private String postPersonId;
    private String postedAt;
    private String activityType;
    private Map<String, Object> summary;
    
	public Activity(Long id, String networkId, String siteId, String feedPersonId,
			String postPersonId, String postedAt, String activityType,
			Map<String, Object> summary) {
		super();
		this.id = id;
		this.networkId = networkId;
		this.siteId = siteId;
		this.feedPersonId = feedPersonId;
		this.postPersonId = postPersonId;
		this.postedAt = postedAt;
		this.activityType = activityType;
		this.summary = summary;
	}

	public Long getId() {
		return id;
	}

	public String getSiteId() {
		return siteId;
	}

	public String getFeedPersonId() {
		return feedPersonId;
	}

	public String getPostPersonId() {
		return postPersonId;
	}

	public String getPostedAt() {
		return postedAt;
	}

	public String getActivityType() {
		return activityType;
	}

	public Map<String, Object> getSummary() {
		return summary;
	}

	public String getNetworkId()
	{
		return networkId;
	}

	@Override
	public String toString()
	{
		return "Activity [id=" + id + ", networkId=" + networkId + ", siteId="
				+ siteId + ", feedPersonId=" + feedPersonId + ", postPersonId="
				+ postPersonId + ", postedAt=" + postedAt + ", activityType="
				+ activityType + ", summary=" + summary + "]";
	}

	@SuppressWarnings("unchecked")
	public static Activity parseActivity(JsonNode jsonObject) throws IOException
	{
		Long id = jsonObject.get("id").longValue();
		String networkId = jsonObject.get("networkId").textValue();
		String siteId = jsonObject.get("siteId").textValue();
		String feedPersonId = jsonObject.get("feedPersonId").textValue();
		String postPersonId = jsonObject.get("postPersonId").textValue();
		String postedAt = jsonObject.get("postedAt").textValue();
		String activityType = jsonObject.get("activityType").textValue();
		Map<String, Object> summary = JsonUtil.convertJSONObjectToMap((ObjectNode) jsonObject.get("activitySummary"));
		Activity activity = new Activity(id, networkId, siteId, feedPersonId, postPersonId, postedAt, activityType, summary);
		return activity;
	}
	
	@SuppressWarnings("unchecked")
	public ObjectNode toJSON()
	{
		ObjectNode jsonObject = AlfrescoDefaultObjectMapper.createObjectNode();
		jsonObject.put("id", String.valueOf(getId()));
		jsonObject.put("networkId", getNetworkId());
		jsonObject.put("siteId", getSiteId());
		jsonObject.put("feedPersonId", getFeedPersonId());
		jsonObject.put("postPersonId", getPostPersonId());
		jsonObject.put("postedAt", getPostedAt());
		jsonObject.put("activityType", getActivityType());
		jsonObject.set("activitySummary", AlfrescoDefaultObjectMapper.convertValue(getSummary(), JsonNode.class));
		return jsonObject;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Activity other = (Activity) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public void expected(Object o)
	{
		assertTrue(o instanceof Activity);
		
		Activity other = (Activity)o;
		AssertUtil.assertEquals("id", id, other.getId());
		AssertUtil.assertEquals("siteId", networkId, other.getNetworkId());
		AssertUtil.assertEquals("siteId", siteId, other.getSiteId());
		AssertUtil.assertEquals("feedPersonId", feedPersonId, other.getFeedPersonId());
		AssertUtil.assertEquals("postPersonId", postPersonId, other.getPostPersonId());
		AssertUtil.assertEquals("postedAt", postedAt, other.getPostedAt());
		AssertUtil.assertEquals("activityType", activityType, other.getActivityType());
		AssertUtil.assertEquals("summary", summary, other.getSummary());
	}

	public static Map<String, Object> getActivitySummary(JsonNode activitySummary, String activityType)
	{
		Map<String, Object> summary = new HashMap<String, Object>();

		if(activityType.equals("org.alfresco.documentlibrary.file-added"))
		{
			String nodeRefStr = (((ObjectNode) activitySummary).remove("nodeRef")).textValue();
			if(NodeRef.isNodeRef(nodeRefStr))
			{
				summary.put("objectId", new NodeRef(nodeRefStr).getId());
			}
			else
			{
				throw new RuntimeException("nodeRef " + nodeRefStr + " in activity feed is not a valid NodeRef");
			}
			String parentNodeRefStr = (((ObjectNode) activitySummary).remove("parentNodeRef")).textValue();
			if(NodeRef.isNodeRef(parentNodeRefStr))
			{
				summary.put("parentObjectId", new NodeRef(parentNodeRefStr).getId());
			}
			else
			{
				throw new RuntimeException("parentNodeRef " + parentNodeRefStr + " in activity feed is not a valid NodeRef");
			}
			summary.put("lastName", activitySummary.get("lastName"));
			summary.put("firstName", activitySummary.get("firstName"));
			summary.put("title", activitySummary.get("title"));
		} else if(activityType.equals("org.alfresco.site.user-joined"))
		{
			summary.put("lastName", activitySummary.get("lastName"));
			summary.put("firstName", activitySummary.get("firstName"));
			summary.put("memberLastName", activitySummary.get("memberLastName"));
			summary.put("memberFirstName", activitySummary.get("memberFirstName"));
			summary.put("memberPersonId", activitySummary.get("memberUserName"));
			summary.put("role", activitySummary.get("role"));
			summary.put("title", activitySummary.get("title"));
		}

		return summary;
	}
}
