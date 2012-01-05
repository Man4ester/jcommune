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

import org.jtalks.jcommune.model.entity.PrivateMessage;
import org.jtalks.jcommune.service.PrivateMessageService;
import org.jtalks.jcommune.service.exceptions.NotFoundException;
import org.jtalks.jcommune.web.dto.PrivateMessageDto;
import org.jtalks.jcommune.web.dto.PrivateMessageDtoBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;

/**
 * MVC controller for Private Messaging. Handles request for inbox, outbox and new private messages.
 *
 * @author Pavel Vervenko
 * @author Max Malakhov
 * @author Kirill Afonin
 * @author Alexandre Teterin
 */
@Controller
public class PrivateMessageController {

    public static final String BREADCRUMB_LIST = "breadcrumbList";
    private final PrivateMessageService pmService;
    private PrivateMessageDtoBuilder pmDtoBuilder;

    //constants are moved here when occurs 4 or more times, as project PMD rule states
    private static final String PM_FORM = "pm/pmForm";
    private static final String PM_ID = "pmId";
    private static final String DTO = "privateMessageDto";

    /**
     * This method turns the trim binder on. Trim bilder
     * removes leading and trailing spaces from the submitted fields.
     * So, it ensures, that all validations will be applied to
     * trimmed field values only.
     *
     * @param binder Binder object to be injected
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    /**
     * @param pmService         the PrivateMessageService instance
     * @param pmDtoBuilder      the object which provides actions on
     *                          {@link org.jtalks.jcommune.web.dto.PrivateMessageDtoBuilder} entity
     */
    @Autowired
    public PrivateMessageController(PrivateMessageService pmService, PrivateMessageDtoBuilder pmDtoBuilder) {
        this.pmService = pmService;
        this.pmDtoBuilder = pmDtoBuilder;
    }

    /**
     * Render the PM inbox page with the list of incoming messages for the /inbox URI.
     *
     * @return {@code ModelAndView} with added list of inbox messages
     */
    @RequestMapping(value = "/inbox", method = RequestMethod.GET)
    public ModelAndView inboxPage() {
        return new ModelAndView("pm/inbox").addObject("pmList", pmService.getInboxForCurrentUser());
    }

    /**
     * Render the PM outbox page with the list of sent messages for the /outbox URI.
     *
     * @return {@code ModelAndView} with added list of outbox messages
     */
    @RequestMapping(value = "/outbox", method = RequestMethod.GET)
    public ModelAndView outboxPage() {
        return new ModelAndView("pm/outbox").addObject("pmList", pmService.getOutboxForCurrentUser());
    }

    /**
     * Get list of current user's list of draft messages.
     *
     * @return {@code ModelAndView} with list of messages
     */
    @RequestMapping(value = "/drafts", method = RequestMethod.GET)
    public ModelAndView draftsPage() {
        return new ModelAndView("pm/drafts").addObject("pmList", pmService.getDraftsFromCurrentUser());
    }

    /**
     * Render the page with a form for creation new Private Message with empty binded {@link PrivateMessageDto}.
     *
     * @return {@code ModelAndView} with the form
     */
    @RequestMapping(value = "/pm/new", method = RequestMethod.GET)
    public ModelAndView newPmPage() {
        return new ModelAndView(PM_FORM).addObject(DTO, new PrivateMessageDto());
    }

    /**
     * Render the page with the form for the reply to original message.
     * The form has the next filled fields: recipient, title
     *
     * @param id {@link PrivateMessage} id
     * @return {@code ModelAndView} with the message having filled recipient, title fields
     * @throws org.jtalks.jcommune.service.exceptions.NotFoundException
     *          when message not found
     */
    @RequestMapping(value = "/reply/{pmId}", method = RequestMethod.GET)
    public ModelAndView replyPage(@PathVariable(PM_ID) Long id) throws NotFoundException {
        PrivateMessage pm = pmService.get(id);
        PrivateMessageDto object = pmDtoBuilder.getReplyDtoFor(pm);
        return new ModelAndView(PM_FORM).addObject(DTO, object);
    }

    /**
     * Render the page with the form for the reply with quoting to original message.
     * The form has the next filled fields: recipient, title, message
     *
     * @param id {@link PrivateMessage} id
     * @return {@code ModelAndView} with the message having filled recipient, title, message fields
     * @throws org.jtalks.jcommune.service.exceptions.NotFoundException
     *          when message not found
     */
    @RequestMapping(value = "/quote/{pmId}", method = RequestMethod.GET)
    public ModelAndView quotePage(@PathVariable(PM_ID) Long id) throws NotFoundException {
        // todo: implement quotation here
        PrivateMessage pm = pmService.get(id);
        PrivateMessageDto object = pmDtoBuilder.getQuoteDtoFor(pm);
        return new ModelAndView(PM_FORM).addObject(DTO, object);
    }

    /**
     * Save the PrivateMessage for the filled in PrivateMessageDto.
     *
     * @param pmDto  {@link PrivateMessageDto} populated in form
     * @param result result of {@link PrivateMessageDto} validation
     * @return redirect to /inbox on success or back to "/new_pm" on validation errors
     */
    @RequestMapping(value = "/pm", method = {RequestMethod.POST, RequestMethod.GET})
    public String sendMessage(@Valid @ModelAttribute PrivateMessageDto pmDto, BindingResult result) {
        if (result.hasErrors()) {
            return PM_FORM;
        }
        try {
            if (pmDto.getId() > 0) {
                pmService.sendDraft(pmDto.getId(), pmDto.getTitle(), pmDto.getBody(), pmDto.getRecipient());
            } else {
                pmService.sendMessage(pmDto.getTitle(), pmDto.getBody(), pmDto.getRecipient());
            }
        } catch (NotFoundException nfe) {
            result.rejectValue("recipient", "label.wrong_recipient");
            return PM_FORM;
        }
        return "redirect:/outbox";
    }

    /**
     * Show page with private message details.
     *
     * @param folder message folder (inbox/outbox/drafts)
     * @param id     {@link PrivateMessage} id
     * @return {@code ModelAndView} with a message
     * @throws org.jtalks.jcommune.service.exceptions.NotFoundException
     *          when message not found
     */
    @RequestMapping(value = "/{folder}/{pmId}", method = RequestMethod.GET)
    public ModelAndView showPmPage(@PathVariable("folder") String folder,
                                   @PathVariable(PM_ID) Long id) throws NotFoundException {
        PrivateMessage pm = pmService.get(id);
        if ("inbox".equals(folder)) {
            pmService.markAsRead(pm);
        }
        return new ModelAndView("pm/showPm")
                .addObject("pm", pm);
    }

    /**
     * Edit private message page.
     *
     * @param id {@link PrivateMessage} id
     * @return private message form view and populated form dto
     * @throws NotFoundException when message not found
     */
    @RequestMapping(value = "/pm/{pmId}/edit", method = RequestMethod.GET)
    public ModelAndView editDraftPage(@PathVariable(PM_ID) Long id) throws NotFoundException {
        PrivateMessage pm = pmService.get(id);
        if (!pm.isDraft()) {
            // todo: 404? we need something more meaninful here
            throw new NotFoundException("Edit allowed only for draft messages.");
        }
        return new ModelAndView(PM_FORM).addObject(DTO, new PrivateMessageDtoBuilder().getFullPmDtoFor(pm));
    }

    /**
     * Save private message as draft.
     *
     * @param pmDto  Dto populated in form
     * @param result validation result
     * @return redirect to "drafts" folder if saved successfully or show form with error message
     */
    @RequestMapping(value = "/pm/save", method = {RequestMethod.POST, RequestMethod.GET})
    public String saveDraft(@Valid @ModelAttribute PrivateMessageDto pmDto, BindingResult result) {
        if (result.hasErrors()) {
            return PM_FORM;
        }
        try {
            pmService.saveDraft(pmDto.getId(), pmDto.getTitle(), pmDto.getBody(), pmDto.getRecipient());
        } catch (NotFoundException nfe) {
            result.rejectValue("recipient", "label.wrong_recipient");
            return PM_FORM;
        }
        return "redirect:/drafts";
    }

}
