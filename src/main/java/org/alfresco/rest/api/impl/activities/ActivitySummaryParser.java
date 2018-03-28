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
package org.alfresco.rest.api.impl.activities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.json.JsonUtil;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.alfresco.util.registry.NamedObjectRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.ISO8601DateFormat;

/*
 * Adapted from JSONtoFmModel
 */
public class ActivitySummaryParser implements ActivitySummaryProcessorRegistry
{
    private final Log logger = LogFactory.getLog(ActivitySummaryParser.class);

    // note: current format is dependent on ISO8601DateFormat.parser, eg. YYYY-MM-DDThh:mm:ss.sssTZD
    private static String REGEXP_ISO8061 = "^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})(.([0-9]){3})?(Z|[\\+\\-]([0-9]{2}):([0-9]{2}))$";
    private static Pattern matcherISO8601 = Pattern.compile(REGEXP_ISO8061);
    private static final Pattern nodeRefPattern = Pattern.compile("^[a-zA-Z]+://.+/.+");
    
	private NamedObjectRegistry<ActivitySummaryProcessor> processors;
	
    private boolean autoConvertISO8601 = true;

	public ActivitySummaryParser()
	{
	}

	public void setProcessors(NamedObjectRegistry<ActivitySummaryProcessor> processors)
	{
		this.processors = processors;
	}

	public void register(String activityType, ActivitySummaryProcessor processor)
	{
		ActivitySummaryProcessor existingProcessor = processors.getNamedObject(activityType);
		if(existingProcessor != null)
		{
			logger.warn("Activity summary processor " + existingProcessor + " is being overridden by " + processor + " for activity type " + activityType);
		}

		processors.register(activityType, processor);
	}

	private void processActivitySummary(String activityType, Map<String, Object> entries)
	{
		ActivitySummaryProcessor processor = processors.getNamedObject(activityType);
		if(processor == null)
		{
			processor = new BaseActivitySummaryProcessor();
		}

		processor.process(entries);
	}

	public Map<String, Object> parse(String activityType, String activitySummary) throws IOException
	{
		JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(activitySummary);
		Map<String, Object> map = convertJSONObjectToMap(json);
		processActivitySummary(activityType, map);
		return map;
	}
	
    /**
     * Determine if passed string conforms to the pattern of a node reference
     * 
     * @param nodeRef  the node reference as a string
     * @return  true => it matches the pattern of a node reference
     */
    private boolean isNodeRef(String nodeRef)
    {
    	Matcher matcher = nodeRefPattern.matcher(nodeRef);
    	return matcher.matches();
    }
    
    Map<String, Object> convertJSONObjectToMap(JsonNode jo)
    {
        Map<String, Object> model = new HashMap<String, Object>();
        Iterator<Map.Entry<String,JsonNode>> fields = jo.fields();

        while(fields.hasNext())
        {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            if (value instanceof ObjectNode)
            {
                model.put(entry.getKey(), convertJSONObjectToMap(value));
            }
            else if (value instanceof ArrayNode)
            {
                model.put(entry.getKey(), convertJSONArrayToList((ArrayNode) value));
            }
            else if (value == null)
            {
                model.put(entry.getKey(), null);
            }
            else
            {
                if ((value instanceof TextNode) && autoConvertISO8601 && (matcherISO8601.matcher(value.textValue()).matches()))
                {
                    model.put(entry.getKey(), ISO8601DateFormat.parse(value.textValue()));
                }
                else if ((value instanceof TextNode) && isNodeRef(value.textValue()))
                {
                	try
                	{
                		model.put(entry.getKey(), new NodeRef(value.textValue()));
                	}
                	catch(AlfrescoRuntimeException e)
                	{
                		// cannot convert to a nodeRef, just keep as a string
                		logger.warn("Cannot convert activity summary NodeRef string " + value + " to a NodeRef");
                	}
                }
                else
                {
                    model.put(entry.getKey(), JsonUtil.convertJSONValue((ValueNode) value));
                }
            }
        }
       
        return model;
    }
   
    List<Object> convertJSONArrayToList(ArrayNode ja)
    {
        List<Object> model = new ArrayList<Object>();
       
        for (int i = 0; i < ja.size(); i++)
        {
            Object o = ja.get(i);
            
            if (o instanceof ArrayNode)
            {
                model.add(convertJSONArrayToList((ArrayNode) o));
            }
            else if (o instanceof ObjectNode)
            {
                model.add(convertJSONObjectToMap((ObjectNode)o));
            }
            else if (o == null || o instanceof NullNode)
            {
                model.add(null);
            }
            else
            {
                if ((o instanceof String) && autoConvertISO8601 && (matcherISO8601.matcher((String)o).matches()))
                {
                    o = ISO8601DateFormat.parse((String)o);
                }
                
                model.add(o);
            }
        }
       
        return model;
    }

}
