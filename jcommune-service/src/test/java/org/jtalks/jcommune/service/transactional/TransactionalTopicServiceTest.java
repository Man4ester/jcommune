/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jtalks.jcommune.service.transactional;

import org.joda.time.DateTime;
import org.jtalks.jcommune.model.dao.BranchDao;
import org.jtalks.jcommune.model.dao.TopicDao;
import org.jtalks.jcommune.model.entity.Branch;
import org.jtalks.jcommune.model.entity.Post;
import org.jtalks.jcommune.model.entity.Topic;
import org.jtalks.jcommune.model.entity.User;
import org.jtalks.jcommune.service.BranchService;
import org.jtalks.jcommune.service.SecurityService;
import org.jtalks.jcommune.service.TopicService;
import org.jtalks.jcommune.service.exceptions.NotFoundException;
import org.jtalks.jcommune.service.security.AclBuilder;
import org.jtalks.jcommune.service.security.SecurityConstants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.jtalks.jcommune.service.TestUtils.mockAclBuilder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * This test cover {@code TransactionalTopicService} logic validation.
 * Logic validation cover update/get/error cases by this class.
 *
 * @author Osadchuck Eugeny
 * @author Kravchenko Vitaliy
 * @author Kirill Afonin
 * @author Max Malakhov
 */
public class TransactionalTopicServiceTest {

    final long TOPIC_ID = 999L;
    final long BRANCH_ID = 1L;
    final long POST_ID = 333L;
    final String TOPIC_TITLE = "topic title";
    final String BRANCH_NAME = "branch name";
    private static final String USERNAME = "username";
    private User user;
    final String ANSWER_BODY = "Test Answer Body";
    private TopicService topicService;
    private SecurityService securityService;
    private BranchService branchService;
    private TopicDao topicDao;
    private BranchDao branchDao;
    private AclBuilder aclBuilder;

    @BeforeMethod
    public void setUp() throws Exception {
        aclBuilder = mockAclBuilder();
        topicDao = mock(TopicDao.class);
        branchService = mock(BranchService.class);
        securityService = mock(SecurityService.class);
        branchDao = mock(BranchDao.class);
        topicService = new TransactionalTopicService(topicDao, securityService,
                branchService, branchDao);
        user = new User(USERNAME, "email@mail.com", "password");
    }

    @Test
    public void testGet() throws NotFoundException {
        Topic expectedTopic = new Topic(user, "title");
        when(topicDao.isExist(TOPIC_ID)).thenReturn(true);
        when(topicDao.get(TOPIC_ID)).thenReturn(expectedTopic);

        Topic actualTopic = topicService.get(TOPIC_ID);

        assertEquals(actualTopic, expectedTopic, "Topics aren't equals");
        verify(topicDao).isExist(TOPIC_ID);
        verify(topicDao).get(TOPIC_ID);
    }

    @Test(expectedExceptions = {NotFoundException.class})
    public void testGetIncorrectId() throws NotFoundException {
        when(topicDao.isExist(POST_ID)).thenReturn(false);

        topicService.get(POST_ID);
    }

    @Test
    public void testAddAnswer() throws NotFoundException {
        Topic answeredTopic = new Topic(user, "title");
        when(securityService.getCurrentUser()).thenReturn(user);
        when(topicDao.isExist(TOPIC_ID)).thenReturn(true);
        when(topicDao.get(TOPIC_ID)).thenReturn(answeredTopic);
        when(securityService.grantToCurrentUser()).thenReturn(aclBuilder);

        Post createdPost = topicService.addAnswer(TOPIC_ID, ANSWER_BODY);

        assertEquals(createdPost.getPostContent(), ANSWER_BODY);
        assertEquals(createdPost.getUserCreated(), user);
        verify(securityService).getCurrentUser();
        verify(topicDao).get(TOPIC_ID);
        verify(topicDao).update(answeredTopic);
        verify(securityService).grantToCurrentUser();
        verify(aclBuilder).role(SecurityConstants.ROLE_ADMIN);
        verify(aclBuilder).admin();
        verify(aclBuilder).on(createdPost);
    }

    @Test(expectedExceptions = {IllegalStateException.class})
    public void testAddAnswerWithoutCurrentUser() throws NotFoundException {
        when(securityService.getCurrentUser()).thenReturn(null);

        topicService.addAnswer(TOPIC_ID, ANSWER_BODY);
    }

    @Test
    public void testCreateTopic() throws NotFoundException {
        Branch branch = new Branch(BRANCH_NAME);
        when(securityService.getCurrentUser()).thenReturn(user);
        when(branchService.get(BRANCH_ID)).thenReturn(branch);
        when(securityService.grantToCurrentUser()).thenReturn(aclBuilder);

        Topic createdTopic = topicService.createTopic(TOPIC_TITLE, ANSWER_BODY, BRANCH_ID);

        Post createdPost = createdTopic.getFirstPost();
        assertEquals(createdTopic.getTitle(), TOPIC_TITLE);
        assertEquals(createdTopic.getTopicStarter(), user);
        assertEquals(createdTopic.getBranch(), branch);
        assertEquals(branch.topicCount(), 1, "Topic not added to branch");
        assertEquals(createdPost.getUserCreated(), user);
        assertEquals(createdPost.getPostContent(), ANSWER_BODY);
        verify(securityService).getCurrentUser();
        verify(branchDao).update(branch);
        verify(branchService).get(BRANCH_ID);
        verify(securityService).grantToCurrentUser();
        verify(aclBuilder, times(2)).role(SecurityConstants.ROLE_ADMIN);
        verify(aclBuilder, times(2)).admin();
        verify(aclBuilder).user(USERNAME);
        verify(aclBuilder).on(createdTopic);
        verify(aclBuilder).on(createdPost);
    }

    @Test(expectedExceptions = {IllegalStateException.class})
    public void testCreateTopicWithoutCurrentUser() throws NotFoundException {
        when(securityService.getCurrentUser()).thenReturn(null);

        topicService.createTopic(TOPIC_TITLE, ANSWER_BODY, 1L);
    }

    @Test
    public void testGetTopicsRangeInBranch() throws NotFoundException {
        int start = 1;
        int max = 2;
        List<Topic> expectedList = new ArrayList<Topic>();
        expectedList.add(new Topic(user, "title"));
        expectedList.add(new Topic(user, "title"));
        when(branchDao.isExist(BRANCH_ID)).thenReturn(true);
        when(topicDao.getTopicRangeInBranch(BRANCH_ID, start, max)).thenReturn(expectedList);

        List<Topic> topics = topicService.getTopicRangeInBranch(BRANCH_ID, start, max);

        assertNotNull(topics);
        assertEquals(topics.size(), max, "Unexpected list size");
        verify(topicDao).getTopicRangeInBranch(BRANCH_ID, start, max);
        verify(branchDao).isExist(BRANCH_ID);
    }

    @Test
    public void testGetAllTopicsPastLastDay() throws NotFoundException {
        int start = 1;
        int max = 2;
        DateTime now = new DateTime();
        List<Topic> expectedList = new ArrayList<Topic>();
        expectedList.add(new Topic(user, "title"));
        expectedList.add(new Topic(user, "title"));
        when(topicDao.getAllTopicsPastLastDay(start, max, now)).thenReturn(expectedList);

        List<Topic> topics = topicService.getAllTopicsPastLastDay(start, max, now);

        assertNotNull(topics);
        assertEquals(topics.size(), max);
    }

    @Test
    public void testGetTopicsPastLastDayCount() throws NotFoundException {
        int expectedCount = 10;
        DateTime now = new DateTime();
        when(topicDao.getTopicsPastLastDayCount(now)).thenReturn(expectedCount);

        int count = topicService.getTopicsPastLastDayCount(now);

        assertEquals(expectedCount, count);
    }

    @Test(expectedExceptions = {NotFoundException.class})
    public void testGetTopicsRangeInNonExistentBranch() throws NotFoundException {
        when(branchDao.isExist(BRANCH_ID)).thenReturn(false);

        topicService.getTopicRangeInBranch(BRANCH_ID, 1, 5);
    }

    @Test
    public void testGetTopicsInBranchCount() throws NotFoundException {
        int expectedCount = 10;
        when(branchDao.isExist(BRANCH_ID)).thenReturn(true);
        when(topicDao.getTopicsInBranchCount(BRANCH_ID)).thenReturn(expectedCount);

        int count = topicService.getTopicsInBranchCount(BRANCH_ID);

        assertEquals(count, expectedCount);
        verify(topicDao).getTopicsInBranchCount(BRANCH_ID);
        verify(branchDao).isExist(BRANCH_ID);
    }

    @Test(expectedExceptions = {NotFoundException.class})
    public void testGetTopicsCountInNonExistentBranch() throws NotFoundException {
        when(branchDao.isExist(BRANCH_ID)).thenReturn(false);

        topicService.getTopicsInBranchCount(BRANCH_ID);
    }

    @Test
    public void testDeleteTopic() throws NotFoundException {
        Topic topic = new Topic(user, "title");
        Branch branch = new Branch(BRANCH_NAME);
        branch.addTopic(topic);
        when(topicDao.isExist(TOPIC_ID)).thenReturn(true);
        when(topicDao.get(TOPIC_ID)).thenReturn(topic);

        Branch branchFromWhichTopicDeleted = topicService.deleteTopic(TOPIC_ID);

        assertEquals(branchFromWhichTopicDeleted, branch);
        assertEquals(branch.topicCount(), 0);
        verify(branchDao).update(branch);
        verify(securityService).deleteFromAcl(Topic.class, TOPIC_ID);
    }

    @Test(expectedExceptions = {NotFoundException.class})
    public void testDeleteNonExistentTopic() throws NotFoundException {
        when(topicDao.isExist(TOPIC_ID)).thenReturn(false);

        topicService.deleteTopic(TOPIC_ID);
    }

    @Test
    void testSaveTopic() throws NotFoundException {
        String newTitle = "new title";
        String newBody = "new body";
        int newWeight = 0;
        boolean newSticked = false;
        boolean newAnnouncement = false;
        Topic topic = new Topic(user, "title");
        topic.setId(TOPIC_ID);
        topic.setTitle("title");
        Post post = new Post(user, "content");
        post.setId(POST_ID);
        topic.addPost(post);

        when(topicDao.isExist(TOPIC_ID)).thenReturn(true);
        when(topicDao.get(TOPIC_ID)).thenReturn(topic);

        topicService.saveTopic(TOPIC_ID, newTitle, newBody, newWeight, newSticked, newAnnouncement);

        assertEquals(topic.getTitle(), newTitle);
        assertEquals(post.getPostContent(), newBody);
        assertEquals(topic.getTopicWeight(), newWeight);
        assertEquals(topic.isSticked(), newSticked);
        assertEquals(topic.isAnnouncement(), newAnnouncement);

        verify(topicDao).isExist(TOPIC_ID);
        verify(topicDao).get(TOPIC_ID);
        verify(topicDao).update(topic);
    }

    @Test
    void testSaveTopicSimple() throws NotFoundException {
        String newTitle = "new title";
        String newBody = "new body";
        int newWeight = 0;
        boolean newSticked = false;
        boolean newAnnouncement = false;
        Topic topic = new Topic(user, "title");
        topic.setId(TOPIC_ID);
        topic.setTitle("title");
        Post post = new Post(user, "content");
        post.setId(POST_ID);
        topic.addPost(post);

        when(topicDao.isExist(TOPIC_ID)).thenReturn(true);
        when(topicDao.get(TOPIC_ID)).thenReturn(topic);

        topicService.saveTopic(TOPIC_ID, newTitle, newBody);

        assertEquals(topic.getTitle(), newTitle);
        assertEquals(post.getPostContent(), newBody);
        assertEquals(topic.getTopicWeight(), newWeight);
        assertEquals(topic.isSticked(), newSticked);
        assertEquals(topic.isAnnouncement(), newAnnouncement);

        verify(topicDao).isExist(TOPIC_ID);
        verify(topicDao).get(TOPIC_ID);
        verify(topicDao).update(topic);
    }

    @Test(expectedExceptions = {NotFoundException.class})
    void testSaveTopicNonExistentTopic() throws NotFoundException {
        String newTitle = "new title";
        String newBody = "new body";
        int newWeight = 0;
        boolean newSticked = false;
        boolean newAnnouncement = false;
        when(topicDao.isExist(TOPIC_ID)).thenReturn(false);

        topicService.saveTopic(TOPIC_ID, newTitle, newBody, newWeight, newSticked, newAnnouncement);
    }
}
