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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Serializable;
import java.text.Collator;
import java.util.*;

import java.util.stream.Collectors;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.rest.api.tests.QueriesPeopleApiTest;
import org.alfresco.rest.api.tests.client.PublicApiClient.ExpectedPaging;
import org.alfresco.rest.api.tests.client.PublicApiClient.ListResponse;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.json.JsonUtil;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;

public class Person
    extends org.alfresco.rest.api.model.Person
        implements Serializable, Comparable<Person>, ExpectedComparison
{
    private static final long serialVersionUID = 3185698391792389751L;

    private String id;

    private static Collator collator = Collator.getInstance();

    public Person()
    {
        super();
    }
    
    public Person(String id, String username, Boolean enabled, String firstName, String lastName,
            Company company, String skype, String location, String tel,
            String mob, String instantmsg, String google, String description)
    {
        super(username, enabled, null, firstName, lastName, null, location, tel, mob, null, skype, instantmsg, null, null, google, null, null, null, description, company);
        this.id = id;
    }

    public Person(String userName,
                  Boolean enabled,
                  NodeRef avatarId,
                  String firstName,
                  String lastName,
                  String jobTitle,
                  String location,
                  String telephone,
                  String mobile,
                  String email,
                  String skypeId,
                  String instantMessageId,
                  String userStatus,
                  Date statusUpdatedAt,
                  String googleId,
                  Long quota,
                  Long quotaUsed,
                  Boolean emailNotificationsEnabled,
                  String description,
                  org.alfresco.rest.api.model.Company company,
                  Map<String, Object> properties,
                  List<String> aspectNames,
                  Map<String, Boolean> capabilities)
    {
        super(userName,
                enabled,
                avatarId,
                firstName,
                lastName,
                jobTitle,
                location,
                telephone,
                mobile,
                email,
                skypeId,
                instantMessageId,
                userStatus,
                statusUpdatedAt,
                googleId,
                quota,
                quotaUsed,
                emailNotificationsEnabled,
                description,
                company);
        this.id = userName;
        this.properties = properties;
        this.aspectNames = aspectNames;
        this.capabilities = capabilities;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Note: used for string comparisons in tests.
     *
     * @see QueriesPeopleApiTest#checkApiCall(java.lang.String, java.lang.String, java.lang.String, org.alfresco.rest.api.tests.client.PublicApiClient.Paging, int, java.lang.String[])
     */
    @Override
    public String toString()
    {
        return "Person [" + (id != null ? "id=" + id + ", " : "")
                + (enabled != null ? "enabled=" + enabled + ", " : "")
                + (firstName != null ? "firstName=" + firstName + ", " : "")
                + (lastName != null ? "lastName=" + lastName + ", " : "")
                + (company != null ? "company=" + company + ", " : "company=" + new Company().toString() + ", ")
                + (skypeId != null ? "skype=" + skypeId + ", " : "")
                + (location != null ? "location=" + location + ", " : "")
                + (telephone != null ? "tel=" + telephone + ", " : "")
                + (mobile != null ? "mob=" + mobile + ", " : "")
                + (instantMessageId != null ? "instantmsg=" + instantMessageId + ", " : "")
                + (googleId != null ? "google=" + googleId + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + "]";
    }

    @SuppressWarnings("unchecked")
    public ObjectNode toJSON(boolean fullVisibility)
    {
        ObjectNode personJson = AlfrescoDefaultObjectMapper.createObjectNode();

        if (getUserName() != null)
        {
            personJson.put("id", getUserName());
        }
        personJson.put("firstName", getFirstName());
        personJson.put("lastName", getLastName());

        if(fullVisibility)
        {
            personJson.put("description", getDescription());
            personJson.put("email", getEmail());
            personJson.put("skypeId", getSkypeId());
            personJson.put("googleId", getGoogleId());
            personJson.put("instantMessageId", getInstantMessageId());
            personJson.put("jobTitle", getJobTitle());
            personJson.put("location", getLocation());
            if (company != null)
            {
                personJson.put("company", new Company(company).toJSON());
            }
            personJson.put("mobile", getMobile());
            personJson.put("telephone", getTelephone());
            personJson.put("userStatus", getUserStatus());
            personJson.put("enabled", isEnabled());
            personJson.put("emailNotificationsEnabled", isEmailNotificationsEnabled());
            personJson.set("properties", AlfrescoDefaultObjectMapper.convertValue(getProperties(), ObjectNode.class));
            personJson.set("aspectNames", AlfrescoDefaultObjectMapper.convertValue(getAspectNames(), ArrayNode.class));
            personJson.set("capabilities", AlfrescoDefaultObjectMapper.convertValue(getCapabilities(), ObjectNode.class));
        }
        return personJson;
    }
    
    public static Person parsePerson(JsonNode jsonObject) throws IOException
    {
        String userId = jsonObject.get("id").textValue();
        String firstName = jsonObject.get("firstName").textValue();
        String lastName = jsonObject.get("lastName").textValue();

        String description = jsonObject.get("description").textValue();
        String email = jsonObject.get("email").textValue();
        String skypeId = jsonObject.get("skypeId").textValue();
        String googleId = jsonObject.get("googleId").textValue();
        String instantMessageId = jsonObject.get("instantMessageId").textValue();
        String jobTitle = jsonObject.get("jobTitle").textValue();
        String location = jsonObject.get("location").textValue();

        Company company = null;
        JsonNode companyJSON = jsonObject.get("company");
        if(companyJSON != null)
        {
            String organization = companyJSON.get("organization").textValue();
            String address1 = companyJSON.get("address1").textValue();
            String address2 = companyJSON.get("address2").textValue();
            String address3 = companyJSON.get("address3").textValue();
            String postcode = companyJSON.get("postcode").textValue();
            String companyTelephone = companyJSON.get("telephone").textValue();
            String fax = companyJSON.get("fax").textValue();
            String companyEmail = companyJSON.get("email").textValue();
            if (organization != null ||
                    address2 != null ||
                    address3 != null ||
                    postcode != null ||
                    companyTelephone != null ||
                    fax != null ||
                    companyEmail != null)
            {
                company = new Company(organization, address1, address2, address3, postcode, companyTelephone, fax, companyEmail);
            }
            else
            {
                company = new Company();
            }
        }

        String mobile = jsonObject.get("mobile").textValue();
        String telephone = jsonObject.get("telephone").textValue();
        String userStatus = jsonObject.get("userStatus").textValue();
        Boolean enabled = jsonObject.get("enabled").booleanValue();
        Boolean emailNotificationsEnabled = jsonObject.get("emailNotificationsEnabled").booleanValue();
        List<String> aspectNames = JsonUtil
                .convertJSONArrayToList((ArrayNode) jsonObject.get("aspectNames"))
                .stream().map(aspectName -> ((String) aspectName)).collect(Collectors.toList());
        Map<String, Object> properties = JsonUtil.convertJSONObjectToMap((ObjectNode) jsonObject.get("properties"));
        Map<String, Boolean> capabilities = JsonUtil
                .convertJSONObjectToMap((ObjectNode) jsonObject.get("capabilities"))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Boolean) e.getValue()));
        
        Person person = new Person(
                userId,
                enabled,
                null, // avatarId is not accepted by "create person"
                firstName,
                lastName,
                jobTitle,
                location,
                telephone,
                mobile,
                email,
                skypeId,
                instantMessageId,
                userStatus,
                null, // userStatusUpdateAt is read only - not used in create person
                googleId,
                null, // quota - not used in create person
                null, // quotaUsers - not used
                emailNotificationsEnabled,
                description,
                company,
                properties,
                aspectNames,
                capabilities
        );
        return person;
    }


    private static class UserContext
    {
        private String networkId;
        private String personId;

        UserContext(String networkId, String personId)
        {
            super();
            this.networkId = networkId;
            this.personId = personId;
        }

        String getNetworkId()
        {
            return networkId;
        }

        String getPersonId()
        {
            return personId;
        }
    }

    private static ThreadLocal<UserContext> userContext = new ThreadLocal<UserContext>();
    public static void setUserContext(String personId)
    {
        String networkId = Person.getNetworkId(personId);
        userContext.set(new UserContext(networkId, personId));
    }
    
    public static void clearUserContext()
    {
        userContext.set(null);
    }
    
    public static UserContext gettUserContext()
    {
        return userContext.get();
    }
    
    public static String getNetworkId(String personId)
    {
        int idx = personId.indexOf("@");
        return(idx == -1 ? TenantService.DEFAULT_DOMAIN : personId.substring(idx + 1));
    }

    private String getNetworkId()
    {
        return Person.getNetworkId(id);
    }
    
    public boolean isVisible()
    {
        boolean ret = true;

        UserContext uc = gettUserContext();
        String networkId = getNetworkId();
        if(uc != null)
        {
            if(!networkId.equals(uc.getNetworkId()))
            {
                ret = false;
            }
        }

        return ret;
    }
    
    @Override
    public void expected(Object o)
    {
        assertTrue("o is an instance of " + o.getClass(), o instanceof Person);

        Person other = (Person)o;
        
        AssertUtil.assertEquals("userId", getId(), other.getId());
        AssertUtil.assertEquals("firstName", firstName, other.getFirstName());
        AssertUtil.assertEquals("lastName", lastName, other.getLastName());
        AssertUtil.assertEquals("enabled", enabled, other.isEnabled());

        if(isVisible())
        {
            AssertUtil.assertEquals("skype", getSkypeId(), other.getSkypeId());
            AssertUtil.assertEquals("location", getLocation(), other.getLocation());
            AssertUtil.assertEquals("tel", getTelephone(), other.getTelephone());
            AssertUtil.assertEquals("mobile", getMobile(), other.getMobile());
            AssertUtil.assertEquals("instanceMessageId", getInstantMessageId(), other.getInstantMessageId());
            AssertUtil.assertEquals("googleId", getGoogleId(), other.getGoogleId());
            if(company != null)
            {
                new Company(company).expected(getCompany());
            }
        }
    }

    @Override
    public int compareTo(Person o)
    {
        int ret = Person.collator.compare(lastName, o.getLastName());
        if(ret == 0)
        {
            ret = Person.collator.compare(firstName, o.getFirstName());
        }
        return ret;
    }

    public static ListResponse<Person> parsePeople(JsonNode jsonObject) throws IOException
    {
        List<Person> people = new ArrayList<Person>();

        JsonNode jsonList = jsonObject.get("list");
        assertNotNull(jsonList);

        ArrayNode jsonEntries = (ArrayNode) jsonList.get("entries");
        assertNotNull(jsonEntries);

        for(int i = 0; i < jsonEntries.size(); i++)
        {
            JsonNode jsonEntry = jsonEntries.get(i);
            JsonNode entry = jsonEntry.get("entry");
            people.add(parsePerson(entry));
        }

        ExpectedPaging paging = ExpectedPaging.parsePagination(jsonList);
        ListResponse<Person> resp = new ListResponse<Person>(paging, people);
        return resp;
    }
    
}
