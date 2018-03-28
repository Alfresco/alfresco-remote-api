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
package org.alfresco.repo.web.scripts.rating;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.util.PropertyMap;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.springframework.extensions.webscripts.TestWebScriptServer.DeleteRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PostRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

/**
 * This class tests the ReST API of the {@link org.alfresco.service.cmr.rating.RatingService}.
 * 
 * @author Neil McErlean
 * @since 3.4
 */
public class RatingRestApiTest extends BaseWebScriptTest
{
    // Miscellaneous constants used throughout this test class.
    private static final String FIVE_STAR_RATING_SCHEME = "fiveStarRatingScheme";
    private static final String LIKES_RATING_SCHEME = "likesRatingScheme";

    private static final String USER_ONE = "UserOne";
    private static final String USER_TWO = "UserTwo";

    private static final String RATING_SCHEMES = "ratingSchemes";
    private static final String NAME = "name";
    private static final String MIN_RATING = "minRating";
    private static final String MAX_RATING = "maxRating";
    private static final String RATINGS = "ratings";
    private static final String NODE_REF = "nodeRef";
    private static final String DATA = "data";
    private static final String RATINGS_TOTAL = "ratingsTotal";
    private static final String RATINGS_COUNT = "ratingsCount";
    private static final String AVERAGE_RATING = "averageRating";
    private static final String NODE_STATISTICS = "nodeStatistics";

    private final static String NODE_RATINGS_URL_FORMAT = "/api/node/{0}/ratings";
    private final static String GET_RATING_DEFS_URL = "/api/rating/schemedefinitions";

    private static final String APPLICATION_JSON = "application/json";
    
    private NodeRef testNode;
    
    private MutableAuthenticationService authenticationService;
    private NodeService nodeService;
    private PersonService personService;
    private Repository repositoryHelper;
    private RetryingTransactionHelper transactionHelper;
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        authenticationService = (MutableAuthenticationService) getServer().getApplicationContext().getBean("AuthenticationService");
        nodeService = (NodeService) getServer().getApplicationContext().getBean("NodeService");
        personService = (PersonService) getServer().getApplicationContext().getBean("PersonService");
        repositoryHelper = (Repository) getServer().getApplicationContext().getBean("repositoryHelper");
        transactionHelper = (RetryingTransactionHelper)getServer().getApplicationContext().getBean("retryingTransactionHelper");  
        
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getSystemUserName());
        
        // Create some users to rate each other's content
        // and a test node which we will rate.
        // It doesn't matter that it has no content.
        testNode = transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>()
                {
                    public NodeRef execute() throws Throwable
                    {
                        createUser(USER_ONE);
                        createUser(USER_TWO);

                        ChildAssociationRef result = nodeService.createNode(repositoryHelper.getCompanyHome(),
                                                                ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS,
                                                                ContentModel.TYPE_CONTENT, null);
                        return result.getChildRef();
                    }          
                });        
    }
    
    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();

        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getSystemUserName());

        transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                        if (testNode != null && nodeService.exists(testNode))
                        {
                            nodeService.deleteNode(testNode);
                            deleteUser(USER_ONE);
                            deleteUser(USER_TWO);
                        }
                        return null;
                    }          
                });        
    }

    public void testGetRatingSchemeDefinitions() throws Exception
    {
        final int expectedStatus = 200;
        Response rsp = sendRequest(new GetRequest(GET_RATING_DEFS_URL), expectedStatus);

        JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(rsp.getContentAsString());
        
        JsonNode dataObj = jsonRsp.get(DATA);
        assertNotNull("JSON 'data' object was null", dataObj);
        
        ArrayNode ratingSchemesArray = (ArrayNode)dataObj.get(RATING_SCHEMES);
        assertNotNull("JSON 'ratingSchemesArray' object was null", ratingSchemesArray);
        assertEquals(2, ratingSchemesArray.size());

        // The array's objects may be in different order
        Map<String, JsonNode> ratingsMap = new HashMap<String, JsonNode>();
        for (int i = 0 ; i < ratingSchemesArray.size(); i++)
        {
            ratingsMap.put(ratingSchemesArray.get(i).get(NAME).textValue(), ratingSchemesArray.get(i));
        }

        JsonNode scheme1 = ratingsMap.get(LIKES_RATING_SCHEME);
        JsonNode scheme2 = ratingsMap.get(FIVE_STAR_RATING_SCHEME);

        assertNotNull("The response did not contain " + LIKES_RATING_SCHEME, scheme1);
        assertEquals(1.0, scheme1.get(MIN_RATING));
        assertEquals(1.0, scheme1.get(MAX_RATING));
        assertTrue(scheme1.get("selfRatingAllowed").booleanValue());
        assertNotNull("The response did not contain " + FIVE_STAR_RATING_SCHEME, scheme2);
        assertEquals(1.0, scheme2.get(MIN_RATING));
        assertEquals(5.0, scheme2.get(MAX_RATING));
        assertFalse(scheme2.get("selfRatingAllowed").booleanValue());
    }
    
    public void testGetRatingsFromUnratedNodeRef() throws Exception
    {
        // GET ratings
        String ratingUrl = getRatingUrl(testNode);

        final int expectedStatus = 200;
        Response rsp = sendRequest(new GetRequest(ratingUrl), expectedStatus);

        JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(rsp.getContentAsString());
        
        JsonNode dataObj = jsonRsp.get(DATA);
        assertNotNull("JSON 'data' object was null", dataObj);
        
        assertEquals(testNode.toString(), dataObj.get(NODE_REF));
        final JsonNode ratingsObject = dataObj.get(RATINGS);
        assertEquals(0, ratingsObject.size());

        // Unrated content
        JsonNode statsObject = dataObj.get(NODE_STATISTICS);
        JsonNode likesStats = statsObject.get(LIKES_RATING_SCHEME);
        assertEquals("Average rating was wrong.", -1.0, likesStats.get(AVERAGE_RATING));
        assertEquals("Ratings count rating was wrong.", 0, likesStats.get(RATINGS_COUNT));
        assertEquals("Ratings total was wrong.", 0.0, likesStats.get(RATINGS_TOTAL));

        JsonNode fiveStarStats = statsObject.get(FIVE_STAR_RATING_SCHEME);
        assertEquals("Average rating was wrong.", -1.0, fiveStarStats.get(AVERAGE_RATING));
        assertEquals("Ratings count rating was wrong.", 0, fiveStarStats.get(RATINGS_COUNT));
        assertEquals("Ratings total was wrong.", 0.0, fiveStarStats.get(RATINGS_TOTAL));
    }

    /**
     * This test method applies ratings from multiple users in a single rating
     * scheme to a single test node. It then retrieves those ratings to ensure they
     * were persisted correctly.
     */
    public void testApplyRatingsAsMultipleUsersAndRetrieve() throws Exception
    {
        // POST a new rating to the testNode - as User One.
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);

        final String testNodeRatingUrl = getRatingUrl(testNode);
        
        final float userOneRatingValue = 4.5f;
        ObjectNode json = AlfrescoDefaultObjectMapper.createObjectNode();
        json.put("rating", userOneRatingValue);
        json.put("ratingScheme", FIVE_STAR_RATING_SCHEME);
        Response postRsp = sendRequest(new PostRequest(testNodeRatingUrl,
                                 json.toString(), APPLICATION_JSON), 200);
        
        String postRspString = postRsp.getContentAsString();
        
        // Get the returned URL and validate
        JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(postRspString);
        
        JsonNode dataObj = jsonRsp.get(DATA);
        assertNotNull("JSON 'data' object was null", dataObj);
        String returnedUrl =  dataObj.get("ratedNodeUrl").textValue();
        assertEquals(testNodeRatingUrl, returnedUrl);
        assertEquals(FIVE_STAR_RATING_SCHEME, dataObj.get("ratingScheme"));
        assertEquals(userOneRatingValue, dataObj.get("rating"));
        assertEquals(userOneRatingValue, dataObj.get("averageRating"));
        assertEquals(userOneRatingValue, dataObj.get("ratingsTotal"));
        assertEquals(1, dataObj.get("ratingsCount"));

        // And a second rating
        json = AlfrescoDefaultObjectMapper.createObjectNode();
        json.put("rating", 1);
        json.put("ratingScheme", LIKES_RATING_SCHEME);
        
        sendRequest(new PostRequest(testNodeRatingUrl, json.toString(), APPLICATION_JSON), 200);

        
        // Now GET the ratings via that returned URL
        Response getRsp = sendRequest(new GetRequest(testNodeRatingUrl), 200);
        String getRspString = getRsp.getContentAsString();

        jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(getRspString);
        
        dataObj = jsonRsp.get(DATA);
        assertNotNull("JSON 'data' object was null", dataObj);
        
        // There should be two ratings in there.
        final JsonNode ratingsObject = dataObj.get(RATINGS);
        assertEquals(2, ratingsObject.size());
        JsonNode recoveredRating = ratingsObject.get(FIVE_STAR_RATING_SCHEME);
        assertEquals(userOneRatingValue, recoveredRating.get("rating"));

        // As well as the average, total ratings.
        JsonNode statsObject = dataObj.get(NODE_STATISTICS);
        JsonNode fiveStarStats = statsObject.get(FIVE_STAR_RATING_SCHEME);
        assertEquals("Average rating was wrong.", userOneRatingValue, fiveStarStats.get(AVERAGE_RATING));
        assertEquals("Ratings count rating was wrong.", 1, fiveStarStats.get(RATINGS_COUNT));
        assertEquals("Ratings total was wrong.", userOneRatingValue, fiveStarStats.get(RATINGS_TOTAL));

        JsonNode likesStats = statsObject.get(LIKES_RATING_SCHEME);
        assertEquals("Average rating was wrong.", 1f, likesStats.get(AVERAGE_RATING));
        assertEquals("Ratings count rating was wrong.", 1, likesStats.get(RATINGS_COUNT));
        assertEquals("Ratings total was wrong.", 1f, likesStats.get(RATINGS_TOTAL));
        

        // Now POST a second new rating to the testNode - as User Two.
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);

        final float userTwoRatingValue = 3.5f;
        json = AlfrescoDefaultObjectMapper.createObjectNode();
        json.put("rating", userTwoRatingValue);
        json.put("ratingScheme", FIVE_STAR_RATING_SCHEME);

        postRsp = sendRequest(new PostRequest(testNodeRatingUrl,
                                 json.toString(), APPLICATION_JSON), 200);
        postRspString = postRsp.getContentAsString();
        
        // Get the returned URL and validate
        jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(postRspString);
        
        dataObj = jsonRsp.get(DATA);
        assertNotNull("JSON 'data' object was null", dataObj);
        returnedUrl =  dataObj.get("ratedNodeUrl").textValue();

        assertEquals((userOneRatingValue + userTwoRatingValue) / 2, dataObj.get("averageRating"));
        assertEquals(userOneRatingValue + userTwoRatingValue,       dataObj.get("ratingsTotal"));
        assertEquals(2, dataObj.get("ratingsCount"));

        // Again GET the ratings via that returned URL
        getRsp = sendRequest(new GetRequest(returnedUrl), 200);
        getRspString = getRsp.getContentAsString();

        jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(getRspString);
        
        dataObj = jsonRsp.get(DATA);
        assertNotNull("JSON 'data' object was null", dataObj);

        // There should still only be the one rating in the results - because we're running
        // as UserTwo and should not see UserOne's rating.
        final JsonNode userTwoRatings = dataObj.get(RATINGS);
        assertEquals(1, userTwoRatings.size());
        JsonNode secondRating = userTwoRatings.get(FIVE_STAR_RATING_SCHEME);
        assertEquals(userTwoRatingValue, secondRating.get("rating"));

        // Now the average should have changed.
        statsObject = dataObj.get(NODE_STATISTICS);
        fiveStarStats = statsObject.get(FIVE_STAR_RATING_SCHEME);
        assertEquals("Average rating was wrong.", (userOneRatingValue + userTwoRatingValue) / 2.0,
                                                  fiveStarStats.get(AVERAGE_RATING));
        assertEquals("Ratings count rating was wrong.", 2, fiveStarStats.get(RATINGS_COUNT));
        assertEquals("Ratings total was wrong.", userOneRatingValue + userTwoRatingValue,
                                                 fiveStarStats.get(RATINGS_TOTAL));
        
        
        // Now DELETE user two's rating.
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        sendRequest(new DeleteRequest(testNodeRatingUrl + "/" + FIVE_STAR_RATING_SCHEME), 200);
        
        // GET the ratings again. Although user_one's rating will still be there,
        // user two can't see it and so we should see zero ratings.
        getRsp = sendRequest(new GetRequest(returnedUrl), 200);
        getRspString = getRsp.getContentAsString();

        jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(getRspString);
        
        dataObj = jsonRsp.get(DATA);
        assertNotNull("JSON 'data' object was null", dataObj);

        final JsonNode remainingRatings = dataObj.get(RATINGS);
        assertEquals(0, remainingRatings.size());

        // Now the average should have changed.
        statsObject = dataObj.get(NODE_STATISTICS);
        fiveStarStats = statsObject.get(FIVE_STAR_RATING_SCHEME);
        assertEquals("Average rating was wrong.", userOneRatingValue,
                                                  fiveStarStats.get(AVERAGE_RATING));
        assertEquals("Ratings count rating was wrong.", 1, fiveStarStats.get(RATINGS_COUNT));
        assertEquals("Ratings total was wrong.", userOneRatingValue,
                                                 fiveStarStats.get(RATINGS_TOTAL));
    }
    
    /**
     * This method gives the 'ratings' URL for the specified NodeRef.
     */
    private String getRatingUrl(NodeRef nodeRef)
    {
        String nodeUrl = nodeRef.toString().replace("://", "/");
        String ratingUrl = MessageFormat.format(NODE_RATINGS_URL_FORMAT, nodeUrl);
        return ratingUrl;
    }

    private void createUser(String userName)
    {
        if (! authenticationService.authenticationExists(userName))
        {
            authenticationService.createAuthentication(userName, "PWD".toCharArray());
        }
        
        if (! personService.personExists(userName))
        {
            PropertyMap ppOne = new PropertyMap(4);
            ppOne.put(ContentModel.PROP_USERNAME, userName);
            ppOne.put(ContentModel.PROP_FIRSTNAME, "firstName");
            ppOne.put(ContentModel.PROP_LASTNAME, "lastName");
            ppOne.put(ContentModel.PROP_EMAIL, "email@email.com");
            ppOne.put(ContentModel.PROP_JOBTITLE, "jobTitle");
            
            personService.createPerson(ppOne);
        }
    }

    private void deleteUser(String userName)
    {
        if (personService.personExists(userName))
        {
            personService.deletePerson(userName);
        }
    }
}
