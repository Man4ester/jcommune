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
package org.jtalks.jcommune.service;

import org.jtalks.jcommune.model.entity.JCUser;
import org.jtalks.jcommune.service.dto.UserInfoContainer;
import org.jtalks.jcommune.service.exceptions.DuplicateEmailException;
import org.jtalks.jcommune.service.exceptions.MailingFailedException;
import org.jtalks.jcommune.service.exceptions.NotFoundException;
import org.jtalks.jcommune.service.exceptions.WrongPasswordException;

/**
 * This interface should have methods which give us more abilities in manipulating User persistent entity.
 *
 * @author Osadchuck Eugeny
 * @author Kirill Afonin
 */
public interface UserService extends EntityService<JCUser> {
    /**
     * Get {@link org.jtalks.jcommune.model.entity.JCUser} by username.
     *
     * @param username username of User
     * @return {@link org.jtalks.jcommune.model.entity.JCUser} with given username
     * @throws NotFoundException if user not found
     * @see org.jtalks.jcommune.model.entity.JCUser
     */
    JCUser getByUsername(String username) throws NotFoundException;

    /**
     * Try to register {@link org.jtalks.jcommune.model.entity.JCUser} with given features.
     *
     * @param user user for register
     * @return registered {@link org.jtalks.jcommune.model.entity.JCUser}
     * @see org.jtalks.jcommune.model.entity.JCUser
     */
    JCUser registerUser(JCUser user);


    /**
     * Updates user last login time to current time.
     *
     * @param user user which must be updated
     * @see org.jtalks.jcommune.model.entity.JCUser
     */
    void updateLastLoginTime(JCUser user);

    /**
     * Update user entity.
     *
     * @param info modified profile info holder
     * @return edited user
     * @throws DuplicateEmailException when user with given email already exist
     * @throws WrongPasswordException  when user enter wrong currentPassword
     */
    JCUser editUserProfile(UserInfoContainer info) throws DuplicateEmailException, WrongPasswordException;

    /**
     * Performs the following:
     * 1. Alters the password for this user to the random string
     * 2. Sends an e-mail with new password to this address to notify user
     *
     * @param email address to identify user
     * @throws org.jtalks.jcommune.service.exceptions.MailingFailedException
     *          if mailing failed
     */
    void restorePassword(String email) throws MailingFailedException;

    /**
     * Activates user account based on enchipehed name passed.
     * We using B64 not to expose username in a plain link.
     *
     * @param b64enchipheredUsername username, prevoiusly incoded with Base65
     * @throws NotFoundException if there is no user mathing username given
     */
    void activateAccount(String b64enchipheredUsername) throws NotFoundException;

    /**
     * This method will be called authomatically every hour to check
     * if there are expired user accounts to be deleted. User account
     * is expired if it's created, but not activated for a day or more.
     */
    public void deleteUnactivatedAccountsByTimer();
}
