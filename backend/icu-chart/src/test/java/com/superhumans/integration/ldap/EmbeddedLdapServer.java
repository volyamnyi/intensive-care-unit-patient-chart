package com.superhumans.integration.ldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.RDN;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hermetic in-memory directory for local-only LDAP integration tests
 * (issue #249, decision D6: isolated LDAP-compatible test double).
 *
 * <p>Seeds user entries exclusively from environment variables <em>by
 * name</em> ({@code APP_TEST_USERNAME1..9}, {@code APP_TEST_PASSWORD1..9}
 * plus optional profile metadata); values are never logged, persisted, or
 * embedded in sources. The service bind password is random per run, so no
 * credential ever needs to exist outside the test JVM. Role variables
 * ({@code APP_TEST_USERROLE*}) are deliberately <em>not</em> consumed:
 * authorization stays with the local database (decision D4).
 *
 * <p>Schema checking is disabled so AD-specific attributes such as
 * {@code sAMAccountName} are accepted as plain strings.
 */
public final class EmbeddedLdapServer {

    /**
     * Base DN shared by the double and the {@code app.ldap.base} test property.
     */
    public static final String BASE_DN = "dc=hospital,dc=local";

    /**
     * Service bind DN shared with the {@code app.ldap.username} test property.
     */
    public static final String SERVICE_DN = "cn=reader," + BASE_DN;

    private final InMemoryDirectoryServer server;
    private final int port;
    private final String servicePassword;
    private final List<String> seededLogins;
    private final Map<String, String> credentials;

    private EmbeddedLdapServer(InMemoryDirectoryServer server, int port,
            String servicePassword, List<String> seededLogins, Map<String, String> credentials) {
        this.server = server;
        this.port = port;
        this.servicePassword = servicePassword;
        this.seededLogins = seededLogins;
        this.credentials = credentials;
    }

    /**
     * Starts the double on a free loopback port and seeds entries for every
     * test identity whose username and password variables are present.
     *
     * @return running server handle, never {@code null}
     * @throws IllegalStateException when the server cannot be started
     */
    public static EmbeddedLdapServer start() {
        try {
            int port = findFreePort();
            InMemoryDirectoryServerConfig config =
                    new InMemoryDirectoryServerConfig(BASE_DN);
            config.setSchema(null);
            config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("test-ldap", port));
            InMemoryDirectoryServer server = new InMemoryDirectoryServer(config);
            String servicePassword = UUID.randomUUID().toString();
            server.add(new Entry(new DN(BASE_DN),
                    new Attribute("objectClass", "domain"),
                    new Attribute("dc", "hospital")));
            server.add(serviceEntry(servicePassword));
            List<String> seeded = new ArrayList<>();
            Map<String, String> credentials = new LinkedHashMap<>();
            for (int i = 1; i <= 9; i++) {
                String login = env("APP_TEST_USERNAME" + i);
                String password = env("APP_TEST_PASSWORD" + i);
                if (login == null || login.isBlank() || password == null || password.isEmpty()) {
                    continue;
                }
                server.add(userEntry(i, login, password));
                seeded.add(login);
                credentials.put(login, password);
            }
            server.startListening();
            return new EmbeddedLdapServer(server, port, servicePassword,
                    Collections.unmodifiableList(seeded),
                    Collections.unmodifiableMap(credentials));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot start embedded LDAP server", e);
        }
    }

    /**
     * Returns the loopback port the server listens on.
     *
     * @return TCP port number
     */
    public int port() {
        return port;
    }

    /**
     * Returns the random service bind password for the test properties.
     *
     * @return service password, never {@code null}
     */
    public String servicePassword() {
        return servicePassword;
    }

    /**
     * Returns the logins seeded from the environment, in identity order.
     *
     * @return unmodifiable login list, possibly empty
     */
    public List<String> seededLogins() {
        return seededLogins;
    }

    /**
     * Returns the seeded password for a login, or {@code null} when unknown.
     * Test-only in-memory lookup; the value is never logged or persisted.
     *
     * @param login seeded directory login
     * @return password or {@code null}
     */
    public String passwordFor(String login) {
        return credentials.get(login);
    }

    /**
     * Stops the server, ignoring repeated calls.
     */
    public void stop() {
        try {
            server.shutDown(true);
        } catch (Exception ignored) {
            // Best effort: the JVM exits anyway after the suite.
        }
    }

    private static Entry serviceEntry(String servicePassword) throws Exception {
        return new Entry(new DN(SERVICE_DN),
                new Attribute("objectClass", "inetOrgPerson"),
                new Attribute("cn", "reader"),
                new Attribute("sn", "reader"),
                new Attribute("userPassword", servicePassword.getBytes(StandardCharsets.UTF_8)));
    }

    private static Entry userEntry(int index, String login, String password) throws Exception {
        DN dn = new DN(new RDN("uid", login), new DN(BASE_DN));
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("objectClass", "inetOrgPerson"));
        attributes.add(new Attribute("uid", login));
        attributes.add(new Attribute("sAMAccountName", login));
        attributes.add(new Attribute("userPassword", password.getBytes(StandardCharsets.UTF_8)));
        String displayName = metadata("APP_TEST_USERFULLNAME", index);
        if (displayName != null && !displayName.isBlank()) {
            attributes.add(new Attribute("displayName", displayName));
        }
        addIfPresent(attributes, "mail", metadata("APP_TEST_EMAIL", index));
        addIfPresent(attributes, "telephoneNumber", metadata("APP_TEST_PHONE", index));
        addIfPresent(attributes, "title", metadata("APP_TEST_PROFESSION", index));
        return new Entry(dn, attributes.toArray(new Attribute[0]));
    }

    private static void addIfPresent(List<Attribute> attributes, String name, String value) {
        if (value != null && !value.isBlank()) {
            attributes.add(new Attribute(name, value));
        }
    }

    private static String metadata(String base, int index) {
        String suffixed = env(base + index);
        if (suffixed != null) {
            return suffixed;
        }
        return index == 1 ? env(base) : null;
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value;
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
