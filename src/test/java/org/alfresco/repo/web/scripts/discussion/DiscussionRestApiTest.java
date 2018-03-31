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
package org.alfresco.repo.web.scripts.discussion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.node.archive.NodeArchiveService;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.TestWebScriptServer.DeleteRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PostRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PutRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

/**
 * Unit Test to test Discussions Web Script API
 */
public class DiscussionRestApiTest extends BaseWebScriptTest
{
    @SuppressWarnings("unused")
    private static Log logger = LogFactory.getLog(DiscussionRestApiTest.class);
    
    private static final String DELETED_REPLY_POST_MARKER = "[[deleted]]";

    private MutableAuthenticationService authenticationService;
    private AuthenticationComponent authenticationComponent;
    private TransactionService transactionService;
    private BehaviourFilter policyBehaviourFilter;
    private PermissionService permissionService;
    private PersonService personService;
    private SiteService siteService;
    private NodeService nodeService;
    private NodeService internalNodeService;
    private NodeArchiveService nodeArchiveService;
    
    private static final String USER_ONE = "UserOneThird";
    private static final String USER_TWO = "UserTwoThird";
    private static final String SITE_SHORT_NAME_DISCUSSION = "DiscussionSiteShortNameThree";
    private static final String COMPONENT_DISCUSSION = "discussions";

    private static final String URL_FORUM_SITE_POST = "/api/forum/post/site/" + SITE_SHORT_NAME_DISCUSSION + "/" + COMPONENT_DISCUSSION + "/";
    private static final String URL_FORUM_SITE_POSTS = "/api/forum/site/" + SITE_SHORT_NAME_DISCUSSION + "/" + COMPONENT_DISCUSSION + "/posts";
    private static final String URL_FORUM_NODE_POST_BASE = "/api/forum/post/node/"; // Plus node id
    private static final String URL_FORUM_NODE_POSTS_BASE = "/api/forum/node/"; // Plus node id + /posts 
    
    private List<String> posts = new ArrayList<String>(5);
    private NodeRef FORUM_NODE;

    
    // General methods

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        this.authenticationService = (MutableAuthenticationService)getServer().getApplicationContext().getBean("AuthenticationService");
        this.authenticationComponent = (AuthenticationComponent)getServer().getApplicationContext().getBean("authenticationComponent");
        this.policyBehaviourFilter = (BehaviourFilter)getServer().getApplicationContext().getBean("policyBehaviourFilter");
        this.transactionService = (TransactionService)getServer().getApplicationContext().getBean("transactionService");
        this.permissionService = (PermissionService)getServer().getApplicationContext().getBean("PermissionService");
        this.personService = (PersonService)getServer().getApplicationContext().getBean("PersonService");
        this.siteService = (SiteService)getServer().getApplicationContext().getBean("SiteService");
        this.nodeService = (NodeService)getServer().getApplicationContext().getBean("NodeService");
        this.internalNodeService = (NodeService)getServer().getApplicationContext().getBean("nodeService");
        this.nodeArchiveService = (NodeArchiveService)getServer().getApplicationContext().getBean("nodeArchiveService");
        
        // Authenticate as user
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        
        // Create test site
        // - only create the site if it doesn't already exist
        SiteInfo siteInfo = this.siteService.getSite(SITE_SHORT_NAME_DISCUSSION);
        if (siteInfo == null)
        {
            siteInfo = this.siteService.createSite("DiscussionSitePreset", SITE_SHORT_NAME_DISCUSSION, 
                  "DiscussionSiteTitle", "DiscussionSiteDescription", SiteVisibility.PUBLIC);
        }
        final NodeRef siteNodeRef = siteInfo.getNodeRef();
        
        // Create the forum
        final String forumNodeName = "TestForum";
        FORUM_NODE = nodeService.getChildByName(siteInfo.getNodeRef(), ContentModel.ASSOC_CONTAINS, forumNodeName);
        if (FORUM_NODE == null)
        {
           FORUM_NODE = transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<NodeRef>() {
              @Override
              public NodeRef execute() throws Throwable {
                 Map<QName, Serializable> props = new HashMap<QName, Serializable>(5);
                 props.put(ContentModel.PROP_NAME, forumNodeName);
                 props.put(ContentModel.PROP_TITLE, forumNodeName);

                 return nodeService.createNode(
                       siteNodeRef, ContentModel.ASSOC_CONTAINS,
                       QName.createQName(forumNodeName), ForumModel.TYPE_FORUM, props 
                 ).getChildRef();
              }
           });
        }
        
        // Create users
        createUser(USER_ONE, SiteModel.SITE_COLLABORATOR, SITE_SHORT_NAME_DISCUSSION);
        createUser(USER_TWO, SiteModel.SITE_CONTRIBUTOR, SITE_SHORT_NAME_DISCUSSION);
        
        // Do tests as inviter user
        this.authenticationComponent.setCurrentUser(USER_ONE);
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        // admin user required to delete user
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        
        // delete the discussions users
        if(personService.personExists(USER_ONE))
        {
           personService.deletePerson(USER_ONE);
        }
        if (this.authenticationService.authenticationExists(USER_ONE))
        {
           this.authenticationService.deleteAuthentication(USER_ONE);
        }
        
        if(personService.personExists(USER_TWO))
        {
           personService.deletePerson(USER_TWO);
        }
        if (this.authenticationService.authenticationExists(USER_TWO))
        {
           this.authenticationService.deleteAuthentication(USER_TWO);
        }
        
        SiteInfo siteInfo = this.siteService.getSite(SITE_SHORT_NAME_DISCUSSION);
        if (siteInfo != null)
        {
           // delete discussions test site
           RetryingTransactionCallback<Void> deleteCallback = new RetryingTransactionCallback<Void>()
           {
               @Override
               public Void execute() throws Throwable
               {
                   siteService.deleteSite(SITE_SHORT_NAME_DISCUSSION);
                   return null;
               }
            };
            transactionService.getRetryingTransactionHelper().doInTransaction(deleteCallback);
            nodeArchiveService.purgeArchivedNode(nodeArchiveService.getArchivedNode(siteInfo.getNodeRef()));
        }
    }
    
    private void createUser(String userName, String role, String siteName)
    {
        // if user with given user name doesn't already exist then create user
        if (!this.authenticationService.authenticationExists(userName))
        {
            // create user
            this.authenticationService.createAuthentication(userName, "password".toCharArray());
        }
         
        if (!this.personService.personExists(userName))
        {
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
        this.siteService.setMembership(siteName, userName, role);
        
        // Give the test user access to the test node
        // They need to be able to read it, and create children of it
        permissionService.setPermission(FORUM_NODE, userName, PermissionService.READ, true);
        permissionService.setPermission(FORUM_NODE, userName, PermissionService.CREATE_CHILDREN, true);
    }
    
    
    // -----------------------------------------------------
    //     Test helper methods
    // -----------------------------------------------------
    
    /**
     * Creates a new topic+post in the test site
     */
    private JsonNode createSitePost(String title, String content, int expectedStatus)
    throws Exception
    {
       return doCreatePost(URL_FORUM_SITE_POSTS, title, content, expectedStatus);
    }
    
    /**
     * Creates a new topic+post under the given node
     */
    private JsonNode createNodePost(NodeRef nodeRef, String title, String content, 
          int expectedStatus) throws Exception
    {
       return doCreatePost(getPostsUrl(nodeRef), title, content, expectedStatus);
    }
    
    private JsonNode doCreatePost(String url, String title, String content, 
          int expectedStatus) throws Exception
    {
       ObjectNode post = AlfrescoDefaultObjectMapper.createObjectNode();
       post.put("title", title);
       post.put("content", content);
       Response response = sendRequest(new PostRequest(url, post.toString(), "application/json"), expectedStatus);

       if (expectedStatus != Status.STATUS_OK)
       {
          return null;
       }

       JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
       JsonNode item = result.get("item");
       posts.add(item.get("name").textValue());
       return item;
    }

    private JsonNode updatePost(NodeRef nodeRef, String title, String content, 
          int expectedStatus) throws Exception
    {
       return doUpdatePost(getPostUrl(nodeRef), title, content, expectedStatus);
    }
    
    private JsonNode updatePost(String name, String title, String content, 
          int expectedStatus) throws Exception
    {
       return doUpdatePost(URL_FORUM_SITE_POST + name, title, content, expectedStatus);
    }
    
    private JsonNode doUpdatePost(String url, String title, String content, 
          int expectedStatus) throws Exception
    {
       ObjectNode post = AlfrescoDefaultObjectMapper.createObjectNode();
       post.put("title", title);
       post.put("content", content);
       Response response = sendRequest(new PutRequest(url, post.toString(), "application/json"), expectedStatus);

       if (expectedStatus != Status.STATUS_OK)
       {
          return null;
       }

       JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
       return result.get("item");
    }
    
    private JsonNode getPost(String name, int expectedStatus) throws Exception
    {
       return doGetPost(URL_FORUM_SITE_POST + name, expectedStatus);
    }
    
    private JsonNode getPost(NodeRef nodeRef, int expectedStatus) throws Exception
    {
       return doGetPost(getPostUrl(nodeRef), expectedStatus);
    }
    
    private JsonNode doGetPost(String url, int expectedStatus) throws Exception
    {
       Response response = sendRequest(new GetRequest(url), expectedStatus);
       if (expectedStatus == Status.STATUS_OK)
       {
          JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
          return result.get("item");
       }
       else
       {
          return null;
       }
    }
    
    private JsonNode getReplies(String name, int expectedStatus) throws Exception
    {
       return doGetReplies(getRepliesUrl(name), expectedStatus);
    }
    
    private JsonNode getReplies(NodeRef nodeRef, int expectedStatus) throws Exception
    {
       return doGetReplies(getRepliesUrl(nodeRef), expectedStatus);
    }
    
    private JsonNode doGetReplies(String url, int expectedStatus) throws Exception
    {
       Response response = sendRequest(new GetRequest(url), expectedStatus);
       if (expectedStatus == Status.STATUS_OK)
       {
          JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
          return result;
       }
       else
       {
          return null;
       }
    }
    
    private JsonNode getPosts(String type, int expectedStatus) throws Exception
    {
       return doGetPosts(URL_FORUM_SITE_POSTS, type, expectedStatus);
    }
    
    private JsonNode getPosts(NodeRef nodeRef, String type, int expectedStatus) throws Exception
    {
       return doGetPosts(getPostsUrl(nodeRef), type, expectedStatus);
    }
    
    private JsonNode doGetPosts(String baseUrl, String type, int expectedStatus) throws Exception
    {
       String url = null;
       if (type == null)
       {
          url = baseUrl;
       }
       else if (type == "limit")
       {
          url = baseUrl + "?pageSize=1";
       }
       else if (type == "hot")
       {
          url = baseUrl + "/hot";
       }
       else if (type == "mine")
       {
          url = baseUrl + "/myposts";
       }
       else if (type.startsWith("new"))
       {
          url = baseUrl + "/" + type;
       }
       else
       {
          throw new IllegalArgumentException("Invalid search type " + type);
       }
       
       Response response = sendRequest(new GetRequest(url), expectedStatus);
       if (expectedStatus == Status.STATUS_OK)
       {
          JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
          return result;
       }
       else
       {
          return null;
       }
    }
    
    private JsonNode deletePost(String name, int expectedStatus) throws Exception
    {
       return doDeletePost(URL_FORUM_SITE_POST + name, expectedStatus);
    }
    
    private JsonNode deletePost(NodeRef nodeRef, int expectedStatus) throws Exception
    {
       return doDeletePost(getPostUrl(nodeRef), expectedStatus);
    }
    
    private JsonNode doDeletePost(String url, int expectedStatus) throws Exception
    {
       Response response = sendRequest(new DeleteRequest(url), Status.STATUS_OK);
       if (expectedStatus == Status.STATUS_OK)
       {
          return AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
       }
       else
       {
          return null;
       }
    }

    private String getRepliesUrl(NodeRef nodeRef)
    {
       return getPostUrl(nodeRef) + "/replies";
    }
    
    private String getRepliesUrl(String postName)
    {
       return URL_FORUM_SITE_POST + postName + "/replies";
    }
    
    private String getPostUrl(NodeRef nodeRef)
    {
       return URL_FORUM_NODE_POST_BASE + nodeRef.toString().replace("://", "/");
    }
    
    private String getPostsUrl(NodeRef nodeRef)
    {
       return URL_FORUM_NODE_POSTS_BASE + nodeRef.toString().replace("://", "/") + "/posts";
    }
    
    private JsonNode createReply(NodeRef nodeRef, String title, String content, int expectedStatus)
    throws Exception
    {
       ObjectNode reply = AlfrescoDefaultObjectMapper.createObjectNode();
       reply.put("title", title);
       reply.put("content", content);
       Response response = sendRequest(new PostRequest(getRepliesUrl(nodeRef), reply.toString(), "application/json"), expectedStatus);

       if (expectedStatus != 200)
       {
          return null;
       }

       JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
       return result.get("item");
    }
    
    private JsonNode updateComment(NodeRef nodeRef, String title, String content,
                                   int expectedStatus) throws Exception
    {
       ObjectNode comment = AlfrescoDefaultObjectMapper.createObjectNode();
       comment.put("title", title);
       comment.put("content", content);
       Response response = sendRequest(new PutRequest(getPostUrl(nodeRef), comment.toString(), "application/json"), expectedStatus);

       if (expectedStatus != Status.STATUS_OK)
       {
          return null;
       }

       //logger.debug("Comment updated: " + response.getContentAsString());
       JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
       return result.get("item");
    }

    /**
     * Monkeys with the created and published dates on a topic+posts
     */
    private void pushCreatedDateBack(NodeRef node, int daysAgo) throws Exception
    {
       Date created = (Date)nodeService.getProperty(node, ContentModel.PROP_CREATED);
       Date newCreated = new Date(created.getTime() - daysAgo*24*60*60*1000);
       Date published = (Date)nodeService.getProperty(node, ContentModel.PROP_PUBLISHED);
       if(published == null) published = created;
       Date newPublished = new Date(published.getTime() - daysAgo*24*60*60*1000);
       
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();

       this.policyBehaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
       internalNodeService.setProperty(node, ContentModel.PROP_CREATED, newCreated);
       internalNodeService.setProperty(node, ContentModel.PROP_MODIFIED, newCreated);
       internalNodeService.setProperty(node, ContentModel.PROP_PUBLISHED, newPublished);
       this.policyBehaviourFilter.enableBehaviour(ContentModel.ASPECT_AUDITABLE);
       
       txn.commit();
       
       // Now chance something else on the node to have it re-indexed
       nodeService.setProperty(node, ContentModel.PROP_CREATED, newCreated);
       nodeService.setProperty(node, ContentModel.PROP_MODIFIED, newCreated);
       nodeService.setProperty(node, ContentModel.PROP_PUBLISHED, newPublished);
       nodeService.setProperty(node, ContentModel.PROP_DESCRIPTION, "Forced change");
       
       // Finally change any children (eg if updating a topic, do the posts)
       for(ChildAssociationRef ref : nodeService.getChildAssocs(node))
       {
          pushCreatedDateBack(ref.getChildRef(), daysAgo);
       }
    }
    
    // -----------------------------------------------------
    //     Tests
    // -----------------------------------------------------
    
    public void testCreateForumPost() throws Exception
    {
        String title = "test";
        String content = "test";
        JsonNode item = createSitePost(title, content, Status.STATUS_OK);
        
        // Check that the values in the response are correct
        assertEquals(title, item.get("title").textValue());
        assertEquals(content, item.get("content").textValue());
        assertEquals(0, item.get("replyCount").intValue());
        assertEquals("Invalid JSON " + item, true, item.has("createdOn"));
        assertEquals("Invalid JSON " + item, true, item.has("modifiedOn"));
        assertEquals("Invalid JSON " + item, true, item.has("author"));
        assertEquals("Invalid JSON " + item, true, item.has("permissions"));
        assertEquals("Invalid JSON " + item, true, item.has("url"));
        assertEquals("Invalid JSON " + item, true, item.has("repliesUrl"));
        assertEquals("Invalid JSON " + item, true, item.has("nodeRef"));
          
        // Save some details
        String name = item.get("name").textValue();
        NodeRef nodeRef = new NodeRef(item.get("nodeRef").textValue());

      
      // Fetch the post by name and check
      item = getPost(name, Status.STATUS_OK);

      assertEquals(title, item.get("title").textValue());
      assertEquals(content, item.get("content").textValue());
      assertEquals(0, item.get("replyCount").intValue());
      assertEquals("Invalid JSON " + item, true, item.has("createdOn"));
      assertEquals("Invalid JSON " + item, true, item.has("modifiedOn"));
      assertEquals("Invalid JSON " + item, true, item.has("author"));
      assertEquals("Invalid JSON " + item, true, item.has("permissions"));
      assertEquals("Invalid JSON " + item, true, item.has("url"));
      assertEquals("Invalid JSON " + item, true, item.has("repliesUrl"));
      assertEquals("Invalid JSON " + item, true, item.has("nodeRef"));


      // Fetch the post by noderef and check
      item = getPost(nodeRef, Status.STATUS_OK);
      
      assertEquals(title, item.get("title").textValue());
      assertEquals(content, item.get("content").textValue());
      assertEquals(0, item.get("replyCount").intValue());
      assertEquals("Invalid JSON " + item, true, item.has("createdOn"));
      assertEquals("Invalid JSON " + item, true, item.has("modifiedOn"));
      assertEquals("Invalid JSON " + item, true, item.has("author"));
      assertEquals("Invalid JSON " + item, true, item.has("permissions"));
      assertEquals("Invalid JSON " + item, true, item.has("url"));
      assertEquals("Invalid JSON " + item, true, item.has("repliesUrl"));
      assertEquals("Invalid JSON " + item, true, item.has("nodeRef"));

      
      // Create another post, this time by noderef
      title = "By Node Title";
      content = "By Node Content";
      item = createNodePost(FORUM_NODE, title, content, Status.STATUS_OK);
      
      assertEquals(title, item.get("title").textValue());
      assertEquals(content, item.get("content").textValue());
      assertEquals(0, item.get("replyCount").intValue());
      
      // Check it by noderef
      nodeRef = new NodeRef(item.get("nodeRef").textValue());
      item = getPost(nodeRef, Status.STATUS_OK);
      
      assertEquals(title, item.get("title").textValue());
      assertEquals(content, item.get("content").textValue());
      assertEquals(0, item.get("replyCount").intValue());
    }
    
    public void testUpdateForumPost() throws Exception
    {
        String title = "test";
        String content = "test";
        JsonNode item = createSitePost(title, content, 200);

        // check that the values
        assertEquals(title, item.get("title").textValue());
        assertEquals(content, item.get("content").textValue());
        assertEquals(false, item.get("isUpdated").booleanValue());
        
        assertEquals(true, item.has("name"));
        String name = item.get("name").textValue();
        assertEquals(true, item.has("nodeRef"));
        NodeRef nodeRef = new NodeRef(item.get("nodeRef").textValue());

        // fetch the post by name
        item = getPost(item.get("name").textValue(), 200);
        assertEquals(title, item.get("title").textValue());
        assertEquals(content, item.get("content").textValue());
        assertEquals(false, item.get("isUpdated").booleanValue());

        // Fetch the post by noderef
        item = getPost(nodeRef, 200);
        assertEquals(title, item.get("title").textValue());
        assertEquals(content, item.get("content").textValue());
        assertEquals(false, item.get("isUpdated").booleanValue());

      
        // Update it by name
        String title2 = "updated test";
        String content2 = "test updated";
        item = updatePost(name, title2, content2, 200);
      
        // Check the response
        assertEquals(title2, item.get("title").textValue());
        assertEquals(content2, item.get("content").textValue());
        assertEquals(name, item.get("name").textValue());
        assertEquals(nodeRef.toString(), item.get("nodeRef").textValue());
        assertEquals(true, item.get("isUpdated").booleanValue());
      
        // Fetch and check
        item = getPost(nodeRef, 200);
        assertEquals(title2, item.get("title").textValue());
        assertEquals(content2, item.get("content").textValue());
        assertEquals(name, item.get("name").textValue());
        assertEquals(nodeRef.toString(), item.get("nodeRef").textValue());
        assertEquals(true, item.get("isUpdated").booleanValue());

      
        // Update it again, this time by noderef
        String title3 = "updated 3 test";
        String content3 = "test 3 updated";
        item = updatePost(nodeRef, title3, content3, 200);
    
        // Check that the values returned are correct
        assertEquals(title3, item.get("title").textValue());
        assertEquals(content3, item.get("content").textValue());
        assertEquals(name, item.get("name").textValue());
        assertEquals(nodeRef.toString(), item.get("nodeRef").textValue());
        assertEquals(true, item.get("isUpdated").booleanValue());
        
        // Fetch and re-check
        item = getPost(nodeRef, 200);
        assertEquals(title3, item.get("title").textValue());
        assertEquals(content3, item.get("content").textValue());
        assertEquals(name, item.get("name").textValue());
        assertEquals(nodeRef.toString(), item.get("nodeRef").textValue());
        assertEquals(true, item.get("isUpdated").booleanValue());
    }
    
    /**
     * Tests that the permissions details included with topics and
     *  posts are correct
     */
    public void testPermissions() throws Exception
    {
       // Create a post, and check the details on it
       JsonNode item = createSitePost("test", "test", Status.STATUS_OK);
       String name = item.get("name").textValue();
       
       JsonNode perms = item.get("permissions");
       assertEquals(true, perms.get("edit").booleanValue());
       assertEquals(true, perms.get("reply").booleanValue());
       assertEquals(true, perms.get("delete").booleanValue());
       
       // Check on a fetch too
       item = getPost(name, Status.STATUS_OK);
       perms = item.get("permissions");
       assertEquals(true, perms.get("edit").booleanValue());
       assertEquals(true, perms.get("reply").booleanValue());
       assertEquals(true, perms.get("delete").booleanValue());
       
       
       // Switch to another user, see what they see
       this.authenticationComponent.setCurrentUser(USER_TWO);
       
       item = getPost(name, Status.STATUS_OK);
       perms = item.get("permissions");
       assertEquals(false, perms.get("edit").booleanValue());
       assertEquals(true, perms.get("reply").booleanValue());
       assertEquals(false, perms.get("delete").booleanValue());
       
       
       // Remove the user from the site, see the change
       this.siteService.removeMembership(SITE_SHORT_NAME_DISCUSSION, USER_TWO);
       
       item = getPost(name, Status.STATUS_OK);
       perms = item.get("permissions");
       assertEquals(false, perms.get("edit").booleanValue());
       assertEquals(false, perms.get("reply").booleanValue());
       assertEquals(false, perms.get("delete").booleanValue());
       
       
       // Make the site private, will vanish
       SiteInfo siteInfo = siteService.getSite(SITE_SHORT_NAME_DISCUSSION);
       siteInfo.setVisibility(SiteVisibility.PRIVATE);
       this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
       this.siteService.updateSite(siteInfo);
       this.authenticationComponent.setCurrentUser(USER_TWO);
       
       // On a private site we're not a member of, shouldn't be visable at all
       getPost(name, Status.STATUS_NOT_FOUND);
    }
       
    /**
     * ALF-1973 - If the user who added a reply has been deleted, don't break
     */
    public void testViewReplyByDeletedUser() throws Exception
    {
       // Create a post
       JsonNode item = createSitePost("test", "test", Status.STATUS_OK);
       String name = item.get("name").textValue();
       NodeRef topicNodeRef = new NodeRef(item.get("nodeRef").textValue());
       
       // Now create a reply as a different user
       this.authenticationComponent.setCurrentUser(USER_TWO);
       createReply(topicNodeRef, "Reply", "By the other user", Status.STATUS_OK);
       
       // Should see the reply
       item = getReplies(name, Status.STATUS_OK);
       assertEquals(1, item.get("items").size());
       
       // Delete the user, check that the reply still shows
       this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
       personService.deletePerson(USER_TWO);
       this.authenticationComponent.setCurrentUser(USER_ONE);
       
       item = getReplies(name, Status.STATUS_OK);
       assertEquals(1, item.get("items").size());
    }
    
    public void testAddReply() throws Exception
    {
        // Create a root post
        JsonNode item = createSitePost("test", "test", Status.STATUS_OK);
        String topicName = item.get("name").textValue();
        NodeRef topicNodeRef = new NodeRef(item.get("nodeRef").textValue());

        // Add a reply
        JsonNode reply = createReply(topicNodeRef, "test", "test", Status.STATUS_OK);
        NodeRef replyNodeRef = new NodeRef(reply.get("nodeRef").textValue());
        assertEquals("test", reply.get("title").textValue());
        assertEquals("test", reply.get("content").textValue());
        
        // Add a reply to the reply
        JsonNode reply2 = createReply(replyNodeRef, "test2", "test2", 200);
          NodeRef reply2NodeRef = new NodeRef(reply2.get("nodeRef").textValue());
        assertEquals("test2", reply2.get("title").textValue());
        assertEquals("test2", reply2.get("content").textValue());
        
        
        // Check things were correctly setup. These should all be siblings
        //  of each other, with relations between the replies
        assertEquals(ForumModel.TYPE_TOPIC, nodeService.getType(topicNodeRef));
        assertEquals(ForumModel.TYPE_POST, nodeService.getType(replyNodeRef));
        assertEquals(ForumModel.TYPE_POST, nodeService.getType(reply2NodeRef));
        assertEquals(topicNodeRef, nodeService.getPrimaryParent(replyNodeRef).getParentRef());
        assertEquals(topicNodeRef, nodeService.getPrimaryParent(reply2NodeRef).getParentRef());

        // Reply 2 should have an assoc to Reply 1
        assertEquals(0, nodeService.getSourceAssocs(reply2NodeRef, RegexQNamePattern.MATCH_ALL).size());
        assertEquals(1, nodeService.getTargetAssocs(reply2NodeRef, RegexQNamePattern.MATCH_ALL).size());
        assertEquals(replyNodeRef, nodeService.getTargetAssocs(reply2NodeRef, RegexQNamePattern.MATCH_ALL).get(0).getTargetRef());
      
        assertEquals(1, nodeService.getSourceAssocs(replyNodeRef, RegexQNamePattern.MATCH_ALL).size());
        assertEquals(1, nodeService.getTargetAssocs(replyNodeRef, RegexQNamePattern.MATCH_ALL).size());
        assertEquals(reply2NodeRef, nodeService.getSourceAssocs(replyNodeRef, RegexQNamePattern.MATCH_ALL).get(0).getSourceRef());

        
        // Fetch all replies for the post
        JsonNode result = getReplies(topicNodeRef, Status.STATUS_OK);
        // check the number of replies
        assertEquals(1, result.get("items").size());
        
        // Check the replies by name too
        result = getReplies(topicName, Status.STATUS_OK);
        assertEquals(1, result.get("items").size());

        
        // Fetch the top level post again, and check the counts there
        // That post should have one direct reply, and one reply to it's reply
        item = getPost(topicName, Status.STATUS_OK);
        assertEquals(2, item.get("totalReplyCount").intValue());
        assertEquals(1, item.get("replyCount").intValue());
    }

    public void testUpdateReply() throws Exception
    {
       // Create a root post
       JsonNode item = createSitePost("test", "test", Status.STATUS_OK);
       String postName = item.get("name").textValue();
       NodeRef postNodeRef = new NodeRef(item.get("nodeRef").textValue());
       assertEquals("test", item.get("title").textValue());
       assertEquals("test", item.get("content").textValue());
       assertEquals(false, item.get("isUpdated").booleanValue());


       // Add a reply to it
       JsonNode reply = createReply(postNodeRef, "rtest", "rtest", Status.STATUS_OK);
       NodeRef replyNodeRef = new NodeRef(reply.get("nodeRef").textValue());
       assertEquals("rtest", reply.get("title").textValue());
       assertEquals("rtest", reply.get("content").textValue());
       assertEquals(false, reply.get("isUpdated").booleanValue());


       // Now update the reply
       JsonNode reply2 = updatePost(replyNodeRef, "test2", "test2", Status.STATUS_OK);
       assertEquals("test2", reply2.get("title").textValue());
       assertEquals("test2", reply2.get("content").textValue());
       assertEquals(true, reply2.get("isUpdated").booleanValue());

       // Fetch it to check
       reply2 = getPost(replyNodeRef, Status.STATUS_OK);
       assertEquals("test2", reply2.get("title").textValue());
       assertEquals("test2", reply2.get("content").textValue());
       assertEquals(true, reply2.get("isUpdated").booleanValue());


       // Ensure the original post wasn't changed
       item = getPost(postName, Status.STATUS_OK);
       assertEquals("test", item.get("title").textValue());
       assertEquals("test", item.get("content").textValue());
       assertEquals(false, item.get("isUpdated").booleanValue());
    }
    
    public void testDeleteToplevelPost() throws Exception
    {
       // Create two posts
       JsonNode item1 = createSitePost("test1", "test1", Status.STATUS_OK);
       JsonNode item2 = createSitePost("test2", "test2", Status.STATUS_OK);
       String name1 = item1.get("name").textValue();
       NodeRef nodeRef1 = new NodeRef(item1.get("nodeRef").textValue());
       NodeRef nodeRef2 = new NodeRef(item2.get("nodeRef").textValue());

       // The node references returned correspond to the topics
       assertEquals(ForumModel.TYPE_TOPIC, nodeService.getType(nodeRef1));
       assertEquals(ForumModel.TYPE_TOPIC, nodeService.getType(nodeRef2));


       // Delete one post by name
       deletePost(name1, Status.STATUS_OK);

       // Check it went
       getPost(name1, Status.STATUS_NOT_FOUND);


       // Delete the other post by noderef
       deletePost(nodeRef2, Status.STATUS_OK);

       // Check it went
       getPost(nodeRef2, Status.STATUS_NOT_FOUND);


       // Check all the nodes have gone
       assertEquals(false, nodeService.exists(nodeRef1));
       assertEquals(false, nodeService.exists(nodeRef2));
    }
    
    public void testDeleteReplyPost() throws Exception
    {
      // Create a root post
      JsonNode item = createSitePost("test", "test", Status.STATUS_OK);
      String postName = item.get("name").textValue();
      NodeRef postNodeRef = new NodeRef(item.get("nodeRef").textValue());
      
      // It doesn't have any replies yet
      assertEquals(0, item.get("totalReplyCount").intValue());
      assertEquals(0, item.get("replyCount").intValue());
      
      
      // Add a reply
      JsonNode reply = createReply(postNodeRef, "testR", "testR", Status.STATUS_OK);
      NodeRef replyNodeRef = new NodeRef(reply.get("nodeRef").textValue());
      String replyName = reply.get("name").textValue();
      assertEquals("testR", reply.get("title").textValue());
      assertEquals("testR", reply.get("content").textValue());
      
      // Fetch the reply and check
      reply = getPost(replyNodeRef, Status.STATUS_OK);
      assertEquals("testR", reply.get("title").textValue());
      assertEquals("testR", reply.get("content").textValue());
      
      // Note - you can't fetch a reply by name, only by noderef
      // It only works for primary posts as they share the topic name
      getPost(replyName, Status.STATUS_NOT_FOUND);
      
      
      // Check the main post, ensure the replies show up
      item = getPost(postName, Status.STATUS_OK);
      assertEquals(1, item.get("totalReplyCount").intValue());
      assertEquals(1, item.get("replyCount").intValue());

      
      // Delete the reply
      deletePost(replyNodeRef, Status.STATUS_OK);
      
      // These nodes don't really get deleted at the moment
      // Due to threading, we just add special marker text
      // TODO Really we should probably delete posts with no attached replies
      reply = getPost(replyNodeRef, Status.STATUS_OK);
      assertEquals(DELETED_REPLY_POST_MARKER, reply.get("title").textValue());
      assertEquals(DELETED_REPLY_POST_MARKER, reply.get("content").textValue());
      
      
      // Fetch the top level post again, replies stay because they
      //  haven't really been deleted...
      // TODO Really we should probably delete posts with no attached replies
      item = getPost(postName, Status.STATUS_OK);
      assertEquals(1, item.get("totalReplyCount").intValue());
      assertEquals(1, item.get("replyCount").intValue());
    }
    
    /**
     * Test for the various listings:
     *  All, New, Hot (Most Active), Mine 
     */
    public void testListings() throws Exception
    {
      JsonNode result;
      JsonNode item;
      
      
      // Check all of the listings, none should have anything yet
      result = getPosts(null, Status.STATUS_OK);
      assertEquals(0, result.get("total").intValue());
      assertEquals(0, result.get("itemCount").intValue());
      assertEquals(0, result.get("items").size());
      
      result = getPosts("hot", Status.STATUS_OK);
      assertEquals(0, result.get("total").intValue());
      assertEquals(0, result.get("itemCount").intValue());
      assertEquals(0, result.get("items").size());

      result = getPosts("mine", Status.STATUS_OK);
      assertEquals(0, result.get("total").intValue());
      assertEquals(0, result.get("itemCount").intValue());
      assertEquals(0, result.get("items").size());

      result = getPosts("new?numdays=100", Status.STATUS_OK);
      assertEquals(0, result.get("total").intValue());
      assertEquals(0, result.get("itemCount").intValue());
      assertEquals(0, result.get("items").size());

      
      // Check with a noderef too
      result = getPosts(FORUM_NODE, null, Status.STATUS_OK);
      assertEquals(0, result.get("total").intValue());
      assertEquals(0, result.get("itemCount").intValue());
      assertEquals(0, result.get("items").size());
      
      result = getPosts(FORUM_NODE, "hot", Status.STATUS_OK);
      assertEquals(0, result.get("total").intValue());
      assertEquals(0, result.get("itemCount").intValue());
      assertEquals(0, result.get("items").size());

      result = getPosts(FORUM_NODE, "mine", Status.STATUS_OK);
      assertEquals(0, result.get("total").intValue());
      assertEquals(0, result.get("itemCount").intValue());
      assertEquals(0, result.get("items").size());

      result = getPosts(FORUM_NODE, "new?numdays=100", Status.STATUS_OK);
      assertEquals(0, result.get("total").intValue());
      assertEquals(0, result.get("itemCount").intValue());
      assertEquals(0, result.get("items").size());
      
      
      // Now add a few topics with replies
      // Some of these will be created as different users
      item = createSitePost("SiteTitle1", "Content", Status.STATUS_OK);
      NodeRef siteTopic1 = new NodeRef(item.get("nodeRef").textValue());
      this.authenticationComponent.setCurrentUser(USER_TWO);
      item = createSitePost("SiteTitle2", "Content", Status.STATUS_OK);
      NodeRef siteTopic2 = new NodeRef(item.get("nodeRef").textValue());
      
      item = createNodePost(FORUM_NODE, "NodeTitle1", "Content", Status.STATUS_OK);
      NodeRef nodeTopic1 = new NodeRef(item.get("nodeRef").textValue());
      this.authenticationComponent.setCurrentUser(USER_ONE);
      item = createNodePost(FORUM_NODE, "NodeTitle2", "Content", Status.STATUS_OK);
      NodeRef nodeTopic2 = new NodeRef(item.get("nodeRef").textValue());
      item = createNodePost(FORUM_NODE, "NodeTitle3", "Content", Status.STATUS_OK);
      NodeRef nodeTopic3 = new NodeRef(item.get("nodeRef").textValue());
      
      item = createReply(siteTopic1, "Reply1a", "Content", Status.STATUS_OK);
      NodeRef siteReply1A = new NodeRef(item.get("nodeRef").textValue());
      item = createReply(siteTopic1, "Reply1b", "Content", Status.STATUS_OK);
      NodeRef siteReply1B = new NodeRef(item.get("nodeRef").textValue());
      
      this.authenticationComponent.setCurrentUser(USER_TWO);
      item = createReply(siteTopic2, "Reply2a", "Content", Status.STATUS_OK);
      NodeRef siteReply2A = new NodeRef(item.get("nodeRef").textValue());
      item = createReply(siteTopic2, "Reply2b", "Content", Status.STATUS_OK);
      NodeRef siteReply2B = new NodeRef(item.get("nodeRef").textValue());
      item = createReply(siteTopic2, "Reply2c", "Content", Status.STATUS_OK);
      NodeRef siteReply2C = new NodeRef(item.get("nodeRef").textValue());

      item = createReply(siteReply2A, "Reply2aa", "Content", Status.STATUS_OK);
      NodeRef siteReply2AA = new NodeRef(item.get("nodeRef").textValue());
      item = createReply(siteReply2A, "Reply2ab", "Content", Status.STATUS_OK);
      NodeRef siteReply2AB = new NodeRef(item.get("nodeRef").textValue());
      this.authenticationComponent.setCurrentUser(USER_ONE);
      item = createReply(siteReply2AA, "Reply2aaa", "Content", Status.STATUS_OK);
      NodeRef siteReply2AAA = new NodeRef(item.get("nodeRef").textValue());
      
      item = createReply(nodeTopic1, "ReplyN1a", "Content", Status.STATUS_OK);
      NodeRef nodeReply1A = new NodeRef(item.get("nodeRef").textValue());
      item = createReply(nodeReply1A, "ReplyN1aa", "Content", Status.STATUS_OK);
      NodeRef nodeReply1AA = new NodeRef(item.get("nodeRef").textValue());
      item = createReply(nodeReply1AA, "ReplyN1aaa", "Content", Status.STATUS_OK);
      NodeRef nodeReply1AAA = new NodeRef(item.get("nodeRef").textValue());
      
      
      // Check for totals
      // We should get all the topics
      result = getPosts(null, Status.STATUS_OK);
      assertEquals(2, result.get("total").intValue());
      assertEquals(2, result.get("itemCount").intValue());
      assertEquals(2, result.get("items").size());
      assertEquals("SiteTitle1", result.get("items").get(1).get("title").textValue());
      assertEquals("SiteTitle2", result.get("items").get(0).get("title").textValue());
      assertEquals(2, result.get("items").get(1).get("replyCount").intValue());
      assertEquals(3, result.get("items").get(0).get("replyCount").intValue());
      
      result = getPosts(FORUM_NODE, null, Status.STATUS_OK);
      assertEquals(3, result.get("total").intValue());
      assertEquals(3, result.get("itemCount").intValue());
      assertEquals(3, result.get("items").size());
      assertEquals("NodeTitle1", result.get("items").get(2).get("title").textValue());
      assertEquals("NodeTitle2", result.get("items").get(1).get("title").textValue());
      assertEquals("NodeTitle3", result.get("items").get(0).get("title").textValue());
      assertEquals(1, result.get("items").get(2).get("replyCount").intValue());
      assertEquals(0, result.get("items").get(1).get("replyCount").intValue());
      assertEquals(0, result.get("items").get(0).get("replyCount").intValue());
      
      
      // Check for "mine"
      // User 1 has Site 1, and Nodes 2 + 3
      result = getPosts("mine", Status.STATUS_OK);
      assertEquals(1, result.get("total").intValue());
      assertEquals(1, result.get("itemCount").intValue());
      assertEquals(1, result.get("items").size());
      assertEquals("SiteTitle1", result.get("items").get(0).get("title").textValue());
      assertEquals(2, result.get("items").get(0).get("replyCount").intValue());
      
      result = getPosts(FORUM_NODE, "mine", Status.STATUS_OK);
      assertEquals(2, result.get("total").intValue());
      assertEquals(2, result.get("itemCount").intValue());
      assertEquals(2, result.get("items").size());
      assertEquals("NodeTitle2", result.get("items").get(0).get("title").textValue());
      assertEquals("NodeTitle3", result.get("items").get(1).get("title").textValue());
      assertEquals(0, result.get("items").get(0).get("replyCount").intValue());
      assertEquals(0, result.get("items").get(1).get("replyCount").intValue());
      
      
      // Check for recent (new)
      // We should get all the topics, with the newest one first (rather than last as with others)
      result = getPosts("new?numdays=2", Status.STATUS_OK);
      assertEquals(2, result.get("total").intValue());
      assertEquals(2, result.get("itemCount").intValue());
      assertEquals(2, result.get("items").size());
      assertEquals("SiteTitle2", result.get("items").get(0).get("title").textValue());
      assertEquals("SiteTitle1", result.get("items").get(1).get("title").textValue());
      assertEquals(3, result.get("items").get(0).get("replyCount").intValue());
      assertEquals(2, result.get("items").get(1).get("replyCount").intValue());
      
      result = getPosts(FORUM_NODE, "new?numdays=2", Status.STATUS_OK);
      assertEquals(3, result.get("total").intValue());
      assertEquals(3, result.get("itemCount").intValue());
      assertEquals(3, result.get("items").size());
      assertEquals("NodeTitle3", result.get("items").get(0).get("title").textValue());
      assertEquals("NodeTitle2", result.get("items").get(1).get("title").textValue());
      assertEquals("NodeTitle1", result.get("items").get(2).get("title").textValue());
      assertEquals(0, result.get("items").get(0).get("replyCount").intValue());
      assertEquals(0, result.get("items").get(1).get("replyCount").intValue());
      assertEquals(1, result.get("items").get(2).get("replyCount").intValue());
      
      
      // Check for hot
      // Will only show topics with replies. Sorting is by replies, not date
      result = getPosts("hot", Status.STATUS_OK);
      assertEquals(2, result.get("total").intValue());
      assertEquals(2, result.get("itemCount").intValue());
      assertEquals(2, result.get("items").size());
      assertEquals("SiteTitle2", result.get("items").get(0).get("title").textValue());
      assertEquals("SiteTitle1", result.get("items").get(1).get("title").textValue());
      assertEquals(3, result.get("items").get(0).get("replyCount").intValue());
      assertEquals(2, result.get("items").get(1).get("replyCount").intValue());
      
      result = getPosts(FORUM_NODE, "hot", Status.STATUS_OK);
      assertEquals(1, result.get("total").intValue());
      assertEquals(1, result.get("itemCount").intValue());
      assertEquals(1, result.get("items").size());
      assertEquals("NodeTitle1", result.get("items").get(0).get("title").textValue());
      assertEquals(1, result.get("items").get(0).get("replyCount").intValue());
      
      
      // Shift some of the posts into the past
      // (Update the created and published dates)
      pushCreatedDateBack(siteTopic1, 10);
      pushCreatedDateBack(siteReply1B, -2); // Make it newer
      
      pushCreatedDateBack(nodeTopic2, 10);
      pushCreatedDateBack(nodeTopic3, 4);
      pushCreatedDateBack(nodeReply1AAA, -1); // Make it newer
      
      
      // Re-check totals, only ordering changes
      result = getPosts(null, Status.STATUS_OK);
      assertEquals(2, result.get("total").intValue());
      assertEquals(2, result.get("itemCount").intValue());
      assertEquals(2, result.get("items").size());
      assertEquals("SiteTitle1", result.get("items").get(1).get("title").textValue());
      assertEquals("SiteTitle2", result.get("items").get(0).get("title").textValue());
      assertEquals(2, result.get("items").get(1).get("replyCount").intValue());
      assertEquals(3, result.get("items").get(0).get("replyCount").intValue());
      
      result = getPosts(FORUM_NODE, null, Status.STATUS_OK);
      assertEquals(3, result.get("total").intValue());
      assertEquals(3, result.get("itemCount").intValue());
      assertEquals(3, result.get("items").size());
      assertEquals("NodeTitle2", result.get("items").get(2).get("title").textValue());
      assertEquals("NodeTitle3", result.get("items").get(1).get("title").textValue());
      assertEquals("NodeTitle1", result.get("items").get(0).get("title").textValue());
      assertEquals(0, result.get("items").get(2).get("replyCount").intValue());
      assertEquals(0, result.get("items").get(1).get("replyCount").intValue());
      assertEquals(1, result.get("items").get(0).get("replyCount").intValue());
      
      
      // Re-check recent, old ones vanish
      result = getPosts("new?numdays=2", Status.STATUS_OK);
      assertEquals(1, result.get("total").intValue());
      assertEquals(1, result.get("itemCount").intValue());
      assertEquals(1, result.get("items").size());
      assertEquals("SiteTitle2", result.get("items").get(0).get("title").textValue());
      assertEquals(3, result.get("items").get(0).get("replyCount").intValue());
      
      result = getPosts(FORUM_NODE, "new?numdays=6", Status.STATUS_OK);
      assertEquals(2, result.get("total").intValue());
      assertEquals(2, result.get("itemCount").intValue());
      assertEquals(2, result.get("items").size());
      assertEquals("NodeTitle1", result.get("items").get(0).get("title").textValue());
      assertEquals("NodeTitle3", result.get("items").get(1).get("title").textValue());
      assertEquals(1, result.get("items").get(0).get("replyCount").intValue());
      assertEquals(0, result.get("items").get(1).get("replyCount").intValue());
      
      result = getPosts(FORUM_NODE, "new?numdays=2", Status.STATUS_OK);
      assertEquals(1, result.get("total").intValue());
      assertEquals(1, result.get("itemCount").intValue());
      assertEquals(1, result.get("items").size());
      assertEquals("NodeTitle1", result.get("items").get(0).get("title").textValue());
      assertEquals(1, result.get("items").get(0).get("replyCount").intValue());
      
      
      // Re-check "mine", no change except ordering
      result = getPosts("mine", Status.STATUS_OK);
      assertEquals(1, result.get("total").intValue());
      assertEquals(1, result.get("itemCount").intValue());
      assertEquals(1, result.get("items").size());
      assertEquals("SiteTitle1", result.get("items").get(0).get("title").textValue());
      assertEquals(2, result.get("items").get(0).get("replyCount").intValue());
      
      result = getPosts(FORUM_NODE, "mine", Status.STATUS_OK);
      assertEquals(2, result.get("total").intValue());
      assertEquals(2, result.get("itemCount").intValue());
      assertEquals(2, result.get("items").size());
      assertEquals("NodeTitle2", result.get("items").get(0).get("title").textValue());
      assertEquals("NodeTitle3", result.get("items").get(1).get("title").textValue());
      assertEquals(0, result.get("items").get(0).get("replyCount").intValue());
      assertEquals(0, result.get("items").get(1).get("replyCount").intValue());
      
      
      // Re-check hot, some old ones vanish
      result = getPosts("hot", Status.STATUS_OK);
      assertEquals(2, result.get("total").intValue());
      assertEquals(2, result.get("itemCount").intValue());
      assertEquals(2, result.get("items").size());
      assertEquals("SiteTitle2", result.get("items").get(0).get("title").textValue());
      assertEquals("SiteTitle1", result.get("items").get(1).get("title").textValue());
      assertEquals(3, result.get("items").get(0).get("replyCount").intValue());
      assertEquals(2, result.get("items").get(1).get("replyCount").intValue());
      
      result = getPosts(FORUM_NODE, "hot", Status.STATUS_OK);
      assertEquals(1, result.get("total").intValue());
      assertEquals(1, result.get("itemCount").intValue());
      assertEquals(1, result.get("items").size());
      assertEquals("NodeTitle1", result.get("items").get(0).get("title").textValue());
      assertEquals(1, result.get("items").get(0).get("replyCount").intValue());
      
      
      // Check paging
      result = getPosts("limit", Status.STATUS_OK);
      assertEquals(2, result.get("total").intValue());
      assertEquals(1, result.get("itemCount").intValue());
      assertEquals(1, result.get("items").size());
      assertEquals("SiteTitle2", result.get("items").get(0).get("title").textValue());
      assertEquals(3, result.get("items").get(0).get("replyCount").intValue());
      
      result = getPosts(FORUM_NODE, "limit", Status.STATUS_OK);
      assertEquals(3, result.get("total").intValue());
      assertEquals(1, result.get("itemCount").intValue());
      assertEquals(1, result.get("items").size());
      assertEquals("NodeTitle1", result.get("items").get(0).get("title").textValue());
      assertEquals(1, result.get("items").get(0).get("replyCount").intValue());
    }
    
    /**
     * https://issues.alfresco.com/jira/browse/ALF-17443 reports that site contributors are unable
     * to edit replies that they have made.
     */
    public void testContributorCanEditReply() throws Exception
    {
        authenticationComponent.setCurrentUser(USER_ONE);
        JsonNode post = createSitePost("Can contributors edit replies?", "The title says it all", Status.STATUS_OK);
        NodeRef postNodeRef = new NodeRef(post.get("nodeRef").textValue());

        authenticationComponent.setCurrentUser(USER_TWO);
        JsonNode reply = createReply(postNodeRef, "", "Let's see.", Status.STATUS_OK);
        NodeRef replyNodeRef = new NodeRef(reply.get("nodeRef").textValue());
        updateComment(replyNodeRef, "", "Yes I can", Status.STATUS_OK);
        
        authenticationComponent.setCurrentUser(USER_ONE);

        post = getPost(postNodeRef, Status.STATUS_OK);
        assertEquals("Can contributors edit replies?", post.get("title").textValue());
        assertEquals("The title says it all", post.get("content").textValue());
        assertEquals(1, post.get("replyCount").intValue());
        
        JsonNode replies = getReplies(postNodeRef, Status.STATUS_OK);
        JsonNode items = replies.get("items");
        assertEquals(1, items.size());
        
        reply = items.get(0);
        assertEquals("Yes I can", reply.get("content").textValue());

    }
    
    /**
     * Test for <a href=https://issues.alfresco.com/jira/browse/MNT-11964>MNT-11964</a>
     * @throws Exception 
     */
    public void testCreateForumPermission() throws Exception
    {
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        
        String siteName = SITE_SHORT_NAME_DISCUSSION + GUID.generate();
        this.siteService.createSite("ForumSitePreset", siteName, "SiteTitle", "SiteDescription", SiteVisibility.PUBLIC);
        
        String userName = USER_ONE + GUID.generate();
        createUser(userName, SiteModel.SITE_COLLABORATOR, siteName);

        // Check permissions for admin
        checkForumPermissions(siteName);
        
        // Check permissions for user
        this.authenticationComponent.setCurrentUser(userName);
        checkForumPermissions(siteName);

        // Cleanup
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        this.siteService.deleteSite(siteName);
        
        // Create a new site as user
        this.authenticationComponent.setCurrentUser(userName);
        siteName = SITE_SHORT_NAME_DISCUSSION + GUID.generate();
        this.siteService.createSite("BlogSitePreset", siteName, "SiteTitle", "SiteDescription", SiteVisibility.PUBLIC);
        
        // Check permissions for user
        checkForumPermissions(siteName);
        
        // Check permissions for admin
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        checkForumPermissions(siteName);
        
        // Cleanup
        this.siteService.deleteSite(siteName);
        this.personService.deletePerson(userName);
    }
    
    private void checkForumPermissions(String siteName) throws Exception
    {
        String url = "/api/forum/site/" + siteName + "/" + COMPONENT_DISCUSSION + "/posts";
        Response response = sendRequest(new GetRequest(url), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        assertTrue("The user sould have permission to create a new discussion.", result.get("forumPermissions").get("create").booleanValue());
    }
}
