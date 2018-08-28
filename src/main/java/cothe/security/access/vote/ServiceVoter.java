package cothe.security.access.vote;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import cothe.security.access.ServicePermissionByOperations;
import cothe.security.access.RequestedServiceMeta;
import cothe.security.access.RequestedServiceMetaExtractor;
import cothe.security.access.ServicePermission;
import cothe.security.core.domain.Permission;
import cothe.security.core.domain.Role;
import cothe.security.core.domain.SecuredObjectType;
import cothe.security.core.domain.providers.RoleProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author Jeongjin Kim
 * @since 2018. 8. 17.
 */
@Slf4j
public class ServiceVoter implements AccessDecisionVoter<Object> {
    private RoleProvider roleProvider;
    private RequestedServiceMetaExtractor requestedServiceMetaExtractor;
    private static final Gson gson = new Gson();

    public ServiceVoter(RoleProvider roleProvider, RequestedServiceMetaExtractor requestedServiceMetaExtractor) {
        this.roleProvider = roleProvider;
        this.requestedServiceMetaExtractor = requestedServiceMetaExtractor;
    }

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return true;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
        Assert.notNull(this.roleProvider, "There is no role provider.");
        Assert.notNull(this.requestedServiceMetaExtractor, "There is no RequestedServiceMetaExtractor.");

        if (!authentication.isAuthenticated()) {
            return ACCESS_DENIED;
        }

        RequestedServiceMeta requestedServiceMeta = this.requestedServiceMetaExtractor.extractRequestedServiceMeta(object);

        for(Permission permission : extractPermissions(authentication, requestedServiceMeta))
        {
            ServicePermission servicePermission = null;
            try {
                servicePermission = gson.fromJson(permission.getPermission(), ServicePermission.class);
            } catch (JsonSyntaxException e) {
                log.error("권한정보를 읽는 중 오류가 발생했습니다. Permission 이 JSON 형식인지 확인하세요.", e);
                continue;
            }

            for (ServicePermissionByOperations servicePermissionByOperations : servicePermission.getPermissions()) {
                if(matchServicePermission(servicePermissionByOperations, requestedServiceMeta)){
                    if(servicePermission.getStrategy().equals("allow")) {
                        return ACCESS_GRANTED;
                    }else if(servicePermission.getStrategy().equals("deny")){
                        return ACCESS_DENIED;
                    }
                }else{
                    if(servicePermission.getStrategy().equals("allow")) {
                        return ACCESS_DENIED;
                    }else if(servicePermission.getStrategy().equals("deny")){
                        return ACCESS_GRANTED;
                    }
                }
            }
        }


        return ACCESS_DENIED;
    }

    private Set<Permission> extractPermissions(Authentication authentication, RequestedServiceMeta requestedServiceMeta) {
        Set<Permission> permissions = new HashSet<>();

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            Role role = roleProvider.getRole(authority.getAuthority());
            if (role == null)
                continue;

            Optional.ofNullable(role.getPermissions()).filter(perms -> perms.stream().anyMatch(
                    perm -> perm.getSecuredObject().getSecuredObjectType() == SecuredObjectType.SERVICE
                            && perm.getSecuredObject().getSecuredObjectId().equals(requestedServiceMeta.getServiceName())
            )).ifPresent(permissions::addAll);
        }
        return permissions;
    }

    private boolean matchServicePermission(ServicePermissionByOperations servicePermissionByOperations, RequestedServiceMeta requestedServiceMeta) {
        if (!doesPermissionHaveRequestedOperation(servicePermissionByOperations,requestedServiceMeta)) {
            return false;
        }

        for (Map<String, String> param : servicePermissionByOperations.getParams()) {

            /*if (evaluate(param, requestedServiceMetaParams)) {
                return
            }
            param.forEach((key, value) -> {
                        if (!matchPermission(requestedServiceMetaParams.get(key), value)) {

                        }
                    }

            );*/
        }
        return false;
    }

    private boolean matchPermission(String s, String value) {
        return false;
    }

    private boolean doesPermissionHaveRequestedOperation( ServicePermissionByOperations op,RequestedServiceMeta requestedServiceMeta) {
        return Arrays.stream(op.getOperations().split(",")).anyMatch(s -> s.contains(
                requestedServiceMeta.getOperation()
        ));
    }
}
