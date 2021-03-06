/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.util.io.IClusterable;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.apache.wicket.Session;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.PasswordHelper;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an object representation of a Hippo User, to be used in the admin interface only.
 */
public class User implements Comparable<User>, IClusterable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(User.class);

    private static final class SerializableEntry<K, V> implements Entry<K, V>, Serializable {

        private final K key;
        private final V value;

        private SerializableEntry(final K key, final V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(final V value) {
            throw new UnsupportedOperationException();
        }
    }

    private static final String NT_USER = "hipposys:user";

    public static final String PROP_FIRSTNAME = "hipposys:firstname";
    public static final String PROP_LASTNAME = "hipposys:lastname";
    public static final String PROP_EMAIL = "hipposys:email";
    public static final String PROP_PASSWORD = HippoNodeType.HIPPO_PASSWORD;
    public static final String PROP_PASSKEY = HippoNodeType.HIPPO_PASSKEY;
    public static final String PROP_PROVIDER = HippoNodeType.HIPPO_SECURITYPROVIDER;
    public static final String PROP_PREVIOUSPASSWORDS = HippoNodeType.HIPPO_PREVIOUSPASSWORDS;
    public static final String PROP_PASSWORDLASTMODIFIED = HippoNodeType.HIPPO_PASSWORDLASTMODIFIED;
    public static final String PROP_SYSTEM = HippoNodeType.HIPPO_SYSTEM;

    private static final String QUERY_USER = "SELECT * FROM hipposys:user WHERE fn:name()='{}'";

    private static final String QUERY_LOCAL_MEMBERSHIPS = "//element(*, hipposys:group)[@hipposys:members='{}']";
    private static final String QUERY_EXTERNAL_MEMBERSHIPS = "//element(*, hipposys:externalgroup)[@hipposys:members='{}']";

    private static final long ONEDAYMS = 1000 * 3600 * 24;

    private boolean external = false;
    private boolean active = true;
    private boolean system = false;
    private String path;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String provider;
    private Calendar passwordLastModified;
    private long passwordMaxAge = -1L;

    private Map<String, String> properties = new TreeMap<String, String>();
    private transient List<DetachableGroup> externalMemberships;

    private transient Node node;

    //-------------- static helpers -------------------//

    /**
     * Returns the QueryManager.
     *
     * @return the QueryManager
     * @throws RepositoryException
     */
    public static QueryManager getQueryManager() throws RepositoryException {
        return ((UserSession) Session.get()).getQueryManager();
    }

    /**
     * Checks if the user with username exists.
     *
     * @param username the name of the user to check
     * @return true if the user exists, false otherwise
     */
    public static boolean userExists(final String username) {
        final String queryString = QUERY_USER.replace("{}", Text.escapeIllegalJcr10Chars(ISO9075.encode(NodeNameCodec.encode(username))));
        try {
            final Query query = getQueryManager().createQuery(queryString, Query.SQL);
            return query.execute().getNodes().hasNext();
        } catch (RepositoryException e) {
            log.error("Unable to check if user '{}' exists, returning true", username, e);
            return true;
        }
    }

    /**
     * Generate password hash from string.
     *
     * @param password the password
     * @return the hash
     * @throws RepositoryException, the wrapper encoding errors
     */
    public static String createPasswordHash(final String password) throws RepositoryException {
        try {
            return PasswordHelper.getHash(password.toCharArray());
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException("Unable to hash password", e);
        } catch (IOException e) {
            throw new RepositoryException("Unable to hash password", e);
        }
    }

    //--------------- getters and setters -----------//
    public String getProvider() {
        return provider;
    }

    public boolean isExternal() {
        return external;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public boolean isSystemUser() {
        return system;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns the properties list.
     *
     * @return the properties list
     */
    public List<Entry<String, String>> getPropertiesList() {
        List<Entry<String, String>> l = new ArrayList<Entry<String, String>>();
        for (Entry<String, String> e : properties.entrySet()) {
            l.add(new SerializableEntry<String, String>(e.getKey(), e.getValue()));
        }
        return l;
    }

    /**
     * Returns the user's display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) {
            sb.append(firstName);
            sb.append(" ");
        }
        if (lastName != null) {
            sb.append(lastName);
        }
        if (sb.length() == 0) {
            return username;
        }
        return sb.toString().trim();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getPath() {
        return path;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public Calendar getPasswordLastModified() {
        return passwordLastModified;
    }

    //----------------------- constructors ---------//

    /**
     * Constructs an empty User.
     */
    public User() {
    }

    /**
     * Constructs a User object based on the username. Effectively fetches the user from the repository and wraps it in
     * this object.
     *
     * @param username the name of the user to fetch
     */
    public User(final String username) {
        final String queryString = QUERY_USER.replace("{}", Text.escapeIllegalJcr10Chars(ISO9075.encode(NodeNameCodec.encode(username))));
        try {
            final Query query = getQueryManager().createQuery(queryString, Query.SQL);
            final NodeIterator iter = query.execute().getNodes();
            if (iter.hasNext()) {
                init(iter.nextNode());
            } else {
                log.error("User {} does not exist, returning object without state.", username);
            }
        } catch (RepositoryException e) {
            log.error("Unable to get node for user '{}' while constructing user", username, e);
            this.username = username;
            throw new IllegalStateException("Error while obtaining user", e);
        }
    }

    /**
     * Constructs a User object based on the node. Effectively fetches the user from the repository and wraps it in this
     * object.
     *
     * @param node the node of the user to fetch
     * @throws RepositoryException thrown when the user with supplied node does not exist in the repository
     */
    public User(final Node node) throws RepositoryException {
        init(node);
    }

    private void init(final Node node) throws RepositoryException {
        this.node = node;
        this.path = node.getPath().substring(1);
        this.username = NodeNameCodec.decode(node.getName());

        if (node.isNodeType(HippoNodeType.NT_EXTERNALUSER)) {
            external = true;
        }

        PropertyIterator pi = node.getProperties();
        while (pi.hasNext()) {
            final Property p = pi.nextProperty();
            String name = p.getName();
            if (name.startsWith("jcr:")) {
                //skip
                continue;
            } else if (name.equals(PROP_EMAIL) || name.equalsIgnoreCase("email")) {
                email = p.getString();
            } else if (name.equals(PROP_FIRSTNAME) || name.equalsIgnoreCase("firstname")) {
                firstName = p.getString();
            } else if (name.equals(PROP_LASTNAME) || name.equalsIgnoreCase("lastname")) {
                lastName = p.getString();
            } else if (name.equals(HippoNodeType.HIPPO_ACTIVE)) {
                active = p.getBoolean();
            } else if (name.equals(PROP_PASSWORD) || name.equals(PROP_PASSKEY) || name.equals(PROP_PREVIOUSPASSWORDS)) {
                // do not expose password hash
                continue;
            } else if (name.equals(PROP_PROVIDER)) {
                provider = p.getString();
            } else if (name.equals(PROP_PASSWORDLASTMODIFIED)) {
                passwordLastModified = p.getDate();
            } else if (name.equals(PROP_SYSTEM)) {
                system = p.getBoolean();
            } else {
                properties.put(name, p.getString());
            }
        }

        Node securityNode = ((UserSession) Session.get()).getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH)
                .getNode(HippoNodeType.SECURITY_PATH);
        if (securityNode.hasProperty(HippoNodeType.HIPPO_PASSWORDMAXAGEDAYS)) {
            passwordMaxAge = (long) (securityNode.getProperty(
                    HippoNodeType.HIPPO_PASSWORDMAXAGEDAYS).getDouble() * ONEDAYMS);
        }
    }

    //--------------- group membership helpers ------------------//

    /**
     * Returns the User's local memberships.
     *
     * @return the User's local memberships
     */
    public List<DetachableGroup> getLocalMemberships() {
        final String escapedUsername = Text.escapeIllegalXpathSearchChars(username).replaceAll("'", "''");
        final String queryString = QUERY_LOCAL_MEMBERSHIPS.replace("{}", escapedUsername);
        final List<DetachableGroup> localMemberships = new ArrayList<DetachableGroup>();
        try {
            final Query query = getQueryManager().createQuery(queryString, Query.XPATH);
            final NodeIterator iter = query.execute().getNodes();
            while (iter.hasNext()) {
                final Node node = iter.nextNode();
                if (node == null) {
                    continue;
                }
                try {
                    localMemberships.add(new DetachableGroup(node.getPath()));
                } catch (RepositoryException e) {
                    log.warn("Unable to add group to local memberships for user '{}'", username, e);
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while querying local memberships of user '{}'", e);
        }
        return localMemberships;
    }

    public List<Group> getLocalMembershipsAsListOfGroups() {
        List<Group> groups = new ArrayList<Group>();
        for (DetachableGroup group : getLocalMemberships()) {
            groups.add(group.getObject());
        }
        return groups;
    }

    /**
     * Returns the User's external memberships.
     *
     * @return the User's external memberships
     */
    public List<DetachableGroup> getExternalMemberships() {
        if (externalMemberships != null) {
            return externalMemberships;
        }

        externalMemberships = new ArrayList<DetachableGroup>();
        final String escapedUserName = Text.escapeIllegalXpathSearchChars(username).replaceAll("'", "''");
        final String queryString = QUERY_EXTERNAL_MEMBERSHIPS.replace("{}", escapedUserName);
        try {
            final Query query = getQueryManager().createQuery(queryString, Query.XPATH);
            final NodeIterator iter = query.execute().getNodes();
            while (iter.hasNext()) {
                final Node node = iter.nextNode();
                if (node == null) {
                    continue;
                }
                try {
                    externalMemberships.add(new DetachableGroup(node.getPath()));
                } catch (RepositoryException e) {
                    log.warn("Unable to add group to external memberships for user '{}'", username, e);
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while querying external memberships of user '{}'", username, e);
        }
        return externalMemberships;
    }

    //-------------------- persistence helpers ----------//

    /**
     * Create a new user.
     *
     * @throws RepositoryException
     */
    public void create() throws RepositoryException {
        if (userExists(getUsername())) {
            throw new RepositoryException("User already exists");
        }

        // FIXME: should be delegated to a usermanager
        StringBuilder relPath = new StringBuilder();
        relPath.append(HippoNodeType.CONFIGURATION_PATH);
        relPath.append("/");
        relPath.append(HippoNodeType.USERS_PATH);
        relPath.append("/");
        relPath.append(NodeNameCodec.encode(getUsername(), true));

        node = UserSession.get().getRootNode().addNode(relPath.toString(), NT_USER);
        setOrRemoveStringProperty(node, PROP_EMAIL, getEmail());
        setOrRemoveStringProperty(node, PROP_FIRSTNAME, getFirstName());
        setOrRemoveStringProperty(node, PROP_LASTNAME, getLastName());
        // save parent when adding a node
        node.getParent().getSession().save();
    }

    /**
     * Wrapper needed for spi layer which doesn't know if a property exists or not.
     *
     * @param node
     * @param name
     * @param value
     * @throws RepositoryException
     */
    private void setOrRemoveStringProperty(final Node node, final String name, final String value) throws RepositoryException {
        if (value == null && !node.hasProperty(name)) {
            return;
        }
        node.setProperty(name, value);
    }

    /**
     * save the current user.
     *
     * @throws RepositoryException
     */
    public void save() throws RepositoryException {
        if (node.isNodeType(NT_USER)) {
            setOrRemoveStringProperty(node, PROP_EMAIL, getEmail());
            setOrRemoveStringProperty(node, PROP_FIRSTNAME, getFirstName());
            setOrRemoveStringProperty(node, PROP_LASTNAME, getLastName());
            node.setProperty(HippoNodeType.HIPPO_ACTIVE, isActive());
            node.getSession().save();
        } else {
            throw new RepositoryException("Only frontend:users can be edited.");
        }
    }

    /**
     * Deletes the current user, including its group memberships.
     *
     * @throws RepositoryException
     */
    public void delete() throws RepositoryException {

        removeAllGroupMemberships();

        // Delete the user from the repository
        Node parent = node.getParent();
        node.remove();
        parent.getSession().save();

    }

    public void removeAllGroupMemberships() throws RepositoryException {
        for (DetachableGroup dg : this.getLocalMemberships()) {
            dg.getGroup().removeMembership(username);
        }
    }

    /**
     * Save the current user's password with a hashing function.
     *
     * @param password the password
     * @throws RepositoryException
     */
    public void savePassword(final String password) throws RepositoryException {
        // remember old password
        if (node.hasProperty(HippoNodeType.HIPPO_PASSWORD)) {
            String oldPassword = node.getProperty(HippoNodeType.HIPPO_PASSWORD).getString();
            Value[] newValues;
            if (node.hasProperty(HippoNodeType.HIPPO_PREVIOUSPASSWORDS)) {
                Value[] oldValues = node.getProperty(HippoNodeType.HIPPO_PREVIOUSPASSWORDS).getValues();
                newValues = new Value[oldValues.length + 1];
                System.arraycopy(oldValues, 0, newValues, 1, oldValues.length);
            } else {
                newValues = new Value[1];
            }
            newValues[0] = UserSession.get().getJcrSession().getValueFactory().createValue(oldPassword);
            node.setProperty(HippoNodeType.HIPPO_PREVIOUSPASSWORDS, newValues);
        }

        // set password last changed date
        Calendar now = Calendar.getInstance();
        node.setProperty(HippoNodeType.HIPPO_PASSWORDLASTMODIFIED, now);
        passwordLastModified = now;

        // set new password
        node.setProperty(HippoNodeType.HIPPO_PASSWORD, createPasswordHash(password));

        node.getSession().save();
    }

    /**
     * Checks the password against the hash.
     *
     * @param password the password
     * @return true if the password is correct, false otherwise
     */
    public boolean checkPassword(final char[] password) {
        try {
            return PasswordHelper.checkHash(password, node.getProperty(HippoNodeType.HIPPO_PASSWORD).getString());
        } catch (NoSuchAlgorithmException e) {
            log.error("Unknown algorith for password", e);
        } catch (UnsupportedEncodingException e) {
            log.error("Unsupported encoding for password", e);
        } catch (RepositoryException e) {
            log.error("Error while checking user password", e);
        }
        return false;
    }

    /**
     * Checks if the password is the same as one of the previous passwords.
     *
     * @param password                  the password to check
     * @param numberOfPreviousPasswords the number of previous passwords to compare to
     * @return true if the password is the same as one of the previous passwords, false otherwise
     * @throws RepositoryException
     */
    public boolean isPreviousPassword(final char[] password, final int numberOfPreviousPasswords) throws RepositoryException {
        // is current password?
        if (node != null && node.hasProperty(HippoNodeType.HIPPO_PASSWORD)) {
            String currentPassword = node.getProperty(HippoNodeType.HIPPO_PASSWORD).getString();
            try {
                if (PasswordHelper.checkHash(password, currentPassword)) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Error while checking if password was previously used", e);
            }
        }
        // is previous password?
        if (node != null && node.hasProperty(HippoNodeType.HIPPO_PREVIOUSPASSWORDS)) {
            Value[] previousPasswords = node.getProperty(HippoNodeType.HIPPO_PREVIOUSPASSWORDS).getValues();
            for (int i = 0; i < previousPasswords.length && i < numberOfPreviousPasswords; i++) {
                try {
                    if (PasswordHelper.checkHash(password, previousPasswords[i].getString())) {
                        return true;
                    }
                } catch (Exception e) {
                    log.error("Error while checking if password was previously used", e);
                }
            }
        }
        return false;
    }

    /**
     * Checks if the User's password is expired.
     *
     * @return true if the password is expired, false otherwise
     */
    public boolean isPasswordExpired() {
        long passwordExpirationTime = getPasswordExpirationTime();
        return passwordExpirationTime > 0 && passwordExpirationTime < System.currentTimeMillis();
    }

    /**
     * Returns the time before the password expires, in miliseconds.
     *
     * @return the time before the password expires, in miliseconds
     */
    public long getPasswordExpirationTime() {
        if (passwordLastModified != null && passwordMaxAge > 0) {
            return passwordLastModified.getTimeInMillis() + passwordMaxAge;
        }
        return -1L;
    }

    //--------------------- default object -------------------//

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || (obj.getClass() != this.getClass())) {
            return false;
        }
        User other = (User) obj;
        return other.getPath().equals(getPath());
    }

    public int hashCode() {
        return (null == path ? 0 : path.hashCode());
    }

    public int compareTo(final User o) {
        return getUsername().compareTo(o.getUsername());
    }
}
