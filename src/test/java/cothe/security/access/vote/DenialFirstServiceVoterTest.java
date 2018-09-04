package cothe.security.access.vote;

import com.google.gson.Gson;
import cothe.security.access.Definition;
import cothe.security.access.PermissionDescription;
import cothe.security.access.RequestedServiceMeta;
import cothe.security.core.domain.Permission;
import cothe.security.core.domain.SecuredObject;
import cothe.security.core.domain.SecuredObjectType;
import cothe.security.mock.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cothe.security.access.PermissionType.PERMISSION_TYPE_ALLOW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jeongjin Kim
 * @since 2018. 8. 31.
 */
@RunWith(SpringRunner.class)
@WithMockSecuredUser(username = "admin", name = "admin", roles = "role1,role2,role3")
public class DenialFirstServiceVoterTest {
    private Authentication authentication;
    private DenialFirstServiceVoter denialFirstServiceVoter;
    private MockRepositoryRoleProvider mockRepositoryRoleProvider = new MockRepositoryRoleProvider();
    private static Gson gson = new Gson();

    @Before
    public void setUp() {
        mockRepositoryRoleProvider.putRole("role1",
                Stream.of(
                        new Permission("permission1", "permission1", serializedPermissionDescriptionOf("permission1"),
                                new SecuredObject("object1", "object1", SecuredObjectType.SERVICE)),
                        new Permission("permission2", "permission2", serializedPermissionDescriptionOf("permission2"),
                                new SecuredObject("object2", "object2", SecuredObjectType.SERVICE))
                ).collect(Collectors.toSet()));

        authentication = SecurityContextHolder.getContext().getAuthentication();

        denialFirstServiceVoter = new DenialFirstServiceVoter(
                mockRepositoryRoleProvider,
                new MockRequestedServiceMetaExtractor()
        );
    }

    @Test
    public void successVoting() {
        assertTrue(denialFirstServiceVoter.vote(authentication,
                new RequestedServiceMeta(
                        "object1",
                        "save",
                        new HashMap<String, String>() {
                            {
                                put("device", "mobile");
                            }
                        }
                ), null) > 0);

        assertTrue(denialFirstServiceVoter.vote(authentication,
                new RequestedServiceMeta(
                        "object1",
                        "save",
                        new HashMap<String, String>() {
                            {
                                put("device", "mobile");
                                put("os", "mac");
                            }
                        }
                ), null) > 0);

        assertTrue(denialFirstServiceVoter.vote(authentication,
                new RequestedServiceMeta(
                        "object1",
                        "input1",
                        new HashMap<String, String>() {
                            {
                                put("device", "pc");
                                put("os", "mac");
                            }
                        }
                ), null) > 0);

        assertTrue(denialFirstServiceVoter.vote(authentication,
                new RequestedServiceMeta(
                        "object1",
                        "input2",
                        new HashMap<String, String>() {
                            {
                                put("device", "pda");
                                put("os", "mac");
                            }
                        }
                ), null) > 0);

        assertTrue(denialFirstServiceVoter.vote(authentication,
                new RequestedServiceMeta(
                        "object1",
                        "persist",
                        new HashMap<String, String>() {
                            {
                                put("device", "pda");
                                put("os", "mac");
                            }
                        }
                ), null) > 0);

        assertTrue(denialFirstServiceVoter.vote(authentication,
                new RequestedServiceMeta(
                        "object2",
                        "search",
                        new HashMap<String, String>() {
                            {
                                put("device", "pda");
                                put("os", "mac");
                                put("version", "6");

                            }
                        }
                ), null) > 0);
    }
    @Test
    public void denyVoting() {
        assertTrue(denialFirstServiceVoter.vote(authentication,
                new RequestedServiceMeta(
                        "object1",
                        "save",
                        new HashMap<String, String>() {
                            {
                                put("device", "pc");
                            }
                        }
                ), null) < 0);

        assertTrue(denialFirstServiceVoter.vote(authentication,
                new RequestedServiceMeta(
                        "object1",
                        "input",
                        new HashMap<String, String>() {
                            {
                                put("device", "mobile");
                                put("os", "mac");
                            }
                        }
                ), null) < 0);

        assertTrue(denialFirstServiceVoter.vote(authentication,
                new RequestedServiceMeta(
                        "object2",
                        "input",
                        new HashMap<String, String>() {
                            {
                                put("device", "mobile");
                                put("os", "mac");
                            }
                        }
                ), null) < 0);
    }

    private String serializedPermissionDescriptionOf(String permissionId) {
        PermissionDescription permissionDescription = null;
        switch (permissionId) {
            case "permission1":
                permissionDescription = new PermissionDescription(
                        PERMISSION_TYPE_ALLOW,
                        Arrays.asList(
                                new Definition("save",
                                        Arrays.asList(new HashMap<String, String>() {
                                            {
                                                put("device", "mobile");
                                            }
                                        }))
                                , new Definition("/input.*/,persist",
                                        Arrays.asList(
                                                new HashMap<String, String>() {
                                                    {
                                                        put("device", "pc");
                                                    }
                                                }
                                                , new HashMap<String, String>() {
                                                    {
                                                        put("device", "pda");
                                                    }
                                                }
                                        )
                                )
                        )
                );
                break;
            case "permission2":
                permissionDescription = new PermissionDescription(
                        PERMISSION_TYPE_ALLOW,
                        Arrays.asList(
                                new Definition("search",
                                        Arrays.asList(new HashMap<String, String>() {
                                            {
                                                put("os", "mac");
                                                put("version", "6");
                                            }
                                        }))
                        )
                );
                break;
        }


        return gson.toJson(permissionDescription);
    }
}