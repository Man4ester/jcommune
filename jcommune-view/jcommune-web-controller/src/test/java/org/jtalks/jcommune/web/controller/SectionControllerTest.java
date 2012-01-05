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
package org.jtalks.jcommune.web.controller;

import org.jtalks.jcommune.model.entity.Section;
import org.jtalks.jcommune.model.entity.User;
import org.jtalks.jcommune.service.SectionService;
import org.jtalks.jcommune.service.SecurityService;
import org.jtalks.jcommune.service.exceptions.NotFoundException;
import org.jtalks.jcommune.service.nontransactional.LocationServiceImpl;
import org.jtalks.jcommune.web.dto.Breadcrumb;
import org.jtalks.jcommune.web.dto.BreadcrumbBuilder;
import org.jtalks.jcommune.web.util.ForumStatisticsProvider;
import org.springframework.web.servlet.ModelAndView;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.ModelAndViewAssert.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Max Malakhov
 * @author Alexandre Teterin
 * @author Evgeniy Naumenko
 */
public class SectionControllerTest {
    private SectionService sectionService;
    private SectionController controller;
    private BreadcrumbBuilder breadcrumbBuilder;
    private ForumStatisticsProvider statisticsProvider;
    private LocationServiceImpl locationServiceImpl;

    @BeforeMethod
    public void init() {
        sectionService = mock(SectionService.class);
        SecurityService securityService = mock(SecurityService.class);
        breadcrumbBuilder = mock(BreadcrumbBuilder.class);
        statisticsProvider = mock(ForumStatisticsProvider.class);
        locationServiceImpl = mock(LocationServiceImpl.class);
        controller = new SectionController(securityService, sectionService,
                statisticsProvider, locationServiceImpl);
    }

    @Test
    public void testDisplayAllSections() {
        //set expectations
        when(sectionService.getAll()).thenReturn(new ArrayList<Section>());

        //invoke the object under test
        ModelAndView mav = controller.sectionList(mock(HttpSession.class));

        //check expectations
        verifyAndAssertAllSections(mav);
    }

    private void verifyAndAssertAllSections(ModelAndView mav) {
        verify(sectionService).getAll();

        //check result
        assertViewName(mav, "sectionList");
        assertModelAttributeAvailable(mav, "pageSize");
        assertModelAttributeAvailable(mav, "sectionList");
        assertModelAttributeAvailable(mav, "messagesCount");
        assertModelAttributeAvailable(mav, "registeredUsersCount");
        assertModelAttributeAvailable(mav, "visitors");
        assertModelAttributeAvailable(mav, "usersRegistered");
        assertModelAttributeAvailable(mav, "visitorsRegistered");
        assertModelAttributeAvailable(mav, "visitorsGuests");
    }

    @Test
    public void testBranchesInSection() throws NotFoundException {
        long sectionId = 1L;
        Section section = new Section("section name");
        section.setId(sectionId);

        //set expectations
        when(sectionService.get(sectionId)).thenReturn(section);
        when(breadcrumbBuilder.getForumBreadcrumb()).thenReturn(new ArrayList<Breadcrumb>());

        //invoke the object under test
        ModelAndView mav = controller.branchList(sectionId);

        //check expectations
        verify(sectionService).get(sectionId);
        //check result
        assertViewName(mav, "branchList");
        assertModelAttributeAvailable(mav, "section");
        Section actualSection = assertAndReturnModelAttributeOfType(mav, "section", Section.class);
        assertEquals(actualSection.getId(), sectionId);
    }

    @Test
    public void testViewList() throws NotFoundException {
        long sectionId = 1L;
        Section section = new Section("section name");
        section.setId(sectionId);

        when(sectionService.get(sectionId)).thenReturn(section);
        when(breadcrumbBuilder.getForumBreadcrumb()).thenReturn(new ArrayList<Breadcrumb>());

        ModelAndView mav = controller.branchList(sectionId);

        assertModelAttributeAvailable(mav, "viewList");

        List<String> actualViewList = assertAndReturnModelAttributeOfType(mav, "viewList", List.class);
        assertEquals(actualViewList, new ArrayList<String>());
    }

}
