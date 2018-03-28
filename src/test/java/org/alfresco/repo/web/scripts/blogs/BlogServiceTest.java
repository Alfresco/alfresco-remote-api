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
package org.alfresco.repo.web.scripts.blogs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.activities.feed.FeedGenerator;
import org.alfresco.repo.activities.post.lookup.PostLookup;
import org.alfresco.repo.management.subsystems.ChildApplicationContextFactory;
import org.alfresco.repo.node.archive.NodeArchiveService;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.activities.ActivityService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.alfresco.util.testing.category.LuceneTests;
import org.alfresco.util.testing.category.RedundantTests;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.TestWebScriptServer.DeleteRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PostRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PutRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

/**
 * Unit Test to test Blog Web Script API
 * 
 * @author mruflin
 */
@Category(LuceneTests.class)
public class BlogServiceTest extends BaseWebScriptTest
{
    @SuppressWarnings("unused")
    private static Log logger = LogFactory.getLog(BlogServiceTest.class);

    private MutableAuthenticationService authenticationService;
    private AuthenticationComponent authenticationComponent;
    private PersonService personService;
    private SiteService siteService;
    private NodeArchiveService nodeArchiveService;
    private ActivityService activityService;
    private FeedGenerator feedGenerator;
    private PostLookup postLookup;

    private static final String USER_ONE = "UserOneSecondToo";
    private static final String USER_TWO = "UserTwoSecondToo";
    private static final String SITE_SHORT_NAME_BLOG = "BlogSiteShortNameTest";
    private static final String COMPONENT_BLOG = "blog";

    private static final String URL_BLOG_POST = "/api/blog/post/site/" + SITE_SHORT_NAME_BLOG + "/" + COMPONENT_BLOG + "/";
    private static final String URL_BLOG_CORE = "/api/blog/site/" + SITE_SHORT_NAME_BLOG + "/" + COMPONENT_BLOG;
    private static final String URL_BLOG_POSTS = URL_BLOG_CORE + "/posts";
    private static final String URL_MY_DRAFT_BLOG_POSTS = "/api/blog/site/" + SITE_SHORT_NAME_BLOG +
                                                          "/" + COMPONENT_BLOG + "/posts/mydrafts";
    private static final String URL_MY_PUBLISHED_BLOG_POSTS = "/api/blog/site/" + SITE_SHORT_NAME_BLOG +
                                                          "/" + COMPONENT_BLOG + "/posts/mypublished";

    private static final String URL_DELETE_COMMENT = "api/comment/node/{0}/{1}/{2}?site={3}&itemtitle={4}&page={5}";

    private List<String> posts;
    private List<String> drafts;

    
    // General methods

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        this.authenticationService = (MutableAuthenticationService)getServer().getApplicationContext().getBean("AuthenticationService");
        this.authenticationComponent = (AuthenticationComponent)getServer().getApplicationContext().getBean("authenticationComponent");
        this.personService = (PersonService)getServer().getApplicationContext().getBean("PersonService");
        this.siteService = (SiteService)getServer().getApplicationContext().getBean("SiteService");
        this.nodeArchiveService = (NodeArchiveService)getServer().getApplicationContext().getBean("nodeArchiveService");
        this.activityService = (ActivityService)getServer().getApplicationContext().getBean("activityService");
        ChildApplicationContextFactory activitiesFeed = (ChildApplicationContextFactory)getServer().getApplicationContext().getBean("ActivitiesFeed");
        ApplicationContext activitiesFeedCtx = activitiesFeed.getApplicationContext();
        this.feedGenerator = (FeedGenerator)activitiesFeedCtx.getBean("feedGenerator");
        this.postLookup = (PostLookup)activitiesFeedCtx.getBean("postLookup");

        // Authenticate as user
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        
        // Create test site
        // - only create the site if it doesn't already exist
        SiteInfo siteInfo = this.siteService.getSite(SITE_SHORT_NAME_BLOG);
        if (siteInfo == null)
        {
            this.siteService.createSite("BlogSitePreset", SITE_SHORT_NAME_BLOG, "BlogSiteTitle", "BlogSiteDescription", SiteVisibility.PUBLIC);
        }
        
        // Create users
        createUser(USER_ONE, SiteModel.SITE_COLLABORATOR, SITE_SHORT_NAME_BLOG);
        createUser(USER_TWO, SiteModel.SITE_COLLABORATOR, SITE_SHORT_NAME_BLOG);

        // Blank our lists used to track things the test creates
        posts = new ArrayList<String>(5);
        drafts = new ArrayList<String>(5);
        
        // Do tests as inviter user
        this.authenticationComponent.setCurrentUser(USER_ONE);
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        // admin user required to delete things
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        
        SiteInfo siteInfo = this.siteService.getSite(SITE_SHORT_NAME_BLOG);
        if (siteInfo != null)
        {
            // delete invite site
            siteService.deleteSite(SITE_SHORT_NAME_BLOG);
            nodeArchiveService.purgeArchivedNode(nodeArchiveService.getArchivedNode(siteInfo.getNodeRef()));
        }

        
        // delete the users
        personService.deletePerson(USER_ONE);
        if (this.authenticationService.authenticationExists(USER_ONE))
        {
           this.authenticationService.deleteAuthentication(USER_ONE);
        }
        
        personService.deletePerson(USER_TWO);
        if (this.authenticationService.authenticationExists(USER_TWO))
        {
           this.authenticationService.deleteAuthentication(USER_TWO);
        }
    }
    
    private void createUser(String userName, String role, String siteMembership)
    {
        // if user with given user name doesn't already exist then create user
        if (this.authenticationService.authenticationExists(userName) == false)
        {
            // create user
            this.authenticationService.createAuthentication(userName, "password".toCharArray());
            
            // create person properties
            PropertyMap personProps = new PropertyMap();
            personProps.put(ContentModel.PROP_USERNAME, userName);
            personProps.put(ContentModel.PROP_FIRSTNAME, "FirstName123");
            personProps.put(ContentModel.PROP_LASTNAME, "LastName123");
            personProps.put(ContentModel.PROP_EMAIL, "FirstName123.LastName123@email.com");
            personProps.put(ContentModel.PROP_JOBTITLE, "JobTitle123");
            personProps.put(ContentModel.PROP_JOBTITLE, "Organisation123");
            
            // create person node for user
            this.personService.createPerson(personProps);
        }
        
        // add the user as a member with the given role
        this.siteService.setMembership(siteMembership, userName, role);
    }
    
    
    // Test helper methods 
    
    private ObjectNode getRequestObject(String title, String content, String[] tags, boolean isDraft)
    {
        ObjectNode post = AlfrescoDefaultObjectMapper.createObjectNode();
        if (title != null)
        {
            post.put("title", title);
        }
        if (content != null)
        {
            post.put("content", content);
        }
        if (tags != null)
        {
            ArrayNode arr = AlfrescoDefaultObjectMapper.createArrayNode();
            for (String s : tags)
            {
                arr.add(s);
            }
            post.put("tags", arr);
        }
        post.put("draft", isDraft);
        return post;
    }
    
    private JsonNode createPost(String title, String content, String[] tags, boolean isDraft, int expectedStatus)
    throws Exception
    {
        JsonNode post = getRequestObject(title, content, tags, isDraft);
        Response response = sendRequest(new PostRequest(URL_BLOG_POSTS, post.toString(), "application/json"), expectedStatus);

        if (expectedStatus != 200)
        {
            return null;
        }
        
        //logger.debug(response.getContentAsString());
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        JsonNode item = result.get("item");
        if (isDraft)
        {
            this.drafts.add(item.get("name").textValue());
        }
        else
        {
            this.posts.add(item.get("name").textValue());
        }
        return item;
    }
    
    private JsonNode updatePost(String name, String title, String content, String[] tags, boolean isDraft, int expectedStatus)
    throws Exception
    {
        JsonNode post = getRequestObject(title, content, tags, isDraft);
        Response response = sendRequest(new PutRequest(URL_BLOG_POST + name, post.toString(), "application/json"), expectedStatus);
        
        if (expectedStatus != 200)
        {
            return null;
        }

        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        return result.get("item");
    }
    
    private JsonNode getPost(String name, int expectedStatus)
    throws Exception
    {
        Response response = sendRequest(new GetRequest(URL_BLOG_POST + name), expectedStatus);
        if (expectedStatus == 200)
        {
            JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            return result.get("item");
        }
        else
        {
            return null;
        }
    }
    
    private String getCommentsUrl(String nodeRef)
    {
        return "/api/node/" + nodeRef.replace("://", "/") + "/comments";
    }
    
    private String getCommentUrl(String nodeRef)
    {
        return "/api/comment/node/" + nodeRef.replace("://", "/");
    }

    private String getDeleteCommentUrl(NodeRef commentNodeRef)
    {
        String itemTitle = "Test Title";
        String page = "document-details";

        String URL = MessageFormat.format(URL_DELETE_COMMENT, new Object[] { commentNodeRef.getStoreRef().getProtocol(),
                commentNodeRef.getStoreRef().getIdentifier(), commentNodeRef.getId(), SITE_SHORT_NAME_BLOG, itemTitle, page});
        return URL;
    }

    private JsonNode createComment(String nodeRef, String title, String content, int expectedStatus)
    throws Exception
    {
        ObjectNode comment = AlfrescoDefaultObjectMapper.createObjectNode();
        comment.put("title", title);
        comment.put("content", content);
        comment.put("site", SITE_SHORT_NAME_BLOG);
        Response response = sendRequest(new PostRequest(getCommentsUrl(nodeRef), comment.toString(), "application/json"), expectedStatus);

        if (expectedStatus != 200)
        {
            return null;
        }

        //logger.debug("Comment created: " + response.getContentAsString());
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        return result.get("item");
    }
    
    private JsonNode updateComment(String nodeRef, String title, String content, int expectedStatus)
    throws Exception
    {
        ObjectNode comment = AlfrescoDefaultObjectMapper.createObjectNode();
        comment.put("title", title);
        comment.put("content", content);
        Response response = sendRequest(new PutRequest(getCommentUrl(nodeRef), comment.toString(), "application/json"), expectedStatus);

        if (expectedStatus != 200)
        {
            return null;
        }

        //logger.debug("Comment updated: " + response.getContentAsString());
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        return result.get("item");
    }
    
    
    // Tests
    
    public void testCreateDraftPost() throws Exception
    {
        String title = "test";
        String content = "test";
        JsonNode item = createPost(title, content, null, true, 200);
        
        // check that the values
        assertEquals(title, item.get("title"));
        assertEquals(content, item.get("content"));
        assertEquals(true, item.get("isDraft"));
        
        // check that other user doesn't have access to the draft
        this.authenticationComponent.setCurrentUser(USER_TWO);
        getPost(item.get("name").textValue(), 404);
        this.authenticationComponent.setCurrentUser(USER_ONE);
        
        // Now we'll GET my-drafts to ensure that the post is there.
        Response response = sendRequest(new GetRequest(URL_MY_DRAFT_BLOG_POSTS), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertTrue("Wrong number of posts", result.size() > 0);
    }
    
    /**
     * @since 4.0
     */
    @Test
    @Category({LuceneTests.class, RedundantTests.class})
    public void testCreateDraftPostWithTagsAndComment() throws Exception
    {
        String[] tags = new String[]{"foo", "bar"};
        String title = "test";
        String content = "test";
        JsonNode item = createPost(title, content, tags, true, 200);
        
        // check that the values
        assertEquals(title, item.get("title"));
        assertEquals(content, item.get("content"));
        assertEquals(true, item.get("isDraft"));
        ArrayNode reportedTags = (ArrayNode)item.get("tags");
        assertEquals("Tags size was wrong.", 2, reportedTags.size());
        List<String> recoveredTagsList = Arrays.asList(new String[]{reportedTags.get(0).textValue(), reportedTags.get(1).textValue()});
        assertEquals("Tags were wrong.", Arrays.asList(tags), recoveredTagsList);
        
        // comment on the blog post.
        NodeRef blogPostNode = new NodeRef(item.get("nodeRef").textValue());
        // Currently (mid-Swift dev) there is no Java CommentService, so we have to post a comment via the REST API.
        String commentsPostUrl = "/api/node/" + blogPostNode.getStoreRef().getProtocol() +
                                 "/" + blogPostNode.getStoreRef().getIdentifier() + "/" +
                                 blogPostNode.getId() + "/comments";
        
        ObjectNode jsonToPost = AlfrescoDefaultObjectMapper.createObjectNode();
        jsonToPost.put("title", "Commented blog title");
        jsonToPost.put("content", "Some content.");

        Response response = sendRequest(new PostRequest(commentsPostUrl, AlfrescoDefaultObjectMapper.writeValueAsString(jsonToPost), "application/json"), 200);
        
        // check that other user doesn't have access to the draft
        this.authenticationComponent.setCurrentUser(USER_TWO);
        getPost(item.get("name").textValue(), 404);
        this.authenticationComponent.setCurrentUser(USER_ONE);
        
        // Now we'll GET my-drafts to ensure that the post is there.
        response = sendRequest(new GetRequest(URL_MY_DRAFT_BLOG_POSTS), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        // Ensure it reports the tag correctly on GET.
        ArrayNode items = (ArrayNode) result.get("items");
        ArrayNode tagsArray = (ArrayNode) items.get(0).get("tags");
        assertEquals("Wrong number of tags", 2, tagsArray.size());
        assertEquals("Tag wrong", tags[0], tagsArray.get(0).textValue());
        assertEquals("Tag wrong", tags[1], tagsArray.get(1).textValue());
        
        // Ensure the comment count is accurate
        assertEquals("Wrong comment count", 1, items.get(0).get("commentCount").intValue());
        
        // and that there is content at the commentsURL.
        String commentsUrl = "/api" + items.get(0).get("commentsUrl");
        response = sendRequest(new GetRequest(commentsUrl), 200);
        
        
        // Now get blog-post by tag.
        // 1. No such tag
        response = sendRequest(new GetRequest(URL_BLOG_POSTS + "?tag=NOSUCHTAG"), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        assertEquals(0, result.get("total").intValue());
        
        // tag created above
        response = sendRequest(new GetRequest(URL_BLOG_POSTS + "?tag=foo"), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        assertEquals(1, result.get("total").intValue());
        
        //TODO More assertions on recovered node.
    }
    
    public void testCreatePublishedPost() throws Exception
    {
        String title = "published";
        String content = "content";
        
        JsonNode item = createPost(title, content, null, false, 200);
        final String postName = item.get("name").textValue();
        
        // check the values
        assertEquals(title, item.get("title"));
        assertEquals(content, item.get("content"));
        assertEquals(false, item.get("isDraft"));
        
        // check that user two has access to it as well
        this.authenticationComponent.setCurrentUser(USER_TWO);
        getPost(item.get("name").textValue(), 200);
        this.authenticationComponent.setCurrentUser(USER_ONE);

        // Now we'll GET my-published to ensure that the post is there.
        Response response = sendRequest(new GetRequest(URL_MY_PUBLISHED_BLOG_POSTS), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        // we should have posts.size + drafts.size together
        assertEquals(this.posts.size() + this.drafts.size(), result.get("total"));
        
        // Finally, we'll delete the blog-post to test the REST DELETE call.
        response = sendRequest(new DeleteRequest(URL_BLOG_POST + postName), 200);

    }
    
    public void testCreateEmptyPost() throws Exception
    {
        JsonNode item = createPost(null, null, null, false, 200);
        
        // check the values
        assertEquals("", item.get("title"));
        assertEquals("", item.get("content"));
        assertEquals(false, item.get("isDraft"));
        
        // check that user two has access to it as well
        this.authenticationComponent.setCurrentUser(USER_TWO);
        getPost(item.get("name").textValue(), 200);
        this.authenticationComponent.setCurrentUser(USER_ONE);
    }
    
    public void testUpdated() throws Exception
    {
        JsonNode item = createPost("test", "test", null, false, 200);
        String name = item.get("name").textValue();
        assertEquals(false, item.get("isUpdated").booleanValue());
        
        item = updatePost(name, "new title", "new content", null, false, 200);
        assertEquals(true, item.get("isUpdated").booleanValue());
        assertEquals("new title", item.get("title").textValue());
        assertEquals("new content", item.get("content").textValue());
    }
    
    public void testUpdateWithEmptyValues() throws Exception
    {
        JsonNode item = createPost("test", "test", null, false, 200);
        String name = item.get("name").textValue();
        assertEquals(false, item.get("isUpdated").booleanValue());
        
        item = updatePost(item.get("name").textValue(), null, null, null, false, 200);
        assertEquals("", item.get("title").textValue());
        assertEquals("", item.get("content").textValue());
    }
    
    public void testPublishThroughUpdate() throws Exception
    {
        JsonNode item = createPost("test", "test", null, true, 200);
        String name = item.get("name").textValue();
        assertEquals(true, item.get("isDraft"));
        
        // check that user two does not have access
        this.authenticationComponent.setCurrentUser(USER_TWO);
        getPost(name, 404);
        this.authenticationComponent.setCurrentUser(USER_ONE);
        
        item = updatePost(name, "new title", "new content", null, false, 200);
        assertEquals("new title", item.get("title").textValue());
        assertEquals("new content", item.get("content").textValue());
        assertEquals(false, item.get("isDraft").booleanValue());
        
        // check that user two does have access
        this.authenticationComponent.setCurrentUser(USER_TWO);
        getPost(name, 200);
        this.authenticationComponent.setCurrentUser(USER_ONE);
    }

    public void testCannotDoUnpublish() throws Exception
    {
        JsonNode item = createPost("test", "test", null, false, 200);
        String name = item.get("name").textValue();
        assertEquals(false, item.get("isDraft").booleanValue());
        
        item = updatePost(name, "new title", "new content", null, true, 400); // should return bad request
    }
    
    public void testGetAll() throws Exception
    {
        String url = URL_BLOG_POSTS;
        Response response = sendRequest(new GetRequest(url), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        JsonNode blog;
        
        // We shouldn't have any posts at this point
        assertEquals(0, this.posts.size());
        assertEquals(0, this.drafts.size());
        
        assertEquals(0, result.get("total").intValue());
        assertEquals(0, result.get("startIndex").intValue());
        assertEquals(0, result.get("itemCount").intValue());
        assertEquals(0, result.get("items").size());
        
        // Check that the permissions are correct
        JsonNode metadata = result.get("metadata");
        JsonNode perms = metadata.get("blogPermissions");
        assertEquals(false, metadata.get("externalBlogConfig").booleanValue());
        assertEquals(false, perms.get("delete").booleanValue()); // No container yet
        assertEquals(true, perms.get("edit").booleanValue());
        assertEquals(true, perms.get("create").booleanValue());
        

        // Create a draft and a full post
        String TITLE_1 = "Published";
        String TITLE_2 = "Draft";
        String TITLE_3 = "Another Published";
        createPost(TITLE_1, "Stuff", null, false, Status.STATUS_OK);
        createPost(TITLE_2, "Draft Stuff", null, true, Status.STATUS_OK);
        
        // Check now
        response = sendRequest(new GetRequest(url), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(2, result.get("total").intValue());
        assertEquals(0, result.get("startIndex").intValue());
        assertEquals(2, result.get("itemCount").intValue());
        assertEquals(2, result.get("items").size());

        // Check the core permissions
        metadata = result.get("metadata");
        perms = metadata.get("blogPermissions");
        assertEquals(false, metadata.get("externalBlogConfig").booleanValue());
        assertEquals(true, perms.get("delete").booleanValue()); // On the container itself
        assertEquals(true, perms.get("edit").booleanValue());
        assertEquals(true, perms.get("create").booleanValue());
        
        // Check each one in detail, they'll come back Published
        //  then draft (newest first within that)
        blog = result.get("items").get(0);
        assertEquals(TITLE_1, blog.get("title").textValue());
        assertEquals(false, blog.get("isDraft").booleanValue());
        perms = blog.get("permissions");
        assertEquals(true, perms.get("delete").booleanValue());
        assertEquals(true, perms.get("edit").booleanValue());
        
        blog = result.get("items").get(1);
        assertEquals(TITLE_2, blog.get("title").textValue());
        assertEquals(true, blog.get("isDraft").booleanValue());
        perms = blog.get("permissions");
        assertEquals(true, perms.get("delete").booleanValue());
        assertEquals(true, perms.get("edit").booleanValue());
        
        
        // Add a third post
        createPost(TITLE_3, "Still Stuff", null, false, Status.STATUS_OK);
        
        response = sendRequest(new GetRequest(url), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(3, result.get("total").intValue());
        assertEquals(0, result.get("startIndex").intValue());
        assertEquals(3, result.get("itemCount").intValue());
        assertEquals(3, result.get("items").size());

        // Published then draft, newest first
        blog = result.get("items").get(0);
        assertEquals(TITLE_3, blog.get("title").textValue());
        blog = result.get("items").get(1);
        assertEquals(TITLE_1, blog.get("title").textValue());
        blog = result.get("items").get(2);
        assertEquals(TITLE_2, blog.get("title").textValue());

        
        // Ensure that paging behaves properly
        response = sendRequest(new GetRequest(url + "?pageSize=2&startIndex=0"), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(3, result.get("total").intValue());
        assertEquals(0, result.get("startIndex").intValue());
        assertEquals(2, result.get("itemCount").intValue());
        assertEquals(2, result.get("items").size());

        assertEquals(TITLE_3, result.get("items").get(0).get("title").textValue());
        assertEquals(TITLE_1, result.get("items").get(1).get("title").textValue());
        
        
        response = sendRequest(new GetRequest(url + "?pageSize=2&startIndex=1"), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(3, result.get("total").intValue());
        assertEquals(1, result.get("startIndex").intValue());
        assertEquals(2, result.get("itemCount").intValue());
        assertEquals(2, result.get("items").size());

        assertEquals(TITLE_1, result.get("items").get(0).get("title").textValue());
        assertEquals(TITLE_2, result.get("items").get(1).get("title").textValue());
        
        
        response = sendRequest(new GetRequest(url + "?pageSize=2&startIndex=2"), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(3, result.get("total").intValue());
        assertEquals(2, result.get("startIndex").intValue());
        assertEquals(1, result.get("itemCount").intValue());
        assertEquals(1, result.get("items").size());

        assertEquals(TITLE_2, result.get("items").get(0).get("title"));

        
        // Switch user, check that permissions are correct
        // (Drafts won't be seen)
        this.authenticationComponent.setCurrentUser(USER_TWO);
        
        response = sendRequest(new GetRequest(url), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(2, result.get("total").intValue());
        assertEquals(0, result.get("startIndex").intValue());
        assertEquals(2, result.get("itemCount").intValue());
        
        assertEquals(2, result.get("items").size());
        blog = result.get("items").get(0);
        assertEquals(TITLE_3, blog.get("title").textValue());
        assertEquals(false, blog.get("isDraft").booleanValue());
        perms = blog.get("permissions");
        assertEquals(false, perms.get("delete").booleanValue());
        assertEquals(true, perms.get("edit").booleanValue());
        
        blog = result.get("items").get(1);
        assertEquals(TITLE_1, blog.get("title").textValue());
        assertEquals(false, blog.get("isDraft").booleanValue());
        perms = blog.get("permissions");
        assertEquals(false, perms.get("delete").booleanValue());
        assertEquals(true, perms.get("edit").booleanValue());
    }
    
    public void testGetNew() throws Exception
    {
        String url = URL_BLOG_POSTS + "/new";
        Response response = sendRequest(new GetRequest(url), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        // we should have posts.size
        assertEquals(this.posts.size(), result.get("total").intValue());
    }
    
    public void testGetDrafts() throws Exception
    {
        String url = URL_BLOG_POSTS + "/mydrafts";
        Response response = sendRequest(new GetRequest(URL_BLOG_POSTS), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        // we should have drafts.size resultss
        assertEquals(this.drafts.size(), result.get("total").intValue());
        
        // the second user should have zero
        this.authenticationComponent.setCurrentUser(USER_TWO);
        response = sendRequest(new GetRequest(url), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(0, result.get("total").intValue());
        this.authenticationComponent.setCurrentUser(USER_ONE);

    }
    
    public void testMyPublished() throws Exception
    {
        String url = URL_BLOG_POSTS + "/mypublished";
        Response response = sendRequest(new GetRequest(url), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        // we should have posts.size results
        assertEquals(this.drafts.size(), result.get("total").intValue());
        
        // the second user should have zero
        this.authenticationComponent.setCurrentUser(USER_TWO);
        response = sendRequest(new GetRequest(url), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(0, result.get("total").intValue());
        this.authenticationComponent.setCurrentUser(USER_ONE);
    }

    public void testComments() throws Exception
    {
        JsonNode item = createPost("test", "test", null, false, 200);
        String name = item.get("name").textValue();
        String nodeRef = item.get("nodeRef").textValue();
        
        JsonNode commentOne = createComment(nodeRef, "comment", "content", 200);
        JsonNode commentTwo = createComment(nodeRef, "comment", "content", 200);
        
        // fetch the comments
        Response response = sendRequest(new GetRequest(getCommentsUrl(nodeRef)), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(2, result.get("total").intValue());
        
        // add another one
        JsonNode commentThree = createComment(nodeRef, "comment", "content", 200);
        
        response = sendRequest(new GetRequest(getCommentsUrl(nodeRef)), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(3, result.get("total").intValue());
        
        // delete the last comment
        response = sendRequest(new DeleteRequest(getCommentUrl(commentThree.get("nodeRef").textValue())), 200);
        
        response = sendRequest(new GetRequest(getCommentsUrl(nodeRef)), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(2, result.get("total").intValue());
        
        JsonNode commentTwoUpdated = updateComment(commentTwo.get("nodeRef").textValue(), "new title", "new content", 200);
        assertEquals("new title", commentTwoUpdated.get("title").textValue());
        assertEquals("new content", commentTwoUpdated.get("content").textValue());
    }

    /**
     * REPO-828 (MNT-16401)
     * @throws Exception
     */
    public void testDeleteCommentPostActivity() throws Exception
    {
        this.authenticationComponent.setCurrentUser(USER_ONE);
        JsonNode item = createPost("testActivity", "test", null, false, 200);
        assertNotNull(item);
        postLookup.execute();
        feedGenerator.execute();
        int activityNumStart = activityService.getSiteFeedEntries(SITE_SHORT_NAME_BLOG).size();
        String nodeRef = item.get("nodeRef").textValue();
        JsonNode commentOne = createComment(nodeRef, "comment", "content", 200);
        assertNotNull(item);
        postLookup.execute();
        feedGenerator.execute();
        int activityNumNext = activityService.getSiteFeedEntries(SITE_SHORT_NAME_BLOG).size();
        assertEquals("The activity feeds were not generated after adding a comment", activityNumStart + 1, activityNumNext);
        activityNumStart = activityNumNext;
        NodeRef commentNodeRef = new NodeRef(commentOne.get("nodeRef").textValue());
        Response resp = sendRequest(new DeleteRequest(getDeleteCommentUrl(commentNodeRef)), 200);
        assertTrue(resp.getStatus() == 200);
        postLookup.execute();
        feedGenerator.execute();
        activityNumNext = activityService.getSiteFeedEntries(SITE_SHORT_NAME_BLOG).size();
        assertEquals("The activity feeds were not generated after deleting a comment", activityNumStart + 1, activityNumNext);
    }

    /**
     * You can attach information to the blog container relating
     *  to integration with external blogs.
     * This tests that feature
     */
    public void testBlogIntegration() throws Exception
    {
       // Try to fetch the details on a new site
       Response response = sendRequest(new GetRequest(URL_BLOG_CORE), 200);
       String json = response.getContentAsString();
       JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(json);
       
       assertEquals("No item in:\n"+json, true, result.has("item"));
       JsonNode item = result.get("item");
       
       assertEquals("Missing key in: " + item, true, item.has("qnamePath"));
       assertEquals("Missing key in: " + item, true, item.has("detailsUrl"));
       assertEquals("Missing key in: " + item, true, item.has("blogPostsUrl"));
       
       // Blog properties are empty to start
       assertEquals("", item.get("type").textValue());
       assertEquals("", item.get("name").textValue());
       assertEquals("", item.get("description").textValue());
       assertEquals("", item.get("url").textValue());
       assertEquals("", item.get("username").textValue());
       assertEquals("", item.get("password").textValue());
       
       
       // Have it updated
       ObjectNode blog = AlfrescoDefaultObjectMapper.createObjectNode();
       blog.put("blogType", "wordpress");
       blog.put("blogName", "A Blog!");
       blog.put("username", "guest");
       sendRequest(new PutRequest(URL_BLOG_CORE, blog.toString(), "application/json"), Status.STATUS_OK);
       
       // Check again now
       response = sendRequest(new GetRequest(URL_BLOG_CORE), 200);
       json = response.getContentAsString();
       result = AlfrescoDefaultObjectMapper.getReader().readTree(json);
       
       assertEquals("No item in:\n"+json, true, result.has("item"));
       item = result.get("item");
       
       assertEquals("Missing key in: " + item, true, item.has("qnamePath"));
       assertEquals("Missing key in: " + item, true, item.has("detailsUrl"));
       assertEquals("Missing key in: " + item, true, item.has("blogPostsUrl"));
       
       // Blog properties should now be set
       assertEquals("wordpress", item.get("type").textValue());
       assertEquals("A Blog!", item.get("name").textValue());
       assertEquals("", item.get("description").textValue());
       assertEquals("", item.get("url").textValue());
       assertEquals("guest", item.get("username").textValue());
       assertEquals("", item.get("password").textValue());
    }
 
    /**
     * Does some stress tests.
     * 
     * Currently observed errors:
     * 1. [repo.action.AsynchronousActionExecutionQueueImpl] Failed to execute asynchronous action: Action[ id=485211db-f117-4976-9530-ab861a19f563, node=null ]
     * org.alfresco.repo.security.permissions.AccessDeniedException: Access Denied.  You do not have the appropriate permissions to perform this operation. 
     * 
     * 2. JSONException, but with root cause being
     *   get(assocs) failed on instance of org.alfresco.repo.template.TemplateNode
     *   The problematic instruction:
     *   ----------
     *   ==> if person.assocs["cm:avatar"]?? [on line 4, column 7 in org/alfresco/repository/blogs/blogpost.lib.ftl]
     *   
     * @throws Exception
     */
    public void _testTagsStressTest() throws Exception
    {
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
        List<Thread> threads = new ArrayList<Thread>();

        System.err.println("Creating and starting threads...");
        for (int x=0; x < 3; x++)
        {
            Thread t = new Thread(new Runnable() 
            {
                public void run() 
                {
                    // set the correct user
                    authenticationComponent.setCurrentUser(USER_ONE);

                    // now do some requests
                    try 
                    {
                        for (int y=0; y < 3; y++)
                        {
                            off_testPostTags();
                            off_testClearTags();
                        }
                        System.err.println("------------- SUCCEEDED ---------------");
                    } catch (Exception e)
                    {
                        System.err.println("------------- ERROR ---------------");
                        exceptions.add(e);
                        e.printStackTrace();
                        return;
                    }
            }});
            
            threads.add(t);
            t.start();
        } 
        /*for (Thread t : threads)
        {
            t.start();
        }*/
        
        for (Thread t : threads)
        {
            t.join();
        }
        
        System.err.println("------------- STACK TRACES ---------------");
        for (Exception e : exceptions)
        {
            e.printStackTrace();
        }
        System.err.println("------------- STACK TRACES END ---------------");
        if (exceptions.size() > 0)
        {
            throw exceptions.get(0);
        }
    }
    
    public void off_testPostTags() throws Exception
    {
        String[] tags = { "first", "test" };
        JsonNode item = createPost("tagtest", "tagtest", tags, false, 200);
        assertEquals(2, item.get("tags").size());
        assertEquals("first", item.get("tags").get(0));
        assertEquals("test", item.get("tags").get(1));
        
        item = updatePost(item.get("name").textValue(), null, null, new String[] { "First", "Test", "Second" }, false, 200);
        assertEquals(3, item.get("tags").size());
        assertEquals("first", item.get("tags").get(0));
        assertEquals("test", item.get("tags").get(1));
        assertEquals("second", item.get("tags").get(2));
    }
    
    public void off_testClearTags() throws Exception
    {
        String[] tags = { "abc", "def"};
        JsonNode item = createPost("tagtest", "tagtest", tags, false, 200);
        assertEquals(2, item.get("tags").size());
        
        item = updatePost(item.get("name").textValue(), null, null, new String[0], false, 200);
        assertEquals(0, item.get("tags").size());
    }

    /**
     * Test for <a href=https://issues.alfresco.com/jira/browse/MNT-11964>MNT-11964</a>
     * @throws Exception 
     */
    public void testBlogPermission() throws Exception
    {
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        
        String siteName = SITE_SHORT_NAME_BLOG + GUID.generate();
        this.siteService.createSite("BlogSitePreset", siteName, "BlogSiteTitle", "BlogSiteDescription", SiteVisibility.PUBLIC);
        
        String userName = USER_ONE + GUID.generate();
        createUser(userName, SiteModel.SITE_COLLABORATOR, siteName);

        // Check permissions for admin
        checkBlogPermissions(siteName);
        
        // Check permissions for user
        this.authenticationComponent.setCurrentUser(userName);
        checkBlogPermissions(siteName);

        // Cleanup
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        this.siteService.deleteSite(siteName);
        
        // Create a new site as user
        this.authenticationComponent.setCurrentUser(userName);
        siteName = SITE_SHORT_NAME_BLOG + GUID.generate();
        this.siteService.createSite("BlogSitePreset", siteName, "BlogSiteTitle", "BlogSiteDescription", SiteVisibility.PUBLIC);
        
        // Check permissions for user
        checkBlogPermissions(siteName);
        
        // Check permissions for admin
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        checkBlogPermissions(siteName);
        
        // Cleanup
        this.siteService.deleteSite(siteName);
        this.personService.deletePerson(userName);
    }
    
    private void checkBlogPermissions(String siteName) throws Exception
    {
        String url = "/api/blog/site/" + siteName + "/" + COMPONENT_BLOG;
        Response response = sendRequest(new GetRequest(url), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        assertTrue("The user sould have permission to create a new blog.",
                result.get("item").get("permissions").get("create").booleanValue());
    }
}