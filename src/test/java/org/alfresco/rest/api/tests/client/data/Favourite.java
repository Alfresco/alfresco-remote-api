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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.rest.api.tests.PublicApiDateFormat;
import org.alfresco.rest.api.tests.client.PublicApiClient.ExpectedPaging;
import org.alfresco.rest.api.tests.client.PublicApiClient.ListResponse;
import org.alfresco.service.cmr.favourites.FavouritesService.Type;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;

public class Favourite implements Serializable, ExpectedComparison, Comparable<Favourite>
{
	private static final long serialVersionUID = 2812585719477560349L;

	private String username;
	private String targetGuid;
	private Date createdAt;
	private Date modifiedAt;
	private FavouritesTarget target;
	private Type type;

	public Favourite(FavouritesTarget target) throws ParseException
	{
		this((Date)null, (Date)null, target);
	}

	public Favourite(Date createdAt, Date modifiedAt, FavouritesTarget target) throws ParseException
	{
		if(target != null)
		{
			this.targetGuid = target.getTargetGuid();
		}
		this.username = null;
		this.createdAt = createdAt;
		this.modifiedAt = modifiedAt;
		this.target = target;
		if(target instanceof FileFavouriteTarget)
		{
			this.type = Type.FILE;
		}
		else if(target instanceof FolderFavouriteTarget)
		{
			this.type = Type.FOLDER;
		}
		else if(target instanceof SiteFavouriteTarget)
		{
			this.type = Type.SITE;
		}
		else
		{
			this.type = null;
		}
	}
	
	public Favourite(String createdAt, String modifiedAt, FavouritesTarget target) throws ParseException
	{
		this(getDate(createdAt), getDate(modifiedAt), target);
	}
	
	private static Date getDate(String dateStr) throws ParseException
	{
		Date date = (dateStr != null ? PublicApiDateFormat.getDateFormat().parse(dateStr) : null);
		return date;
	}

	public String getTargetGuid()
	{
		return targetGuid;
	}

	public String getUsername()
	{
		return username;
	}

	public Date getCreatedAt()
	{
		return createdAt;
	}

	public FavouritesTarget getTarget()
	{
		return target;
	}
	
	public Date getModifiedAt()
	{
		return modifiedAt;
	}

	public Type getType()
	{
		Type type = null;
		if(target instanceof FileFavouriteTarget)
		{
			type = Type.FILE;
		}
		else if(target instanceof FolderFavouriteTarget)
		{
			type = Type.FOLDER;
		}
		else if(target instanceof SiteFavouriteTarget)
		{
			type = Type.SITE;
		}
		return type;
	}

	@SuppressWarnings("unchecked")
	public ObjectNode toJSON()
	{
		ObjectNode favouriteJson = AlfrescoDefaultObjectMapper.createObjectNode();
		if(target != null)
		{
			favouriteJson.set("target", target.toJSON());
		}
		return favouriteJson;
	}

	public static FavouritesTarget parseTarget(JsonNode jsonObject) throws ParseException
	{
		FavouritesTarget ret = null;

		if(jsonObject.has("site"))
		{
			JsonNode siteJSON = jsonObject.get("site");
			Site site = SiteImpl.parseSite(siteJSON);
			ret = new SiteFavouriteTarget(site);
			
		}
		else if(jsonObject.has("file"))
		{
			JsonNode documentJSON = jsonObject.get("file");
			FavouriteDocument document = FavouriteDocument.parseDocument(documentJSON);
			ret = new FileFavouriteTarget(document);
			
		}
		else if(jsonObject.has("folder"))
		{
			JsonNode folderJSON = jsonObject.get("folder");
			FavouriteFolder folder = FavouriteFolder.parseFolder(folderJSON);
			ret = new FolderFavouriteTarget(folder);
		}

		return ret;
	}

	public static Favourite parseFavourite(JsonNode jsonObject) throws ParseException
	{
		String createdAt = jsonObject.path("createdAt").textValue();
		String modifiedAt = jsonObject.path("modifiedAt").textValue();
		JsonNode jsonTarget = jsonObject.path("target");
		FavouritesTarget target = parseTarget(jsonTarget);
		Favourite favourite = new Favourite(createdAt, modifiedAt, target);
		return favourite;
	}

	public static ListResponse<Favourite> parseFavourites(JsonNode jsonObject) throws ParseException
	{
		List<Favourite> favourites = new ArrayList<Favourite>();

		JsonNode jsonList = jsonObject.get("list");
		assertNotNull(jsonList);

		ArrayNode jsonEntries = (ArrayNode)jsonList.get("entries");
		assertNotNull(jsonEntries);

		for(int i = 0; i < jsonEntries.size(); i++)
		{
			JsonNode jsonEntry = jsonEntries.get(i);
			JsonNode entry = jsonEntry.get("entry");
			favourites.add(Favourite.parseFavourite(entry));
		}

		ExpectedPaging paging = ExpectedPaging.parsePagination(jsonList);
		return new ListResponse<Favourite>(paging, favourites);
	}
	
	@Override
	public void expected(Object o)
	{
		assertTrue(o instanceof Favourite);
		
		Favourite other = (Favourite)o;

		if(target == null)
		{
			fail();
		}
		target.expected(other.getTarget());
		if(createdAt != null)
		{
			assertTrue(other.getCreatedAt().equals(createdAt) || other.getCreatedAt().after(createdAt));
		}
		if(modifiedAt != null)
		{
			AssertUtil.assertEquals("modifiedAt", modifiedAt, other.getModifiedAt());
		}
		AssertUtil.assertEquals("targetGuid", targetGuid, other.getTargetGuid());
	}

	@Override
	public String toString()
	{
		return "Favourite [username=" + username + ", targetGuid=" + targetGuid
				+ ", createdAt=" + createdAt + ", modifiedAt = " + modifiedAt + ", target=" + target + "]";
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((targetGuid == null) ? 0 : targetGuid.hashCode());
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
		Favourite other = (Favourite) obj;
		if (targetGuid == null) {
			if (other.targetGuid != null)
				return false;
		} else if (!targetGuid.equals(other.targetGuid))
			return false;
		return true;
	}

	@Override
	public int compareTo(Favourite o)
	{
		int idx = (type != null ? type.compareTo(o.getType()) : 0);
		if(idx == 0)
		{
			idx = o.getCreatedAt().compareTo(createdAt);			
		}
		
		return idx;
	}
}
